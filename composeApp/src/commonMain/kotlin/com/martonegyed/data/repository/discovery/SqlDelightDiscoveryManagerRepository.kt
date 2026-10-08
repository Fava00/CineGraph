package com.martonegyed.data.repository.discovery

import com.martonegyed.data.database.CineGraphDatabase
import com.martonegyed.domain.model.DiscoveryMovie
import com.martonegyed.domain.repository.DiscoveryManagerRepository
import com.martonegyed.domain.model.DiscoveryCandidate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class SqlDelightDiscoveryManagerRepository(
    private val database: CineGraphDatabase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : DiscoveryManagerRepository {

    override suspend fun getCachedMovies(): List<DiscoveryMovie> = withContext(dispatcher) {
        database.movieEntityQueries
            .getCachedCollectionRows()
            .executeAsList()
            .map { row ->
                DiscoveryMovie(
                    localMovieId = row.id,
                    tmdbId = row.tmdbId?.toIntOrNull(),
                    title = row.name,
                    year = row.year.toInt(),
                    posterPath = row.posterPath,
                    tmdbVoteAverage = row.tmdbVoteAverage
                )
            }
    }

    override suspend fun getIgnoredMovies(): List<DiscoveryMovie> = withContext(dispatcher) {
        database.movieEntityQueries
            .getIgnoredMovies()
            .executeAsList()
            .map { row ->
                DiscoveryMovie(
                    tmdbId = row.tmdbId.toInt(),
                    title = row.title,
                    year = row.year?.toInt(),
                    posterPath = row.posterPath,
                    tmdbVoteAverage = row.tmdbVoteAverage
                )
            }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun ignoreMovie(movie: DiscoveryCandidate): Unit = withContext(dispatcher) {
        val tmdbId = movie.tmdbId ?: return@withContext
        database.movieEntityQueries.insertOrReplaceIgnoredMovie(
            tmdbId = tmdbId.toLong(),
            title = movie.title,
            year = movie.year,
            posterPath = movie.posterPath,
            overview = movie.overview,
            runtimeMinutes = movie.runtimeMinutes?.toLong(),
            tmdbVoteAverage = movie.tmdbVoteAverage,
            createdAt = Clock.System.now().toString()
        )
    }

    override suspend fun unignoreMovie(tmdbId: Int): Unit = withContext(dispatcher) {
        database.movieEntityQueries.deleteIgnoredMovie(tmdbId.toLong())
    }

    override suspend fun isIgnored(tmdbId: Int): Boolean = withContext(dispatcher) {
        database.movieEntityQueries
            .isMovieIgnored(tmdbId.toLong())
            .executeAsOne()
    }
}