package com.martonegyed.presentation.analytics

import com.martonegyed.data.database.CineGraphDatabase
import com.martonegyed.domain.model.Movie
import com.martonegyed.domain.model.Person
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class AnalyticsRepository(
    private val database: CineGraphDatabase
) {

    data class WatchedMoviesSnapshot(
        val movies: List<Movie>,
        val availableYears: List<Int>,
        val availableMonthsByYear: Map<Int, List<Int>>
    )

    fun normalizeYear(selectedYear: Int?, availableYears: List<Int>): Int? {
        return when {
            selectedYear != null && selectedYear in availableYears -> selectedYear
            availableYears.isNotEmpty() -> availableYears.first()
            else -> null
        }
    }

    fun filterMoviesByYear(movies: List<Movie>, year: Int?): List<Movie> {
        if (year == null) return emptyList()

        return movies.filter { movie ->
            movie.watchedDate?.take(4)?.toIntOrNull() == year
        }
    }

    fun computeMapCountries(movies: List<Movie>): List<AnalyticsSharedModels.MapCountryRow> {
        return movies
            .flatMap { movie ->
                movie.productionCountries
                    .orEmpty()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
            }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { (name, count) ->
                AnalyticsSharedModels.MapCountryRow(
                    name = name,
                    count = count
                )
            }
    }

    @OptIn(ExperimentalTime::class)
    suspend fun getSnapshot(forceRefresh: Boolean = false): AnalyticsSnapshot {
        if (!forceRefresh) {
            val cached = AnalyticsSnapshotCache.snapshot
            if (cached != null && AnalyticsSnapshotCache.isFresh()) return cached
        }

        val baseMovies = withContext(Dispatchers.Default) {
            database.movieEntityQueries
                .getWatchedMoviesForList(AnalyticsMovieMappers::mapBaseMovie)
                .executeAsList()
        }

        if (baseMovies.isEmpty()) {
            val empty = AnalyticsSnapshot()
            AnalyticsSnapshotCache.snapshot = empty
            AnalyticsSnapshotCache.lastUpdatedMillis = Clock.System.now().toEpochMilliseconds()
            return empty
        }

        val watchedIds = baseMovies.map { it.id.toLong() }

        val peopleByMovieId = withContext(Dispatchers.Default) {
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

        val movies = withContext(Dispatchers.Default) {
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

        val snapshot = AnalyticsSnapshot(
            movies = movies,
            availableYears = AnalyticsFilters.extractAvailableYears(movies),
            availableMonthsByYear = AnalyticsFilters.extractAvailableMonthsByYear(movies)
        )

        AnalyticsSnapshotCache.snapshot = snapshot
        AnalyticsSnapshotCache.lastUpdatedMillis = Clock.System.now().toEpochMilliseconds()
        return snapshot
    }

    fun clearCache() {
        AnalyticsSnapshotCache.clear()
    }
}