package com.martonegyed.presentation.screens.import

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.martonegyed.core.AppLogger
import com.martonegyed.data.local.DataSyncManager
import com.martonegyed.data.local.CsvImportService
import com.martonegyed.data.remote.TmdbApiService
import com.martonegyed.data.database.CineGraphDatabase
import com.martonegyed.data.local.export.BackupExportService
import com.martonegyed.data.local.export.ImdbExportService
import com.martonegyed.data.local.export.LetterboxdExportService
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SyncState {
    object Idle : SyncState()
    data class Loading(val message: String) : SyncState()
    data class Success(val message: String) : SyncState()
    data class Error(val error: String) : SyncState()
}

sealed interface ExportPayload {
    data class SingleFile(
        val fileName: String,
        val mimeType: String,
        val bytes: ByteArray
    ) : ExportPayload {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as SingleFile

            if (fileName != other.fileName) return false
            if (mimeType != other.mimeType) return false
            if (!bytes.contentEquals(other.bytes)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = fileName.hashCode()
            result = 31 * result + mimeType.hashCode()
            result = 31 * result + bytes.contentHashCode()
            return result
        }
    }

    data class MultiFile(
        val files: List<ExportFile>
    ) : ExportPayload
}

data class ExportFile(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ExportFile

        if (fileName != other.fileName) return false
        if (mimeType != other.mimeType) return false
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}

