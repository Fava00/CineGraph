package com.martonegyed.presentation.analytics

import com.martonegyed.domain.model.Movie

object AnalyticsFilters {

    fun extractAvailableYears(movies: List<Movie>): List<Int> =
        movies.mapNotNull { it.watchedDate?.take(4)?.toIntOrNull() }
            .distinct()
            .sortedDescending()

    fun extractAvailableMonthsByYear(movies: List<Movie>): Map<Int, List<Int>> =
        movies.mapNotNull { movie ->
            val year = movie.watchedDate?.take(4)?.toIntOrNull() ?: return@mapNotNull null
            val month = movie.watchedDate?.drop(5)?.take(2)?.toIntOrNull() ?: return@mapNotNull null
            year to month
        }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, months) -> months.distinct().sorted() }

    fun filterMoviesByRange(
        movies: List<Movie>,
        range: StatRange,
        selectedYear: Int?,
        selectedMonth: Int?
    ): List<Movie> {
        return when (range) {
            StatRange.ALL_TIME -> movies
            StatRange.YEAR -> movies.filter {
                it.watchedDate?.take(4)?.toIntOrNull() == selectedYear
            }

            StatRange.MONTH -> movies.filter {
                val year = it.watchedDate?.take(4)?.toIntOrNull()
                val month = it.watchedDate?.drop(5)?.take(2)?.toIntOrNull()
                year == selectedYear && month == selectedMonth
            }
        }
    }

    fun normalizeRangeSelection(
        range: StatRange,
        selectedYear: Int?,
        selectedMonth: Int?,
        availableYears: List<Int>,
        availableMonthsByYear: Map<Int, List<Int>>
    ): Triple<StatRange, Int?, Int?> {
        val normalizedYear = when (range) {
            StatRange.ALL_TIME -> null
            StatRange.YEAR, StatRange.MONTH -> when {
                selectedYear != null && selectedYear in availableYears -> selectedYear
                availableYears.isNotEmpty() -> availableYears.first()
                else -> null
            }
        }

        val normalizedMonth = when (range) {
            StatRange.MONTH -> {
                val validMonths = availableMonthsByYear[normalizedYear].orEmpty()
                when {
                    selectedMonth != null && selectedMonth in validMonths -> selectedMonth
                    validMonths.isNotEmpty() -> validMonths.first()
                    else -> null
                }
            }

            else -> null
        }

        val normalizedRange = when {
            range == StatRange.YEAR && normalizedYear == null -> StatRange.ALL_TIME
            range == StatRange.MONTH && (normalizedYear == null || normalizedMonth == null) -> StatRange.ALL_TIME
            else -> range
        }

        return Triple(normalizedRange, normalizedYear, normalizedMonth)
    }
}