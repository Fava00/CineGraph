package com.martonegyed.domain.model

data class UnmatchedMovie(
    val id: Long,
    val name: String,
    val year: Int,
    val imdbId: String?,
    val isWatched: Boolean,
    val inWatchlist: Boolean,
    val logCount: Int,
    val listCount: Int,
    val letterboxdUri: String? = null
)

@kotlinx.serialization.Serializable
data class TmdbMatchCandidate(
    val id: Int,
    val title: String,
    val year: Int?,
    val overview: String?,
    val posterPath: String? = null,
    val directors: List<String> = emptyList(),
    val originalTitle: String? = null,
    val runtimeMinutes: Int? = null,
    val popularity: Double? = null,
    val voteCount: Int? = null,
    val alternativeTitles: List<String> = emptyList()
)

data class SuggestedTmdbMatch(val movie: UnmatchedMovie, val candidates: List<TmdbMatchCandidate>,
    val selectedId: Int? = candidates.firstOrNull()?.id)

data class SuggestionPreparation(val running: Boolean = false, val completed: Int = 0,
    val total: Int = 0, val failed: Int = 0)

fun tmdbMovieIdFromUrl(value: String): Int {
    val match = Regex("^https://(?:www\\.)?themoviedb\\.org/movie/([0-9]+)(?:-[^/?#\\s]+)?/?(?:[?#][^\\s]*)?$", RegexOption.IGNORE_CASE)
        .matchEntire(value.trim())
    require(match != null) { "Paste a TMDB movie URL, such as https://www.themoviedb.org/movie/12244. TV series URLs cannot be linked as movies." }
    return match.groupValues[1].toIntOrNull()?.takeIf { it > 0 }
        ?: throw IllegalArgumentException("The TMDB movie ID is invalid.")
}
