package com.martonegyed.domain.model

data class AnalyticsSnapshot(
    val movies: List<Movie> = emptyList(),
    val availableYears: List<Int> = emptyList(),
    val availableMonthsByYear: Map<Int, List<Int>> = emptyMap(),
    val viewings: List<Movie> = movies
)
