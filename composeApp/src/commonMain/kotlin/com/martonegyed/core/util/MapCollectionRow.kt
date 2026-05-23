package com.martonegyed.core.util

import com.martonegyed.presentation.screens.movies.MovieCollectionRow

fun mapCollectionRow(
    id: Long,
    name: String,
    year: Long,
    posterPath: String?,
    tmdbId: String?,
    letterboxdUri: String?,
    imdbId: String?,
    tmdbVoteAverage: Double?,
    userRating: Double?,
    watchedDate: String?,
    watchlistDate: String?
): MovieCollectionRow = MovieCollectionRow(
    id = id.toInt(),
    name = name,
    year = year.toInt(),
    posterPath = posterPath,
    tmdbId = tmdbId?.toIntOrNull(),
    letterboxdUri = letterboxdUri,
    imdbId = imdbId,
    tmdbVoteAverage = tmdbVoteAverage,
    userRating = userRating,
    watchedDate = watchedDate,
    watchlistDate = watchlistDate
)