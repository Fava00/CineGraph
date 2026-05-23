package com.martonegyed.presentation.analytics

class AnalyticsSharedModels {
    data class MapCountryRow(
        val name: String,
        val count: Int
    )

    data class AnalyticsEntityRow(
        val name: String,
        val count: Int,
        val avgRating: Double?,
        val totalMinutes: Int,
        val totalRevenue: Long,
        val photoPath: String? = null,
        val initials: String = ""
    )

    enum class AnalyticsEntityMetric {
        COUNT,
        AVG_RATING,
        WATCH_TIME,
        REVENUE
    }
}