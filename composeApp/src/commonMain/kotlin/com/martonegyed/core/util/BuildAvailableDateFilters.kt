package com.martonegyed.core.util

import com.martonegyed.domain.model.Movie

fun buildAvailableDateFilters(
    movies: List<Movie>
): Pair<List<Int>, Map<Int, List<Int>>> {
    val yearMonthPairs = movies.mapNotNull { movie ->
        val watchedDate = movie.watchedDate ?: return@mapNotNull null
        val year = watchedDate.take(4).toIntOrNull() ?: return@mapNotNull null
        val month = watchedDate.drop(5).take(2).toIntOrNull() ?: return@mapNotNull null
        year to month
    }

    val years = yearMonthPairs
        .map { it.first }
        .distinct()
        .sortedDescending()

    val monthsByYear = yearMonthPairs
        .groupBy(keySelector = { it.first }, valueTransform = { it.second })
        .mapValues { (_, months) -> months.distinct().sorted() }

    return years to monthsByYear
}