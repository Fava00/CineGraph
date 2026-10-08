package com.martonegyed.data.repository.library

import com.martonegyed.data.database.CineGraphDatabase
import com.martonegyed.domain.model.ManualMovieLog
import com.martonegyed.domain.model.MovieLogRules
import com.martonegyed.domain.repository.AnalyticsRepository
import com.martonegyed.domain.repository.MovieLogRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalTime::class)
class SqlDelightMovieLogRepository(
    private val database: CineGraphDatabase,
    private val analyticsRepository: AnalyticsRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val today: () -> String = {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
    }
) : MovieLogRepository {
    override suspend fun addViewing(movieId: Long, log: ManualMovieLog) = withContext(dispatcher) {
        val watchedDate = MovieLogRules.dateOrNull(log.watchedDate)
        val rating = MovieLogRules.ratingOrNull(log.rating)
        val review = log.review?.trim()?.takeIf { it.isNotEmpty() }
        val date = today()
        val queries = database.movieEntityQueries
        queries.transaction {
            require(queries.getMovieById(movieId).executeAsOneOrNull() != null) { "This movie is no longer available." }
            require(watchedDate == null || queries.getLogsForMovie(movieId).executeAsList().none { it.watchedDate == watchedDate }) {
                "A viewing already exists on this date. A second viewing on the same day cannot be added."
            }
            queries.getLogsForMovie(movieId).executeAsList()
                .filter { it.sourceType == "WATCHED" && it.watchedDate == null && it.rating == null && it.review.isNullOrBlank() }
                .forEach { queries.deleteImportedLog(it.id) }
            queries.insertMovieLog(movieId, watchedDate, date, rating, review, 0, "MANUAL")
            val logId = queries.getLastInsertId().executeAsOne()
            if (rating != null) queries.setLogRatingDate(date, logId)
            queries.markMovieViewed(movieId)
            updateRewatches(movieId)
        }
        analyticsRepository.clearCache()
    }

    override suspend fun rateMovie(movieId: Long, rating: Double) = withContext(dispatcher) {
        val score = requireNotNull(MovieLogRules.ratingOrNull(rating)) { "Choose a rating from 0.5 to 5 stars." }
        val date = today()
        val queries = database.movieEntityQueries
        queries.transaction {
            require(queries.getMovieById(movieId).executeAsOneOrNull() != null) { "This movie is no longer available." }
            val logs = queries.getLogsForMovie(movieId).executeAsList()
            val previous = logs.filter { it.sourceType == "MANUAL_RATING" }
                .mapNotNull { it.createdAt?.let { value -> runCatching { Instant.parse(value) }.getOrNull() } }.maxOrNull()
            val now = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
            val createdAt = if (previous != null && previous >= now) previous + 1.milliseconds else now
            queries.insertMovieLog(movieId, null, date, score, null, 0, "MANUAL_RATING")
            val logId = queries.getLastInsertId().executeAsOne()
            queries.setLogRatingDate(date, logId)
            queries.setLogCreationTime(createdAt.toString(), logId)
            queries.markMovieRated(movieId)
        }
        analyticsRepository.clearCache()
    }

    override suspend fun updateLog(movieId: Long, logId: Long, log: ManualMovieLog) = withContext(dispatcher) {
        val watchedDate = MovieLogRules.dateOrNull(log.watchedDate)
        val rating = MovieLogRules.ratingOrNull(log.rating)
        val ratedDate = if (rating == null) null else MovieLogRules.dateOrNull(log.ratedDate)
        val review = log.review?.trim()?.takeIf { it.isNotEmpty() }
        val queries = database.movieEntityQueries
        queries.transaction {
            val logs = queries.getLogsForMovie(movieId).executeAsList()
            val existing = requireNotNull(logs.singleOrNull { it.id == logId }) { "This log is no longer available." }
            require(watchedDate == null || logs.none { it.id != logId && it.watchedDate == watchedDate }) {
                "Another viewing already exists on this date. Choose a different date."
            }
            val ratingOnly = existing.sourceType.uppercase() in setOf("RATINGS", "MANUAL_RATING")
            val source = if (ratingOnly && watchedDate != null) "MANUAL" else existing.sourceType
            queries.updateMovieLog(watchedDate, existing.loggedDate, rating, review, existing.isRewatch, source, logId)
            queries.setLogRatingDate(if (rating == null) null else ratedDate, logId)
            if (!ratingOnly || watchedDate != null) {
                queries.updateWatchedFromLogs(movieId)
            }
            updateRewatches(movieId)
        }
        analyticsRepository.clearCache()
    }

    override suspend fun deleteLog(movieId: Long, logId: Long) = withContext(dispatcher) {
        val queries = database.movieEntityQueries
        queries.transaction {
            require(queries.getLogsForMovie(movieId).executeAsList().any { it.id == logId }) { "This log is no longer available." }
            queries.deletePersonalMovieLog(logId, movieId)
            queries.updateWatchedFromLogs(movieId)
            updateRewatches(movieId)
        }
        analyticsRepository.clearCache()
    }

    override suspend fun removeMovie(movieId: Long) = withContext(dispatcher) {
        val queries = database.movieEntityQueries
        queries.transaction {
            require(queries.getMovieById(movieId).executeAsOneOrNull() != null) { "This movie is no longer available." }
            queries.deletePersonalMovie(movieId)
        }
        analyticsRepository.clearCache()
    }

    private fun updateRewatches(movieId: Long) {
        val queries = database.movieEntityQueries
        val logs = queries.getLogsForMovie(movieId).executeAsList()
        var datedCount = 0
        logs.forEach { log ->
            val rewatch = if (log.watchedDate != null) {
                if (datedCount++ > 0) 1L else 0L
            } else log.isRewatch
            queries.updateMovieLogRewatch(rewatch, log.id)
        }
    }
}
