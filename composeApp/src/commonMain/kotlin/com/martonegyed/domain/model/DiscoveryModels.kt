package com.martonegyed.domain.model

enum class MoviePickerSearchSource {
    MY_LIBRARY,
    DISCOVER_NEW,
    BOTH
}

enum class MoviePickerWatchIntent {
    SOMETHING_NEW,
    REWATCH,
    ANYTHING
}

data class MoviePickerRequest(
    val source: MoviePickerSearchSource,
    val searchDepth: Int,
    val watchIntent: MoviePickerWatchIntent,
    val includedGenreIds: Set<Int>,
    val excludedGenreIds: Set<Int>,
    val runtimeMinutes: IntRange,
    val minimumRating: Float,
    val selectedDecades: Set<Int>,
    val languages: Set<String>,
)

data class DiscoveryCandidate(
    val localMovieId: Long? = null,
    val tmdbId: Int? = null,
    val title: String,
    val year: Long? = null,
    val posterPath: String? = null,
    val overview: String? = null,
    val runtimeMinutes: Int? = null,
    val tmdbVoteAverage: Double? = null,
    val source: MoviePickerCandidateSource = MoviePickerCandidateSource.LOCAL
)

enum class MoviePickerCandidateSource {
    LOCAL,
    REMOTE
}

data class DiscoveryMovie(
    val localMovieId: Long? = null,
    val tmdbId: Int? = null,
    val title: String,
    val year: Int? = null,
    val posterPath: String? = null,
    val tmdbVoteAverage: Double? = null
)

enum class PersonRole(
    val localJob: String,
    val tmdbDepartment: String
) {
    ACTOR(
        localJob = "Actor",
        tmdbDepartment = "Acting"
    ),
    DIRECTOR(
        localJob = "Director",
        tmdbDepartment = "Directing"
    )
}

data class MovieGenre(val id: Int, val name: String)
data class CrossoverMovie(val id: Int, val title: String, val posterPath: String?, val releaseDate: String?)

data class CrossoverRequest(val actors: List<SelectedPerson>, val directors: List<SelectedPerson>, val genreIds: Set<Int>, val startYear: Int, val endYear: Int)

fun DiscoveryCandidate.stableKey(): String =
    tmdbId?.toString() ?: "local-${localMovieId ?: title}"

