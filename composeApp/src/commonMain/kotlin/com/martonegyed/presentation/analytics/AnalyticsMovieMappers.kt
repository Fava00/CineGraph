package com.martonegyed.presentation.analytics

import com.martonegyed.domain.model.Movie
import com.martonegyed.domain.model.Person
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

object AnalyticsMovieMappers {

    fun mapBaseMovie(
        id: Long,
        name: String,
        year: Long,
        posterPath: String?,
        tmdbId: String?,
        letterboxdUri: String?,
        imdbId: String?,
        rating: Double?,
        watchedDate: String?,
        isRewatch: Long
    ): Movie = Movie(
        id = id.toInt(),
        name = name,
        year = year.toInt(),
        posterPath = posterPath,
        tmdbId = tmdbId?.toIntOrNull(),
        letterboxdUri = letterboxdUri,
        imdbId = imdbId,
        rating = rating,
        watchedDate = watchedDate,
        isRewatch = isRewatch == 1L
    )

    fun mapRow(
        peopleByMovieId: Map<Long, List<Person>>,
        id: Long,
        name: String,
        year: Long,
        letterboxdUri: String?,
        imdbId: String?,
        isWatched: Long,
        inWatchlist: Long,
        isCached: Long,
        posterPath: String?,
        backdropPath: String?,
        overview: String?,
        runtimeMinutes: Long?,
        tmdbId: String?,
        tagline: String?,
        originalTitle: String?,
        originalLanguage: String?,
        budget: Long?,
        revenue: Long?,
        genres: String?,
        hungarianTitle: String?,
        tmdbPopularity: Double?,
        tmdbVoteAverage: Double?,
        tmdbVoteCount: Long?,
        collectionName: String?,
        trailerKey: String?,
        mpaaRating: String?,
        addedDate: String?,
        studios: String?,
        productionCountries: String?,
        spokenLanguages: String?,
        similarMovies: String?,
        tmdbReviews: String?,
        rating: Double?,
        watchedDate: String?,
        isRewatch: Long
    ): Movie {
        val persons = peopleByMovieId[id].orEmpty()

        return Movie(
            id = id.toInt(),
            name = name,
            year = year.toInt(),
            posterPath = posterPath,
            backdropPath = backdropPath,
            tmdbId = tmdbId?.toIntOrNull(),
            tmdbVoteAverage = tmdbVoteAverage,
            rating = rating,
            watchedDate = watchedDate,
            isRewatch = isRewatch == 1L,
            runtimeMinutes = runtimeMinutes?.toInt(),
            originalLanguage = originalLanguage,
            revenue = revenue,
            genres =  decodeStringList(genres),
            studios =  decodeStringList(studios),
            productionCountries = decodeStringList(productionCountries),
            actors = persons.filter { it.job == "Actor" }.map {
                Person(name = it.name, profilePath = it.profilePath)
            },
            crew = persons.filter { it.job != "Actor" }.map {
                Person(name = it.name, job = it.job, profilePath = it.profilePath)
            },
            letterboxdUri = letterboxdUri
        )
    }
}

private fun decodeStringList(raw: String?): List<String>? {
    if (raw.isNullOrBlank()) return null

    return runCatching {
        Json.decodeFromString(ListSerializer(String.serializer()), raw)
    }.getOrElse {
        raw.split(",")
            .map { it.trim().trim('"', '[', ']') }
            .filter { it.isNotBlank() }
    }
}