class ImportScreenModel(
    private val csvService: CsvImportService,
    private val tmdbService: TmdbApiService,
    private val database: CineGraphDatabase,
    private val dataSyncManager: DataSyncManager,
    private val backupExportService: BackupExportService,
    private val letterboxdExportService: LetterboxdExportService,
    private val imdbExportService: ImdbExportService,
) : ScreenModel {

    private data class SourcePayload(
        val platform: String,
        val type: String,
        val items: List<Map<String, Any>>
    )

    private val _exportPayload = MutableSharedFlow<ExportPayload>(extraBufferCapacity = 1)
    val exportPayload = _exportPayload.asSharedFlow()

    private val _state = MutableStateFlow<SyncState>(SyncState.Idle)
    val state: StateFlow<SyncState> = _state.asStateFlow()

    private val _newMoviesCount = MutableStateFlow(0)
    val newMoviesCount = _newMoviesCount.asStateFlow()

    private val stagedPayloads = mutableMapOf<String, SourcePayload>()
    private val stagedMovies = mutableMapOf<String, MutableMap<String, Any>>()
    private val _stagedCount = MutableStateFlow(0)
    val stagedCount = _stagedCount.asStateFlow()
    private val _stagedSources = MutableStateFlow<Set<String>>(emptySet())
    val stagedSources: StateFlow<Set<String>> = _stagedSources.asStateFlow()

    private fun sourceKey(platform: String, type: String): String {
        return "${platform.lowercase()}:${type.lowercase()}"
    }


    fun removeStagedSource(platform: String, type: String) {
        screenModelScope.launch {
            stagedPayloads.remove(sourceKey(platform, type))
            rebuildStagedState()
            _state.value = SyncState.Idle
        }
    }

    fun clearStaged() {
        stagedPayloads.clear()
        stagedMovies.clear()
        _stagedSources.value = emptySet()
        _stagedCount.value = 0
        _newMoviesCount.value = 0
    }

    fun stageMultipleLetterboxdFiles(files: List<PlatformFile>) {
        screenModelScope.launch {
            try {
                _state.value = SyncState.Loading("Analyzing ${files.size} files...")

                files.forEach { file ->
                    val fileName = file.name.lowercase()
                    if (fileName == "comments.csv" || fileName == "profile.csv" || !fileName.endsWith(".csv")) {
                        return@forEach
                    }

                    _state.value = SyncState.Loading("Reading $fileName...")

                    val type = when (fileName) {
                        "diary.csv" -> "Diary"
                        "ratings.csv" -> "Ratings"
                        "watched.csv" -> "Watched"
                        "reviews.csv" -> "Reviews"
                        "watchlist.csv" -> "Watchlist"
                        else -> "Lists"
                    }


                    val content = file.readBytes().decodeToString()
                    val parsedData = csvService.parseCsv(content, "Letterboxd", type)


                    val payloadKey = sourceKey("Letterboxd", type)
                    stagedPayloads[sourceKey("Letterboxd", type)] = SourcePayload(
                        platform = "Letterboxd",
                        type = type,
                        items = parsedData
                    )
                }
                rebuildStagedState()
                _state.value = SyncState.Idle
            } catch (e: IllegalArgumentException) {
                AppLogger.exception(
                    tag = "ImportScreenModel",
                    throwable = e,
                    message = "stageMultipleLetterboxdFiles invalid input, ${e.message}"
                )
                _state.value = SyncState.Error(e.message ?: "Invalid CSV format.")
            } catch (e: Exception) {
                AppLogger.exception(
                    tag = "ImportScreenModel",
                    throwable = e,
                    message = "stageMultipleLetterboxdFiles failed, ${e.message}"
                )
                _state.value = SyncState.Error("An error occurred: ${e.message}")
            }
        }
    }

    fun stageSingleCsv(file: PlatformFile, type: String, platform: String) {
        screenModelScope.launch {
            try {
                _state.value = SyncState.Loading("Parsing $platform $type...")

                val content = file.readBytes().decodeToString()

                val parsedData = csvService.parseCsv(content, platform, type)

                val payloadKey = sourceKey(platform, type)
                stagedPayloads[sourceKey(platform, type)] = SourcePayload(
                    platform = platform,
                    type = type,
                    items = parsedData
                )

                rebuildStagedState()
                _state.value = SyncState.Idle
            } catch (e: IllegalArgumentException) {
                AppLogger.exception(
                    tag = "ImportScreenModel",
                    throwable = e,
                    message = "stageSingleCsv invalid csv file=${file.name} platform=$platform type=$type"
                )
                _state.value = SyncState.Error(e.message ?: "Invalid CSV format.")
            } catch (e: Exception) {
                AppLogger.exception(
                    tag = "ImportScreenModel",
                    throwable = e,
                    message = "stageSingleCsv failed file=${file.name} platform=$platform type=$type"
                )
                _state.value = SyncState.Error("Failed to parse CSV: ${e.message}")
            }
        }
    }

    private fun recomputeNewMoviesCount() {
        val existing = database.movieEntityQueries
            .getAllMovieKeys()
            .executeAsList()

        val existingUris = existing.mapNotNull { it.letterboxdUri }.toHashSet()
        val existingImdbs = existing.mapNotNull { it.imdbId }.toHashSet()
        val existingNameYear = existing.map { it.name.lowercase() to it.year.toInt() }.toHashSet()

        var newCount = 0

        for (staged in stagedMovies.values) {
            val name = staged["name"]?.toString()?.trim() ?: "Unknown"
            val yearInt = staged["year"] as? Int ?: 0
            val uri = staged["letterboxdUri"]?.toString()
            val imdb = staged["imdbId"]?.toString()

            val isExisting = when {
                !uri.isNullOrBlank() && existingUris.contains(uri) -> true
                !imdb.isNullOrBlank() && existingImdbs.contains(imdb) -> true
                yearInt > 0 && existingNameYear.contains(name.lowercase() to yearInt) -> true
                else -> false
            }

            if (!isExisting) newCount++
        }

        _newMoviesCount.value = newCount
    }


    private fun mergeIntoStaged(parsedData: List<Map<String, Any>>, type: String) {
        val safeType = type.lowercase()
        val isWatchedFile = safeType in listOf("diary", "watched", "ratings", "reviews")
        val isWatchlistFile = safeType == "watchlist"

        for (movieData in parsedData) {
            val name = movieData["name"]?.toString()?.trim() ?: "Unknown"
            val yearStr = movieData["year"]?.toString() ?: "0"
            val yearInt = yearStr.toIntOrNull() ?: 0
            var uri = movieData["letterboxdUri"]?.toString()
            val imdb = movieData["imdbId"]?.toString()

            if (uri != null && uri.contains("/film/")) {
                val slug = uri.substringAfter("/film/").substringBefore("/")
                uri = "https://letterboxd.com/film/$slug/"
            }

            val key = "${name.lowercase()}_$yearInt"

            if (stagedMovies.containsKey(key)) {
                val existing = stagedMovies[key]!!

                if (isWatchedFile) existing["isWatched"] = true
                if (isWatchlistFile) existing["inWatchlist"] = true

                if (existing["letterboxdUri"] == null && uri != null) {
                    existing["letterboxdUri"] = uri
                }
                if (existing["imdbId"] == null && imdb != null) {
                    existing["imdbId"] = imdb
                }

                val logs = (existing["logs"] as? MutableList<Map<String, Any>>) ?: mutableListOf()
                logs.add(movieData)
                existing["logs"] = logs
            } else {
                val newMap = movieData.toMutableMap()
                if (uri != null) {
                    newMap["letterboxdUri"] = uri
                }
                if (imdb != null) {
                    newMap["imdbId"] = imdb
                }
                newMap["year"] = yearInt
                newMap["isWatched"] = isWatchedFile
                newMap["inWatchlist"] = isWatchlistFile
                newMap["logs"] = mutableListOf(movieData)
                stagedMovies[key] = newMap
            }
        }
    }

    fun commitToDatabase() {
        screenModelScope.launch {
            val stagedSnapshot = stagedMovies.values.map { staged ->
                staged.toMap()
            }

            val actuallyNew = stagedSnapshot.count { staged ->
                val name = staged["name"]?.toString()?.trim() ?: "Unknown"
                val yearInt = staged["year"] as? Int ?: 0
                val uri = staged["letterboxdUri"]?.toString()
                val imdb = staged["imdbId"]?.toString()

                database.movieEntityQueries.getMovieByUniqueData(
                    uri = uri,
                    imdb = imdb,
                    name = name,
                    year = yearInt.toLong()
                ).executeAsOneOrNull() == null
            }

            _state.value = if (actuallyNew == 0) {
                SyncState.Loading("Updating logs and flags for existing movies...")
            } else {
                SyncState.Loading("Adding $actuallyNew new movies and updating existing ones...")
            }

            dataSyncManager.startImportAndEnrich(stagedMovies = stagedSnapshot)
            clearStaged()
            reset()
        }
    }

    fun restoreBackup(file: PlatformFile) {
        screenModelScope.launch {
            try {
                _state.value = SyncState.Loading("Restoring CineGraph backup...")
                val json = file.readBytes().decodeToString()
                backupExportService.restoreJsonBackup(json)
                _state.value = SyncState.Success("Backup restored successfully!")
            } catch (t: Throwable) {
                AppLogger.exception(
                    tag = "ImportScreenModel",
                    throwable = t,
                    message = "restoreBackup failed file=${file.name}"
                )
                _state.value = SyncState.Error("Restore failed: ${t.message}")
            }
        }
    }

    fun exportData(platform: String) {
        screenModelScope.launch {
            try {
                when (platform) {
                    "CineGraph" -> {
                        _state.value = SyncState.Loading("Creating JSON backup...")
                        val json = backupExportService.exportJsonBackup()

                        _exportPayload.emit(
                            ExportPayload.SingleFile(
                                fileName = "cinegraph-backup.json",
                                mimeType = "application/json",
                                bytes = json.encodeToByteArray()
                            )
                        )
                    }

                    "Letterboxd" -> {
                        _state.value = SyncState.Loading("Creating Letterboxd CSV files...")
                        val bundle = letterboxdExportService.export()

                        _exportPayload.emit(
                            ExportPayload.MultiFile(
                                files = listOf(
                                    ExportFile("diary.csv", "text/csv", bundle.diaryCsv.encodeToByteArray()),
                                    ExportFile("watched.csv", "text/csv", bundle.watchedCsv.encodeToByteArray()),
                                    ExportFile("watchlist.csv", "text/csv", bundle.watchlistCsv.encodeToByteArray()),
                                    ExportFile("ratings.csv", "text/csv", bundle.ratingsCsv.encodeToByteArray()),
                                    ExportFile("reviews.csv", "text/csv", bundle.reviewsCsv.encodeToByteArray())
                                )
                            )
                        )
                    }

                    "IMDb" -> {
                        _state.value = SyncState.Loading("Creating IMDb CSV files...")
                        val bundle = imdbExportService.export()

                        _exportPayload.emit(
                            ExportPayload.MultiFile(
                                files = listOf(
                                    ExportFile("ratings.csv", "text/csv", bundle.ratingsCsv.encodeToByteArray()),
                                    ExportFile("watchlist.csv", "text/csv", bundle.watchlistCsv.encodeToByteArray())
                                )
                            )
                        )
                    }

                    else -> {
                        _state.value = SyncState.Error("Unsupported export type: $platform")
                    }
                }
            } catch (t: Throwable) {
                AppLogger.exception(
                    tag = "ImportScreenModel",
                    throwable = t,
                    message = "exportData failed platform=$platform, ${t.message}"
                )
                _state.value = SyncState.Error("Export failed: ${t.message}")
            }
        }
    }

    fun onExportSaved(message: String) {
        _state.value = SyncState.Success(message)
    }

    fun onExportCancelled() {
        _state.value = SyncState.Idle
    }

    fun reset() {
        _state.value = SyncState.Idle
    }

    private fun rebuildStagedState() {
        stagedMovies.clear()

        stagedPayloads.values.forEach { payload ->
            mergeIntoStaged(parsedData = payload.items, type = payload.type)
        }

        _stagedSources.value = stagedPayloads.keys.toSet()
        _stagedCount.value = stagedMovies.size

        recomputeNewMoviesCount()
    }

}