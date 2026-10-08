package com.martonegyed.data.local.catalog

import com.martonegyed.data.remote.TmdbMovie
import com.martonegyed.data.remote.TmdbApiService
import com.martonegyed.domain.model.normalizedMovieTitle

internal fun selectTmdbSearchMatch(name: String, year: Int, results: List<TmdbMovie>): TmdbMovie? {
    val exactTitle = results.distinctBy { it.id }
        .filter { matchesTmdbTitle(name, it) }
    if (year <= 0) return exactTitle.singleOrNull()

    val exactYear = exactTitle.filter { it.releaseDate?.take(4)?.toIntOrNull() == year }
    if (exactYear.isNotEmpty()) return exactYear.singleOrNull()

    val adjacentYears = exactTitle.filter {
        it.releaseDate?.take(4)?.toIntOrNull()?.let { candidateYear ->
            candidateYear == year - 1 || candidateYear == year + 1
        } == true
    }
    if (adjacentYears.isNotEmpty()) return adjacentYears.singleOrNull()

    val twoYears = exactTitle.filter {
        it.releaseDate?.take(4)?.toIntOrNull()?.let { candidateYear -> kotlin.math.abs(candidateYear - year) == 2 } == true
    }
    if (twoYears.isNotEmpty()) return twoYears.singleOrNull()

    return exactTitle.singleOrNull()?.takeIf { it.releaseDate?.take(4)?.toIntOrNull() == null }
}

internal fun matchesTmdbTitle(name: String, movie: TmdbMovie): Boolean =
    normalizedMovieTitle(name).let { normalized ->
        normalized.isNotEmpty() && (normalized == normalizedMovieTitle(movie.title) ||
            movie.originalTitle?.let { normalized == normalizedMovieTitle(it) } == true ||
            movie.alternativeTitles.any { normalized == normalizedMovieTitle(it) })
    }

internal suspend fun searchTmdbMatchCandidates(api: TmdbApiService, name: String, year: Int): List<TmdbMovie> {
    val results = mutableListOf<TmdbMovie>()
    suspend fun search(searchYear: Int) {
        results += (api.searchMovie(name, searchYear) ?: error("TMDB title search failed")).results
    }
    search(year)
    if (year > 0 && selectTmdbSearchMatch(name, year, results)?.releaseDate?.take(4)?.toIntOrNull() != year) {
        search(year - 1)
        search(year + 1)
    }
    val hasNearbyTitle = results.any { matchesTmdbTitle(name, it) && (year <= 0 ||
        it.releaseDate?.take(4)?.toIntOrNull()?.let { candidateYear -> kotlin.math.abs(candidateYear - year) <= 1 } == true) }
    if (hasNearbyTitle) return results.distinctBy { it.id }
    if (year > 0) search(0)
    return results.distinctBy { it.id }.map { movie ->
        val candidateYear = movie.releaseDate?.take(4)?.toIntOrNull()
        if (!matchesTmdbTitle(name, movie) && (year <= 0 || candidateYear == null || kotlin.math.abs(candidateYear - year) <= 2)) {
            movie.copy(alternativeTitles = api.getMovieAlternativeTitles(movie.id))
        } else movie
    }
}
