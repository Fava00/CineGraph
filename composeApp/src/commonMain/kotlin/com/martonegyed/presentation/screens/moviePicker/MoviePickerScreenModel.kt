package com.martonegyed.presentation.screens.moviePicker

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.martonegyed.data.database.CineGraphDatabase
import com.martonegyed.data.remote.TmdbApiService
import com.martonegyed.data.remote.TmdbGenre
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class MoviePickerSearchSource {
    MY_LIBRARY,
    DISCOVER_NEW,
    BOTH
}

enum class MoviePickerWatchIntent {
    SOMETHING_NEW,
    REWATCH,
    ANYTHING
}

enum class TriStateFilter {
    NEUTRAL,
    INCLUDE,
    EXCLUDE;

    fun next(): TriStateFilter = when (this) {
        NEUTRAL -> INCLUDE
        INCLUDE -> EXCLUDE
        EXCLUDE -> NEUTRAL
    }
}

data class PickerOption(
    val code: String,
    val label: String
)

data class DecadeOption(
    val startYear: Int,
    val label: String
)

data class MoviePickerRequest(
    val source: MoviePickerSearchSource,
    val searchDepth: Int,
    val watchIntent: MoviePickerWatchIntent,
    val includedGenreIds: Set<Int>,
    val excludedGenreIds: Set<Int>,
    val runtimeMinutes: IntRange,
    val minimumRating: Float,
    val selectedDecades: Set<Int>,
    val languages: Set<String>,
    val productionCountries: Set<String>
)

data class MoviePickerUiState(
    val source: MoviePickerSearchSource = MoviePickerSearchSource.MY_LIBRARY,
    val searchDepth: Int = 100,
    val watchIntent: MoviePickerWatchIntent = MoviePickerWatchIntent.SOMETHING_NEW,
    val availableGenres: List<TmdbGenre> = emptyList(),
    val genreStates: Map<Int, TriStateFilter> = emptyMap(),
    val runtimeRange: ClosedFloatingPointRange<Float> = 0f..240f,
    val minimumRating: Float = 0f,
    val decades: List<DecadeOption> = emptyList(),
    val selectedDecades: Set<Int> = emptySet(),
    val availableLanguages: List<PickerOption> = emptyList(),
    val selectedLanguages: Set<String> = emptySet(),
    val availableCountries: List<PickerOption> = emptyList(),
    val selectedCountries: Set<String> = emptySet(),
    val isLoadingMeta: Boolean = false,
    val errorMessage: String? = null
) {
    val needsSearchDepth: Boolean
        get() = source == MoviePickerSearchSource.DISCOVER_NEW || source == MoviePickerSearchSource.BOTH
}

