package com.martonegyed.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Movie(
    var id: Int = 0,
    var tmdbId: Int? = null,
    var name: String = "",
    var year: Int = 0,
    var rating: Double? = null,

    var watchedDate: String? = null,
    var addedDate: String? = null,

    var inWatchlist: Boolean = false,
    var isRewatch: Boolean = false,
    var userReview: String? = null,

    var posterPath: String? = null,
    var backdropPath: String? = null,
    var overview: String? = null,
    var tagline: String? = null,
    var runtimeMinutes: Int? = null,
    var originalTitle: String? = null,
    var originalLanguage: String? = null,
    var hungarianTitle: String? = null,
    var budget: Int? = null,
    var revenue: Int? = null,
    var tmdbPopularity: Double? = null,
    var tmdbVoteAverage: Double? = null,
    var tmdbVoteCount: Int? = null,
    var collectionName: String? = null,
    var trailerKey: String? = null,
    var mpaaRating: String? = null,
    var imdbId: String? = null,

    var genres: List<String>? = null,
    var actors: List<Person>? = null,
    var crew: List<Person>? = null,
    var studios: List<String>? = null,
    var productionCountries: List<String>? = null,
    var spokenLanguages: List<String>? = null,
    var similarMovies: List<SimilarMovie>? = null,
    var tmdbReviews: List<String>? = null,
    val letterboxdUri: String?
) {
    val directors: List<Person>
        get() = crew?.filter { it.job == "Director" } ?: emptyList()

    val writers: List<Person>
        get() = crew?.filter { it.job == "Screenplay" || it.job == "Writer" } ?: emptyList()
}

@Serializable
data class SimilarMovie(
    var tmdbId: Int? = null,
    var name: String? = null,
    var year: Int? = null,
    var posterPath: String? = null,
    var originalTitle: String? = null,
    var originalLanguage: String? = null,
    var hungarianTitle: String? = null,
    var backdropPath: String? = null,
    var overview: String? = null,
    var tagline: String? = null,
    var runtimeMinutes: Int? = null,
    var mpaaRating: String? = null,
    var genres: List<String>? = null,
    var budget: Int? = null,
    var revenue: Int? = null,
    var tmdbPopularity: Double? = null,
    var tmdbVoteAverage: Double? = null,
    var tmdbVoteCount: Int? = null,
    var productionCountries: List<String>? = null,
    var spokenLanguages: List<String>? = null,
    var trailerKey: String? = null,
    var imdbId: String? = null,
    var studios: List<String>? = null,
    var crew: List<Person>? = null,
    var actors: List<Person>? = null
)
