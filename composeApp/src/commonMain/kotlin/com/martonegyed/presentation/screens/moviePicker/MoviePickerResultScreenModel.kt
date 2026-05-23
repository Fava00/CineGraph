package com.martonegyed.presentation.screens.moviePicker

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.martonegyed.data.database.CineGraphDatabase
import com.martonegyed.data.remote.TmdbApiService
import com.martonegyed.data.remote.TmdbMovie
import com.martonegyed.data.remote.TmdbMovieDetailsResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class MoviePickerCandidateUi(
    val tmdbId: Int?,
    val localMovieId: Long?,
    val title: String,
    val year: Int?,
    val posterPath: String?,
    val overview: String?,
    val runtimeMinutes: Int?,
    val tmdbVoteAverage: Double?,
    val originalLanguage: String?,
    val spokenLanguages: List<String>,
    val productionCountries: List<String>,
    val isWatched: Boolean,
    val inWatchlist: Boolean,
    val isCached: Boolean,
    val isRewatchCandidate: Boolean
)

data class MoviePickerResultsUiState(
    val isLoading: Boolean = true,
    val isSavingCached: Boolean = false,
    val errorMessage: String? = null,
    val totalLocalCandidates: Int = 0,
    val totalRemoteCandidates: Int = 0,
    val filteredCandidates: List<MoviePickerCandidateUi> = emptyList()
)

