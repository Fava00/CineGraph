package com.martonegyed.presentation.screens.movies

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.martonegyed.domain.model.Movie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SortOption { DATE_WATCHED, RELEASE_YEAR, RATING, NAME }

class AllMoviesScreenModel(private val database: com.martonegyed.data.database.CineGraphDatabase) : ScreenModel {
    private var allMovies = listOf<Movie>()

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

    init {
        loadMoviesFromDatabase()
    }

    private fun loadMoviesFromDatabase() {
        screenModelScope.launch {
            database.movieEntityQueries.getWatchedMovies()
                .asFlow()
                .mapToList(Dispatchers.Default)
                .collect { entities ->
                    allMovies = entities.map { entity ->
                        Movie(
                            id = entity.id.toInt(),
                            name = entity.name,
                            year = entity.year.toInt(),
                            letterboxdUri = entity.letterboxdUri,
                            imdbId = entity.imdbId,
                            rating = entity.rating,
                            watchedDate = entity.watchedDate,
                            isRewatch = entity.isRewatch == 1L,
                            inWatchlist = entity.inWatchlist == 1L,
                            posterPath = entity.posterPath,
                            backdropPath = entity.backdropPath,
                            overview = entity.overview,
                            runtimeMinutes = entity.runtimeMinutes?.toInt()
                        )
                    }
                    applyFiltersAndSort()
                }
        }
    }

    fun toggleGrid() { _isGrid.value = !_isGrid.value }

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
            SortOption.RATING -> if (_isAscending.value) filtered.sortedBy { it.rating ?: 0.0 } else filtered.sortedByDescending { it.rating ?: 0.0 }
            SortOption.DATE_WATCHED -> if (_isAscending.value) filtered.sortedBy { it.watchedDate ?: "" } else filtered.sortedByDescending { it.watchedDate ?: "" }
        }

        _displayedMovies.value = filtered
    }
}