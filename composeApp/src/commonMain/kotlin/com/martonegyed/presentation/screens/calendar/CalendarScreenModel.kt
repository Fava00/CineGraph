package com.martonegyed.presentation.screens.calendar

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.martonegyed.data.database.CineGraphDatabase
import com.martonegyed.domain.model.Movie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CalendarMovieItem(
    val id: Int,
    val name: String,
    val year: Int,
    val posterPath: String?,
    val tmdbId: Int?,
    val letterboxdUri: String?,
    val imdbId: String?,
    val rating: Double?,
    val watchedDate: String?
) {
    fun toMovie(): Movie = Movie(
        id = id,
        name = name,
        year = year,
        posterPath = posterPath,
        tmdbId = tmdbId,
        letterboxdUri = letterboxdUri,
        imdbId = imdbId,
        rating = rating,
        watchedDate = watchedDate
    )
}

data class CalendarDayCell(
    val dateKey: String?,
    val dayLabel: String,
    val isCurrentMonth: Boolean,
    val movieCount: Int,
    val isSelected: Boolean,
    val isWeekend: Boolean,
    val posterPath: String?
)

data class CinemaCalendarState(
    val isLoading: Boolean = true,
    val currentYear: Int = 2026,
    val currentMonth: Int = 1,
    val monthLabel: String = "January 2026",
    val monthMovieCount: Int = 0,
    val days: List<CalendarDayCell> = emptyList(),
    val selectedDateKey: String? = null,
    val selectedDateLabel: String? = null,
    val selectedMovies: List<CalendarMovieItem> = emptyList()
)

private data class DateParts(
    val year: Int,
    val month: Int,
    val day: Int
)

