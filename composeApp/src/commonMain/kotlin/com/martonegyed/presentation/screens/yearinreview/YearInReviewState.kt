package com.martonegyed.presentation.screens.yearinreview

import com.martonegyed.domain.model.Movie
import com.martonegyed.presentation.analytics.AnalyticsSharedModels

data class YearInReviewState(
    val isLoading: Boolean = true,
    val selectedYear: Int? = null,
    val availableYears: List<Int> = emptyList(),

    val hero: YearHeroSummary = YearHeroSummary(),
    val mostWatchedDirector: AnalyticsSharedModels.AnalyticsEntityRow? = null,
    val mostWatchedActor: AnalyticsSharedModels.AnalyticsEntityRow? = null,

    val highestRatedByUser: List<Movie> = emptyList(),
    val highestRatedByTmdb: List<Movie> = emptyList(),
    val lowestRatedByTmdb: List<Movie> = emptyList(),

    val moviesByWeek: List<WeekCountRow> = emptyList(),
    val moviesByDay: List<WeekdayCountRow> = emptyList(),

    val milestones: List<MilestoneMovieRow> = emptyList(),

    val firstMovie: WatchedMovieRow? = null,
    val lastMovie: WatchedMovieRow? = null,

    val genreRankingByCount: List<RankRow> = emptyList(),
    val genreRankingByAverageRating: List<RankRow> = emptyList(),

    val centuryRankingByCount: List<RankRow> = emptyList(),
    val centuryRankingByAverageRating: List<RankRow> = emptyList(),

    val languageRankingByCount: List<RankRow> = emptyList(),
    val languageRankingByAverageRating: List<RankRow> = emptyList(),

    val newVsOld: List<PieSliceRow> = emptyList(),
    val rewatchesVsFirstTime: List<PieSliceRow> = emptyList(),

    val actorRankingByCount: List<RankRow> = emptyList(),
    val actorRankingByAverageRating: List<RankRow> = emptyList(),

    val directorRankingByCount: List<RankRow> = emptyList(),
    val directorRankingByAverageRating: List<RankRow> = emptyList(),

    val countryRankingByCount: List<RankRow> = emptyList(),
    val countryRankingByAverageRating: List<RankRow> = emptyList(),

    val mapCountries: List<AnalyticsSharedModels.MapCountryRow> = emptyList()
)

data class MilestoneMovieRow(
    val milestone: Int,
    val movie: Movie,
    val watchedDate: String
)

data class YearHeroSummary(
    val filmCount: Int = 0,
    val hoursWatched: Int = 0,
    val averageRating: Double? = null,
    val totalRevenue: Long = 0L
)

data class PersonWatchRow(
    val name: String,
    val count: Int,
    val profilePath: String? = null
)

data class WeekCountRow(
    val week: Int,
    val count: Int
)

data class WeekdayCountRow(
    val label: String,
    val count: Int
)

data class WatchedMovieRow(
    val movie: Movie,
    val watchedDate: String
)

data class RankRow(
    val label: String,
    val count: Int,
    val averageRating: Double? = null
)

data class PieSliceRow(
    val label: String,
    val count: Int
)

fun emptyYearState(state: YearInReviewState): YearInReviewState =
    state.copy(
        isLoading = false,
        hero = YearHeroSummary(),
        highestRatedByUser = emptyList(),
        highestRatedByTmdb = emptyList(),
        lowestRatedByTmdb = emptyList(),
        moviesByWeek = emptyList(),
        moviesByDay = emptyList(),
        firstMovie = null,
        lastMovie = null,
        genreRankingByCount = emptyList(),
        genreRankingByAverageRating = emptyList(),
        centuryRankingByCount = emptyList(),
        centuryRankingByAverageRating = emptyList(),
        languageRankingByCount = emptyList(),
        languageRankingByAverageRating = emptyList(),
        newVsOld = emptyList(),
        rewatchesVsFirstTime = emptyList(),
        actorRankingByCount = emptyList(),
        actorRankingByAverageRating = emptyList(),
        directorRankingByCount = emptyList(),
        directorRankingByAverageRating = emptyList(),
        countryRankingByCount = emptyList(),
        countryRankingByAverageRating = emptyList(),
        mostWatchedActor = null,
        mostWatchedDirector = null,
        mapCountries = emptyList()
    )