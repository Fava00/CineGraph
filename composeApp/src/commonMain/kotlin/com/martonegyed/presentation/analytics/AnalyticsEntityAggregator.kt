package com.martonegyed.presentation.analytics

import com.martonegyed.domain.model.Movie

object AnalyticsEntityAggregator {

    private data class Acc(
        var count: Int = 0,
        var sumRating: Double = 0.0,
        var ratingCount: Int = 0,
        var totalMinutes: Int = 0,
        var totalRevenue: Long = 0L,
        var photoPath: String? = null
    )

    fun <T> aggregate(
        movies: List<Movie>,
        selector: (Movie) -> List<T>,
        nameOf: (T) -> String?,
        photoOf: (T) -> String? = { null }
    ): List<AnalyticsSharedModels.AnalyticsEntityRow> {
        val map = linkedMapOf<String, Acc>()

        movies.forEach { movie ->
            val rating = movie.rating
            val minutes = movie.runtimeMinutes ?: 0
            val revenue = movie.revenue ?: 0L

            selector(movie)
                .mapNotNull { item ->
                    nameOf(item)?.trim()?.takeIf { it.isNotEmpty() }?.let { name ->
                        name to photoOf(item)
                    }
                }
                .distinctBy { it.first }
                .forEach { (name, photoPath) ->
                    val acc = map.getOrPut(name) { Acc() }

                    acc.count++
                    if (rating != null) {
                        acc.sumRating += rating
                        acc.ratingCount++
                    }
                    acc.totalMinutes += minutes
                    acc.totalRevenue += revenue

                    if (acc.photoPath == null && !photoPath.isNullOrBlank()) {
                        acc.photoPath = photoPath
                    }
                }
        }

        return map.map { (name, acc) ->
            AnalyticsSharedModels.AnalyticsEntityRow(
                name = name,
                count = acc.count,
                avgRating = if (acc.ratingCount > 0) acc.sumRating / acc.ratingCount else null,
                totalMinutes = acc.totalMinutes,
                totalRevenue = acc.totalRevenue,
                photoPath = acc.photoPath,
                initials = name
                    .split(" ")
                    .filter { it.isNotBlank() }
                    .take(2)
                    .joinToString("") { it.first().uppercase() }
            )
        }
    }

    fun sort(
        rows: List<AnalyticsSharedModels.AnalyticsEntityRow>,
        metric: AnalyticsSharedModels.AnalyticsEntityMetric
    ): List<AnalyticsSharedModels.AnalyticsEntityRow> =
        when (metric) {
            AnalyticsSharedModels.AnalyticsEntityMetric.COUNT -> rows.sortedByDescending { it.count }

            AnalyticsSharedModels.AnalyticsEntityMetric.AVG_RATING -> rows.sortedWith(
                compareByDescending<AnalyticsSharedModels.AnalyticsEntityRow> {
                    it.avgRating ?: Double.NEGATIVE_INFINITY
                }
                    .thenByDescending { it.count }
                    .thenBy { it.name }
            )

            AnalyticsSharedModels.AnalyticsEntityMetric.WATCH_TIME -> rows.sortedWith(
                compareByDescending<AnalyticsSharedModels.AnalyticsEntityRow> { it.totalMinutes }
                    .thenByDescending { it.count }
                    .thenBy { it.name }
            )

            AnalyticsSharedModels.AnalyticsEntityMetric.REVENUE -> rows.sortedWith(
                compareByDescending<AnalyticsSharedModels.AnalyticsEntityRow> { it.totalRevenue }
                    .thenByDescending { it.count }
                    .thenBy { it.name }
            )
        }
}