class CalendarScreenModel(
    private val database: CineGraphDatabase
) : ScreenModel {

    private val _state = MutableStateFlow(CinemaCalendarState())
    val state = _state.asStateFlow()

    private var dbJob: Job? = null
    private var allWatchedMovies: List<CalendarMovieItem> = emptyList()
    private var monthInitialized = false

    init {
        observeWatchedMovies()
    }

    fun goToPreviousMonth() {
        val current = _state.value
        val year = if (current.currentMonth == 1) current.currentYear - 1 else current.currentYear
        val month = if (current.currentMonth == 1) 12 else current.currentMonth - 1

        _state.value = _state.value.copy(
            currentYear = year,
            currentMonth = month
        )
        rebuildState(keepSelection = false)
    }

    fun goToNextMonth() {
        val current = _state.value
        val year = if (current.currentMonth == 12) current.currentYear + 1 else current.currentYear
        val month = if (current.currentMonth == 12) 1 else current.currentMonth + 1

        _state.value = _state.value.copy(
            currentYear = year,
            currentMonth = month
        )
        rebuildState(keepSelection = false)
    }

    fun selectDate(dateKey: String) {
        _state.value = _state.value.copy(selectedDateKey = dateKey)
        rebuildState(keepSelection = true)
    }

    private fun observeWatchedMovies() {
        dbJob?.cancel()

        dbJob = screenModelScope.launch {
            database.movieEntityQueries
                .getWatchedCollectionRows(::mapCollectionRow)
                .asFlow()
                .mapToList(Dispatchers.Default)
                .collect { rows ->
                    allWatchedMovies = rows.sortedByDescending { it.watchedDate.orEmpty() }

                    if (!monthInitialized) {
                        initializeMonthFromLatestWatch()
                    }

                    rebuildState(keepSelection = true)
                }
        }
    }

    private fun initializeMonthFromLatestWatch() {
        val latest = allWatchedMovies
            .mapNotNull { parseDate(it.watchedDate) }
            .maxWithOrNull(compareBy({ it.year }, { it.month }, { it.day }))

        val year = latest?.year ?: _state.value.currentYear
        val month = latest?.month ?: _state.value.currentMonth

        _state.value = _state.value.copy(
            currentYear = year,
            currentMonth = month
        )
        monthInitialized = true
    }

    private fun rebuildState(keepSelection: Boolean) {
        val current = _state.value
        val year = current.currentYear
        val month = current.currentMonth
        val monthPrefix = "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-"

        val monthMovies = allWatchedMovies
            .filter { it.watchedDate?.startsWith(monthPrefix) == true }
            .sortedByDescending { it.watchedDate.orEmpty() }

        val moviesByDate = monthMovies.groupBy { it.watchedDate.orEmpty() }

        val currentSelectionIsInVisibleMonth =
            current.selectedDateKey?.startsWith(monthPrefix) == true

        val resolvedSelectedDate = when {
            keepSelection && currentSelectionIsInVisibleMonth -> current.selectedDateKey
            current.selectedDateKey != null && currentSelectionIsInVisibleMonth -> current.selectedDateKey
            monthMovies.isNotEmpty() -> monthMovies.first().watchedDate
            else -> "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-1"
        }

        val days = buildMonthGrid(
            year = year,
            month = month,
            moviesByDate = moviesByDate,
            selectedDateKey = resolvedSelectedDate
        )

        _state.value = current.copy(
            isLoading = false,
            monthLabel = "${monthName(month)} $year",
            monthMovieCount = monthMovies.size,
            days = days,
            selectedDateKey = resolvedSelectedDate,
            selectedDateLabel = resolvedSelectedDate?.let(::formatLongDate),
            selectedMovies = resolvedSelectedDate?.let { moviesByDate[it].orEmpty() }.orEmpty()
        )
    }

    private fun buildMonthGrid(
        year: Int,
        month: Int,
        moviesByDate: Map<String, List<CalendarMovieItem>>,
        selectedDateKey: String?
    ): List<CalendarDayCell> {
        val result = mutableListOf<CalendarDayCell>()
        val daysInMonth = daysInMonth(year, month)
        val firstWeekday = firstDayOfWeekMondayFirst(year, month, 1)

        repeat(firstWeekday) {
            result += CalendarDayCell(
                dateKey = null,
                dayLabel = "",
                isCurrentMonth = false,
                movieCount = 0,
                isSelected = false,
                isWeekend = false,
                posterPath = null
            )
        }

        for (day in 1..daysInMonth) {
            val key = "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${
                day.toString().padStart(2, '0')
            }"
            val weekday = firstDayOfWeekMondayFirst(year, month, day)
            val movies = moviesByDate[key].orEmpty()

            result += CalendarDayCell(
                dateKey = key,
                dayLabel = day.toString(),
                isCurrentMonth = true,
                movieCount = movies.size,
                isSelected = selectedDateKey == key,
                isWeekend = weekday >= 5,
                posterPath = movies.firstOrNull()?.posterPath
            )
        }

        while (result.size % 7 != 0) {
            result += CalendarDayCell(
                dateKey = null,
                dayLabel = "",
                isCurrentMonth = false,
                movieCount = 0,
                isSelected = false,
                isWeekend = false,
                posterPath = null
            )
        }

        return result
    }

    private fun mapCollectionRow(
        id: Long,
        name: String,
        year: Long,
        posterPath: String?,
        tmdbId: String?,
        letterboxdUri: String?,
        imdbId: String?,
        tmdbVoteAverage: Double?,
        userRating: Double?,
        watchedDate: String?,
        watchlistDate: String?
    ): CalendarMovieItem = CalendarMovieItem(
        id = id.toInt(),
        name = name,
        year = year.toInt(),
        posterPath = posterPath,
        tmdbId = tmdbId?.toIntOrNull(),
        letterboxdUri = letterboxdUri,
        imdbId = imdbId,
        rating = userRating,
        watchedDate = watchedDate
    )

    private fun parseDate(value: String?): DateParts? {
        if (value.isNullOrBlank() || value.length < 10) return null

        val year = value.substring(0, 4).toIntOrNull() ?: return null
        val month = value.substring(5, 7).toIntOrNull() ?: return null
        val day = value.substring(8, 10).toIntOrNull() ?: return null

        return DateParts(year, month, day)
    }

    private fun formatLongDate(dateKey: String): String {
        val parts = parseDate(dateKey) ?: return dateKey
        return "${monthName(parts.month)} ${parts.day}, ${parts.year}"
    }

    private fun monthName(month: Int): String = when (month) {
        1 -> "January"
        2 -> "February"
        3 -> "March"
        4 -> "April"
        5 -> "May"
        6 -> "June"
        7 -> "July"
        8 -> "August"
        9 -> "September"
        10 -> "October"
        11 -> "November"
        12 -> "December"
        else -> "Unknown"
    }

    private fun daysInMonth(year: Int, month: Int): Int = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (isLeapYear(year)) 29 else 28
        else -> 30
    }

    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }

    private fun firstDayOfWeekMondayFirst(year: Int, month: Int, day: Int): Int {
        var y = year
        val t = intArrayOf(0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4)
        if (month < 3) y -= 1
        val sundayZero = (y + y / 4 - y / 100 + y / 400 + t[month - 1] + day) % 7
        return (sundayZero + 6) % 7
    }

    override fun onDispose() {
        dbJob?.cancel()
        super.onDispose()
    }
}