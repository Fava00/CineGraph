package com.martonegyed.presentation.screens.statistics

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.martonegyed.data.database.CineGraphDatabase
import com.martonegyed.domain.model.Movie
import com.martonegyed.domain.model.Person
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

enum class StatEntityType { DIRECTORS, ACTORS, GENRES, STUDIOS, COUNTRIES }
enum class StatMetric { COUNT, AVG_RATING, WATCH_TIME, REVENUE }
enum class StatRange { ALL_TIME, YEAR, MONTH }

data class EntityRow(
    val name: String,
    val count: Int,
    val avgRating: Double?,
    val totalMinutes: Int,
    val totalRevenue: Long,
    val photoPath: String? = null,
    val initials: String = ""
)

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

    val rows: List<EntityRow> = emptyList()
)

object StatisticsCache {
    var lastState: StatisticsState? = null
    var allMovies: List<Movie>? = null
    var rowsCache: MutableMap<String, Map<StatEntityType, List<EntityRow>>> = mutableMapOf()
    var lastUpdatedMillis: Long? = null

    @OptIn(ExperimentalTime::class)
    fun isFresh(maxAgeMillis: Long = 5 * 60 * 1000): Boolean {
        val ts = lastUpdatedMillis ?: return false
        return (Clock.System.now().toEpochMilliseconds() - ts) <= maxAgeMillis
    }
}

