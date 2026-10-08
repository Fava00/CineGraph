package com.martonegyed.domain.model

internal fun normalizedMovieTitle(value: String) = value.lowercase()
    .replace(Regex("[-\u2010-\u2015\u2212]"), " ")
    .replace(Regex("\\bcolours\\b"), "colors")
    .replace(Regex("\\bcolour\\b"), "color")
    .replace(Regex("[\u2018\u2019\u02bc]"), "'")
    .replace(Regex("(^|[\\s\u00a0\u202f])'(?=[0-9]{2}s\\b)"), "$1")
    .replace(Regex("[\u201c\u201d]"), "\"")
    .replace(Regex("[\\s\u00a0\u202f]+"), " ").trim()

fun TmdbMatchCandidate.hasMatchingTitle(name: String): Boolean = normalizedMovieTitle(name).let {
    it.isNotEmpty() && (it == normalizedMovieTitle(title) || originalTitle?.let { original ->
        it == normalizedMovieTitle(
            original
        )
    } == true ||
            alternativeTitles.any { title -> it == normalizedMovieTitle(title) })
}

fun rankTmdbCandidates(movie: UnmatchedMovie, candidates: List<TmdbMatchCandidate>): List<TmdbMatchCandidate> =
    candidates.distinctBy { it.id }.sortedWith(
        compareByDescending<TmdbMatchCandidate> { it.hasMatchingTitle(movie.name) }
            .thenBy {
                if (movie.year > 0) it.year?.let { year -> kotlin.math.abs(year - movie.year) } ?: Int.MAX_VALUE else 0
            }
            .thenByDescending { it.runtimeMinutes?.let { runtime -> if (runtime >= 40) 2 else 0 } ?: 1 }
            .thenByDescending { it.popularity ?: 0.0 }
            .thenByDescending { it.voteCount ?: 0 }
            .thenBy { it.id }
    )
