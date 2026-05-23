package com.martonegyed.presentation.screens.insights

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.martonegyed.domain.model.Movie
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
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlin.time.ExperimentalTime


data class DecadeBucket(
    val label: String,
    val count: Int
)

data class InsightsState(
    val isInitialLoading: Boolean = true,
    val isRangeLoading: Boolean = true,
    val isSectionLoading: Boolean = false,

    val selectedMode: InsightMode = InsightMode.RATINGS,
    val selectedRange: StatRange = StatRange.ALL_TIME,
    val selectedYear: Int? = null,
    val selectedMonth: Int? = null,

    val ratingDistribution: List<RatingBucket> = emptyList(),
    val selectedDuoType: DuoType = DuoType.ACTOR_DIRECTOR,
    val topDuos: List<DuoRow> = emptyList(),
    val decadeBuckets: List<DecadeBucket> = emptyList(),
    val habitsSummary: HabitsSummary = HabitsSummary(),
    val mapCountries: List<AnalyticsSharedModels.MapCountryRow> = emptyList(),

    val availableYears: List<Int> = emptyList(),
    val availableMonthsByYear: Map<Int, List<Int>> = emptyMap()
)

data class WeekdayBucket(
    val day: DayOfWeek,
    val label: String,
    val count: Int
)

data class HabitsSummary(
    val totalWatches: Int = 0,
    val datedWatches: Int = 0,
    val rewatchCount: Int = 0,
    val rewatchPercent: Int = 0,
    val favoriteWeekdayLabel: String = "—",
    val favoriteWeekdayCount: Int = 0,
    val weekendCount: Int = 0,
    val weekendPercent: Int = 0,
    val weekdayBuckets: List<WeekdayBucket> = emptyList()
)

object InsightsCache {
    var lastState: InsightsState? = null
    var computedCache: MutableMap<String, InsightComputedData> = mutableMapOf()
    var duoCache: MutableMap<String, List<DuoRow>> = mutableMapOf()
}

data class InsightComputedData(
    val ratingDistribution: List<RatingBucket> = emptyList(),
    val decadeBuckets: List<DecadeBucket> = emptyList(),
    val habitsSummary: HabitsSummary = HabitsSummary(),
    val mapCountries: List<AnalyticsSharedModels.MapCountryRow> = emptyList()
)


