package com.martonegyed.core.util

import com.martonegyed.domain.model.Movie
import com.martonegyed.presentation.screens.statistics.EntityRow
import com.martonegyed.presentation.screens.statistics.StatisticsState

object StatisticsCache {
    var lastState: StatisticsState? = null
    var allMovies: List<Movie>? = null
    var rowsCache: MutableMap<String, List<EntityRow>> = mutableMapOf()
    var lastUpdatedMillis: Long? = null
}