class MoviePickerResultsScreenModel(
    private val request: MoviePickerRequest,
    private val tmdbApiService: TmdbApiService,
    private val database: CineGraphDatabase
) : ScreenModel {

    private val _uiState = MutableStateFlow(MoviePickerResultsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        screenModelScope.launch {
            _uiState.value = MoviePickerResultsUiState(isLoading = true)

            try {
                val allMovies = database.movieEntityQueries
                    .getAllMovies()
                    .executeAsList()

                val existingTmdbIds: Set<Int> = allMovies.mapNotNull { row ->
                    row.tmdbId?.toIntOrNull()
                }.toSet()

                val localCandidates = loadLocalCandidates(allMovies)

                val remoteCandidates = when (request.source) {
                    MoviePickerSearchSource.MY_LIBRARY -> emptyList()
                    MoviePickerSearchSource.DISCOVER_NEW,
                    MoviePickerSearchSource.BOTH -> fetchRemoteCandidates(existingTmdbIds)
                }

                val merged = (localCandidates + remoteCandidates)
                    .distinctBy { candidate ->
                        candidate.tmdbId?.toString() ?: "local-${candidate.localMovieId}"
                    }

                val finalFiltered = merged.filter { candidate ->
                    matchesWatchIntent(candidate) &&
                            matchesDecades(candidate) &&
                            matchesLanguages(candidate) &&
                            matchesCountries(candidate) &&
                            matchesRuntime(candidate) &&
                            matchesMinimumRating(candidate)
                }

                _uiState.value = MoviePickerResultsUiState(
                    isLoading = false,
                    totalLocalCandidates = localCandidates.size,
                    totalRemoteCandidates = remoteCandidates.size,
                    filteredCandidates = finalFiltered
                )
            } catch (t: Throwable) {
                _uiState.value = MoviePickerResultsUiState(
                    isLoading = false,
                    errorMessage = t.message ?: "Failed to load movie picker results."
                )
            }
        }
    }

    private fun loadLocalCandidates(
        allMovies: List<com.martonegyed.data.database.GetAllMovies>
    ): List<MoviePickerCandidateUi> {
        val pool = when (request.source) {
            MoviePickerSearchSource.MY_LIBRARY,
            MoviePickerSearchSource.BOTH -> {
                allMovies.filter { row ->
                    row.isWatched == 1L || row.inWatchlist == 1L
                }
            }

            MoviePickerSearchSource.DISCOVER_NEW -> emptyList()
        }

        return pool
            .map { row -> rowToCandidate(row) }
            .filter { candidate ->
                matchesIncludedGenres(candidate) &&
                        matchesExcludedGenres(candidate) &&
                        matchesWatchIntent(candidate) &&
                        matchesDecades(candidate) &&
                        matchesLanguages(candidate) &&
                        matchesCountries(candidate) &&
                        matchesRuntime(candidate) &&
                        matchesMinimumRating(candidate)
            }
    }

    private suspend fun fetchRemoteCandidates(
        existingTmdbIds: Set<Int>
    ): List<MoviePickerCandidateUi> {
        val kept = mutableListOf<MoviePickerCandidateUi>()
        var page = 1

        val fromYear = request.selectedDecades.minOrNull()
        val toYear = request.selectedDecades.maxOrNull()?.plus(9)

        while (kept.size < request.searchDepth && page <= 10) {
            val response = tmdbApiService.discoverMovies(
                castIds = emptyList(),
                crewIds = emptyList(),
                genreIds = request.includedGenreIds.toList(),
                fromYear = fromYear,
                toYear = toYear,
                page = page
            ) ?: break

            if (response.results.isEmpty()) break

            for (basic: TmdbMovie in response.results) {
                if (basic.id in existingTmdbIds) continue

                val candidate = enrichRemoteCandidate(basic) ?: continue

                if (!matchesExcludedGenres(candidate)) continue
                if (!matchesWatchIntent(candidate)) continue
                if (!matchesDecades(candidate)) continue
                if (!matchesLanguages(candidate)) continue
                if (!matchesCountries(candidate)) continue
                if (!matchesRuntime(candidate)) continue
                if (!matchesMinimumRating(candidate)) continue

                kept += candidate

                if (kept.size >= request.searchDepth) break
            }

            if (page >= response.totalPages) break
            page++
        }

        saveRemoteCandidatesAsCached(kept)
        return kept
    }

    private suspend fun enrichRemoteCandidate(
        basic: TmdbMovie
    ): MoviePickerCandidateUi? {
        val details: TmdbMovieDetailsResponse = tmdbApiService.getMovieDetails(basic.id) ?: return null

        val productionCountries: List<String> = details.productionCountries
            .map { country -> country.name.trim() }
            .filter { name -> name.isNotBlank() }

        val spokenLanguages: List<String> = details.spokenLanguages
            .map { language -> language.englishName.trim() }
            .filter { name -> name.isNotBlank() }

        return MoviePickerCandidateUi(
            tmdbId = details.id,
            localMovieId = null,
            title = basic.title,
            year = basic.releaseDate?.take(4)?.toIntOrNull(),
            posterPath = details.posterPath ?: basic.posterPath,
            overview = details.overview ?: basic.overview,
            runtimeMinutes = details.runtime,
            tmdbVoteAverage = details.voteAverage ?: basic.voteAverage,
            originalLanguage = details.originalLanguage ?: basic.originalLanguage,
            spokenLanguages = spokenLanguages,
            productionCountries = productionCountries,
            isWatched = false,
            inWatchlist = false,
            isCached = true,
            isRewatchCandidate = false
        )
    }

    private fun saveRemoteCandidatesAsCached(
        candidates: List<MoviePickerCandidateUi>
    ) {
        if (candidates.isEmpty()) return

        _uiState.value = _uiState.value.copy(isSavingCached = true)

        database.transaction {
            candidates.forEach { candidate ->
                val title = candidate.title.trim()
                val year = candidate.year ?: return@forEach

                database.movieEntityQueries.insertMovie(
                    name = title,
                    year = year.toLong(),
                    letterboxdUri = null,
                    imdbId = null,
                    isWatched = 0L,
                    inWatchlist = 0L,
                    isCached = 1L,
                    posterPath = candidate.posterPath,
                    backdropPath = null,
                    overview = candidate.overview,
                    runtimeMinutes = candidate.runtimeMinutes?.toLong(),
                    tmdbId = candidate.tmdbId?.toString(),
                    tagline = null,
                    originalTitle = null,
                    originalLanguage = candidate.originalLanguage,
                    budget = null,
                    revenue = null,
                    genres = null,
                    hungarianTitle = null,
                    tmdbPopularity = null,
                    tmdbVoteAverage = candidate.tmdbVoteAverage,
                    tmdbVoteCount = null,
                    collectionName = null,
                    trailerKey = null,
                    mpaaRating = null,
                    addedDate = null,
                    studios = null,
                    productionCountries = candidate.productionCountries.joinToString("|"),
                    spokenLanguages = candidate.spokenLanguages.joinToString("|"),
                    similarMovies = null,
                    tmdbReviews = null
                )
            }
        }

        _uiState.value = _uiState.value.copy(isSavingCached = false)
    }

    private fun rowToCandidate(
        row: com.martonegyed.data.database.GetAllMovies
    ): MoviePickerCandidateUi {
        return MoviePickerCandidateUi(
            tmdbId = row.tmdbId?.toIntOrNull(),
            localMovieId = row.id,
            title = row.name,
            year = row.year.toInt(),
            posterPath = row.posterPath,
            overview = row.overview,
            runtimeMinutes = row.runtimeMinutes?.toInt(),
            tmdbVoteAverage = row.tmdbVoteAverage,
            originalLanguage = row.originalLanguage,
            spokenLanguages = row.spokenLanguages
                .orEmpty()
                .split("|")
                .map { it.trim() }
                .filter { it.isNotBlank() },
            productionCountries = row.productionCountries
                .orEmpty()
                .split("|")
                .map { it.trim() }
                .filter { it.isNotBlank() },
            isWatched = row.isWatched == 1L,
            inWatchlist = row.inWatchlist == 1L,
            isCached = row.isCached == 1L,
            isRewatchCandidate = row.isRewatch == 1L
        )
    }

    private fun matchesWatchIntent(candidate: MoviePickerCandidateUi): Boolean {
        return when (request.watchIntent) {
            MoviePickerWatchIntent.ANYTHING -> true
            MoviePickerWatchIntent.SOMETHING_NEW -> !candidate.isWatched
            MoviePickerWatchIntent.REWATCH -> candidate.isWatched
        }
    }

    private fun matchesDecades(candidate: MoviePickerCandidateUi): Boolean {
        if (request.selectedDecades.isEmpty()) return true
        val year = candidate.year ?: return false
        return request.selectedDecades.any { decadeStart ->
            year in decadeStart..(decadeStart + 9)
        }
    }

    private fun matchesLanguages(candidate: MoviePickerCandidateUi): Boolean {
        if (request.languages.isEmpty()) return true

        val wanted = request.languages.map { it.trim().lowercase() }.toSet()

        val actual = buildSet {
            candidate.originalLanguage
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let { add(it.lowercase()) }

            candidate.spokenLanguages.forEach { add(it.lowercase()) }
        }

        return actual.any { it in wanted }
    }

    private fun matchesCountries(candidate: MoviePickerCandidateUi): Boolean {
        if (request.productionCountries.isEmpty()) return true

        val wanted = request.productionCountries.map { it.trim().lowercase() }.toSet()
        val actual = candidate.productionCountries.map { it.trim().lowercase() }.toSet()

        return actual.any { it in wanted }
    }

    private fun matchesRuntime(candidate: MoviePickerCandidateUi): Boolean {
        val runtime = candidate.runtimeMinutes ?: return true
        return runtime in request.runtimeMinutes
    }

    private fun matchesMinimumRating(candidate: MoviePickerCandidateUi): Boolean {
        val rating = candidate.tmdbVoteAverage ?: return true
        return rating >= request.minimumRating
    }

    private fun matchesIncludedGenres(candidate: MoviePickerCandidateUi): Boolean {
            if (request.includedGenreIds.isEmpty()) return true

            val candidateGenreIds = extractGenreIdsFromCandidate(candidate)
            if (candidateGenreIds.isEmpty()) return true

            return candidateGenreIds.any { it in request.includedGenreIds }
        }

        private fun matchesExcludedGenres(candidate: MoviePickerCandidateUi): Boolean {
            if (request.excludedGenreIds.isEmpty()) return true

            val candidateGenreIds = extractGenreIdsFromCandidate(candidate)
            if (candidateGenreIds.isEmpty()) return true

            return candidateGenreIds.none { it in request.excludedGenreIds }
        }

        private fun extractGenreIdsFromCandidate(candidate: MoviePickerCandidateUi): Set<Int> {
            val map = try {
                com.martonegyed.core.util.GenreMap.nameToId
            } catch (_: Throwable) {
                return emptySet()
            }

            if (map.isEmpty()) return emptySet()

            val text = listOfNotNull(candidate.title, candidate.overview)
                .joinToString(" ")
                .lowercase()

            return map.entries.fold(mutableSetOf()) { acc, entry ->
                val name = entry.key
                val idValue = entry.value
                if (name.isNotBlank() && text.contains(name.lowercase())) {
                    when (idValue) {
                        else -> idValue.toIntOrNull()?.let { acc.add(it) }
                    }
                }
                acc
            }
        }
}