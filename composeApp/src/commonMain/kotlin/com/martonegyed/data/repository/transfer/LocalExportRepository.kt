package com.martonegyed.data.repository.transfer

import com.martonegyed.data.database.CineGraphDatabase
import com.martonegyed.data.local.transfer.export.BackupExportService
import com.martonegyed.data.local.transfer.export.ImdbExportService
import com.martonegyed.data.local.transfer.export.LetterboxdExportService
import com.martonegyed.domain.repository.ExportRepository
import com.martonegyed.domain.repository.BackupSource
import com.martonegyed.domain.repository.BackupOutput
import com.martonegyed.domain.model.BackupRestoreProgress
import com.martonegyed.data.remote.TmdbApiService
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class LocalExportRepository(
    private val database: CineGraphDatabase,
    private val backup: BackupExportService,
    private val letterboxd: LetterboxdExportService,
    private val imdb: ImdbExportService,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val api: TmdbApiService? = null
) : ExportRepository {
    override suspend fun exportJsonBackup() = withContext(dispatcher) {
        database.transactionWithResult { backup.exportJsonBackup() }
    }

    override suspend fun exportJsonBackup(output: BackupOutput) = withContext(dispatcher) {
        try {
            val context = currentCoroutineContext()
            database.transaction { backup.exportJsonBackup(output) { context.ensureActive() } }
        } finally {
            output.close()
        }
    }

    override suspend fun restoreJsonBackup(json: String) = withContext(dispatcher) {
        backup.restoreJsonBackup(json)
    }

    override suspend fun restoreJsonBackup(source: BackupSource, onProgress: (BackupRestoreProgress) -> Unit) =
        withContext(dispatcher) {
            val context = currentCoroutineContext()
            backup.restoreJsonBackup(source, { context.ensureActive() }, onProgress)
        }

    override suspend fun exportLetterboxd() = withContext(dispatcher) {
        database.transactionWithResult { letterboxd.export() }
    }

    override suspend fun exportImdb(onLookupProgress: (Int, Int) -> Unit) = withContext(dispatcher) {
        val duplicateMatch = database.movieEntityQueries.getImdbExportIdentities().executeAsList()
            .groupBy { it.tmdbId }.values.firstOrNull { it.size > 1 }
        check(duplicateMatch == null) {
            "Two library films share TMDB ID ${duplicateMatch!!.first().tmdbId}: " +
                    duplicateMatch.joinToString { "'${it.name}' (local ID ${it.id})" } +
                    ". Review their TMDB matches before IMDb export; no records were changed."
        }
        val missing = database.movieEntityQueries.getMoviesMissingExportImdbId().executeAsList()
        onLookupProgress(0, missing.size)
        if (missing.isNotEmpty()) {
            val tmdbApi = checkNotNull(api) { "TMDB is unavailable for IMDb ID lookup" }
            val unmatched = missing.filter { it.tmdbId?.toIntOrNull()?.let { id -> id > 0 } != true }
            check(unmatched.isEmpty()) {
                "IMDb IDs cannot be found until these films have a TMDB match: " +
                        unmatched.take(3).joinToString { "${it.name} (local ID ${it.id})" } +
                        if (unmatched.size > 3) " and ${unmatched.size - 3} more" else ""
            }

            val usedIds = database.movieEntityQueries.getStoredImdbIds().executeAsList()
                .mapNotNull { row ->
                    row.imdbId.trim().takeIf(String::isNotBlank)?.lowercase()
                        ?.let { it to (row.id to row.name) }
                }
                .toMap().toMutableMap()
            var completed = 0
            for (batch in missing.chunked(4)) {
                val resolved = coroutineScope {
                    batch.map { movie ->
                        async { movie to tmdbApi.getMovieImdbId(movie.tmdbId!!.toInt()) }
                    }.awaitAll()
                }
                val unresolved = resolved.filter { it.second.isNullOrBlank() }
                check(unresolved.isEmpty()) {
                    "Could not retrieve IMDb IDs for " +
                            unresolved.take(3).joinToString { "${it.first.name} (TMDB ${it.first.tmdbId})" } +
                            ". Check the TMDB matches and internet connection, then retry."
                }
                resolved.forEach { (movie, imdbId) ->
                    val key = imdbId!!.lowercase()
                    val owner = usedIds[key]
                    check(owner == null || owner.first == movie.id) {
                        "IMDb ID $imdbId belongs to both '${owner!!.second}' (local ID ${owner.first}) " +
                                "and '${movie.name}' (local ID ${movie.id}). Review these duplicate or mismatched films before exporting."
                    }
                    usedIds[key] = movie.id to movie.name
                }
                database.transaction {
                    resolved.forEach { (movie, imdbId) ->
                        database.movieEntityQueries.updateMissingImdbId(imdbId!!, movie.id, movie.tmdbId)
                    }
                }
                completed += batch.size
                onLookupProgress(completed, missing.size)
                if (completed < missing.size) delay(150.milliseconds)
            }
        }
        check(database.movieEntityQueries.getMoviesMissingExportImdbId().executeAsList().isEmpty()) {
            "Some films still have no IMDb ID. Check their TMDB matches and retry."
        }
        database.transactionWithResult { imdb.export() }
    }
}
