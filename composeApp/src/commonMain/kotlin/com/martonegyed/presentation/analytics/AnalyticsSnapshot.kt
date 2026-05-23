package com.martonegyed.presentation.analytics

import com.martonegyed.domain.model.Movie

data class AnalyticsSnapshot(
    val movies: List<Movie> = emptyList(),
    val availableYears: List<Int> = emptyList(),
    val availableMonthsByYear: Map<Int, List<Int>> = emptyMap()
)