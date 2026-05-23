package com.martonegyed.presentation.screens.collabSearch

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.martonegyed.data.database.CineGraphDatabase
import com.martonegyed.data.remote.TmdbApiService
import com.martonegyed.data.remote.TmdbGenre
import com.martonegyed.data.remote.TmdbMovie
import com.martonegyed.data.remote.TmdbPerson
import com.martonegyed.domain.model.PersonSuggestion
import com.martonegyed.domain.model.SelectedPerson
import com.martonegyed.domain.model.SuggestionSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class PersonRole(
    val localJob: String,
    val tmdbDepartment: String
) {
    ACTOR(
        localJob = "Actor",
        tmdbDepartment = "Acting"
    ),
    DIRECTOR(
        localJob = "Director",
        tmdbDepartment = "Directing"
    )
}

data class CollabSearchUiState(
    val actorInput: String = "",
    val directorInput: String = "",
    val selectedActors: List<SelectedPerson> = emptyList(),
    val selectedDirectors: List<SelectedPerson> = emptyList(),

    val actorSuggestions: List<PersonSuggestion> = emptyList(),
    val directorSuggestions: List<PersonSuggestion> = emptyList(),
    val showActorSuggestions: Boolean = false,
    val showDirectorSuggestions: Boolean = false,

    val availableGenres: List<TmdbGenre> = emptyList(),
    val selectedGenreIds: Set<Int> = emptySet(),
    val minYear: Int = 1888,
    val maxYear: Int = 2026,
    val selectedStartYear: Int = 1888,
    val selectedEndYear: Int = 2026,
    val isLoadingGenres: Boolean = false,
    val isSearching: Boolean = false,
    val errorMessage: String? = null,
    val results: List<TmdbMovie> = emptyList()
) {
    val canSearch: Boolean
        get() = selectedActors.size >= 2 || (selectedActors.isNotEmpty() && selectedDirectors.isNotEmpty())
}

class CollabSearchScreenModel(
    private val tmdbApiService: TmdbApiService,
    private val database: CineGraphDatabase
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
            val local = loadLocalSuggestions(role, trimmed)

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

            val remote = tmdbApiService.searchPerson(trimmed)
                ?.results
                .orEmpty()
                .filter {
                    it.knownForDepartment == null ||
                            it.knownForDepartment.equals(role.tmdbDepartment, ignoreCase = true)
                }
                .map {
                    PersonSuggestion(
                        name = it.name,
                        tmdbPersonId = it.id,
                        source = SuggestionSource.TMDB
                    )
                }
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

            val response = tmdbApiService.getMovieGenres()
            _uiState.value = _uiState.value.copy(
                isLoadingGenres = false,
                availableGenres = response?.genres.orEmpty()
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
                val actorPeople = resolvePeople(
                    selectedPeople = _uiState.value.selectedActors,
                    expectedDepartment = "Acting"
                )

                val directorPeople = resolvePeople(
                    selectedPeople = _uiState.value.selectedDirectors,
                    expectedDepartment = "Directing"
                )

                if (actorPeople.size != _uiState.value.selectedActors.size) {
                    _uiState.value = _uiState.value.copy(
                        isSearching = false,
                        errorMessage = "Could not find one or more actors."
                    )
                    return@launch
                }

                if (directorPeople.size != _uiState.value.selectedDirectors.size) {
                    _uiState.value = _uiState.value.copy(
                        isSearching = false,
                        errorMessage = "Could not find one or more directors."
                    )
                    return@launch
                }

                val discovered = mutableListOf<TmdbMovie>()
                val selectedGenres = _uiState.value.selectedGenreIds.toList()

                for (page in 1..3) {
                    val response = tmdbApiService.discoverMovies(
                        castIds = actorPeople.map { it.id },
                        crewIds = directorPeople.map { it.id },
                        genreIds = selectedGenres,
                        fromYear = _uiState.value.selectedStartYear,
                        toYear = _uiState.value.selectedEndYear,
                        page = page
                    ) ?: continue

                    if (response.results.isEmpty()) break
                    discovered += response.results
                    if (page >= response.totalPages) break
                }

                val uniqueDiscovered = discovered.distinctBy { it.id }

                val filtered = uniqueDiscovered.filter { movie ->
                    movieMatchesAllCriteria(
                        movieId = movie.id,
                        actorNames = _uiState.value.selectedActors,
                        directorNames = _uiState.value.selectedDirectors,
                        selectedGenreIds = _uiState.value.selectedGenreIds
                    )
                }

                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    results = filtered
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    errorMessage = e.message ?: "Search failed."
                )
            }
        }
    }

    private suspend fun resolvePeople(
        selectedPeople: List<SelectedPerson>,
        expectedDepartment: String
    ): List<TmdbPerson> {
        return selectedPeople.mapNotNull { selected ->
            selected.tmdbPersonId?.let { knownId ->
                return@mapNotNull TmdbPerson(
                    id = knownId,
                    name = selected.name,
                    knownForDepartment = expectedDepartment
                )
            }

            val query = selected.name.trim()
            if (query.isBlank()) return@mapNotNull null

            val results = tmdbApiService.searchPerson(query)?.results.orEmpty()

            results.firstOrNull {
                it.name.equals(query, ignoreCase = true) &&
                        (it.knownForDepartment == null ||
                                it.knownForDepartment.equals(expectedDepartment, ignoreCase = true))
            } ?: results.firstOrNull {
                it.knownForDepartment == null ||
                        it.knownForDepartment.equals(expectedDepartment, ignoreCase = true)
            } ?: results.firstOrNull()
        }
    }

    private suspend fun movieMatchesAllCriteria(
        movieId: Int,
        actorNames: List<SelectedPerson>,
        directorNames: List<SelectedPerson>,
        selectedGenreIds: Set<Int>
    ): Boolean {
        val details = tmdbApiService.getMovieDetails(movieId) ?: return false

        val castNames = details.credits?.cast
            ?.map { it.name.trim().lowercase() }
            .orEmpty()
            .toSet()

        val directorSet = details.credits?.crew
            ?.filter { it.job.equals("Director", ignoreCase = true) }
            ?.map { it.name.trim().lowercase() }
            .orEmpty()
            .toSet()

        val requiredActors = actorNames.map { it.name.trim().lowercase() }
        val requiredDirectors = directorNames.map { it.name.trim().lowercase() }

        val actorsMatch = requiredActors.all { it in castNames }
        val directorsMatch = requiredDirectors.all { it in directorSet }

        val genresMatch = if (selectedGenreIds.isEmpty()) {
            true
        } else {
            details.genres.any { it.id in selectedGenreIds }
        }

        return actorsMatch && directorsMatch && genresMatch
    }

    private fun loadLocalSuggestions(
        role: PersonRole,
        query: String
    ): List<PersonSuggestion> {
        return database.movieEntityQueries
            .getPersonSuggestionsByJob(
                job = role.localJob,
                query = query
            )
            .executeAsList()
            .map { name ->
                PersonSuggestion(
                    name = name,
                    tmdbPersonId = null,
                    source = SuggestionSource.LOCAL
                )
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