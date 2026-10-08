package com.martonegyed.presentation.screens.collabSearch

import com.martonegyed.domain.repository.CrossoverRepository
import com.martonegyed.domain.model.PersonRole
import com.martonegyed.domain.model.MovieGenre
import com.martonegyed.domain.model.CrossoverMovie
import com.martonegyed.domain.model.CrossoverRequest
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.martonegyed.core.AppLogger
import com.martonegyed.domain.model.PersonSuggestion
import com.martonegyed.domain.model.SelectedPerson
import com.martonegyed.domain.model.SuggestionSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CollabSearchUiState(
    val actorInput: String = "",
    val directorInput: String = "",
    val selectedActors: List<SelectedPerson> = emptyList(),
    val selectedDirectors: List<SelectedPerson> = emptyList(),

    val actorSuggestions: List<PersonSuggestion> = emptyList(),
    val directorSuggestions: List<PersonSuggestion> = emptyList(),
    val showActorSuggestions: Boolean = false,
    val showDirectorSuggestions: Boolean = false,

    val availableGenres: List<MovieGenre> = emptyList(),
    val selectedGenreIds: Set<Int> = emptySet(),
    val minYear: Int = 1888,
    val maxYear: Int = 2026,
    val selectedStartYear: Int = 1888,
    val selectedEndYear: Int = 2026,
    val isLoadingGenres: Boolean = false,
    val isSearching: Boolean = false,
    val errorMessage: String? = null,
    val results: List<CrossoverMovie> = emptyList()
) {
    val canSearch: Boolean
        get() = selectedActors.size >= 2 || (selectedActors.isNotEmpty() && selectedDirectors.isNotEmpty())
}