@ExperimentalTime
class StatisticsScreenModel(
    private val database: CineGraphDatabase
) : ScreenModel {

    private val _state = MutableStateFlow(StatisticsState())
    val state = _state.asStateFlow()

    private var allWatchedMovies: List<Movie> = emptyList()
    private var peopleByMovieId: Map<Long, List<Person>> = emptyMap()
    private var rowsCache = mutableMapOf<String, Map<StatEntityType, List<EntityRow>>>()

    init {
        val cachedState = StatisticsCache.lastState
        val cachedMovies = StatisticsCache.allMovies

        if (cachedState != null && cachedMovies != null && StatisticsCache.isFresh()) {
            allWatchedMovies = cachedMovies
            rowsCache = StatisticsCache.rowsCache
            _state.value = cachedState.copy(isLoading = false)
        } else {
            loadStatistics()
        }
    }

    fun refresh() {
        loadStatistics()
    }

    fun refreshIfStale() {
        if (!StatisticsCache.isFresh()) {
            loadStatistics()
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun loadStatistics() {
        screenModelScope.launch {
            val baseMovies = withContext(Dispatchers.Default) {
                database.movieEntityQueries
                    .getWatchedMoviesForList(::mapBaseMovie)
                    .executeAsList()
            }

            val heroMinutes = baseMovies.sumOf { it.runtimeMinutes ?: 0 }
            val heroRatings = baseMovies.mapNotNull { it.rating }
            val heroAvg = if (heroRatings.isNotEmpty()) heroRatings.average() else 0.0
            val heroRevenue = baseMovies.sumOf { it.revenue ?: 0L }

            val current = _state.value
            _state.value = current.copy(
                isLoading = true,
                totalMovies = baseMovies.size,
                totalHours = heroMinutes / 60.0,
                averageRating = heroAvg,
                totalRevenue = heroRevenue
            )

            if (baseMovies.isEmpty()) {
                val emptyState = _state.value.copy(
                    isLoading = false,
                    rows = emptyList()
                )
                _state.value = emptyState
                StatisticsCache.lastState = emptyState
                StatisticsCache.allMovies = emptyList()
                StatisticsCache.rowsCache = mutableMapOf()
                StatisticsCache.lastUpdatedMillis = Clock.System.now().toEpochMilliseconds()
                return@launch
            }
            val watchedIds = baseMovies.map { it.id.toLong() }

            peopleByMovieId = withContext(Dispatchers.Default) {
                database.movieEntityQueries
                    .getPersonsForMovies(watchedIds)
                    .executeAsList()
                    .groupBy { it.movieId }
                    .mapValues { (_, rows) ->
                        rows.map { row ->
                            Person(
                                name = row.name,
                                job = row.job,
                                character = row.character,
                                profilePath = row.profilePath
                            )
                        }
                    }
            }

            allWatchedMovies = withContext(Dispatchers.Default) {
                database.movieEntityQueries
                    .getWatchedMovies(::mapRow)
                    .executeAsList()
            }

            rowsCache.clear()
            val afterRank = compute(allWatchedMovies, _state.value)
            val finalState = afterRank.copy(isLoading = false)
            _state.value = finalState

            StatisticsCache.lastState = finalState
            StatisticsCache.allMovies = allWatchedMovies
            StatisticsCache.rowsCache = rowsCache
            StatisticsCache.lastUpdatedMillis = Clock.System.now().toEpochMilliseconds()
        }
    }

    fun setEntityType(type: StatEntityType) {
        val newState = _state.value.copy(entityType = type)
        val finalState = compute(allWatchedMovies, newState)
        _state.value = finalState

        StatisticsCache.lastState = finalState
        StatisticsCache.allMovies = allWatchedMovies
        StatisticsCache.rowsCache = rowsCache
        StatisticsCache.lastUpdatedMillis = Clock.System.now().toEpochMilliseconds()
    }

    fun setMetric(metric: StatMetric) {
        val newState = _state.value.copy(metric = metric)
        val finalState = compute(allWatchedMovies, newState)
        _state.value = finalState

        StatisticsCache.lastState = finalState
        StatisticsCache.allMovies = allWatchedMovies
        StatisticsCache.rowsCache = rowsCache
        StatisticsCache.lastUpdatedMillis = Clock.System.now().toEpochMilliseconds()
    }

    fun setRange(range: StatRange, year: Int? = null, month: Int? = null) {
        val newState = _state.value.copy(
            range = range,
            selectedYear = year,
            selectedMonth = month
        )
        val finalState = compute(allWatchedMovies, newState)
        _state.value = finalState

        StatisticsCache.lastState = finalState
        StatisticsCache.allMovies = allWatchedMovies
        StatisticsCache.rowsCache = rowsCache
        StatisticsCache.lastUpdatedMillis = Clock.System.now().toEpochMilliseconds()
    }

    private fun compute(movies: List<Movie>, state: StatisticsState): StatisticsState {
        val filtered = filterMoviesByRange(movies, state)

        if (filtered.isEmpty()) {
            return state.copy(
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

        val baseRows = getBaseRows(filtered, state)

        val metricFiltered = when (state.metric) {
            StatMetric.COUNT -> baseRows
            StatMetric.AVG_RATING -> baseRows.filter { it.count >= 3 && it.avgRating != null }
            StatMetric.WATCH_TIME -> baseRows
            StatMetric.REVENUE -> baseRows
        }

        val sortedRows = when (state.metric) {
            StatMetric.COUNT -> metricFiltered.sortedByDescending { it.count }
            StatMetric.AVG_RATING -> metricFiltered.sortedWith(
                compareByDescending<EntityRow> { it.avgRating ?: Double.NEGATIVE_INFINITY }
                    .thenByDescending { it.count }
            )

            StatMetric.WATCH_TIME -> metricFiltered.sortedWith(
                compareByDescending<EntityRow> { it.totalMinutes }
                    .thenByDescending { it.count }
            )

            StatMetric.REVENUE -> metricFiltered.sortedWith(
                compareByDescending<EntityRow> { it.totalRevenue }
                    .thenByDescending { it.count }
            )
        }.take(100)

        return state.copy(
            isLoading = false,
            totalMovies = filtered.size,
            totalHours = totalMinutes / 60.0,
            averageRating = averageRating,
            totalRevenue = totalRevenue,
            rows = sortedRows
        )
    }

    private fun filterMoviesByRange(
        movies: List<Movie>,
        state: StatisticsState
    ): List<Movie> {
        return when (state.range) {
            StatRange.ALL_TIME -> movies
            StatRange.YEAR -> movies.filter {
                it.watchedDate?.take(4)?.toIntOrNull() == state.selectedYear
            }

            StatRange.MONTH -> movies.filter {
                val year = it.watchedDate?.take(4)?.toIntOrNull()
                val month = it.watchedDate?.drop(5)?.take(2)?.toIntOrNull()
                year == state.selectedYear && month == state.selectedMonth
            }
        }
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
            StatEntityType.DIRECTORS -> aggregateByPersons(filtered) { it.directors }
            StatEntityType.ACTORS -> aggregateByPersons(filtered) { it.actors ?: emptyList() }
            StatEntityType.GENRES -> aggregateByStrings(filtered) { it.genres ?: emptyList() }
            StatEntityType.STUDIOS -> aggregateByStrings(filtered) { it.studios ?: emptyList() }
            StatEntityType.COUNTRIES -> aggregateByStrings(filtered) { it.productionCountries ?: emptyList() }
        }

        rowsCache[key] = (cachedForRange ?: emptyMap()) + (state.entityType to rows)
        return rows
    }

    private fun aggregateByPersons(
        movies: List<Movie>,
        selector: (Movie) -> List<Person>
    ): List<EntityRow> {
        data class Acc(
            var count: Int = 0,
            var sumRating: Double = 0.0,
            var ratingCount: Int = 0,
            var minutes: Int = 0,
            var revenue: Long = 0L,
            var photoPath: String? = null
        )

        val map = mutableMapOf<String, Acc>()

        movies.forEach { movie ->
            val rating = movie.rating
            val minutes = movie.runtimeMinutes ?: 0
            val revenue = movie.revenue ?: 0L

            selector(movie)
                .distinctBy { it.name }
                .forEach { person ->
                    val name = person.name ?: return@forEach
                    val acc = map.getOrPut(name) { Acc() }

                    acc.count++
                    if (rating != null) {
                        acc.sumRating += rating
                        acc.ratingCount++
                    }
                    acc.minutes += minutes
                    acc.revenue += revenue

                    if (acc.photoPath == null && !person.profilePath.isNullOrBlank()) {
                        acc.photoPath = person.profilePath
                    }
                }
        }

        return map.map { (name, acc) ->
            EntityRow(
                name = name,
                count = acc.count,
                avgRating = if (acc.ratingCount > 0) acc.sumRating / acc.ratingCount else null,
                totalMinutes = acc.minutes,
                totalRevenue = acc.revenue,
                photoPath = acc.photoPath,
                initials = name
                    .split(" ")
                    .filter { it.isNotBlank() }
                    .take(2)
                    .joinToString("") { it.first().uppercase() }
            )
        }
    }

    private fun aggregateByStrings(
        movies: List<Movie>,
        selector: (Movie) -> List<String>
    ): List<EntityRow> {
        data class Acc(
            var count: Int = 0,
            var sumRating: Double = 0.0,
            var ratingCount: Int = 0,
            var minutes: Int = 0,
            var revenue: Long = 0L
        )

        val map = mutableMapOf<String, Acc>()

        movies.forEach { movie ->
            val rating = movie.rating
            val minutes = movie.runtimeMinutes ?: 0
            val revenue = movie.revenue ?: 0L

            selector(movie)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .forEach { value ->
                    val acc = map.getOrPut(value) { Acc() }
                    acc.count++
                    if (rating != null) {
                        acc.sumRating += rating
                        acc.ratingCount++
                    }
                    acc.minutes += minutes
                    acc.revenue += revenue
                }
        }

        return map.map { (name, acc) ->
            EntityRow(
                name = name,
                count = acc.count,
                avgRating = if (acc.ratingCount > 0) acc.sumRating / acc.ratingCount else null,
                totalMinutes = acc.minutes,
                totalRevenue = acc.revenue
            )
        }
    }

    private fun mapBaseMovie(
        id: Long,
        name: String,
        year: Long,
        posterPath: String?,
        tmdbId: String?,
        letterboxdUri: String?,
        imdbId: String?,
        rating: Double?,
        watchedDate: String?,
        isRewatch: Long
    ): Movie = Movie(
        id = id.toInt(),
        name = name,
        year = year.toInt(),
        posterPath = posterPath,
        tmdbId = tmdbId?.toIntOrNull(),
        letterboxdUri = letterboxdUri,
        imdbId = imdbId,
        rating = rating,
        watchedDate = watchedDate,
        isRewatch = isRewatch == 1L
    )

    private fun mapRow(
        id: Long,
        name: String,
        year: Long,
        letterboxdUri: String?,
        imdbId: String?,
        isWatched: Long,
        inWatchlist: Long,
        isCached: Long,
        posterPath: String?,
        backdropPath: String?,
        overview: String?,
        runtimeMinutes: Long?,
        tmdbId: String?,
        tagline: String?,
        originalTitle: String?,
        originalLanguage: String?,
        budget: Long?,
        revenue: Long?,
        genres: String?,
        hungarianTitle: String?,
        tmdbPopularity: Double?,
        tmdbVoteAverage: Double?,
        tmdbVoteCount: Long?,
        collectionName: String?,
        trailerKey: String?,
        mpaaRating: String?,
        addedDate: String?,
        studios: String?,
        productionCountries: String?,
        spokenLanguages: String?,
        similarMovies: String?,
        tmdbReviews: String?,
        rating: Double?,
        watchedDate: String?,
        isRewatch: Long
    ): Movie {
        val persons = peopleByMovieId[id].orEmpty()

        return Movie(
            id = id.toInt(),
            name = name,
            year = year.toInt(),
            rating = rating,
            watchedDate = watchedDate,
            isRewatch = isRewatch == 1L,
            runtimeMinutes = runtimeMinutes?.toInt(),
            originalLanguage = originalLanguage,
            revenue = revenue,
            genres = genres?.split(", ")?.filter { it.isNotBlank() },
            studios = studios?.split(", ")?.filter { it.isNotBlank() },
            productionCountries = productionCountries?.split(", ")?.filter { it.isNotBlank() },
            actors = persons.filter { it.job == "Actor" }.map {
                Person(
                    name = it.name,
                    profilePath = it.profilePath
                )
            },
            crew = persons.filter { it.job != "Actor" }.map {
                Person(
                    name = it.name,
                    job = it.job,
                    profilePath = it.profilePath
                )
            },
            letterboxdUri = letterboxdUri
        )
    }
}