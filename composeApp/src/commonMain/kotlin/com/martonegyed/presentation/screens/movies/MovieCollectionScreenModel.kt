package com.martonegyed.presentation.screens.movies

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.martonegyed.data.database.CineGraphDatabase
import com.martonegyed.domain.model.Movie
import com.martonegyed.presentation.screens.movies.CollectionType.*
import com.martonegyed.presentation.screens.statistics.StatEntityType
import com.martonegyed.presentation.screens.statistics.StatRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.collections.emptyList

enum class SortOption { DATE_WATCHED, RELEASE_YEAR, RATING, NAME }

enum class MovieFilterType { GENRE, STUDIO, COUNTRY, NONE }

enum class MovieListType { WATCHED, WATCHLIST }

enum class CollectionType(val title: String) {
    LIBRARY("My Library"),
    WATCHLIST("Watchlist"),
    BY_ENTITY("Movies")
    //CACHED("Cached Movies") TODO
}

class MovieCollectionScreenModel(
    private val database: CineGraphDatabase
) : ScreenModel {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _displayedMovies = MutableStateFlow<List<Movie>>(emptyList())
    val displayedMovies = _displayedMovies.asStateFlow()

    private val _isGrid = MutableStateFlow(true)
    val isGrid = _isGrid.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()


    private val _currentSort = MutableStateFlow(SortOption.DATE_WATCHED)
    val currentSort = _currentSort.asStateFlow()

    private val _isAscending = MutableStateFlow(false)
    val isAscending = _isAscending.asStateFlow()
    private var currentType: CollectionType = LIBRARY
    private val _currentListType = MutableStateFlow(MovieListType.WATCHED)
    val currentListType = _currentListType.asStateFlow()

    private var allMovies: List<Movie> = emptyList()
    private var dbJob: Job? = null

    var entityType: StatEntityType? = null
        private set
    var entityName: String? = null
        private set
    var range: StatRange? = null
        private set
    var rangeYear: Int? = null
        private set
    var rangeMonth: Int? = null
        private set


    init {
        println("🐛 DEBUG MODEL: ✨ ÚJ ScreenModel példány jött létre a memóriában!")
    }

    override fun onDispose() {
        println("🐛 DEBUG MODEL: 🗑️ ScreenModel megsemmisítve (Kuka)!")
        super.onDispose()
    }

    fun initCollection(type: CollectionType) {
        println("🐛 DEBUG MODEL: initCollection hívva! Kért: $type | Jelenlegi: $currentType | DB méret eddig: ${allMovies.size}")

        if (currentType == type && allMovies.isNotEmpty()) {
            println("🐛 DEBUG MODEL: ⏭️ Betöltés kihagyva, mert a típus megegyezik és van adat.")
            return
        }

        currentType = type
        dbJob?.cancel()
        println("🐛 DEBUG MODEL: 🔄 Korábbi lekérdezés leállítva, új lekérdezés indul: $currentType")
        loadMoviesFromDatabase()
    }

    fun initCollectionForEntity(
        entityType: StatEntityType,
        entityName: String,
        range: StatRange,
        year: Int?,
        month: Int?
    ) {
        currentType = BY_ENTITY
        _isLoading.value = true
        this.entityType = entityType
        this.entityName = entityName
        this.range = range
        this.rangeYear = year
        this.rangeMonth = month

        dbJob?.cancel()
        loadMoviesFromDatabase()
    }

    fun switchListType(listType: MovieListType) {
        if (_currentListType.value == listType) return
        println("🐛 switchListType: ${_currentListType.value} -> $listType")
        _currentListType.value = listType
        dbJob?.cancel()
        loadMoviesFromDatabase()
    }


    private fun loadMoviesFromDatabase() {
        dbJob = screenModelScope.launch {
            println(
                "🐛 loadMoviesFromDatabase currentType=$currentType " +
                        "listType=${_currentListType.value}"
            )
            when (currentType) {
                BY_ENTITY -> {
                    _isLoading.value = true
                    allMovies = loadMoviesForEntityOnce()
                    applyFiltersAndSort()
                    _isLoading.value = false
                }

                else -> {
                    val query = when (currentType) {
                        LIBRARY ->
                            database.movieEntityQueries.getWatchedMoviesForList(::mapToListItem)

                        WATCHLIST ->
                            database.movieEntityQueries.getWatchlistMoviesForList(::mapToListItem)

                        BY_ENTITY -> TODO()
                    }
                    query.asFlow()
                        .mapToList(Dispatchers.Default)
                        .collect { movies ->
                            allMovies = movies
                            applyFiltersAndSort()
                        }
                }
            }

        }
    }

    private fun loadMoviesForEntityOnce(): List<Movie> {
        val (startDate, endDate) = computeDateRange(range ?: StatRange.ALL_TIME, rangeYear, rangeMonth)

        val (filterType, filterName) = when (entityType) {
            StatEntityType.GENRES -> MovieFilterType.GENRE to entityName
            StatEntityType.STUDIOS -> MovieFilterType.STUDIO to entityName
            StatEntityType.COUNTRIES -> MovieFilterType.COUNTRY to entityName
            StatEntityType.DIRECTORS,
            StatEntityType.ACTORS,
            null -> MovieFilterType.NONE to null
        }
        val filterTypeParam = filterType.takeUnless { it == MovieFilterType.NONE }?.name
        val listTypeParam = _currentListType.value.name

        return when (entityType) {
            StatEntityType.DIRECTORS ->
                database.movieEntityQueries
                    .getMoviesByPersonAndDate(
                        listType = listTypeParam,
                        personName = entityName!!,
                        job = "Director",
                        startDate = startDate,
                        endDate = endDate,
                        mapper = ::mapStatsListMovie
                    )
                    .executeAsList()

            StatEntityType.ACTORS ->
                database.movieEntityQueries
                    .getMoviesByPersonAndDate(
                        listType = listTypeParam,
                        personName = entityName!!,
                        job = "Actor",
                        startDate = startDate,
                        endDate = endDate,
                        mapper = ::mapStatsListMovie
                    )
                    .executeAsList()

            StatEntityType.GENRES,
            StatEntityType.STUDIOS,
            StatEntityType.COUNTRIES -> {


                database.movieEntityQueries
                    .getMoviesByFilterAndDate(
                        listType = listTypeParam,
                        filterType = filterTypeParam,
                        filterName = filterName,
                        startDate = startDate,
                        endDate = endDate,
                        mapper = ::mapToListItem
                    )
                    .executeAsList()
            }

            null -> emptyList()
        }
    }

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

    private fun mapToListItem(
        id: Long, name: String, year: Long,
        posterPath: String?, tmdbId: String?,
        letterboxdUri: String?, imdbId: String?,
        rating: Double?, watchedDate: String?, isRewatch: Long
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

    private fun mapStatsListMovie(
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

    private fun mapToDomain(
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
        rating: Double?,
        watchedDate: String?,
        isRewatch: Long
    ): Movie {
        val persons = database.movieEntityQueries.getPersonsForMovie(id).executeAsList()

        val mappedActors = persons.filter { it.job == "Actor" }.map {
            com.martonegyed.domain.model.Person(
                name = it.name,
                profilePath = it.profilePath,
                character = it.character,
                job = it.job
            )
        }

        val mappedCrew = persons.filter { it.job != "Actor" }.map {
            com.martonegyed.domain.model.Person(
                name = it.name,
                profilePath = it.profilePath,
                character = null,
                job = it.job
            )
        }

        return Movie(
            id = id.toInt(),
            tmdbId = tmdbId?.toIntOrNull(),
            name = name,
            year = year.toInt(),
            rating = rating,
            watchedDate = watchedDate,
            inWatchlist = inWatchlist == 1L,
            isRewatch = isRewatch == 1L,
            posterPath = posterPath,
            backdropPath = backdropPath,
            overview = overview,
            tagline = tagline,
            runtimeMinutes = runtimeMinutes?.toInt(),
            originalTitle = originalTitle,
            originalLanguage = originalLanguage,
            budget = budget?.toInt(),
            revenue = revenue,
            imdbId = imdbId,
            letterboxdUri = letterboxdUri,
            genres = genres?.split(", "),
            actors = mappedActors,
            crew = mappedCrew
        )
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

    private fun applyFiltersAndSort() {
        var filtered = allMovies
        if (_searchQuery.value.isNotEmpty()) {
            filtered = filtered.filter { it.name.contains(_searchQuery.value, ignoreCase = true) }
        }
        filtered = when (_currentSort.value) {
            SortOption.NAME -> if (_isAscending.value) filtered.sortedBy { it.name } else filtered.sortedByDescending { it.name }
            SortOption.RELEASE_YEAR -> if (_isAscending.value) filtered.sortedBy { it.year } else filtered.sortedByDescending { it.year }
            SortOption.RATING -> if (_isAscending.value) filtered.sortedBy {
                it.rating ?: 0.0
            } else filtered.sortedByDescending { it.rating ?: 0.0 }

            SortOption.DATE_WATCHED -> if (_isAscending.value) filtered.sortedBy { it.watchedDate } else filtered.sortedByDescending { it.watchedDate }
        }
        _displayedMovies.value = filtered
    }
}