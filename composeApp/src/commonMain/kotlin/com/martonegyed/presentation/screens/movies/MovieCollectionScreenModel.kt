package com.martonegyed.presentation.screens.movies

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.martonegyed.core.util.mapCollectionRow
import com.martonegyed.data.database.CineGraphDatabase
import com.martonegyed.domain.model.Movie
import com.martonegyed.presentation.analytics.StatRange
import com.martonegyed.presentation.components.common.CollectionEntityType
import com.martonegyed.presentation.screens.movies.CollectionType.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.collections.emptyList
import kotlin.math.abs

enum class SortOption { DATE_WATCHED, RELEASE_YEAR, RATING, TMDB_RATING, NAME }

enum class MovieFilterType { GENRE, STUDIO, COUNTRY }

enum class MovieListType { WATCHED, WATCHLIST }

enum class CollectionType(val title: String) {
    LIBRARY("My Library"),
    WATCHLIST("Watchlist"),
    BY_ENTITY("Movies"),
    BY_DECADE("Movies in this decade"),
    BY_RATING("Movies with this rating"),
    BY_DUO("Movies with this Duo"),
    CACHED("Cached Movies")
}

data class MovieCollectionRow(
    val id: Int,
    val name: String,
    val year: Int,
    val posterPath: String?,
    val tmdbId: Int?,
    val letterboxdUri: String?,
    val imdbId: String?,
    val tmdbVoteAverage: Double?,
    val userRating: Double?,
    val watchedDate: String?,
    val watchlistDate: String?
) {
    fun toMovie(preferWatchlistDate: Boolean = false): Movie =
        Movie(
            id = id,
            name = name,
            year = year,
            posterPath = posterPath,
            tmdbId = tmdbId,
            letterboxdUri = letterboxdUri,
            imdbId = imdbId,
            rating = userRating,
            watchedDate = if (preferWatchlistDate) {
                watchlistDate ?: watchedDate
            } else {
                watchedDate ?: watchlistDate
            },
            tmdbVoteAverage = tmdbVoteAverage
        )
}

