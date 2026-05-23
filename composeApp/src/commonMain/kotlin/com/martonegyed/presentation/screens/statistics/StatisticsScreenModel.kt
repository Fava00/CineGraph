package com.martonegyed.presentation.screens.statistics

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.martonegyed.domain.model.Movie
import com.martonegyed.presentation.analytics.AnalyticsEntityAggregator
import com.martonegyed.presentation.analytics.AnalyticsFilters
import com.martonegyed.presentation.analytics.AnalyticsRepository
import com.martonegyed.presentation.analytics.AnalyticsSharedModels
import com.martonegyed.presentation.analytics.AnalyticsSnapshotCache
import com.martonegyed.presentation.analytics.StatRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class StatEntityType { DIRECTORS, ACTORS, GENRES, STUDIOS, COUNTRIES }
enum class StatMetric { COUNT, AVG_RATING, WATCH_TIME, REVENUE }


typealias EntityRow = AnalyticsSharedModels.AnalyticsEntityRow

data class StatisticsState(
    val isLoading: Boolean = true,

    val totalMovies: Int = 0,
    val totalHours: Double = 0.0,
    val averageRating: Double = 0.0,
    val totalRevenue: Long = 0L,

    val entityType: StatEntityType = StatEntityType.DIRECTORS,
    val metric: StatMetric = StatMetric.COUNT,
    val range: StatRange = StatRange.ALL_TIME,
    val selectedYear: Int? = null,
    val selectedMonth: Int? = null,
    val availableYears: List<Int> = emptyList(),
    val availableMonthsByYear: Map<Int, List<Int>> = emptyMap(),

    val rows: List<EntityRow> = emptyList()
)

object StatisticsCache {
    var lastState: StatisticsState? = null
    var rowsCache: MutableMap<String, Map<StatEntityType, List<EntityRow>>> = mutableMapOf()
}