class InsightsScreenModel(
    private val analyticsRepository: AnalyticsRepository
) : ScreenModel {

    private val _state = MutableStateFlow(
        InsightsState(
            isInitialLoading = true,
            isRangeLoading = true,
            isSectionLoading = true
        )
    )
    val state = _state.asStateFlow()

    private var allWatchedMovies: List<Movie> = emptyList()
    private var computedCache: MutableMap<String, InsightComputedData> = mutableMapOf()
    private var duoCache: MutableMap<String, List<DuoRow>> = mutableMapOf()

    init {
        val cachedState = InsightsCache.lastState
        val cachedSnapshot = AnalyticsSnapshotCache.snapshot

        if (cachedState != null && cachedSnapshot != null && AnalyticsSnapshotCache.isFresh()) {
            allWatchedMovies = cachedSnapshot.movies
            computedCache = InsightsCache.computedCache.toMutableMap()
            duoCache = InsightsCache.duoCache.toMutableMap()
            _state.value = cachedState.copy(
                isInitialLoading = false,
                isRangeLoading = false,
                isSectionLoading = false
            )
        } else {
            loadInsights()
        }
    }

    fun setMode(mode: InsightMode) {
        val current = _state.value
        val loadingState = current.copy(
            selectedMode = mode,
            isSectionLoading = true
        )
        _state.value = loadingState

        screenModelScope.launch {
            when (mode) {
                InsightMode.DUOS -> {
                    reloadDuos(loadingState)
                }

                else -> {
                    val computed = withContext(Dispatchers.Default) {
                        compute(
                            movies = allWatchedMovies,
                            state = loadingState,
                            availableYears = loadingState.availableYears,
                            availableMonthsByYear = loadingState.availableMonthsByYear
                        )
                    }
                    _state.value = computed.copy(isSectionLoading = false)
                    cacheState()
                }
            }
        }
    }


    fun setRange(range: StatRange, year: Int? = null, month: Int? = null) {
        val current = _state.value

        val loadingState = current.copy(
            selectedRange = range,
            selectedYear = year ?: current.selectedYear,
            selectedMonth = month ?: current.selectedMonth,
            isSectionLoading = true
        )
        _state.value = loadingState

        screenModelScope.launch {
            val computed = withContext(Dispatchers.Default) {
                compute(
                    movies = allWatchedMovies,
                    state = loadingState,
                    availableYears = loadingState.availableYears,
                    availableMonthsByYear = loadingState.availableMonthsByYear
                )
            }

            if (computed.selectedMode == InsightMode.DUOS) {
                reloadDuos(computed.copy(isSectionLoading = true))
            } else {
                _state.value = computed.copy(isSectionLoading = false)
                cacheState()
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun loadInsights(forceRefresh: Boolean = false) {
        screenModelScope.launch {
            _state.value = _state.value.copy(
                isInitialLoading = true,
                isRangeLoading = true,
                isSectionLoading = true
            )

            val snapshot = analyticsRepository.getSnapshot(forceRefresh)
            allWatchedMovies = snapshot.movies

            if (snapshot.movies.isEmpty()) {
                val emptyState = InsightsState(
                    isInitialLoading = false,
                    isRangeLoading = false,
                    isSectionLoading = false,
                    availableYears = snapshot.availableYears,
                    availableMonthsByYear = snapshot.availableMonthsByYear
                )
                _state.value = emptyState
                InsightsCache.lastState = emptyState
                return@launch
            }

            val computed = withContext(Dispatchers.Default) {
                compute(
                    movies = snapshot.movies,
                    state = _state.value,
                    availableYears = snapshot.availableYears,
                    availableMonthsByYear = snapshot.availableMonthsByYear
                )
            }

            if (computed.selectedMode == InsightMode.DUOS) {
                _state.value = computed.copy(
                    isInitialLoading = false,
                    isRangeLoading = false,
                    isSectionLoading = true
                )
                reloadDuos(_state.value)
            } else {
                val finalState = computed.copy(
                    isInitialLoading = false,
                    isRangeLoading = false,
                    isSectionLoading = false
                )
                _state.value = finalState
                cacheState()
            }
        }
    }

    private fun rangeKey(state: InsightsState): String {
        return when (state.selectedRange) {
            StatRange.ALL_TIME -> "all"
            StatRange.YEAR -> "year-${state.selectedYear ?: 0}"
            StatRange.MONTH -> "month-${state.selectedYear ?: 0}-${state.selectedMonth ?: 0}"
        }
    }

    private fun duoKey(state: InsightsState): String =
        "duos-${rangeKey(state)}-${state.selectedDuoType.name}"

    private fun compute(
        movies: List<Movie>,
        state: InsightsState,
        availableYears: List<Int>,
        availableMonthsByYear: Map<Int, List<Int>>
    ): InsightsState {

        val (normalizedRange, normalizedYear, normalizedMonth) =
            AnalyticsFilters.normalizeRangeSelection(
                range = state.selectedRange,
                selectedYear = state.selectedYear,
                selectedMonth = state.selectedMonth,
                availableYears = availableYears,
                availableMonthsByYear = availableMonthsByYear
            )

        val normalizedState = state.copy(
            selectedRange = normalizedRange,
            selectedYear = normalizedYear,
            selectedMonth = normalizedMonth,
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
                isInitialLoading = false,
                isRangeLoading = false,
                isSectionLoading = false,
                ratingDistribution = emptyList(),
                topDuos = emptyList(),
                decadeBuckets = emptyList(),
                habitsSummary = HabitsSummary(),
                mapCountries = emptyList()
            )
        }

        val key = rangeKey(normalizedState)

        val computed = computedCache.getOrPut(key) {
            InsightComputedData(
                ratingDistribution = computeRatingDistribution(filtered),
                decadeBuckets = computeDecadeBreakdown(filtered),
                habitsSummary = computeHabits(filtered),
                mapCountries = analyticsRepository.computeMapCountries(filtered)
            )
        }

        return normalizedState.copy(
            isInitialLoading = false,
            isRangeLoading = false,
            isSectionLoading = false,
            ratingDistribution = computed.ratingDistribution,
            topDuos = state.topDuos,
            decadeBuckets = computed.decadeBuckets,
            habitsSummary = computed.habitsSummary,
            mapCountries = computed.mapCountries
        )
    }


    private fun computeRatingDistribution(
        movies: List<Movie>
    ): List<RatingBucket> {
        val labels = listOf("0.5", "1.0", "1.5", "2.0", "2.5", "3.0", "3.5", "4.0", "4.5", "5.0")

        val counts = labels.associateWith { 0 }.toMutableMap()

        movies.forEach { movie ->
            val rating = movie.rating ?: return@forEach
            val normalized = (rating * 2.0).toInt() / 2.0
            val key = if (normalized % 1.0 == 0.0) "${normalized.toInt()}.0" else normalized.toString()
            if (counts.containsKey(key)) {
                counts[key] = counts.getValue(key) + 1
            }
        }

        return labels.map { label ->
            RatingBucket(label = label, count = counts[label] ?: 0)
        }
    }

    private fun computeDecadeBreakdown(
        movies: List<Movie>
    ): List<DecadeBucket> {
        return movies
            .mapNotNull { movie ->
                val year = movie.year
                if (year <= 0) null else (year / 10) * 10
            }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedBy { it.key }
            .map { (decade, count) ->
                DecadeBucket(
                    label = "${decade}s",
                    count = count
                )
            }
    }

    private fun computeHabits(
        movies: List<Movie>
    ): HabitsSummary {
        val orderedDays = listOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY
        )

        val dayCounts = orderedDays.associateWith { 0 }.toMutableMap()

        val parsedDays = movies.mapNotNull { movie ->
            val raw = movie.watchedDate ?: return@mapNotNull null
            runCatching { LocalDate.parse(raw).dayOfWeek }.getOrNull()
        }

        parsedDays.forEach { day ->
            dayCounts[day] = dayCounts.getValue(day) + 1
        }

        val weekdayBuckets = orderedDays.map { day ->
            WeekdayBucket(
                day = day,
                label = day.toShortLabel(),
                count = dayCounts.getValue(day)
            )
        }

        val totalWatches = movies.size
        val datedWatches = weekdayBuckets.sumOf { it.count }
        val rewatchCount = movies.count { it.isRewatch }
        val favorite = weekdayBuckets.maxByOrNull { it.count }?.takeIf { it.count > 0 }
        val weekendCount = weekdayBuckets
            .filter { it.day == DayOfWeek.SATURDAY || it.day == DayOfWeek.SUNDAY }
            .sumOf { it.count }

        return HabitsSummary(
            totalWatches = totalWatches,
            datedWatches = datedWatches,
            rewatchCount = rewatchCount,
            rewatchPercent = if (totalWatches == 0) 0 else ((rewatchCount * 100f) / totalWatches).toInt(),
            favoriteWeekdayLabel = favorite?.label ?: "—",
            favoriteWeekdayCount = favorite?.count ?: 0,
            weekendCount = weekendCount,
            weekendPercent = if (datedWatches == 0) 0 else ((weekendCount * 100f) / datedWatches).toInt(),
            weekdayBuckets = weekdayBuckets
        )
    }

    private fun DayOfWeek.toShortLabel(): String = when (this) {
        DayOfWeek.MONDAY -> "Mon"
        DayOfWeek.TUESDAY -> "Tue"
        DayOfWeek.WEDNESDAY -> "Wed"
        DayOfWeek.THURSDAY -> "Thu"
        DayOfWeek.FRIDAY -> "Fri"
        DayOfWeek.SATURDAY -> "Sat"
        DayOfWeek.SUNDAY -> "Sun"
    }

    fun setDuoType(type: DuoType) {
        val newState = _state.value.copy(selectedDuoType = type)
        _state.value = newState
        reloadDuos(newState)
    }
    private fun reloadDuos(state: InsightsState) {
        screenModelScope.launch {
            _state.value = state.copy(isSectionLoading = true)

            val key = duoKey(state)
            val duos = duoCache[key] ?: withContext(Dispatchers.Default) {
                val filtered = AnalyticsFilters.filterMoviesByRange(
                    movies = allWatchedMovies,
                    range = state.selectedRange,
                    selectedYear = state.selectedYear,
                    selectedMonth = state.selectedMonth
                )
                computeDuosInMemory(filtered, state.selectedDuoType)
            }.also { duoCache[key] = it }

            _state.value = _state.value.copy(
                isInitialLoading = false,
                isRangeLoading = false,
                isSectionLoading = false,
                topDuos = duos
            )
            cacheState()
        }
    }

    private fun computeDuosInMemory(
        movies: List<Movie>,
        type: DuoType
    ): List<DuoRow> {
        data class DuoAcc(
            var count: Int = 0,
            var leftPhotoPath: String? = null,
            var rightPhotoPath: String? = null
        )

        val duoMap = linkedMapOf<Pair<String, String>, DuoAcc>()

        movies.forEach { movie ->
            when (type) {
                DuoType.ACTOR_ACTOR -> {
                    val actors = movie.actors
                        .orEmpty()
                        .mapNotNull { person ->
                            val name = person.name?.trim()
                            name?.let { if (it.isBlank()) null else name to person.profilePath }
                        }
                        .distinctBy { it.first }
                        .take(60)

                    for (i in actors.indices) {
                        for (j in i + 1 until actors.size) {
                            val (nameA, photoA) = actors[i]
                            val (nameB, photoB) = actors[j]
                            val key = if (nameA <= nameB) nameA to nameB else nameB to nameA

                            val acc = duoMap.getOrPut(key) { DuoAcc() }
                            acc.count++

                            if (key.first == nameA) {
                                if (acc.leftPhotoPath == null) acc.leftPhotoPath = photoA
                                if (acc.rightPhotoPath == null) acc.rightPhotoPath = photoB
                            } else {
                                if (acc.leftPhotoPath == null) acc.leftPhotoPath = photoB
                                if (acc.rightPhotoPath == null) acc.rightPhotoPath = photoA
                            }
                        }
                    }
                }

                DuoType.ACTOR_DIRECTOR -> {
                    val actors = movie.actors
                        .orEmpty()
                        .mapNotNull { person ->
                            val name = person.name?.trim()
                            name?.let { if (it.isBlank()) null else name to person.profilePath }
                        }
                        .distinctBy { it.first }

                    val directors = movie.crew
                        .orEmpty()
                        .filter { it.job == "Director" }
                        .mapNotNull { person ->
                            val name = person.name?.trim()
                            name?.let { if (it.isBlank()) null else name to person.profilePath }
                        }
                        .distinctBy { it.first }

                    for ((actorName, actorPhoto) in actors) {
                        for ((directorName, directorPhoto) in directors) {
                            if (actorName == directorName) continue

                            val key = actorName to directorName
                            val acc = duoMap.getOrPut(key) { DuoAcc() }
                            acc.count++

                            if (acc.leftPhotoPath == null) acc.leftPhotoPath = actorPhoto
                            if (acc.rightPhotoPath == null) acc.rightPhotoPath = directorPhoto
                        }
                    }
                }
            }
        }

        return duoMap
            .map { (pair, acc) ->
                DuoRow(
                    leftName = pair.first,
                    rightName = pair.second,
                    count = acc.count,
                    leftPhotoPath = acc.leftPhotoPath,
                    rightPhotoPath = acc.rightPhotoPath
                )
            }
            .filter { it.count >= 2 }
            .sortedWith(
                compareByDescending<DuoRow> { it.count }
                    .thenBy { it.leftName }
                    .thenBy { it.rightName }
            )
            .take(50)
    }

    private fun cacheState() {
        InsightsCache.lastState = _state.value
        InsightsCache.computedCache = computedCache.toMutableMap()
        InsightsCache.duoCache = duoCache.toMutableMap()
    }


}