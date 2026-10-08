package com.martonegyed.domain.model

enum class CsvImportType(val label: String, val marksWatched: Boolean = false) {
    DIARY("Diary", true), REVIEWS("Reviews", true), WATCHED("Watched", true),
    RATINGS("Ratings", true), WATCHLIST("Watchlist"), LISTS("Lists");

    companion object {
        fun from(value: String): CsvImportType = entries.firstOrNull { it.name.equals(value, true) }
            ?: throw IllegalArgumentException("Unsupported CSV type: $value")
    }
}

data class StagedLog(
    val source: CsvImportType,
    val watchedDate: String? = null,
    val loggedDate: String? = null,
    val ratedDate: String? = null,
    val rating: Double? = null,
    val review: String? = null,
    val isRewatch: Boolean = false
)

data class StagedMovie(
    val name: String,
    val year: Int = 0,
    val letterboxdUri: String? = null,
    val imdbId: String? = null,
    val originalTitle: String? = null,
    val imdbUrl: String? = null,
    val addedDate: String? = null,
    val isWatched: Boolean = false,
    val inWatchlist: Boolean = false,
    val logs: List<StagedLog> = emptyList()
)
