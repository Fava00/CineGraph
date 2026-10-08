package com.martonegyed.domain.model

data class MovieSearchResult(
    val localId: Long?,
    val tmdbId: Int?,
    val title: String,
    val year: Int?,
    val posterPath: String?,
    val overview: String?,
    val isWatched: Boolean,
    val inWatchlist: Boolean,
    val isInLibrary: Boolean
) {
    val key: String get() = localId?.let { "local:$it" }
        ?: tmdbId?.takeIf { it > 0 }?.let { "tmdb:$it" }
        ?: "title:$title:$year"
}
