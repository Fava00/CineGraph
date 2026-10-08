package com.martonegyed.data.repository.transfer

import com.martonegyed.data.local.transfer.CsvImportService
import com.martonegyed.data.local.transfer.DataSyncManager

import com.martonegyed.data.database.CineGraphDatabase
import com.martonegyed.domain.model.StagedMovie
import com.martonegyed.domain.model.MovieImportKey
import com.martonegyed.domain.repository.ImportRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class LocalImportRepository(
    private val database: CineGraphDatabase,
    private val csvService: CsvImportService,
    private val manager: DataSyncManager,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : ImportRepository {
    override val phase = manager.phase.asStateFlow()
    override val importedCount = manager.importedCount.asStateFlow()
    override val importedTotal = manager.importedTotal.asStateFlow()
    override val enrichedCount = manager.enrichedCount.asStateFlow()
    override val enrichedTotal = manager.enrichedTotal.asStateFlow()
    override val lastMessage = manager.lastMessage.asStateFlow()
    override val hasPendingEnrichment = manager.hasPendingEnrichment.asStateFlow()
    override val resumePromptShown = manager.resumePromptShown.asStateFlow()

    override suspend fun parseCsv(content: String, platform: String, type: String) =
        withContext(dispatcher) { csvService.parseCsv(content, platform, type) }

    override suspend fun parseCsvWithReport(content: String, platform: String, type: String) =
        withContext(dispatcher) { csvService.parseCsvWithReport(content, platform, type) }

    override suspend fun inspectCsv(content: String, platform: String, fileName: String, selectedType: String?) =
        withContext(dispatcher) { csvService.inspectCsv(content, platform, fileName, selectedType) }

    override suspend fun getMovieKeys(): List<MovieImportKey> = withContext(dispatcher) {
        database.movieEntityQueries.getAllMovieKeys().executeAsList().map {
            MovieImportKey(it.name, it.year.toInt(), it.letterboxdUri, it.imdbId)
        }
    }

    override suspend fun containsMovie(key: MovieImportKey): Boolean = withContext(dispatcher) {
        database.movieEntityQueries.getMovieByUniqueData(
            uri = key.letterboxdUri, imdb = key.imdbId, name = key.name, year = key.year.toLong()
        ).executeAsOneOrNull() != null
    }

    override fun startImportAndEnrich(stagedMovies: List<StagedMovie>) =
        manager.startImportAndEnrich(stagedMovies)

    override fun retryUnmatchedMovies() = manager.retryUnmatchedMovies()

    override fun refreshPendingEnrichment(force: Boolean) = manager.refreshPendingEnrichment(force)

    override fun markResumePromptShown() {
        manager.resumePromptShown.value = true
    }

    override fun cancelAll() = manager.cancelAll()
}