class MoviePickerScreenModel(
    private val tmdbApiService: TmdbApiService,
    @Suppress("unused") private val database: CineGraphDatabase
) : ScreenModel {

    private val _uiState = MutableStateFlow(
        MoviePickerUiState(
            decades = defaultDecades,
            availableLanguages = defaultLanguages,
            availableCountries = defaultCountries
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        loadGenres()
    }

    fun setSource(source: MoviePickerSearchSource) {
        _uiState.value = _uiState.value.copy(source = source)
    }

    fun setSearchDepth(depth: Int) {
        _uiState.value = _uiState.value.copy(searchDepth = depth.coerceIn(20, 500))
    }

    fun setWatchIntent(intent: MoviePickerWatchIntent) {
        _uiState.value = _uiState.value.copy(watchIntent = intent)
    }

    fun cycleGenre(genreId: Int) {
        val current = _uiState.value.genreStates[genreId] ?: TriStateFilter.NEUTRAL
        _uiState.value = _uiState.value.copy(
            genreStates = _uiState.value.genreStates.toMutableMap().apply {
                val next = current.next()
                if (next == TriStateFilter.NEUTRAL) remove(genreId) else put(genreId, next)
            }
        )
    }

    fun updateRuntime(range: ClosedFloatingPointRange<Float>) {
        _uiState.value = _uiState.value.copy(
            runtimeRange = range.start.coerceAtLeast(0f)..range.endInclusive.coerceAtMost(240f)
        )
    }

    fun updateMinimumRating(rating: Float) {
        _uiState.value = _uiState.value.copy(
            minimumRating = rating.coerceIn(0f, 10f)
        )
    }

    fun toggleDecade(startYear: Int) {
        val current = _uiState.value.selectedDecades
        _uiState.value = _uiState.value.copy(
            selectedDecades = if (startYear in current) current - startYear else current + startYear
        )
    }

    fun toggleLanguage(code: String) {
        val selected = _uiState.value.selectedLanguages
        _uiState.value = _uiState.value.copy(
            selectedLanguages = if (code in selected) selected - code else selected + code
        )
    }

    fun toggleCountry(code: String) {
        val selected = _uiState.value.selectedCountries
        _uiState.value = _uiState.value.copy(
            selectedCountries = if (code in selected) selected - code else selected + code
        )
    }

    fun buildRequest(): MoviePickerRequest {
        val state = _uiState.value
        return MoviePickerRequest(
            source = state.source,
            searchDepth = state.searchDepth,
            watchIntent = state.watchIntent,
            includedGenreIds = state.genreStates.filterValues { it == TriStateFilter.INCLUDE }.keys,
            excludedGenreIds = state.genreStates.filterValues { it == TriStateFilter.EXCLUDE }.keys,
            runtimeMinutes = state.runtimeRange.start.toInt()..state.runtimeRange.endInclusive.toInt(),
            minimumRating = state.minimumRating,
            selectedDecades = state.selectedDecades,
            languages = state.selectedLanguages,
            productionCountries = state.selectedCountries
        )
    }

    private fun loadGenres() {
        screenModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMeta = true, errorMessage = null)
            try {
                val response = tmdbApiService.getMovieGenres()
                _uiState.value = _uiState.value.copy(
                    isLoadingMeta = false,
                    availableGenres = response?.genres.orEmpty().sortedBy { it.name }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingMeta = false,
                    errorMessage = e.message ?: "Failed to load picker metadata."
                )
            }
        }
    }

    companion object {
        private val defaultDecades = (1920..2020 step 10).map {
            DecadeOption(startYear = it, label = "${it}s")
        }

        private val defaultLanguages = listOf(
            PickerOption("en", "English"),
            PickerOption("es", "Spanish"),
            PickerOption("fr", "French"),
            PickerOption("de", "German"),
            PickerOption("it", "Italian"),
            PickerOption("ja", "Japanese"),
            PickerOption("ko", "Korean"),
            PickerOption("zh", "Chinese"),
            PickerOption("hi", "Hindi"),
            PickerOption("hu", "Hungarian"),
            PickerOption("pt", "Portuguese"),
            PickerOption("sv", "Swedish"),
            PickerOption("da", "Danish"),
            PickerOption("no", "Norwegian"),
            PickerOption("fi", "Finnish"),
            PickerOption("pl", "Polish"),
            PickerOption("cs", "Czech"),
            PickerOption("tr", "Turkish"),
            PickerOption("ru", "Russian"),
            PickerOption("ar", "Arabic")
        ).sortedBy { it.label }

        private val defaultCountries = listOf(
            PickerOption("US", "United States"),
            PickerOption("GB", "United Kingdom"),
            PickerOption("FR", "France"),
            PickerOption("DE", "Germany"),
            PickerOption("IT", "Italy"),
            PickerOption("ES", "Spain"),
            PickerOption("HU", "Hungary"),
            PickerOption("JP", "Japan"),
            PickerOption("KR", "South Korea"),
            PickerOption("CN", "China"),
            PickerOption("HK", "Hong Kong"),
            PickerOption("IN", "India"),
            PickerOption("SE", "Sweden"),
            PickerOption("DK", "Denmark"),
            PickerOption("NO", "Norway"),
            PickerOption("FI", "Finland"),
            PickerOption("PL", "Poland"),
            PickerOption("CZ", "Czech Republic"),
            PickerOption("TR", "Turkey"),
            PickerOption("BR", "Brazil"),
            PickerOption("MX", "Mexico"),
            PickerOption("CA", "Canada"),
            PickerOption("AU", "Australia"),
            PickerOption("NZ", "New Zealand")
        ).sortedBy { it.label }
    }
}