package com.martonegyed.presentation.components.common

import cafe.adriel.voyager.navigator.Navigator
import com.martonegyed.presentation.analytics.StatRange
import com.martonegyed.presentation.screens.movies.CollectionType
import com.martonegyed.presentation.screens.movies.MovieCollectionScreen

enum class CollectionEntityType {
    DIRECTORS,
    ACTORS,
    SCREENWRITERS,
    CINEMATOGRAPHERS,
    GENRES,
    STUDIOS,
    COUNTRIES
}

fun openPersonCollection(
    navigator: Navigator,
    personName: String?,
    entityType: CollectionEntityType,
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