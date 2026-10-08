package com.martonegyed.data.repository.analytics

import com.martonegyed.data.local.analytics.AnalyticsMovieMappers

import com.martonegyed.data.database.CineGraphDatabase
import com.martonegyed.domain.model.AnalyticsSnapshot
import com.martonegyed.domain.model.Person
import com.martonegyed.domain.repository.AnalyticsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import app.cash.sqldelight.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, kotlinx.coroutines.FlowPreview::class)
class SqlDelightAnalyticsRepository(
    private val database: CineGraphDatabase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) : AnalyticsRepository {
    private data class CachedSnapshot(val snapshot: AnalyticsSnapshot, val timestamp: Long, val revision: Long)
    private val cache = MutableStateFlow<CachedSnapshot?>(null)
    private val revision = MutableStateFlow(0L)
    private val mutex = Mutex()

    override fun getCachedSnapshot(): AnalyticsSnapshot? = cache.value?.let {
        if (it.revision == revision.value && nowMillis() - it.timestamp in 0..300_000L) it.snapshot else null
    }

    override suspend fun getSnapshot(forceRefresh: Boolean): AnalyticsSnapshot = mutex.withLock {
        if (!forceRefresh) getCachedSnapshot()?.let { return@withLock it }
        val currentRevision = revision.value
        val snapshot = withContext(dispatcher) { loadSnapshot() }
        cache.value = CachedSnapshot(snapshot, nowMillis(), currentRevision)
        snapshot
    }

    override suspend fun clearCache() = mutex.withLock {
        cache.value = null
    }

    override fun observeSnapshots(): Flow<AnalyticsSnapshot> = callbackFlow {
        val query = database.movieEntityQueries.analyticsChangeSignal()
        val listener = object : Query.Listener {
            override fun queryResultsChanged() {
                revision.update { it + 1 }
                trySend(Unit)
            }
        }
        query.addListener(listener)
        revision.update { it + 1 }
        trySend(Unit)
        awaitClose { query.removeListener(listener) }
    }.conflate().debounce(100).map { getSnapshot() }

    private fun loadSnapshot(): AnalyticsSnapshot = database.transactionWithResult {
        val baseMovies = run {
            database.movieEntityQueries
                .getWatchedMoviesForList(AnalyticsMovieMappers::mapBaseMovie)
                .executeAsList()
        }

        if (baseMovies.isEmpty()) return@transactionWithResult AnalyticsSnapshot()

        val watchedIds = baseMovies.map { it.id.toLong() }

        val peopleByMovieId = run {
            database.movieEntityQueries
                .getPersonsForMovies(watchedIds)
                .executeAsList()
                .groupBy { it.movieId }
                .mapValues { (_, rows) ->
                    rows.map { row ->
                        Person(
                            name = row.name,
                            job = row.job,
                            character = row.character,
                            profilePath = row.profilePath
                        )
                    }
                }
        }

        val movies = run {
            database.movieEntityQueries
                .getWatchedMovies { id, name, year, letterboxdUri, imdbId, isWatched, inWatchlist, isCached,
                                    posterPath, backdropPath, overview, runtimeMinutes, tmdbId, tagline,
                                    originalTitle, originalLanguage, budget, revenue, genres, hungarianTitle,
                                    tmdbPopularity, tmdbVoteAverage, tmdbVoteCount, collectionName, trailerKey,
                                    mpaaRating, addedDate, studios, productionCountries, spokenLanguages,
                                    similarMovies, tmdbReviews, rating, watchedDate, isRewatch ->
                    AnalyticsMovieMappers.mapRow(
                        peopleByMovieId,
                        id, name, year, letterboxdUri, imdbId, isWatched, inWatchlist, isCached,
                        posterPath, backdropPath, overview, runtimeMinutes, tmdbId, tagline,
                        originalTitle, originalLanguage, budget, revenue, genres, hungarianTitle,
                        tmdbPopularity, tmdbVoteAverage, tmdbVoteCount, collectionName, trailerKey,
                        mpaaRating, addedDate, studios, productionCountries, spokenLanguages,
                        similarMovies, tmdbReviews, rating, watchedDate, isRewatch
                    )
                }
                .executeAsList()
        }

        val logsByMovie = database.movieEntityQueries.getAnalyticsViewingLogs().executeAsList().groupBy { it.movieId }
        val viewings = movies.flatMap { movie ->
            val logs = logsByMovie[movie.id.toLong()].orEmpty().distinctBy { it.watchedDate ?: it.stableId }
            if (logs.isEmpty()) listOf(movie.copy(watchedDate = null, isRewatch = false))
            else logs.map { movie.copy(watchedDate = it.watchedDate, isRewatch = it.isRewatch == 1L) }
        }
        AnalyticsSnapshot(
            movies = movies,
            viewings = viewings,
            availableYears = viewings.mapNotNull { it.watchedDate?.take(4)?.toIntOrNull() }
                .distinct().sortedDescending(),
            availableMonthsByYear = viewings.mapNotNull { movie ->
                val year = movie.watchedDate?.take(4)?.toIntOrNull() ?: return@mapNotNull null
                val month = movie.watchedDate?.drop(5)?.take(2)?.toIntOrNull() ?: return@mapNotNull null
                year to month
            }.groupBy({ it.first }, { it.second })
                .mapValues { (_, months) -> months.distinct().sorted() }
        )
    }
}
