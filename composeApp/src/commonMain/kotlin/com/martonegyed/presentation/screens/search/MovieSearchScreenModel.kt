package com.martonegyed.presentation.screens.search

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.martonegyed.domain.model.MovieSearchResult
import com.martonegyed.domain.repository.MovieSearchRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MovieSearchUiState(
    val query: String = "",
    val tmdbTab: Boolean = false,
    val results: List<MovieSearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val savingMovieKey: String? = null,
    val error: String? = null,
    val notice: String? = null,
    val hasSearched: Boolean = false
)

class MovieSearchScreenModel(private val repository: MovieSearchRepository) : ScreenModel {
    private val _state = MutableStateFlow(MovieSearchUiState())
    val state = _state.asStateFlow()
    private var searchJob: Job? = null

    fun setQuery(query: String) {
        searchJob?.cancel()
        _state.value = _state.value.copy(query = query, results = emptyList(),
            isSearching = false, hasSearched = false, error = null, notice = null)
    }

    fun setTmdbTab(tmdb: Boolean) {
        if (_state.value.tmdbTab == tmdb) return
        searchJob?.cancel()
        _state.value = _state.value.copy(tmdbTab = tmdb, results = emptyList(),
            isSearching = false, hasSearched = false, error = null, notice = null)
    }

    fun search() {
        val query = _state.value.query.trim()
        if (query.length < 2) {
            _state.value = _state.value.copy(error = "Enter at least two characters", results = emptyList())
            return
        }
        searchJob?.cancel()
        val tmdb = _state.value.tmdbTab
        searchJob = screenModelScope.launch {
            _state.value = _state.value.copy(isSearching = true, error = null, notice = null)
            try {
                val results = if (tmdb) repository.searchTmdb(query) else repository.searchMyLibrary(query)
                _state.value = _state.value.copy(results = results, isSearching = false,
                    hasSearched = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(isSearching = false, error = e.message ?: "Search failed")
            }
        }
    }

    fun addToWatchlist(movie: MovieSearchResult) {
        if (_state.value.savingMovieKey != null || (movie.tmdbId == null && movie.localId == null)) return
        screenModelScope.launch {
            _state.value = _state.value.copy(savingMovieKey = movie.key, error = null, notice = null)
            try {
                val saved = repository.addToWatchlist(movie)
                _state.value = _state.value.copy(
                    results = _state.value.results.map { if (it.key == movie.key) saved else it },
                    savingMovieKey = null,
                    notice = "${saved.title} added to your watchlist"
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(savingMovieKey = null,
                    error = e.message ?: "Could not add the movie")
            }
        }
    }
}