class MovieCollectionScreenModel(
    private val database: CineGraphDatabase
) : ScreenModel {
    private sealed interface CollectionRequest {
        object Library : CollectionRequest
        object Watchlist : CollectionRequest
        object Cached : CollectionRequest

        data class ByEntity(
            val entityType: CollectionEntityType,
            val entityName: String,
            val range: StatRange,
            val year: Int?,
            val month: Int?
        ) : CollectionRequest

        data class ByDecade(
            val decadeStart: Int,
            val range: StatRange = StatRange.ALL_TIME,
            val year: Int? = null,
            val month: Int? = null
        ) : CollectionRequest

        data class ByRating(
            val rating: Double,
            val range: StatRange = StatRange.ALL_TIME,
            val year: Int? = null,
            val month: Int? = null
        ) : CollectionRequest

        data class ByDuo(
            val firstName: String,
            val secondName: String,
            val firstJob: String? = null,
            val secondJob: String? = null,
            val range: StatRange = StatRange.ALL_TIME,
            val year: Int? = null,
            val month: Int? = null
        ) : CollectionRequest
    }

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _displayedMovies = MutableStateFlow<List<MovieCollectionRow>>(emptyList())
    val displayedMovies = _displayedMovies.asStateFlow()

    private val _isGrid = MutableStateFlow(true)
    val isGrid = _isGrid.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()


    private val _currentSort = MutableStateFlow(SortOption.DATE_WATCHED)
    val currentSort = _currentSort.asStateFlow()

    private val _isAscending = MutableStateFlow(false)
    val isAscending = _isAscending.asStateFlow()

    private val _currentListType = MutableStateFlow(MovieListType.WATCHED)
    val currentListType = _currentListType.asStateFlow()

    private var currentRequest: CollectionRequest = CollectionRequest.Library
    private var allMovies: List<MovieCollectionRow> = emptyList()
    private var dbJob: Job? = null


    init {
        println("🐛 DEBUG MODEL: ✨ ÚJ ScreenModel példány jött létre a memóriában!")
    }

    override fun onDispose() {
        println("🐛 DEBUG MODEL: 🗑️ ScreenModel megsemmisítve (Kuka)!")
        super.onDispose()
    }

    fun initCollection(type: CollectionType) {
        val request = when (type) {
            LIBRARY -> CollectionRequest.Library
            WATCHLIST -> CollectionRequest.Watchlist
            CACHED -> CollectionRequest.Cached
            BY_DECADE, BY_ENTITY, BY_RATING, BY_DUO ->
                error("Use the typed init function for $type")
        }
        setRequest(request)
    }

    fun initCollectionForDecade(
        decadeStart: Int,
        range: StatRange = StatRange.ALL_TIME,
        year: Int? = null,
        month: Int? = null
    ) {
        setRequest(
            CollectionRequest.ByDecade(
                decadeStart = decadeStart,
                range = range,
                year = year,
                month = month
            ),
            forceReload = true
        )
    }

    fun initCollectionForEntity(
        entityType: CollectionEntityType,
        entityName: String,
        range: StatRange,
        year: Int?,
        month: Int?
    ) {
        setRequest(
            CollectionRequest.ByEntity(
                entityType = entityType,
                entityName = entityName,
                range = range,
                year = year,
                month = month
            ),
            forceReload = true
        )
    }

    fun initCollectionForRating(
        rating: Double,
        range: StatRange = StatRange.ALL_TIME,
        year: Int? = null,
        month: Int? = null
    ) {
        setRequest(
            CollectionRequest.ByRating(
                rating = rating,
                range = range,
                year = year,
                month = month
            ),
            forceReload = true
        )
    }

    fun initCollectionForDuo(
        firstName: String,
        secondName: String,
        firstJob: String? = null,
        secondJob: String? = null,
        range: StatRange = StatRange.ALL_TIME,
        year: Int? = null,
        month: Int? = null
    ) {
        setRequest(
            CollectionRequest.ByDuo(
                firstName = firstName,
                secondName = secondName,
                firstJob = firstJob,
                secondJob = secondJob,
                range = range,
                year = year,
                month = month
            ),
            forceReload = true
        )
    }

    fun switchListType(listType: MovieListType) {
        if (_currentListType.value == listType) return
        _currentListType.value = listType

        if (supportsListToggle(currentRequest)) {
            reloadMovies()
        }
    }

    fun toggleGrid() {
        _isGrid.value = !_isGrid.value
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        applyFiltersAndSort()
    }

    fun updateSort(option: SortOption) {
        _currentSort.value = option
        _isAscending.value = option == SortOption.NAME
        applyFiltersAndSort()
    }

    fun toggleAscending(ascending: Boolean) {
        _isAscending.value = ascending
        applyFiltersAndSort()
    }

    private fun setRequest(
        request: CollectionRequest,
        forceReload: Boolean = false
    ) {
        if (!forceReload && currentRequest == request && allMovies.isNotEmpty()) return
        currentRequest = request
        reloadMovies()
    }

    private fun reloadMovies() {
        dbJob?.cancel()

        dbJob = screenModelScope.launch {
            when (val request = currentRequest) {
                CollectionRequest.Library,
                CollectionRequest.Cached,
                CollectionRequest.Watchlist -> observeReactiveCollection(request)


                is CollectionRequest.ByEntity,
                is CollectionRequest.ByDecade,
                is CollectionRequest.ByRating,
                is CollectionRequest.ByDuo -> loadSnapshotCollection(request)
            }
        }
    }

    private suspend fun observeReactiveCollection(request: CollectionRequest) {
        val query = when (request) {
            CollectionRequest.Library ->
                database.movieEntityQueries.getWatchedCollectionRows(::mapCollectionRow)

            CollectionRequest.Watchlist ->
                database.movieEntityQueries.getWatchlistCollectionRows(::mapCollectionRow)

            CollectionRequest.Cached ->
                database.movieEntityQueries.getCachedCollectionRows(::mapCollectionRow)

            else -> error("Reactive loader called with non-reactive request: $request")
        }

        query.asFlow()
            .mapToList(Dispatchers.Default)
            .collect { rows ->
                allMovies = rows
                applyFiltersAndSort()
            }
    }

    private fun loadSnapshotCollection(request: CollectionRequest) {
        _isLoading.value = true
        try {
            allMovies = when (request) {
                is CollectionRequest.ByEntity -> loadRowsForEntity(request)
                is CollectionRequest.ByDecade -> loadRowsForDecade(request)
                is CollectionRequest.ByRating -> loadRowsForRating(request)
                is CollectionRequest.ByDuo -> loadRowsForDuo(request)
                else -> emptyList()
            }
            applyFiltersAndSort()
        } finally {
            _isLoading.value = false
        }
    }

    private fun loadRowsForEntity(request: CollectionRequest.ByEntity): List<MovieCollectionRow> {
        val (startDate, endDate) = computeDateRange(request.range, request.year, request.month)
        val listTypeParam = _currentListType.value.name

        return when (request.entityType) {
            CollectionEntityType.DIRECTORS -> loadRowsByPerson(
                listType = listTypeParam,
                personName = request.entityName,
                job = "Director",
                startDate = startDate,
                endDate = endDate
            )

            CollectionEntityType.ACTORS -> loadRowsByPerson(
                listType = listTypeParam,
                personName = request.entityName,
                job = "Actor",
                startDate = startDate,
                endDate = endDate
            )

            CollectionEntityType.GENRES -> loadRowsByFilter(
                listType = listTypeParam,
                filterType = MovieFilterType.GENRE,
                filterName = request.entityName,
                startDate = startDate,
                endDate = endDate
            )

            CollectionEntityType.STUDIOS -> loadRowsByFilter(
                listType = listTypeParam,
                filterType = MovieFilterType.STUDIO,
                filterName = request.entityName,
                startDate = startDate,
                endDate = endDate
            )

            CollectionEntityType.COUNTRIES -> loadRowsByFilter(
                listType = listTypeParam,
                filterType = MovieFilterType.COUNTRY,
                filterName = request.entityName,
                startDate = startDate,
                endDate = endDate
            )

            CollectionEntityType.SCREENWRITERS -> loadRowsByPerson(
                listType = listTypeParam,
                personName = request.entityName,
                jobs = listOf("Screenplay", "Writer", "Story"),
                startDate = startDate,
                endDate = endDate
            )

            CollectionEntityType.CINEMATOGRAPHERS -> loadRowsByPerson(
                listType = listTypeParam,
                personName = request.entityName,
                job = "Director of Photography",
                startDate = startDate,
                endDate = endDate
            )
        }
    }

    private fun loadRowsForDecade(request: CollectionRequest.ByDecade): List<MovieCollectionRow> {
        val start = request.decadeStart
        val endExclusive = start + 10

        return loadRowsForCurrentList(
            range = request.range,
            year = request.year,
            month = request.month
        ).filter { row ->
            row.year in start until endExclusive
        }
    }

    private fun loadRowsForRating(request: CollectionRequest.ByRating): List<MovieCollectionRow> {
        val rows = loadRowsForCurrentList(request.range, request.year, request.month)
        return rows.filter { row ->
            row.userRating?.let { abs(it - request.rating) < 0.001 } == true
        }
    }

    private fun loadRowsForDuo(request: CollectionRequest.ByDuo): List<MovieCollectionRow> {
        val (startDate, endDate) = computeDateRange(request.range, request.year, request.month)

        return database.movieEntityQueries
            .getCollectionRowsByDuoAndDate(
                listType = _currentListType.value.name,
                firstName = request.firstName,
                secondName = request.secondName,
                firstJob = request.firstJob,
                secondJob = request.secondJob,
                startDate = startDate,
                endDate = endDate,
                mapper = ::mapCollectionRow
            )
            .executeAsList()
    }

    private fun loadRowsForCurrentList(
        range: StatRange = StatRange.ALL_TIME,
        year: Int? = null,
        month: Int? = null
    ): List<MovieCollectionRow> {
        return when (_currentListType.value) {
            MovieListType.WATCHED -> {
                val rows = database.movieEntityQueries
                    .getWatchedCollectionRows(::mapCollectionRow)
                    .executeAsList()

                val (startDate, endDate) = computeDateRange(range, year, month)
                if (startDate == null || endDate == null) rows
                else rows.filter { row ->
                    val d = row.watchedDate
                    d != null && d in startDate..endDate
                }
            }

            MovieListType.WATCHLIST ->
                database.movieEntityQueries
                    .getWatchlistCollectionRows(::mapCollectionRow)
                    .executeAsList()
        }
    }

    private fun loadRowsByPerson(
        listType: String,
        personName: String,
        job: String?,
        startDate: String?,
        endDate: String?
    ): List<MovieCollectionRow> =
        database.movieEntityQueries
            .getCollectionRowsByPersonAndDate(
                listType = listType,
                personName = personName,
                job = job,
                startDate = startDate,
                endDate = endDate,
                mapper = ::mapCollectionRow
            )
            .executeAsList()

    private fun loadRowsByPerson(
        listType: String,
        personName: String,
        jobs: List<String>,
        startDate: String?,
        endDate: String?
    ): List<MovieCollectionRow> =
        jobs.flatMap { job ->
            database.movieEntityQueries
                .getCollectionRowsByPersonAndDate(
                    listType = listType,
                    personName = personName,
                    job = job,
                    startDate = startDate,
                    endDate = endDate,
                    mapper = ::mapCollectionRow
                )
                .executeAsList()
        }.distinctBy { it.id }

    private fun loadRowsByFilter(
        listType: String,
        filterType: MovieFilterType,
        filterName: String,
        startDate: String?,
        endDate: String?
    ): List<MovieCollectionRow> =
        database.movieEntityQueries
            .getCollectionRowsByFilterAndDate(
                listType = listType,
                filterType = filterType.name,
                filterName = filterName,
                startDate = startDate,
                endDate = endDate,
                mapper = ::mapCollectionRow
            )
            .executeAsList()

    private fun computeDateRange(
        range: StatRange,
        year: Int?,
        month: Int?
    ): Pair<String?, String?> =
        when (range) {
            StatRange.ALL_TIME -> null to null
            StatRange.YEAR -> {
                val y = year ?: return null to null
                "$y-01-01" to "$y-12-31"
            }

            StatRange.MONTH -> {
                val y = year ?: return null to null
                val m = month ?: return null to null
                val mStr = m.toString().padStart(2, '0')
                "$y-$mStr-01" to "$y-$mStr-31"
            }
        }

    private fun supportsListToggle(request: CollectionRequest): Boolean =
        request is CollectionRequest.ByEntity ||
                request is CollectionRequest.ByDecade ||
                request is CollectionRequest.ByRating ||
                request is CollectionRequest.ByDuo

    private fun applyFiltersAndSort() {
        var filtered = allMovies
        if (_searchQuery.value.isNotEmpty()) {
            filtered = filtered.filter { it.name.contains(_searchQuery.value, ignoreCase = true) }
        }
        filtered = when (_currentSort.value) {
            SortOption.NAME -> if (_isAscending.value) filtered.sortedBy { it.name } else filtered.sortedByDescending { it.name }
            SortOption.RELEASE_YEAR -> if (_isAscending.value) filtered.sortedBy { it.year } else filtered.sortedByDescending { it.year }
            SortOption.RATING -> if (_isAscending.value) filtered.sortedBy {
                it.userRating ?: 0.0
            } else filtered.sortedByDescending { it.userRating ?: 0.0 }

            SortOption.TMDB_RATING ->
                if (_isAscending.value) filtered.sortedBy { it.tmdbVoteAverage ?: Double.NEGATIVE_INFINITY }
                else filtered.sortedByDescending { it.tmdbVoteAverage ?: Double.NEGATIVE_INFINITY }

            SortOption.DATE_WATCHED -> if (_isAscending.value) filtered.sortedBy { sortDateFor(it) } else filtered.sortedByDescending {
                sortDateFor(
                    it
                )
            }
        }
        _displayedMovies.value = filtered
    }

    private fun sortDateFor(row: MovieCollectionRow): String? =
        when (_currentListType.value) {
            MovieListType.WATCHED -> row.watchedDate
            MovieListType.WATCHLIST -> row.watchlistDate ?: row.watchedDate
        }
}