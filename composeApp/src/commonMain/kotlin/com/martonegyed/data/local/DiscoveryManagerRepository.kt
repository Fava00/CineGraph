package com.martonegyed.data.local

import com.martonegyed.data.database.CineGraphDatabase
import com.martonegyed.presentation.screens.moviePicker.DiscoveryManagerMovieUi
import com.martonegyed.presentation.screens.moviePicker.DiscoveryManagerRepository
import com.martonegyed.presentation.screens.moviePicker.MoviePickerCandidateUi
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class SqlDelightDiscoveryManagerRepository(
    private val database: CineGraphDatabase
) : DiscoveryManagerRepository {

    override suspend fun getCachedMovies(): List<DiscoveryManagerMovieUi> {
        return database.movieEntityQueries
            .getCachedCollectionRows()
            .executeAsList()
            .map { row ->
                DiscoveryManagerMovieUi(
                    localMovieId = row.id,
                    tmdbId = row.tmdbId?.toIntOrNull(),
                    title = row.name,
                    year = row.year.toInt(),
                    posterPath = row.posterPath,
                    tmdbVoteAverage = row.tmdbVoteAverage
                )
            }
    }

    override suspend fun getIgnoredMovies(): List<DiscoveryManagerMovieUi> {
        return database.movieEntityQueries
            .getIgnoredMovies()
            .executeAsList()
            .map { row ->
                DiscoveryManagerMovieUi(
                    tmdbId = row.tmdbId.toInt(),
                    title = row.title,
                    year = row.year?.toInt(),
                    posterPath = row.posterPath,
                    tmdbVoteAverage = row.tmdbVoteAverage
                )
            }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun ignoreMovie(movie: MoviePickerCandidateUi) {
        val tmdbId = movie.tmdbId ?: return
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

    override suspend fun unignoreMovie(tmdbId: Int) {
        database.movieEntityQueries.deleteIgnoredMovie(tmdbId.toLong())
    }

    override suspend fun isIgnored(tmdbId: Int): Boolean {
        return database.movieEntityQueries
            .isMovieIgnored(tmdbId.toLong())
            .executeAsOne()
    }
}