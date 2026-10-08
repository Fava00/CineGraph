package com.martonegyed.data.local.transfer

import com.martonegyed.data.local.catalog.MovieMetadataWriter
import com.martonegyed.data.local.catalog.selectTmdbSearchMatch
import com.martonegyed.data.local.catalog.searchTmdbMatchCandidates
import com.martonegyed.data.local.catalog.TmdbSuggestionSearchCache

import com.martonegyed.domain.model.StagedMovie
import com.martonegyed.domain.model.MovieLogRules

import com.martonegyed.core.AppLogger
import com.martonegyed.data.database.CineGraphDatabase
import com.martonegyed.data.remote.TmdbApiService
import com.martonegyed.domain.repository.ImportPhase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.dsl.module
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class DataSyncManager(
    private val database: CineGraphDatabase,
    private val api: TmdbApiService,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val metadataWriter: MovieMetadataWriter = MovieMetadataWriter(database),
    private val onUnmatched: () -> Unit = {}
) {
    private val job = SupervisorJob()
    private val suggestionSearchCache = TmdbSuggestionSearchCache(database)
    private val scope = CoroutineScope(job + dispatcher)

    val phase = MutableStateFlow(ImportPhase.IDLE)
    val importedCount = MutableStateFlow(0)
    val importedTotal = MutableStateFlow(0)
    val enrichedCount = MutableStateFlow(0)
    val enrichedTotal = MutableStateFlow(0)
    val lastMessage = MutableStateFlow<String?>(null)

    val hasPendingEnrichment = MutableStateFlow(false)

    private var lastRefreshMillis: Long = 0L

    val resumePromptShown = MutableStateFlow(false)

    fun startImportAndEnrich(stagedMovies: List<StagedMovie>): Boolean {

        if (!phase.compareAndSet(ImportPhase.IDLE, ImportPhase.IMPORTING)) return false

        scope.launch {
            try {
                importedTotal.value = stagedMovies.size
                importedCount.value = 0
                lastMessage.value = $$"Importing ${stagedMovies.size} movies..."


                database.movieEntityQueries.transaction {

                    for (staged in stagedMovies) {
                        importSingleMovie(staged)
                        importedCount.value += 1
                    }
                }
                phase.value = ImportPhase.ENRICHING
                enrichedCount.value = 0
                lastMessage.value = "Enriching from TMDb..."
                enrichDatabaseWithTmdb()


                phase.value = ImportPhase.IDLE
                enrichedCount.value = 0
                refreshPendingEnrichment(force = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.exception(
                    tag = "DataSyncManager",
                    throwable = e,
                    message = "startImportAndEnrich failed"
                )
                phase.value = ImportPhase.IDLE
                lastMessage.value = "Error during import/enrich: ${e.message}"
            }
        }
        return true
    }

    fun retryUnmatchedMovies(): Boolean {
        if (!phase.compareAndSet(ImportPhase.IDLE, ImportPhase.ENRICHING)) return false
        importedCount.value = 0
        importedTotal.value = 0
        scope.launch {
            try {
                lastMessage.value = "Retrying previously unmatched TMDb movies..."
                enrichDatabaseWithTmdb(includeUnmatched = true)
                phase.value = ImportPhase.IDLE
                refreshPendingEnrichment(force = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.exception("DataSyncManager", e, "retryUnmatchedMovies failed")
                phase.value = ImportPhase.IDLE
                lastMessage.value = "Unmatched retry failed: ${e.message}"
            }
        }
        return true
    }

    private fun importSingleMovie(staged: StagedMovie) {
        val name = staged.name
        val year = staged.year.toLong()
        val uri = staged.letterboxdUri
        val imdb = staged.imdbId
        val addedDate = staged.addedDate
        val logs = staged.logs
        val inWatchlist = if (staged.inWatchlist) 1L else 0L
        val hasAnyLog = logs.any {
            it.source.marksWatched || it.watchedDate != null || it.loggedDate != null || it.rating != null || it.review != null
        }
        val isWatchedFlag = if (staged.isWatched || hasAnyLog) 1L else 0L

        val existingMovie = database.movieEntityQueries.getMovieByUniqueData(
            uri = uri,
            imdb = imdb,
            name = name,
            year = year
        ).executeAsOneOrNull()

        val movieId: Long = if (existingMovie != null) {
            val mergedWatched = if (existingMovie.isWatched == 1L || isWatchedFlag == 1L) 1L else 0L
            val mergedWatchlist = if (existingMovie.inWatchlist == 1L || inWatchlist == 1L) 1L else 0L

            database.movieEntityQueries.updateMovieFlags(
                isWatched = mergedWatched,
                inWatchlist = mergedWatchlist,
                id = existingMovie.id
            )

            if (!addedDate.isNullOrBlank() && existingMovie.addedDate == null) {
                database.movieEntityQueries.updateMovieAddedDate(
                    addedDate = addedDate,
                    id = existingMovie.id
                )
            }

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
                addedDate = addedDate,
                studios = null,
                productionCountries = null,
                spokenLanguages = null,
                similarMovies = null,
                tmdbReviews = null
            )
            database.movieEntityQueries.getLastInsertId().executeAsOne()
        }

        reconcileImportedLogs(database, movieId)
        for (rawLog in logs.sortedBy { when (it.source) {
            com.martonegyed.domain.model.CsvImportType.WATCHED -> 2
            com.martonegyed.domain.model.CsvImportType.RATINGS -> 1
            else -> 0
        } }) {
            val sourceType = rawLog.source.name
            val watchedDate = MovieLogRules.dateOrNull(rawLog.watchedDate)
            val loggedDate = MovieLogRules.dateOrNull(rawLog.loggedDate)
            val rating = MovieLogRules.ratingOrNull(rawLog.rating)
            val ratedDate = MovieLogRules.dateOrNull(rawLog.ratedDate)
            val review = rawLog.review?.ifBlank { null }

            if (!rawLog.source.marksWatched && watchedDate == null && rating == null && review == null) continue
            val storedLogs = database.movieEntityQueries.getLogsForMovie(movieId).executeAsList()
            if (sourceType == "WATCHED" && storedLogs.any { it.sourceType != "WATCHED" }) continue
            val sameDay = watchedDate?.let { date -> storedLogs.firstOrNull { it.watchedDate == date } }
            require(sameDay == null || sameDay.sourceType == sourceType || rating == null || sameDay.rating == null || sameDay.rating == rating) {
                "Different ratings for $name on $watchedDate. Resolve the conflicting source rows before importing."
            }
            val watchedFallback = if (sourceType == "WATCHED") storedLogs.firstOrNull {
                it.sourceType == "WATCHED" && it.watchedDate == null && it.rating == null && it.review.isNullOrBlank()
            } else null
            val existingLog = sameDay ?: watchedFallback ?: database.movieEntityQueries.getLogByMovieAndSource(
                movieId = movieId,
                sourceType = sourceType,
                watchedDate = watchedDate,
                loggedDate = loggedDate
            ).executeAsOneOrNull()

            if (existingLog != null) {
                database.movieEntityQueries.updateMovieLog(
                    watchedDate = watchedDate ?: existingLog.watchedDate,
                    loggedDate = loggedDate ?: existingLog.loggedDate,
                    rating = rating ?: existingLog.rating,
                    review = mergeImportedReviews(existingLog.review, review),
                    isRewatch = existingLog.isRewatch,
                    sourceType = if (existingLog.sourceType == "DIARY" || sourceType == "DIARY") "DIARY" else sourceType,
                    id = existingLog.id
                )
                if (rating != null && ratedDate != null && (existingLog.ratedDate == null || rating != existingLog.rating)) {
                    database.movieEntityQueries.setLogRatingDate(ratedDate, existingLog.id)
                }
            } else {
                database.movieEntityQueries.insertMovieLog(
                    movieId = movieId,
                    watchedDate = watchedDate,
                    loggedDate = loggedDate,
                    rating = rating,
                    review = review,
                    isRewatch = 0L,
                    sourceType = sourceType
                )
                database.movieEntityQueries.setLogRatingDate(
                    ratedDate, database.movieEntityQueries.getLastInsertId().executeAsOne()
                )
            }
        }
        reconcileImportedLogs(database, movieId)
        recomputeRewatchFlags(movieId)

    }

    private fun recomputeRewatchFlags(movieId: Long) {
        val logs = database.movieEntityQueries.getLogsForMovieByWatchOrder(movieId).executeAsList()

        var seenFirstWatch = false

        for (log in logs) {
            val hasRealWatchDate = !log.watchedDate.isNullOrBlank()
            val newIsRewatch = if (hasRealWatchDate) {
                if (!seenFirstWatch) {
                    seenFirstWatch = true
                    0L
                } else {
                    1L
                }
            } else {
                0L
            }

            database.movieEntityQueries.updateMovieLogRewatch(
                isRewatch = newIsRewatch,
                id = log.id
            )
        }
    }

    private suspend fun enrichDatabaseWithTmdb(includeUnmatched: Boolean = false) {
        enrichedCount.value = 0

        var processed = 0
        var matched = 0
        var notFound = 0
        var retryable = 0
        var lastProblem: String? = null
        var afterId = 0L

        val allToEnrich = if (includeUnmatched) {
            database.movieEntityQueries.countUnmatchedMoviesToEnrich().executeAsOne()
        } else {
            database.movieEntityQueries.countMoviesToEnrich().executeAsOne()
        }
        enrichedTotal.value = allToEnrich.toInt()

        while (true) {
            val moviesBatch = if (includeUnmatched) {
                database.movieEntityQueries.getUnmatchedMoviesToEnrich(afterId).executeAsList()
            } else {
                database.movieEntityQueries.getMoviesToEnrich(afterId).executeAsList()
            }
            if (moviesBatch.isEmpty()) break
            afterId = moviesBatch.last().id

            val enrichmentResults = moviesBatch.map { entity ->
                scope.async {
                    try {
                        var tmdbIdToUse: Int? = null
                        if (!entity.imdbId.isNullOrEmpty()) {
                            val findResult = api.findByImdbId(entity.imdbId)
                                ?: return@async Pair(entity, null)
                            tmdbIdToUse = findResult.movieResults.firstOrNull()?.id
                        }
                        if (tmdbIdToUse == null) {
                            val year = entity.year.toInt()
                            val searchResults = searchTmdbMatchCandidates(api, entity.name, year)
                            val selected = selectTmdbSearchMatch(entity.name, year, searchResults)
                            tmdbIdToUse = selected?.id
                            if (tmdbIdToUse == null) suggestionSearchCache.record(entity.id, entity.name, year, searchResults)
                            if (tmdbIdToUse == null && searchResults.isNotEmpty()) {
                                AppLogger.exception(
                                    "DataSyncManager",
                                    IllegalStateException("No unambiguous title match within two years among ${searchResults.size} TMDb results"),
                                    "TMDb match needs review for movieId=${entity.id}, name=${entity.name}"
                                )
                            }
                        }
                        val result = if (tmdbIdToUse == null) {
                            Pair("-1", null)
                        } else {
                            api.getMovieDetails(tmdbIdToUse)?.let { details ->
                                Pair(tmdbIdToUse.toString(), details)
                            }
                        }
                        Pair(entity, result)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        AppLogger.exception(
                            tag = "DataSyncManager",
                            throwable = e,
                            message = "TMDb enrichment failed for movieId=${entity.id}, name=${entity.name}"
                        )
                        Pair(entity, null)
                    }
                }
            }.awaitAll()

            for ((entity, tmdbData) in enrichmentResults) {
                processed++
                if (tmdbData == null) {
                    retryable++
                    lastProblem = "${entity.name}: TMDb request failed, timed out, or match needs review"
                    enrichedCount.value = processed
                    continue
                }
                try {
                    val details = tmdbData.second
                    if (details != null) {
                        require(details.id.toString() == tmdbData.first) { "TMDb returned a different movie" }
                        metadataWriter.enrichExisting(entity.id, details)
                    } else {
                        metadataWriter.markUnmatched(entity.id)
                        onUnmatched()
                    }
                    if (tmdbData.second == null) notFound++ else matched++
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    retryable++
                    lastProblem = "${entity.name}: ${e.message ?: "database update failed"}"
                    AppLogger.exception("DataSyncManager", e, "TMDb database update failed for movieId=${entity.id}, name=${entity.name}")
                }
                enrichedCount.value = processed
                lastMessage.value = "Processed $processed / $allToEnrich (matched $matched, retry later $retryable)"
            }

            delay(500)
        }
        val remaining = if (includeUnmatched) {
            database.movieEntityQueries.countUnmatchedMoviesToEnrich().executeAsOne()
        } else {
            database.movieEntityQueries.countMoviesToEnrich().executeAsOne()
        }
        lastMessage.value = if (remaining == 0L) {
            if (includeUnmatched) "Unmatched TMDb retry finished; all movies matched" else "Import and enrichment finished"
        } else {
            (if (includeUnmatched) "Unmatched retry" else "Enrichment") +
                " processed $processed films; $remaining still need TMDb review or data. " +
                "Matched $matched, not found $notFound, retryable errors $retryable." +
                (lastProblem?.let { " Last issue: $it" } ?: "")
        }
    }

    @OptIn(ExperimentalTime::class)
    fun refreshPendingEnrichment(force: Boolean = false) {
        val now = Clock.System.now().toEpochMilliseconds()
        if (!force && now - lastRefreshMillis < 20_000L) return
        lastRefreshMillis = now
        scope.launch {
            val count = database.movieEntityQueries.countMoviesToEnrich().executeAsOne()
            hasPendingEnrichment.value = count > 0
        }
    }

    fun cancelAll() {
        job.cancelChildren()
        phase.value = ImportPhase.IDLE
        lastMessage.value = "Sync cancelled"
        scope.launch {
            refreshPendingEnrichment(force = true)
        }
    }
}

val coreModule = module {
    single { DataSyncManager(get(), get()) }
}
