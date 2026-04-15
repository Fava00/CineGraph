package com.martonegyed.data.local

import com.martonegyed.data.database.CineGraphDatabase
import com.martonegyed.data.remote.TmdbApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class DataSyncManager(
    private val database: CineGraphDatabase,
    private val api: TmdbApiService
) {
    enum class Phase { IDLE, IMPORTING, ENRICHING }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Default)

    val phase = MutableStateFlow(Phase.IDLE)
    val importedCount = MutableStateFlow(0)
    val importedTotal = MutableStateFlow(0)
    val enrichedCount = MutableStateFlow(0)
    val enrichedTotal = MutableStateFlow(0)
    val lastMessage = MutableStateFlow<String?>(null)

    val hasPendingEnrichment = MutableStateFlow(false)

    private var lastRefreshMillis: Long = 0L

    val resumePromptShown = MutableStateFlow(false)

    fun startImportAndEnrich(stagedMovies: List<Map<String, Any?>>) {

        if (phase.value != Phase.IDLE) return

        scope.launch {
            try {
                phase.value = Phase.IMPORTING
                importedTotal.value = stagedMovies.size
                importedCount.value = 0
                lastMessage.value = $$"Importing ${stagedMovies.size} movies..."

                database.movieEntityQueries.transaction {
                    for (staged in stagedMovies) {
                        importSingleMovie(staged)
                        importedCount.value += 1
                    }
                }
                phase.value = Phase.ENRICHING
                enrichedCount.value = 0
                lastMessage.value = "Enriching from TMDb..."
                enrichDatabaseWithTmdb()


                phase.value = Phase.IDLE
                lastMessage.value = "Import and enrichment finished"
                enrichedCount.value = 0
                refreshPendingEnrichment(force = true)
            } catch (e: Exception) {
                phase.value = Phase.IDLE
                lastMessage.value = "Error during import/enrich: ${e.message}"
            }
        }
    }

    private fun importSingleMovie(staged: Map<String, Any?>) {
        val name = staged["name"]?.toString() ?: "Unknown"
        val year = (staged["year"] as? Int)?.toLong() ?: 0L
        val uri = staged["letterboxdUri"]?.toString()
        val imdb = staged["imdbId"]?.toString()

        val explicitWatched = staged["isWatched"] == true
        val inWatchlist = if (staged["inWatchlist"] == true) 1L else 0L
        val logs = staged["logs"] as? List<Map<String, Any?>> ?: emptyList()

        val hasAnyLog = logs.any {
            (it["rating"] as? Double)?.let { r -> r > 0.0 } == true ||
                    !it["watchedDate"]?.toString().isNullOrEmpty() ||
                    !it["userReview"]?.toString().isNullOrEmpty()
        }

        val isWatchedFlag = if (explicitWatched || hasAnyLog) 1L else 0L

        val existingMovie = database.movieEntityQueries.getMovieByUniqueData(
            uri = uri, imdb = imdb, name = name, year = year
        ).executeAsOneOrNull()

        val movieId: Long = if (existingMovie != null) {
            val mergedWatched = if (existingMovie.isWatched == 1L || isWatchedFlag == 1L) 1L else 0L
            val mergedWatchlist = if (existingMovie.inWatchlist == 1L || inWatchlist == 1L) 1L else 0L

            database.movieEntityQueries.updateMovieFlags(
                isWatched = mergedWatched,
                inWatchlist = mergedWatchlist,
                id = existingMovie.id
            )
            existingMovie.id
        } else {
            database.movieEntityQueries.insertMovie(
                name = name,
                year = year,
                letterboxdUri = uri,
                imdbId = imdb,
                isWatched = isWatchedFlag,
                inWatchlist = inWatchlist,
                isCached = 0L,
                posterPath = null,
                backdropPath = null,
                overview = null,
                runtimeMinutes = null,
                tmdbId = null,
                tagline = null,
                originalTitle = null,
                originalLanguage = null,
                budget = null,
                revenue = null,
                genres = null,
                hungarianTitle = null,
                tmdbPopularity = null,
                tmdbVoteAverage = null,
                tmdbVoteCount = null,
                collectionName = null,
                trailerKey = null,
                mpaaRating = null,
                addedDate = null,
                studios = null,
                productionCountries = null,
                spokenLanguages = null,
                similarMovies = null,
                tmdbReviews = null
            )
            database.movieEntityQueries.getLastInsertId().executeAsOne()
        }

        val mergedLogsByDate = mutableMapOf<String, MutableMap<String, Any?>>()
        for (log in logs) {
            val date = log["watchedDate"]?.toString() ?: "unknown_date"
            if (mergedLogsByDate.containsKey(date)) {
                val existingLog = mergedLogsByDate[date]!!
                if (existingLog["rating"] == null && log["rating"] != null) existingLog["rating"] = log["rating"]!!
                if (existingLog["userReview"] == null && log["userReview"] != null) existingLog["userReview"] =
                    log["userReview"]!!
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
                val safeDate = if (watchedDate == "unknown_date") null else watchedDate

                val existingLog = database.movieEntityQueries.getLogByMovieAndDate(
                    movieId = movieId, date = safeDate
                ).executeAsOneOrNull()

                if (existingLog != null) {
                    database.movieEntityQueries.updateMovieLog(
                        rating = rating ?: existingLog.rating,
                        review = review ?: existingLog.review,
                        isRewatch = if (isRewatch == 1L || existingLog.isRewatch == 1L) 1L else 0L,
                        id = existingLog.id
                    )
                } else {
                    database.movieEntityQueries.insertMovieLog(
                        movieId = movieId,
                        watchedDate = safeDate,
                        rating = rating,
                        review = review,
                        isRewatch = isRewatch
                    )
                }
            }
        }
    }

    private suspend fun enrichDatabaseWithTmdb() {
        println("--- TMDB ENRICH START ---")
        enrichedCount.value = 0

        var totalEnrichedSoFar = 0
        var hasMoreToEnrich = true

        val allToEnrich = database.movieEntityQueries.countMoviesToEnrich().executeAsOne()
        enrichedTotal.value = allToEnrich.toInt()

        while (hasMoreToEnrich) {
            val moviesBatch = database.movieEntityQueries.getMoviesToEnrich().executeAsList()
            if (moviesBatch.isEmpty()) {
                hasMoreToEnrich = false
                break
            }

            val enrichmentResults = moviesBatch.map { entity ->
                scope.async {
                    try {
                        var tmdbIdToUse: Int? = null
                        if (!entity.imdbId.isNullOrEmpty()) {
                            val findResult = api.findByImdbId(entity.imdbId)
                            tmdbIdToUse = findResult?.movieResults?.firstOrNull()?.id
                        }
                        if (tmdbIdToUse == null) {
                            val searchResult = api.searchMovie(entity.name, entity.year.toInt())
                            tmdbIdToUse = searchResult?.results?.firstOrNull()?.id
                        }
                        if (tmdbIdToUse != null) {
                            val fullDetails = api.getMovieDetails(tmdbIdToUse)
                            Pair(entity, Pair(tmdbIdToUse.toString(), fullDetails))
                        } else {
                            Pair(entity, null)
                        }
                    } catch (e: Exception) {
                        println("TMDb error [${entity.name}]: ${e.message}")
                        Pair(entity, null)
                    }
                }
            }.awaitAll()

            database.movieEntityQueries.transaction {
                for ((entity, tmdbData) in enrichmentResults) {
                    if (tmdbData != null && tmdbData.second != null) {
                        val tmdbIdStr = tmdbData.first
                        val details = tmdbData.second!!

                        database.movieEntityQueries.updateMovieWithTmdb(
                            posterPath = details.posterPath,
                            backdropPath = details.backdropPath,
                            overview = details.overview,
                            runtimeMinutes = details.runtime?.toLong(),
                            tmdbId = tmdbIdStr,
                            tagline = details.tagline,
                            originalTitle = details.originalTitle,
                            originalLanguage = details.originalLanguage,
                            budget = details.budget,
                            revenue = details.revenue,
                            genres = details.genres.joinToString(", ") { it.name }.ifEmpty { null },
                            hungarianTitle = details.hungarianTitle,
                            tmdbPopularity = details.popularity,
                            tmdbVoteAverage = details.voteAverage,
                            tmdbVoteCount = details.voteCount?.toLong(),
                            collectionName = details.collection?.name,
                            trailerKey = details.trailerKey,
                            mpaaRating = details.mpaaRating,
                            studios = details.studios.joinToString(", ") { it.name }.ifEmpty { null },
                            productionCountries = details.productionCountries.joinToString(", ") { it.name }
                                .ifEmpty { null },
                            spokenLanguages = details.spokenLanguages.joinToString(", ") { it.englishName }
                                .ifEmpty { null },
                            similarMovies = details.similar?.results
                                ?.take(10)
                                ?.let { Json.encodeToString(it) },
                            tmdbReviews = details.reviews?.results
                                ?.take(5)
                                ?.map { "${it.author}: ${it.content}" }
                                ?.let { Json.encodeToString(it) },
                            id = entity.id
                        )

                        details.credits?.cast?.forEach { actor ->
                            database.movieEntityQueries.insertMoviePerson(
                                movieId = entity.id,
                                name = actor.name,
                                job = "Actor",
                                character = actor.character,
                                profilePath = actor.profilePath
                            )
                        }

                        details.credits?.crew?.forEach { crewMember ->
                            database.movieEntityQueries.insertMoviePerson(
                                movieId = entity.id,
                                name = crewMember.name,
                                job = crewMember.job,
                                character = null,
                                profilePath = crewMember.profilePath
                            )
                        }
                    } else {
                        database.movieEntityQueries.updateMovieWithTmdb(
                            posterPath = null,
                            backdropPath = null,
                            overview = null,
                            runtimeMinutes = null,
                            tmdbId = "-1",
                            tagline = entity.tagline,
                            originalTitle = entity.originalTitle,
                            originalLanguage = entity.originalLanguage,
                            budget = entity.budget,
                            revenue = entity.revenue,
                            genres = entity.genres,
                            hungarianTitle = null,
                            tmdbPopularity = null,
                            tmdbVoteAverage = null,
                            tmdbVoteCount = null,
                            collectionName = null,
                            trailerKey = null,
                            mpaaRating = null,
                            studios = null,
                            productionCountries = null,
                            spokenLanguages = null,
                            similarMovies = null,
                            tmdbReviews = null,
                            id = entity.id
                        )
                    }
                }
            }

            totalEnrichedSoFar += moviesBatch.size
            enrichedCount.value = totalEnrichedSoFar
            delay(500)
        }

        println("--- TMDB ENRICH DONE, total: $totalEnrichedSoFar ---")
    }

    @OptIn(ExperimentalTime::class)
    fun refreshPendingEnrichment(force: Boolean = false) {
        val now = Clock.System.now().toEpochMilliseconds()
        if (!force && now - lastRefreshMillis < 20_000L) return
        lastRefreshMillis = now
        val count = database.movieEntityQueries.countMoviesToEnrich().executeAsOne()
        hasPendingEnrichment.value = count > 0
    }

    fun cancelAll() {
        job.cancelChildren()
        phase.value = Phase.IDLE
        lastMessage.value = "Sync cancelled"
        scope.launch {
            refreshPendingEnrichment(force = true)
        }
    }
}

val coreModule = module {
    single { DataSyncManager(get(), get()) }
}