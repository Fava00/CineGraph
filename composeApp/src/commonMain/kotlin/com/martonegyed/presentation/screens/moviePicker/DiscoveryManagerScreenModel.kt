package com.martonegyed.presentation.screens.moviePicker

import com.martonegyed.domain.repository.DiscoveryManagerRepository
import com.martonegyed.domain.model.DiscoveryMovie
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DiscoveryManagerScreenModel(
    private val repository: DiscoveryManagerRepository
) : ScreenModel {

    private val _uiState = MutableStateFlow(DiscoveryManagerUiState())
    val uiState: StateFlow<DiscoveryManagerUiState> = _uiState

    init {
        refresh()
    }

    fun selectTab(tab: DiscoveryManagerTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun refresh() {
        screenModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val cached = repository.getCachedMovies()
            val ignored = repository.getIgnoredMovies()

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                cachedMovies = cached,
                ignoredMovies = ignored
            )
        }
    }

    fun unignore(movie: DiscoveryMovie) {
        val tmdbId = movie.tmdbId ?: return
        screenModelScope.launch {
            repository.unignoreMovie(tmdbId)
            refresh()
        }
    }
}
