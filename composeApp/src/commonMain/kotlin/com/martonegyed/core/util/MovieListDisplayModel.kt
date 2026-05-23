package com.martonegyed.core.util

import com.martonegyed.domain.model.Movie
import com.martonegyed.presentation.screens.movies.MovieCollectionRow
import com.martonegyed.presentation.screens.movies.MovieListType

data class MovieListDisplayModel(
    val id: Int,
    val title: String,
    val year: Int,
    val posterPath: String?,
    val userRating: Double?,
    val TMDBRating: Double? = null,
    val watchedDate: String? = null,
    val subtitle: String? = null,
    val isRewatch: Boolean? = null,
)

fun Movie.toListDisplayModel(): MovieListDisplayModel =
    MovieListDisplayModel(
        id = id,
        title = name,
        year = year,
        posterPath = posterPath,
        userRating = rating,
        watchedDate = watchedDate,
        isRewatch = isRewatch
    )

fun MovieCollectionRow.toListDisplayModel(
    listType: MovieListType
): MovieListDisplayModel =
    MovieListDisplayModel(
        id = id,
        title = name,
        year = year,
        posterPath = posterPath,
        userRating = userRating,
        TMDBRating = tmdbVoteAverage,
        watchedDate = when (listType) {
            MovieListType.WATCHED -> watchedDate
            MovieListType.WATCHLIST -> watchlistDate ?: watchedDate
        },

        )
