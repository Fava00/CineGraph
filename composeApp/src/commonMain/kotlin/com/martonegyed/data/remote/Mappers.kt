package com.martonegyed.data.remote

import com.martonegyed.domain.model.Person
import com.martonegyed.domain.model.SimilarMovie

fun TmdbMovie.toSimilarMovie(): SimilarMovie {
    return SimilarMovie(
        tmdbId = id,
        name = title,
        year = releaseDate?.take(4)?.toIntOrNull(),
        posterPath = posterPath,
        originalTitle = null,
        originalLanguage = originalLanguage,
        backdropPath = backdropPath,
        overview = overview,
        tmdbVoteAverage = voteAverage,
        tmdbVoteCount = voteCount
    )
}

fun TmdbMovieDetailsResponse.toSimilarMovies(limit: Int = 10): List<SimilarMovie>? {
    return similar?.results
        ?.take(limit)
        ?.map { it.toSimilarMovie() }
        ?.takeIf { it.isNotEmpty() }
}

fun TmdbMovieDetailsResponse.toActors(): List<Person>? {
    return credits?.cast
        ?.map {
            Person(
                name = it.name,
                character = it.character,
                profilePath = it.profilePath,
                job = "Actor"
            )
        }
        ?.takeIf { it.isNotEmpty() }
}

fun TmdbMovieDetailsResponse.toCrew(): List<Person>? {
    return credits?.crew
        ?.map {
            Person(
                name = it.name,
                job = it.job,
                character = null,
                profilePath = it.profilePath
            )
        }
        ?.takeIf { it.isNotEmpty() }
}

fun TmdbMovieDetailsResponse.toReviewStrings(limit: Int = 5): List<String>? {
    return reviews?.results
        ?.take(limit)
        ?.mapNotNull { review ->
            val author = review.author.trim()
            val content = review.content.trim()
            if (author.isBlank() || content.isBlank()) null else "$author: $content"
        }
        ?.takeIf { it.isNotEmpty() }
}