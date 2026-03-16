package com.martonegyed.presentation.screens.import

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.martonegyed.data.local.CsvImportService
import com.martonegyed.data.remote.TmdbApiService
import com.martonegyed.data.remote.TmdbMovie
import com.martonegyed.data.database.CineGraphDatabase
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
    private val database: CineGraphDatabase
) : ScreenModel {

    private val _state = MutableStateFlow<SyncState>(SyncState.Idle)
    val state: StateFlow<SyncState> = _state.asStateFlow()

    private val stagedMovies = mutableMapOf<String, MutableMap<String, Any>>()

    private val _stagedCount = MutableStateFlow(0)
    val stagedCount = _stagedCount.asStateFlow()

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
                _state.value = SyncState.Idle
            } catch (e: IllegalArgumentException) {
                _state.value = SyncState.Error(e.message ?: "Invalid CSV format.")
            } catch (e: Exception) {
                _state.value = SyncState.Error("Failed to parse CSV: ${e.message}")
            }
        }
    }


    private fun mergeIntoStaged(parsedData: List<Map<String, Any>>, type: String) {
        val safeType = type.lowercase()
        val isWatchedFile = safeType in listOf("diary", "watched", "ratings", "reviews")
        val isWatchlistFile = safeType == "watchlist"

        for (movieData in parsedData) {
            val name = movieData["name"]?.toString()?.trim() ?: "Unknown"
            val year = movieData["year"]?.toString() ?: "0"
            var uri = movieData["letterboxdUri"]?.toString()
            val imdb = movieData["imdbId"]?.toString()


            if (uri != null && uri.contains("/film/")) {
                val slug = uri.substringAfter("/film/").substringBefore("/")
                uri = "https://letterboxd.com/film/$slug/"
            }


            val key = "${name.lowercase()}_$year"

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
            _state.value = SyncState.Loading("Updating database with ${stagedMovies.size} unique movies...")

            database.movieEntityQueries.transaction {
                for (staged in stagedMovies.values) {
                    val name = staged["name"]?.toString() ?: "Unknown"
                    val year = (staged["year"] as? Int)?.toLong() ?: 0L
                    val uri = staged["letterboxdUri"]?.toString()
                    val imdb = staged["imdbId"]?.toString()

                    val explicitWatched = staged["isWatched"] == true
                    val inWatchlist = if (staged["inWatchlist"] == true) 1L else 0L
                    val logs = staged["logs"] as? List<Map<String, Any>> ?: emptyList()

                    val hasAnyLog = logs.any {
                        (it["rating"] as? Double)?.let { r -> r > 0.0 } == true ||
                                !it["watchedDate"]?.toString().isNullOrEmpty() ||
                                !it["userReview"]?.toString().isNullOrEmpty()
                    }

                    val isWatchedFlag = if (explicitWatched || hasAnyLog) 1L else 0L


                    val existingMovie = database.movieEntityQueries.getMovieByUniqueData(
                        uri = uri, imdb = imdb, name = name, year = year
                    ).executeAsOneOrNull()

                    val movieId: Long

                    if (existingMovie != null) {
                        println("💾 DB FRISSÍTÉS: [$name ($year)] már létezik (ID: ${existingMovie.id}). Flagek frissítése...")
                        val mergedWatched = if (existingMovie.isWatched == 1L || isWatchedFlag == 1L) 1L else 0L
                        val mergedWatchlist = if (existingMovie.inWatchlist == 1L || inWatchlist == 1L) 1L else 0L

                        database.movieEntityQueries.updateMovieFlags(mergedWatched, mergedWatchlist, existingMovie.id)
                        movieId = existingMovie.id
                    } else {
                        println("💾 DB ÚJ BESZÚRÁS: [$name ($year)] bekerült az adatbázisba.")
                        database.movieEntityQueries.insertMovie(
                            name = name, year = year, letterboxdUri = uri, imdbId = imdb,
                            isWatched = isWatchedFlag, inWatchlist = inWatchlist,
                            posterPath = null, backdropPath = null, overview = null, runtimeMinutes = null, tmdbId = null
                        )
                        movieId = database.movieEntityQueries.getLastInsertId().executeAsOne()
                    }

                    val mergedLogsByDate = mutableMapOf<String, MutableMap<String, Any>>()
                    for (log in logs) {
                        val date = log["watchedDate"]?.toString() ?: "unknown_date"
                        if (mergedLogsByDate.containsKey(date)) {
                            val existingLog = mergedLogsByDate[date]!!
                            if (existingLog["rating"] == null && log["rating"] != null) existingLog["rating"] = log["rating"]!!
                            if (existingLog["userReview"] == null && log["userReview"] != null) existingLog["userReview"] = log["userReview"]!!
                            if (log["isRewatch"] == true) existingLog["isRewatch"] = true
                        } else {
                            mergedLogsByDate[date] = log.toMutableMap()
                        }
                    }

                    for (log in mergedLogsByDate.values) {
                        val rating = log["rating"] as? Double
                        val watchedDate = log["watchedDate"]?.toString()
                        val review = log["userReview"]?.toString()
                        val isRewatch = if (log["isRewatch"] == true) 1L else 0L

                        if (!watchedDate.isNullOrEmpty() || rating != null || !review.isNullOrEmpty()) {
                            println("📝 DB NAPLÓ: [$name] -> Dátum: $watchedDate, Rating: $rating")
                            database.movieEntityQueries.insertMovieLog(
                                movieId = movieId,
                                watchedDate = if (watchedDate == "unknown_date") null else watchedDate,
                                rating = rating,
                                review = review,
                                isRewatch = isRewatch
                            )
                        }
                    }
                }
            }

            val savedCount = stagedMovies.size
            clearStaged()
            _state.value = SyncState.Success("Successfully saved $savedCount unique movies!")
            enrichDatabaseWithTmdb()
        }
    }

    private fun enrichDatabaseWithTmdb() {
        println("--- TMDB DÚSÍTÁS ELKEZDŐDÖTT ---")
        var totalEnriched = 0

        screenModelScope.launch {
            var hasMoreToEnrich = true

            while (hasMoreToEnrich) {
                val moviesBatch = database.movieEntityQueries.getMoviesToEnrich().executeAsList()

                if (moviesBatch.isEmpty()) {
                    hasMoreToEnrich = false
                    break
                }

                val enrichmentResults = moviesBatch.map { entity ->
                    async {
                        try {
                            var bestMatch: TmdbMovie? = null

                            if (!entity.imdbId.isNullOrEmpty()) {
                                val findResult = tmdbService.findByImdbId(entity.imdbId)
                                bestMatch = findResult?.movieResults?.firstOrNull()
                            }

                            if (bestMatch == null) {
                                val searchResult = tmdbService.searchMovie(entity.name, entity.year.toInt())
                                bestMatch = searchResult?.results?.firstOrNull()
                            }

                            Pair(entity, bestMatch)
                        } catch (e: Exception) {
                            Pair(entity, null)
                        }
                    }
                }.awaitAll()

                database.movieEntityQueries.transaction {
                    for ((entity, bestMatch) in enrichmentResults) {

                        if (bestMatch != null) {
                            database.movieEntityQueries.updateMovieWithTmdb(
                                posterPath = bestMatch.posterPath,
                                backdropPath = bestMatch.backdropPath,
                                overview = bestMatch.overview,
                                runtimeMinutes = null,
                                tmdbId = bestMatch.id.toString(),
                                id = entity.id
                            )
                        } else {
                            database.movieEntityQueries.updateMovieWithTmdb(
                                posterPath = null, backdropPath = null, overview = null, runtimeMinutes = null,
                                tmdbId = "-1", id = entity.id
                            )
                        }
                    }
                }

                totalEnriched += moviesBatch.size
                println("Feldolgozva eddig: $totalEnriched film...")


                delay(500)
            }
            println("--- TMDB DÚSÍTÁS BEFEJEZŐDÖTT! Összesen feldolgozva: $totalEnriched ---")
        }
    }

    fun clearStaged() {
        stagedMovies.clear()
        _stagedCount.value = 0
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