package com.martonegyed.presentation.screens.randompicker

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.martonegyed.domain.repository.MovieCollectionRepository
import com.martonegyed.domain.model.Movie
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RandomPickerUiState(
    val isLoading: Boolean = true,
    val watchlist: List<Movie> = emptyList(),
    val pickedMovie: Movie? = null
)

class RandomPickerScreenModel(
    private val repository: MovieCollectionRepository
) : ScreenModel {

    private val _state = MutableStateFlow(RandomPickerUiState())
    val state = _state.asStateFlow()

    private var watchlistJob: Job? = null

    init {
        loadWatchlist()
    }

    private fun loadWatchlist() {
        watchlistJob?.cancel()

        watchlistJob = screenModelScope.launch {
            repository.observeWatchlistMovies()
                .collect { rows ->
                    val movies = rows.map { row ->
                        row.toMovie(preferWatchlistDate = true)
                    }
                    _state.update {
                        it.copy(
                            isLoading = false,
                            watchlist = movies
                        )
                    }
                }
        }
    }


    fun setPickedMovie(movie: Movie) {
        _state.update { it.copy(pickedMovie = movie) }
    }

    override fun onDispose() {
        watchlistJob?.cancel()
        super.onDispose()
    }
}