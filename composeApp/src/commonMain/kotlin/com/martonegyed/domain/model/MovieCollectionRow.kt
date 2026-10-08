package com.martonegyed.domain.model

data class MovieCollectionRow(
    val id: Int,
    val name: String,
    val year: Int,
    val posterPath: String?,
    val tmdbId: Int?,
    val letterboxdUri: String?,
    val imdbId: String?,
    val tmdbVoteAverage: Double?,
    val userRating: Double?,
    val watchedDate: String?,
    val watchlistDate: String?
) {
    fun toMovie(preferWatchlistDate: Boolean = false): Movie =
        Movie(
            id = id,
            name = name,
            year = year,
            posterPath = posterPath,
            tmdbId = tmdbId,
            letterboxdUri = letterboxdUri,
            imdbId = imdbId,
            rating = userRating,
            watchedDate = if (preferWatchlistDate) {
                watchlistDate ?: watchedDate
            } else {
                watchedDate ?: watchlistDate
            },
            tmdbVoteAverage = tmdbVoteAverage
        )
}