class CollabSearchScreenModel(
    private val repository: CrossoverRepository
) : ScreenModel {
    private val _uiState = MutableStateFlow(CollabSearchUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadGenres()
    }

    private var actorSuggestionJob: Job? = null
    private var directorSuggestionJob: Job? = null

    private fun cancelSuggestionJob(role: PersonRole) {
        when (role) {
            PersonRole.ACTOR -> actorSuggestionJob?.cancel()
            PersonRole.DIRECTOR -> directorSuggestionJob?.cancel()
        }
    }

    private fun setSuggestionJob(role: PersonRole, job: Job) {
        when (role) {
            PersonRole.ACTOR -> actorSuggestionJob = job
            PersonRole.DIRECTOR -> directorSuggestionJob = job
        }
    }

    private fun currentInput(role: PersonRole): String =
        when (role) {
            PersonRole.ACTOR -> _uiState.value.actorInput
            PersonRole.DIRECTOR -> _uiState.value.directorInput
        }

    private fun currentSelected(role: PersonRole): List<SelectedPerson> =
        when (role) {
            PersonRole.ACTOR -> _uiState.value.selectedActors
            PersonRole.DIRECTOR -> _uiState.value.selectedDirectors
        }

    private fun currentSuggestions(role: PersonRole): List<PersonSuggestion> =
        when (role) {
            PersonRole.ACTOR -> _uiState.value.actorSuggestions
            PersonRole.DIRECTOR -> _uiState.value.directorSuggestions
        }

    private fun updateRoleState(
        role: PersonRole,
        input: String? = null,
        selected: List<SelectedPerson>? = null,
        suggestions: List<PersonSuggestion>? = null,
        showSuggestions: Boolean? = null,
        errorMessage: String? = _uiState.value.errorMessage
    ) {
        val state = _uiState.value
        _uiState.value = when (role) {
            PersonRole.ACTOR -> state.copy(
                actorInput = input ?: state.actorInput,
                selectedActors = selected ?: state.selectedActors,
                actorSuggestions = suggestions ?: state.actorSuggestions,
                showActorSuggestions = showSuggestions ?: state.showActorSuggestions,
                errorMessage = errorMessage
            )

            PersonRole.DIRECTOR -> state.copy(
                directorInput = input ?: state.directorInput,
                selectedDirectors = selected ?: state.selectedDirectors,
                directorSuggestions = suggestions ?: state.directorSuggestions,
                showDirectorSuggestions = showSuggestions ?: state.showDirectorSuggestions,
                errorMessage = errorMessage
            )
        }
    }

    fun updateInput(role: PersonRole, value: String) {
        val trimmed = value.trim()

        updateRoleState(
            role = role,
            input = value,
            suggestions = if (trimmed.length < 2) emptyList() else currentSuggestions(role),
            showSuggestions = trimmed.length >= 2
        )

        loadSuggestions(role, value)
    }

    private fun loadSuggestions(role: PersonRole, query: String) {
        cancelSuggestionJob(role)

        val trimmed = query.trim()
        if (trimmed.length < 2) {
            updateRoleState(
                role = role,
                suggestions = emptyList(),
                showSuggestions = false
            )
            return
        }

        val job = screenModelScope.launch {
            val local = repository.getLocalSuggestions(role, trimmed)

            if (local.size >= 5) {
                if (currentInput(role).trim() != trimmed) return@launch
                updateRoleState(
                    role = role,
                    suggestions = filterOutSelected(local, currentSelected(role)),
                    showSuggestions = true
                )
                return@launch
            }

            delay(400)

            if (currentInput(role).trim() != trimmed) return@launch

            val remote = repository.getRemoteSuggestions(role, trimmed)
            if (currentInput(role).trim() != trimmed) return@launch

            val merged = mergeSuggestions(local, remote)

            updateRoleState(
                role = role,
                suggestions = filterOutSelected(merged, currentSelected(role)),
                showSuggestions = merged.isNotEmpty()
            )
        }

        setSuggestionJob(role, job)
    }

    private fun mergeSuggestions(
        local: List<PersonSuggestion>,
        remote: List<PersonSuggestion>
    ): List<PersonSuggestion> {
        return (local + remote)
            .distinctBy { it.name.trim().lowercase() }
            .take(8)
    }

    fun selectSuggestion(role: PersonRole, suggestion: PersonSuggestion) {
        val selected = currentSelected(role)

        if (selected.any { it.name.equals(suggestion.name, ignoreCase = true) }) {
            updateRoleState(
                role = role,
                input = "",
                suggestions = emptyList(),
                showSuggestions = false
            )
            return
        }

        updateRoleState(
            role = role,
            input = "",
            selected = selected + SelectedPerson(
                name = suggestion.name,
                tmdbPersonId = suggestion.tmdbPersonId
            ),
            suggestions = emptyList(),
            showSuggestions = false,
            errorMessage = null
        )
    }

    fun dismissSuggestions(role: PersonRole) {
        updateRoleState(
            role = role,
            showSuggestions = false
        )
    }

    fun addPerson(role: PersonRole) {
        val value = currentInput(role).trim()
        if (value.isBlank()) return

        val selected = currentSelected(role)
        if (selected.any { it.name.equals(value, ignoreCase = true) }) {
            updateRoleState(
                role = role,
                input = "",
                suggestions = emptyList(),
                showSuggestions = false
            )
            return
        }

        updateRoleState(
            role = role,
            input = "",
            selected = selected + SelectedPerson(name = value),
            suggestions = emptyList(),
            showSuggestions = false,
            errorMessage = null
        )
    }

    fun removePerson(role: PersonRole, name: String) {
        updateRoleState(
            role = role,
            selected = currentSelected(role).filterNot { it.name == name },
            errorMessage = null
        )
    }

    fun updateYearRange(start: Int, end: Int) {
        _uiState.value = _uiState.value.copy(
            selectedStartYear = start,
            selectedEndYear = end,
            errorMessage = null
        )
    }

    fun toggleGenre(genreId: Int) {
        val current = _uiState.value.selectedGenreIds
        _uiState.value = _uiState.value.copy(
            selectedGenreIds = if (genreId in current) current - genreId else current + genreId
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun loadGenres() {
        screenModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingGenres = true, errorMessage = null)

            val genres = repository.getGenres()
            _uiState.value = _uiState.value.copy(
                isLoadingGenres = false,
                availableGenres = genres
            )
        }
    }

    fun search() {
        val state = _uiState.value
        if (state.isSearching) return

        if (!(state.selectedActors.size >= 2 || (state.selectedActors.isNotEmpty() && state.selectedDirectors.isNotEmpty()))) {
            _uiState.value = state.copy(
                errorMessage = "Enter at least 2 actors, or 1 actor and 1 director."
            )
            return
        }

        if (state.selectedStartYear > state.selectedEndYear) {
            _uiState.value = state.copy(
                errorMessage = "Invalid year range."
            )
            return
        }

        screenModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSearching = true,
                errorMessage = null,
                results = emptyList()
            )

            try {
                val filtered = repository.search(CrossoverRequest(
                    state.selectedActors, state.selectedDirectors, state.selectedGenreIds,
                    state.selectedStartYear, state.selectedEndYear
                ))

                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    results = filtered
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                AppLogger.exception(
                    tag = "Collab Search Screen",
                    throwable = e,
                    message = "Search failed, ${e.message}"
                )
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    errorMessage = e.message ?: "Search failed."
                )
            }
        }
    }

    private fun filterOutSelected(
        suggestions: List<PersonSuggestion>,
        selected: List<SelectedPerson>
    ): List<PersonSuggestion> {
        val selectedNames = selected.map { it.name.trim().lowercase() }.toSet()
        return suggestions.filterNot { it.name.trim().lowercase() in selectedNames }
    }
}