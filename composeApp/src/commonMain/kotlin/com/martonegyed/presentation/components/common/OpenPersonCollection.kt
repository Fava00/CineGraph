package com.martonegyed.presentation.components.common

import cafe.adriel.voyager.navigator.Navigator
import com.martonegyed.presentation.analytics.StatRange
import com.martonegyed.presentation.screens.movies.CollectionType
import com.martonegyed.presentation.screens.movies.MovieCollectionScreen
import com.martonegyed.presentation.screens.statistics.StatEntityType


public fun openPersonCollection(
    navigator: Navigator,
    personName: String?,
    entityType: StatEntityType,
    range: StatRange = StatRange.ALL_TIME,
    selectedYear: Int? = null,
    selectedMonth: Int? = null
) {
    val name = personName ?: return
    navigator.push(
        MovieCollectionScreen(
            type = CollectionType.BY_ENTITY,
            entityType = entityType,
            entityName = name,
            range = range,
            year = selectedYear,
            month = selectedMonth,
        )
    )
}