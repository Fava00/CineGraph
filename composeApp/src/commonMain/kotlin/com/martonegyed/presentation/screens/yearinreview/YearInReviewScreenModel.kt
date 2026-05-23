package com.martonegyed.presentation.screens.yearinreview

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.martonegyed.core.ui.languageDisplayName
import com.martonegyed.domain.model.Movie
import com.martonegyed.presentation.analytics.AnalyticsEntityAggregator
import com.martonegyed.presentation.analytics.AnalyticsRepository
import com.martonegyed.presentation.analytics.AnalyticsSharedModels
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlin.collections.emptyList

class YearInReviewScreenModel(
    private val analyticsRepository: AnalyticsRepository
) : ScreenModel {

    private val _state = MutableStateFlow(YearInReviewState())
    val state = _state.asStateFlow()

    private var allWatchedMovies: List<Movie> = emptyList()

    init {
        load()
    }


    fun setYear(year: Int) {
        if (_state.value.selectedYear == year) return
        _state.value = compute(
            movies = allWatchedMovies,
            state = _state.value.copy(selectedYear = year, isLoading = true)
        )
    }

    private fun load(forceRefresh: Boolean = false) {
        screenModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            val snapshot = analyticsRepository.getSnapshot(forceRefresh)
            allWatchedMovies = snapshot.movies

            val resolvedYear = analyticsRepository.normalizeYear(
                selectedYear = _state.value.selectedYear,
                availableYears = snapshot.availableYears
            )

            _state.value = compute(
                movies = allWatchedMovies,
                state = YearInReviewState(
                    isLoading = false,
                    selectedYear = resolvedYear,
                    availableYears = snapshot.availableYears
                )
            )
        }
    }

    private fun compute(
        movies: List<Movie>,
        state: YearInReviewState
    ): YearInReviewState {
        val year = state.selectedYear ?: return emptyYearState(state)

        val filtered = analyticsRepository.filterMoviesByYear(movies, year)
        if (filtered.isEmpty()) {
            return emptyYearState(state)
        }

        val totalMinutes = filtered.sumOf { it.runtimeMinutes ?: 0 }
        val ratings = filtered.mapNotNull { it.rating }
        val averageRating = ratings.takeIf { it.isNotEmpty() }?.average()


        val highestRatedByUser = filtered
            .filter { (it.rating ?: 0.0) > 0.0 }
            .sortedWith(
                compareByDescending<Movie> { it.rating ?: 0.0 }
                    .thenByDescending { it.watchedDate ?: "" }
            )
            .take(10)

        val highestRatedByTmdb = filtered
            .filter { (it.tmdbVoteAverage ?: 0.0) > 0.0 }
            .sortedByDescending { it.tmdbVoteAverage ?: 0.0 }
            .take(10)

        val lowestRatedByTmdb = filtered
            .filter { (it.tmdbVoteAverage ?: 0.0) > 0.0 }
            .sortedBy { it.tmdbVoteAverage ?: Double.MAX_VALUE }
            .take(10)

        val watchedOrdered = filtered
            .filter { !it.watchedDate.isNullOrBlank() }
            .sortedBy { it.watchedDate }

        val firstMovie = watchedOrdered.firstOrNull()?.let {
            WatchedMovieRow(movie = it, watchedDate = it.watchedDate.orEmpty())
        }

        val lastMovie = watchedOrdered.lastOrNull()?.let {
            WatchedMovieRow(movie = it, watchedDate = it.watchedDate.orEmpty())
        }

        val parsedDates = filtered.mapNotNull { movie ->
            val raw = movie.watchedDate ?: return@mapNotNull null
            runCatching { LocalDate.parse(raw) }.getOrNull()
        }

        val orderedDays = listOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY
        )

        val moviesByDay = orderedDays.map { day ->
            WeekdayCountRow(
                label = day.shortLabel(),
                count = parsedDates.count { it.dayOfWeek == day }
            )
        }

        val moviesByWeek = parsedDates
            .groupingBy { ((it.dayOfYear - 1) / 7) + 1 }
            .eachCount()
            .entries
            .sortedBy { it.key }
            .map { WeekCountRow(week = it.key, count = it.value) }

        val milestoneTargets = buildList {
            add(1)
            var value = 50
            while (value <= watchedOrdered.size) {
                add(value)
                value += 50
            }
        }

        val milestones = milestoneTargets.mapNotNull { target ->
            watchedOrdered.getOrNull(target - 1)?.let { movie ->
                MilestoneMovieRow(
                    milestone = target,
                    movie = movie,
                    watchedDate = movie.watchedDate.orEmpty()
                )
            }
        }

        val actorRows = AnalyticsEntityAggregator.aggregate(
            movies = filtered,
            selector = { it.actors.orEmpty() },
            nameOf = { it.name },
            photoOf = { it.profilePath }
        )

        val directorRows = AnalyticsEntityAggregator.aggregate(
            movies = filtered,
            selector = { it.crew.orEmpty().filter { person -> person.job == "Director" } },
            nameOf = { it.name },
            photoOf = { it.profilePath }
        )

        val genreRows = AnalyticsEntityAggregator.aggregate(
            movies = filtered,
            selector = { it.genres.orEmpty() },
            nameOf = { it }
        )

        val languageRows = AnalyticsEntityAggregator.aggregate(
            movies = filtered,
            selector = { listOfNotNull(it.originalLanguage) },
            nameOf = { code -> languageDisplayName(code) }
        )

        val countryRows = AnalyticsEntityAggregator.aggregate(
            movies = filtered,
            selector = { it.productionCountries.orEmpty() },
            nameOf = { it }
        )

        val releasedThisYear = filtered.count { it.year == year }
        val olderTitles = filtered.count { it.year in 1 until year }

        return state.copy(
            isLoading = false,
            hero = YearHeroSummary(
                filmCount = filtered.size,
                hoursWatched = totalMinutes / 60,
                averageRating = averageRating,
                totalRevenue = filtered.sumOf { it.revenue ?: 0L }
            ),
            mostWatchedDirector = AnalyticsEntityAggregator.sort(
                directorRows,
                AnalyticsSharedModels.AnalyticsEntityMetric.COUNT
            ).firstOrNull(),
            mostWatchedActor = AnalyticsEntityAggregator.sort(
                actorRows,
                AnalyticsSharedModels.AnalyticsEntityMetric.COUNT
            ).firstOrNull(),
            highestRatedByUser = highestRatedByUser,
            highestRatedByTmdb = highestRatedByTmdb,
            lowestRatedByTmdb = lowestRatedByTmdb,
            moviesByWeek = moviesByWeek,
            moviesByDay = moviesByDay,
            milestones = milestones,
            firstMovie = firstMovie,
            lastMovie = lastMovie,
            genreRankingByCount = AnalyticsEntityAggregator
                .sort(genreRows, AnalyticsSharedModels.AnalyticsEntityMetric.COUNT)
                .take(20)
                .map { RankRow(label = it.name, count = it.count, averageRating = it.avgRating) },
            genreRankingByAverageRating = AnalyticsEntityAggregator
                .sort(genreRows, AnalyticsSharedModels.AnalyticsEntityMetric.AVG_RATING)
                .take(20)
                .map { RankRow(label = it.name, count = it.count, averageRating = it.avgRating) },
            /*centuryRankingByCount = AnalyticsEntityAggregator
                .sort(centuryRows, AnalyticsSharedModels.AnalyticsEntityMetric.AVG_RATING)
                .take(20)
                .map { RankRow(label = it.name, count = it.count, averageRating = it.avgRating) },
            centuryRankingByAverageRating = AnalyticsEntityAggregator
                .sort(centuryRows, AnalyticsSharedModels.AnalyticsEntityMetric.AVG_RATING)
                .take(20)
                .map { RankRow(label = it.name, count = it.count, averageRating = it.avgRating) },*/
            languageRankingByCount = AnalyticsEntityAggregator
                .sort(languageRows, AnalyticsSharedModels.AnalyticsEntityMetric.COUNT)
                .take(20)
                .map { RankRow(label = it.name, count = it.count, averageRating = it.avgRating) },
            languageRankingByAverageRating = AnalyticsEntityAggregator
                .sort(languageRows, AnalyticsSharedModels.AnalyticsEntityMetric.AVG_RATING)
                .take(20)
                .map { RankRow(label = it.name, count = it.count, averageRating = it.avgRating) },
            newVsOld = listOf(
                PieSliceRow(label = "Released in $year", count = releasedThisYear),
                PieSliceRow(label = "Older titles", count = olderTitles)
            ).filter { it.count > 0 },
            rewatchesVsFirstTime = listOf(
                PieSliceRow(label = "First-time", count = filtered.count { !it.isRewatch }),
                PieSliceRow(label = "Rewatch", count = filtered.count { it.isRewatch })
            ).filter { it.count > 0 },
            actorRankingByCount = AnalyticsEntityAggregator
                .sort(actorRows, AnalyticsSharedModels.AnalyticsEntityMetric.COUNT)
                .take(20)
                .map { RankRow(label = it.name, count = it.count, averageRating = null) },

            actorRankingByAverageRating = AnalyticsEntityAggregator
                .sort(actorRows, AnalyticsSharedModels.AnalyticsEntityMetric.AVG_RATING)
                .take(20)
                .map { RankRow(label = it.name, count = it.count, averageRating = it.avgRating) },
            directorRankingByCount = AnalyticsEntityAggregator
                .sort(directorRows, AnalyticsSharedModels.AnalyticsEntityMetric.COUNT)
                .take(20)
                .map { RankRow(label = it.name, count = it.count, averageRating = it.avgRating) },
            directorRankingByAverageRating = AnalyticsEntityAggregator
                .sort(directorRows, AnalyticsSharedModels.AnalyticsEntityMetric.AVG_RATING)
                .take(20)
                .map { RankRow(label = it.name, count = it.count, averageRating = it.avgRating) },
            countryRankingByCount = AnalyticsEntityAggregator
                .sort(countryRows, AnalyticsSharedModels.AnalyticsEntityMetric.COUNT)
                .take(20)
                .map { RankRow(label = it.name, count = it.count, averageRating = it.avgRating) },
            countryRankingByAverageRating = AnalyticsEntityAggregator
                .sort(countryRows, AnalyticsSharedModels.AnalyticsEntityMetric.AVG_RATING)
                .take(20)
                .map { RankRow(label = it.name, count = it.count, averageRating = it.avgRating) },
            mapCountries = analyticsRepository.computeMapCountries(filtered)
        )
    }

    private fun DayOfWeek.shortLabel(): String = when (this) {
        DayOfWeek.MONDAY -> "Mon"
        DayOfWeek.TUESDAY -> "Tue"
        DayOfWeek.WEDNESDAY -> "Wed"
        DayOfWeek.THURSDAY -> "Thu"
        DayOfWeek.FRIDAY -> "Fri"
        DayOfWeek.SATURDAY -> "Sat"
        DayOfWeek.SUNDAY -> "Sun"
    }

    private fun centuryLabel(year: Int): String {
        val century = ((year - 1) / 100) + 1
        return "${century}th century"
    }
}