class StatisticsScreenModel(
    private val analyticsRepository: AnalyticsRepository
) : ScreenModel {

    private val _state = MutableStateFlow(StatisticsState())
    val state = _state.asStateFlow()

    private var allWatchedMovies: List<Movie> = emptyList()
    private var rowsCache = mutableMapOf<String, Map<StatEntityType, List<EntityRow>>>()


    init {
        val cachedState = StatisticsCache.lastState
        val cachedSnapshot = AnalyticsSnapshotCache.snapshot

        if (cachedState != null && cachedSnapshot != null && AnalyticsSnapshotCache.isFresh()) {
            allWatchedMovies = cachedSnapshot.movies
            rowsCache = StatisticsCache.rowsCache.toMutableMap()
            _state.value = cachedState.copy(isLoading = false)
        } else {
            loadStatistics()
        }
    }

    private fun loadStatistics(forceRefresh: Boolean = false) {
        screenModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            val snapshot = analyticsRepository.getSnapshot(forceRefresh)
            allWatchedMovies = snapshot.movies

            if (snapshot.movies.isEmpty()) {
                val emptyState = StatisticsState(
                    isLoading = false,
                    availableYears = snapshot.availableYears,
                    availableMonthsByYear = snapshot.availableMonthsByYear,
                    rows = emptyList()
                )
                _state.value = emptyState
                StatisticsCache.lastState = emptyState
                return@launch
            }

            rowsCache.clear()

            val finalState = withContext(Dispatchers.Default) {
                compute(
                    movies = snapshot.movies,
                    state = _state.value,
                    availableYears = snapshot.availableYears,
                    availableMonthsByYear = snapshot.availableMonthsByYear
                ).copy(isLoading = false)
            }

            _state.value = finalState
            cacheState()
        }
    }

    fun setEntityType(type: StatEntityType) {
        val newState = _state.value.copy(entityType = type)
        val finalState = compute(
            movies = allWatchedMovies,
            state = newState,
            availableYears = newState.availableYears,
            availableMonthsByYear = newState.availableMonthsByYear
        )
        _state.value = finalState
        cacheState()
    }

    fun setMetric(metric: StatMetric) {
        val newState = _state.value.copy(metric = metric)

        val finalState = compute(
            movies = allWatchedMovies,
            state = newState,
            availableYears = newState.availableYears,
            availableMonthsByYear = newState.availableMonthsByYear
        )
        _state.value = finalState
        cacheState()
    }

    fun setRange(range: StatRange, year: Int? = null, month: Int? = null) {
        val newState = _state.value.copy(
            range = range,
            selectedYear = year,
            selectedMonth = month
        )
        val finalState = compute(
            movies = allWatchedMovies,
            state = newState,
            availableYears = newState.availableYears,
            availableMonthsByYear = newState.availableMonthsByYear
        )
        _state.value = finalState
        cacheState()
    }

    private fun compute(
        movies: List<Movie>,
        state: StatisticsState,
        availableYears: List<Int>,
        availableMonthsByYear: Map<Int, List<Int>>
    ): StatisticsState {
        val (normalizedRange, normalizedYear, normalizedMonth) =
            AnalyticsFilters.normalizeRangeSelection(
                range = state.range,
                selectedYear = state.selectedYear,
                selectedMonth = state.selectedMonth,
                availableYears = availableYears,
                availableMonthsByYear = availableMonthsByYear
            )

        val normalizedState = state.copy(
            range = normalizedRange,
            selectedYear = normalizedYear,
            selectedMonth = if (normalizedRange == StatRange.MONTH) normalizedMonth else null,
            availableYears = availableYears,
            availableMonthsByYear = availableMonthsByYear
        )

        val filtered = AnalyticsFilters.filterMoviesByRange(
            movies = movies,
            range = normalizedRange,
            selectedYear = normalizedYear,
            selectedMonth = normalizedMonth
        )

        if (filtered.isEmpty()) {
            return normalizedState.copy(
                isLoading = false,
                totalMovies = 0,
                totalHours = 0.0,
                averageRating = 0.0,
                totalRevenue = 0L,
                rows = emptyList()
            )
        }

        val totalMinutes = filtered.sumOf { it.runtimeMinutes ?: 0 }
        val ratings = filtered.mapNotNull { it.rating }
        val averageRating = if (ratings.isNotEmpty()) ratings.average() else 0.0
        val totalRevenue = filtered.sumOf { it.revenue ?: 0L }

        val baseRows = getBaseRows(filtered, normalizedState)

        val metricFiltered = when (normalizedState.metric) {
            StatMetric.COUNT -> baseRows
            StatMetric.AVG_RATING -> baseRows.filter { it.count >= 3 && it.avgRating != null }
            StatMetric.WATCH_TIME -> baseRows
            StatMetric.REVENUE -> baseRows
        }

        val sortedRows = when (normalizedState.metric) {
            StatMetric.COUNT -> metricFiltered.sortedByDescending { it.count }

            StatMetric.AVG_RATING -> metricFiltered.sortedWith(
                compareByDescending<EntityRow> { it.avgRating ?: Double.NEGATIVE_INFINITY }
                    .thenByDescending { it.count }
                    .thenBy { it.name }
            )

            StatMetric.WATCH_TIME -> metricFiltered.sortedWith(
                compareByDescending<EntityRow> { it.totalMinutes }
                    .thenByDescending { it.count }
                    .thenBy { it.name }
            )

            StatMetric.REVENUE -> metricFiltered.sortedWith(
                compareByDescending<EntityRow> { it.totalRevenue }
                    .thenByDescending { it.count }
                    .thenBy { it.name }
            )
        }.take(100)

        return normalizedState.copy(
            isLoading = false,
            totalMovies = filtered.size,
            totalHours = totalMinutes / 60.0,
            averageRating = averageRating,
            totalRevenue = totalRevenue,
            rows = sortedRows
        )
    }

    private fun rangeKey(state: StatisticsState): String {
        return when (state.range) {
            StatRange.ALL_TIME -> "all"
            StatRange.YEAR -> "year:${state.selectedYear ?: 0}"
            StatRange.MONTH -> "month:${state.selectedYear ?: 0}-${state.selectedMonth ?: 0}"
        }
    }

    private fun getBaseRows(
        filtered: List<Movie>,
        state: StatisticsState
    ): List<EntityRow> {
        val key = rangeKey(state)
        val cachedForRange = rowsCache[key]

        cachedForRange?.get(state.entityType)?.let { return it }

        val rows = when (state.entityType) {
            StatEntityType.DIRECTORS -> AnalyticsEntityAggregator.aggregate(
                movies = filtered,
                selector = { movie -> movie.directors },
                nameOf = { person -> person.name },
                photoOf = { person -> person.profilePath }
            )

            StatEntityType.ACTORS -> AnalyticsEntityAggregator.aggregate(
                movies = filtered,
                selector = { movie -> movie.actors ?: emptyList() },
                nameOf = { person -> person.name },
                photoOf = { person -> person.profilePath }
            )

            StatEntityType.GENRES -> AnalyticsEntityAggregator.aggregate(
                movies = filtered,
                selector = { movie -> movie.genres ?: emptyList() },
                nameOf = { value -> value }
            )

            StatEntityType.STUDIOS -> AnalyticsEntityAggregator.aggregate(
                movies = filtered,
                selector = { movie -> movie.studios ?: emptyList() },
                nameOf = { value -> value }
            )

            StatEntityType.COUNTRIES -> AnalyticsEntityAggregator.aggregate(
                movies = filtered,
                selector = { movie -> movie.productionCountries ?: emptyList() },
                nameOf = { value -> value }
            )
        }

        rowsCache[key] = (cachedForRange ?: emptyMap()) + (state.entityType to rows)
        return rows
    }

    private fun cacheState() {
        StatisticsCache.lastState = _state.value
        StatisticsCache.rowsCache = rowsCache.toMutableMap()
    }
}