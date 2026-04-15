package com.martonegyed.presentation.screens.import

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.martonegyed.data.local.DataSyncManager
import com.martonegyed.data.local.CsvImportService
import com.martonegyed.data.remote.TmdbApiService
import com.martonegyed.data.database.CineGraphDatabase
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SyncState {
    object Idle : SyncState()
    data class Loading(val message: String) : SyncState()
    data class Success(val message: String) : SyncState()
    data class Error(val error: String) : SyncState()
}

class ImportScreenModel(
    private val csvService: CsvImportService,
    private val tmdbService: TmdbApiService,
    private val database: CineGraphDatabase,
    private val dataSyncManager: DataSyncManager
) : ScreenModel {
    private val _state = MutableStateFlow<SyncState>(SyncState.Idle)
    val state: StateFlow<SyncState> = _state.asStateFlow()

    private val stagedMovies = mutableMapOf<String, MutableMap<String, Any>>()

    private val _stagedCount = MutableStateFlow(0)
    val stagedCount = _stagedCount.asStateFlow()

    private val _newMoviesCount = MutableStateFlow(0)
    val newMoviesCount = _newMoviesCount.asStateFlow()

    fun stageMultipleLetterboxdFiles(files: List<PlatformFile>) {
        screenModelScope.launch {
            try {
                _state.value = SyncState.Loading("Analyzing ${files.size} files...")
                for (file in files) {
                    val fileName = file.name.lowercase()
                    if (fileName == "comments.csv" || fileName == "profile.csv" || !fileName.endsWith(".csv")) continue

                    _state.value = SyncState.Loading("Reading $fileName...")
                    val content = file.readBytes().decodeToString()
                    val type = when (fileName) {
                        "diary.csv" -> "diary"
                        "ratings.csv" -> "ratings"
                        "watched.csv" -> "watched"
                        "reviews.csv" -> "reviews"
                        "watchlist.csv" -> "watchlist"
                        else -> "lists"
                    }


                    val parsedData = csvService.parseCsv(content, "letterboxd", type)
                    mergeIntoStaged(parsedData, type)
                }

                _stagedCount.value = stagedMovies.size
                recomputeNewMoviesCount()
                _state.value = SyncState.Idle
            } catch (e: IllegalArgumentException) {
                _state.value = SyncState.Error(e.message ?: "Invalid CSV format.")
            } catch (e: Exception) {
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
                mergeIntoStaged(parsedData, type)

                _stagedCount.value = stagedMovies.size
                recomputeNewMoviesCount()
                _state.value = SyncState.Idle
            } catch (e: IllegalArgumentException) {
                _state.value = SyncState.Error(e.message ?: "Invalid CSV format.")
            } catch (e: Exception) {
                _state.value = SyncState.Error("Failed to parse CSV: ${e.message}")
            }
        }
    }

    private suspend fun recomputeNewMoviesCount() {
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


                if (existing["letterboxdUri"] == null && uri != null) existing["letterboxdUri"] = uri
                if (existing["imdbId"] == null && imdb != null) existing["imdbId"] = imdb

                val logs = existing["logs"] as? MutableList<Map<String, Any>> ?: mutableListOf()
                logs.add(movieData)
                existing["logs"] = logs

                println("🔄 MEMÓRIA ÖSSZEFÉSÜLÉS: [$key] már a memóriában van! Naplók száma: ${logs.size}")
            } else {
                val newMap = movieData.toMutableMap()
                newMap["letterboxdUri"] = uri as Any
                newMap["year"] = yearInt
                newMap["isWatched"] = isWatchedFile
                newMap["inWatchlist"] = isWatchlistFile
                newMap["logs"] = mutableListOf(movieData)
                stagedMovies[key] = newMap

                println("🆕 ÚJ FILM A MEMÓRIÁBAN: [$key]")
            }
        }
    }

    fun commitToDatabase() {
        screenModelScope.launch {
            val stagedSnapshot = stagedMovies.values.map { it.toMap() }

            val actuallyNew = stagedSnapshot.count { staged ->
                val name = staged["name"]?.toString()?.trim() ?: "Unknown"
                val yearInt = staged["year"] as? Int ?: 0
                val uri = staged["letterboxdUri"]?.toString()
                val imdb = staged["imdbId"]?.toString()

                val existing = database.movieEntityQueries.getMovieByUniqueData(
                    uri = uri,
                    imdb = imdb,
                    name = name,
                    year = yearInt.toLong()
                ).executeAsOneOrNull()

                if (existing == null) {
                    println("⚠ NEW DETECTED: [$name ($yearInt)] uri=$uri imdb=$imdb")
                } else {
                    println("✅ EXISTING DETECTED: [$name ($yearInt)] id=${existing.id}")
                }

                existing == null
            }

            if (actuallyNew == 0) {
                _state.value = SyncState.Loading("Updating logs and flags for existing movies...")
            } else {
                _state.value = SyncState.Loading(
                    "Adding $actuallyNew new movies and updating existing ones..."
                )
            }
            clearStaged()
            reset()

            dataSyncManager.startImportAndEnrich(stagedMovies = stagedSnapshot)
        }
    }

    fun clearStaged() {
        stagedMovies.clear()
        _stagedCount.value = 0
        _newMoviesCount.value = 0
    }

    fun restoreBackup(file: PlatformFile) {
        screenModelScope.launch {
            _state.value = SyncState.Loading("Restoring CineGraph backup...")
            delay(1500)
            _state.value = SyncState.Success("Backup restored successfully!")
        }
    }

    fun exportData(platform: String) {
        screenModelScope.launch {
            _state.value = SyncState.Loading("Generating $platform export...")
            delay(2000)
            _state.value = SyncState.Success("Successfully exported to $platform format!")
        }
    }

    fun reset() {
        _state.value = SyncState.Idle
    }
}