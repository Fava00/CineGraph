package com.martonegyed.presentation.screens.moviePicker

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.martonegyed.data.database.CineGraphDatabase
import com.martonegyed.data.remote.TmdbApiService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class MoviePickerSwipeDecision {
    PASS,
    SAVE,
    IGNORE
}

data class MoviePickerCandidateUi(
    val localMovieId: Long? = null,
    val tmdbId: Int? = null,
    val title: String,
    val year: Long? = null,
    val posterPath: String? = null,
    val overview: String? = null,
    val runtimeMinutes: Int? = null,
    val tmdbVoteAverage: Double? = null,
    val source: MoviePickerCandidateSource = MoviePickerCandidateSource.LOCAL
)

enum class MoviePickerCandidateSource {
    LOCAL,
    REMOTE
}

data class MoviePickerDeckUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val totalLocalCandidates: Int = 0,
    val totalRemoteCandidates: Int = 0,
    val queue: List<MoviePickerCandidateUi> = emptyList(),
    val mightWatch: List<MoviePickerCandidateUi> = emptyList(),
    val hiddenForSession: Set<String> = emptySet(),
    val lastRemoved: MoviePickerCandidateUi? = null,
    val lastDecision: MoviePickerSwipeDecision? = null,
    val showUndo: Boolean = false,
    val showMightWatchSheet: Boolean = false
)

fun MoviePickerCandidateUi.stableKey(): String =
    tmdbId?.toString() ?: "local-${localMovieId ?: title}"

class MoviePickerResultsScreenModel(
    private val request: MoviePickerRequest,
    private val tmdbApiService: TmdbApiService,
    private val database: CineGraphDatabase,
    private val discoveryManagerRepository: DiscoveryManagerRepository,
) : ScreenModel {

    private val _uiState = MutableStateFlow(MoviePickerDeckUiState())
    val uiState: StateFlow<MoviePickerDeckUiState> = _uiState

    private var originalQueue: List<MoviePickerCandidateUi> = emptyList()
    private var lastLoadedLocalCount: Int = 0
    private var lastLoadedRemoteCount: Int = 0

    private var undoDismissJob: Job? = null

    init {
        load()
    }

    private fun load() {
        screenModelScope.launch {
            _uiState.value = MoviePickerDeckUiState(isLoading = true)

            try {
                val loadedResults = loadCandidatesFromExistingPipeline()
                    .filterNot { candidate ->
                        val tmdbId = candidate.tmdbId ?: return@filterNot false
                        discoveryManagerRepository.isIgnored(tmdbId)
                    }

                cacheRemoteCandidates(loadedResults)

                originalQueue = loadedResults.shuffled()

                _uiState.value = MoviePickerDeckUiState(
                    isLoading = false,
                    totalLocalCandidates = loadedResults.count { it.source == MoviePickerCandidateSource.LOCAL },
                    totalRemoteCandidates = loadedResults.count { it.source == MoviePickerCandidateSource.REMOTE },
                    queue = originalQueue
                )
            } catch (t: Throwable) {
                _uiState.value = MoviePickerDeckUiState(
                    isLoading = false,
                    errorMessage = t.message ?: "Failed to load movie picker results."
                )
            }
        }
    }

    private suspend fun loadCandidatesFromExistingPipeline(): List<MoviePickerCandidateUi> {
        val allLocalRows = database.movieEntityQueries.getAllMovies().executeAsList()
        val watchedIds: Set<Long> =
            database.movieEntityQueries.getWatchedMovieIds().executeAsList().toSet()

        val localLibraryTmdbIds = allLocalRows
            .mapNotNull { row -> row.tmdbId?.toIntOrNull() }
            .toSet()

        val localCandidates = when (request.source) {
            MoviePickerSearchSource.MY_LIBRARY,
            MoviePickerSearchSource.BOTH -> {
                allLocalRows
                    .asSequence()
                    .filter { row ->
                        when (request.watchIntent) {
                            MoviePickerWatchIntent.SOMETHING_NEW -> row.id !in watchedIds
                            MoviePickerWatchIntent.REWATCH -> row.id in watchedIds
                            MoviePickerWatchIntent.ANYTHING -> true
                        }
                    }
                    .filter { row ->
                        matchesCommonFilters(
                            candidate = MoviePickerCandidateUi(
                                localMovieId = row.id,
                                tmdbId = row.tmdbId?.toIntOrNull(),
                                title = row.name,
                                year = row.year,
                                posterPath = row.posterPath,
                                overview = row.overview,
                                runtimeMinutes = row.runtimeMinutes?.toInt(),
                                tmdbVoteAverage = row.tmdbVoteAverage,
                                source = MoviePickerCandidateSource.LOCAL
                            ),
                            originalLanguage = row.originalLanguage,
                            genresJson = row.genres,
                        )
                    }
                    .map { row ->
                        MoviePickerCandidateUi(
                            localMovieId = row.id,
                            tmdbId = row.tmdbId?.toIntOrNull(),
                            title = row.name,
                            year = row.year,
                            posterPath = row.posterPath,
                            overview = row.overview,
                            runtimeMinutes = row.runtimeMinutes?.toInt(),
                            tmdbVoteAverage = row.tmdbVoteAverage,
                            source = MoviePickerCandidateSource.LOCAL
                        )
                    }
                    .distinctBy { it.stableKey() }
                    .toList()
            }

            MoviePickerSearchSource.DISCOVER_NEW -> emptyList()
        }

        val remoteCandidates = when (request.source) {
            MoviePickerSearchSource.DISCOVER_NEW,
            MoviePickerSearchSource.BOTH -> {
                if (request.watchIntent == MoviePickerWatchIntent.REWATCH) {
                    emptyList()
                } else {
                    loadRemoteCandidates(
                        excludedTmdbIds = localLibraryTmdbIds
                    )
                }
            }

            MoviePickerSearchSource.MY_LIBRARY -> emptyList()
        }

        lastLoadedLocalCount = localCandidates.size
        lastLoadedRemoteCount = remoteCandidates.size

        return when (request.source) {
            MoviePickerSearchSource.MY_LIBRARY -> localCandidates
            MoviePickerSearchSource.DISCOVER_NEW -> remoteCandidates
            MoviePickerSearchSource.BOTH -> (localCandidates + remoteCandidates)
                .distinctBy { it.stableKey() }
        }
    }

    private suspend fun loadRemoteCandidates(
        excludedTmdbIds: Set<Int>
    ): List<MoviePickerCandidateUi> {
        val results = mutableListOf<MoviePickerCandidateUi>()
        val seenIds = mutableSetOf<Int>()

        var page = 1
        var totalPages = Int.MAX_VALUE
        val maxCandidatesToInspect = request.searchDepth * 3

        val fromYear = request.selectedDecades.minOrNull()
        val toYear = request.selectedDecades.maxOrNull()?.plus(9)

        val minRuntime = request.runtimeMinutes.first.takeIf { it > 0 }
        val maxRuntime = request.runtimeMinutes.endInclusive.takeIf { it < 240 }

        val minVoteAverage = request.minimumRating.takeIf { it > 0f }

        while (
            results.size < request.searchDepth &&
            seenIds.size < maxCandidatesToInspect &&
            page <= totalPages
        ) {
            val response = tmdbApiService.discoverMovies(
                castIds = emptyList(),
                crewIds = emptyList(),
                includedGenreIds = request.includedGenreIds.toList(),
                excludedGenreIds = request.excludedGenreIds.toList(),
                originalLanguages = request.languages.toList(),
                fromYear = fromYear,
                toYear = toYear,
                minRuntime = minRuntime,
                maxRuntime = maxRuntime,
                minVoteAverage = minVoteAverage,
                page = page
            ) ?: break

            totalPages = response.totalPages

            for (summary in response.results) {
                if (seenIds.size >= maxCandidatesToInspect) break
                if (!seenIds.add(summary.id)) continue
                if (summary.id in excludedTmdbIds) continue
                if (results.size >= request.searchDepth) break

                val ignored = discoveryManagerRepository.isIgnored(summary.id)
                if (ignored) continue

                results += MoviePickerCandidateUi(
                    localMovieId = null,
                    tmdbId = summary.id,
                    title = summary.title,
                    year = summary.releaseDate?.take(4)?.toLongOrNull(),
                    posterPath = summary.posterPath,
                    overview = summary.overview,
                    runtimeMinutes = null,
                    tmdbVoteAverage = summary.voteAverage,
                    source = MoviePickerCandidateSource.REMOTE
                )
            }

            page++
        }

        return results
    }

    private fun matchesCommonFilters(
        candidate: MoviePickerCandidateUi,
        originalLanguage: String?,
        genresJson: String? = null,
        genreIds: List<Int> = emptyList(),
    ): Boolean {
        val yearOk = request.selectedDecades.isEmpty() || candidate.year?.let { year ->
            request.selectedDecades.any { decadeStart -> year in decadeStart..(decadeStart + 9) }
        } == true

        val runtimeOk = candidate.runtimeMinutes?.let { it in request.runtimeMinutes } ?: true
        val ratingOk = (candidate.tmdbVoteAverage ?: 0.0) >= request.minimumRating.toDouble()

        val languageOk = request.languages.isEmpty() ||
                originalLanguage?.lowercase() in request.languages.map { it.lowercase() }.toSet()

        val includeGenresOk = request.includedGenreIds.isEmpty() ||
                request.includedGenreIds.all { genreId ->
                    genreIds.contains(genreId) || jsonContainsGenreId(genresJson, genreId)
                }

        val excludeGenresOk = request.excludedGenreIds.none { genreId ->
            genreIds.contains(genreId) || jsonContainsGenreId(genresJson, genreId)
        }

        return yearOk &&
                runtimeOk &&
                ratingOk &&
                languageOk &&
                includeGenresOk &&
                excludeGenresOk
    }

    private fun jsonContainsGenreId(genresJson: String?, genreId: Int): Boolean {
        if (genresJson.isNullOrBlank()) return false
        return genresJson.contains("\"id\":$genreId") || genresJson.contains("\"id\": $genreId")
    }

    fun onPassTop() {
        swipeTop(MoviePickerSwipeDecision.PASS)
    }

    fun onSaveTop() {
        swipeTop(MoviePickerSwipeDecision.SAVE)
    }

    fun onIgnoreTop() {
        swipeTop(MoviePickerSwipeDecision.IGNORE)
    }

    private fun swipeTop(decision: MoviePickerSwipeDecision) {
        val current = _uiState.value
        val top = current.queue.lastOrNull() ?: return
        val updatedQueue = current.queue.dropLast(1)

        val updatedMightWatch = when (decision) {
            MoviePickerSwipeDecision.SAVE -> current.mightWatch + top
            else -> current.mightWatch
        }

        val updatedHidden = when (decision) {
            MoviePickerSwipeDecision.IGNORE -> current.hiddenForSession + top.stableKey()
            else -> current.hiddenForSession
        }

        _uiState.value = current.copy(
            queue = updatedQueue,
            mightWatch = updatedMightWatch,
            hiddenForSession = updatedHidden,
            lastRemoved = top,
            lastDecision = decision,
            showUndo = true
        )

        undoDismissJob?.cancel()
        undoDismissJob = screenModelScope.launch {
            delay(3_000)
            dismissUndo()
        }

        if (decision == MoviePickerSwipeDecision.IGNORE) {
            persistIgnore(top)
        }
    }

    fun undoLastSwipe() {
        undoDismissJob?.cancel()

        val current = _uiState.value
        val removed = current.lastRemoved ?: return
        val decision = current.lastDecision ?: return

        val restoredQueue = current.queue + removed

        val restoredMightWatch = when (decision) {
            MoviePickerSwipeDecision.SAVE ->
                current.mightWatch.toMutableList().also { list ->
                    val lastIndex = list.indexOfLast { it.stableKey() == removed.stableKey() }
                    if (lastIndex >= 0) list.removeAt(lastIndex)
                }

            else -> current.mightWatch
        }

        val restoredHidden = when (decision) {
            MoviePickerSwipeDecision.IGNORE -> current.hiddenForSession - removed.stableKey()
            else -> current.hiddenForSession
        }

        _uiState.value = current.copy(
            queue = restoredQueue,
            mightWatch = restoredMightWatch,
            hiddenForSession = restoredHidden,
            lastRemoved = null,
            lastDecision = null,
            showUndo = false
        )

        if (decision == MoviePickerSwipeDecision.IGNORE) {
            undoPersistIgnore(removed)
        }
    }

    fun dismissUndo() {
        undoDismissJob?.cancel()
        _uiState.value = _uiState.value.copy(
            showUndo = false,
            lastRemoved = null,
            lastDecision = null
        )
    }

    fun openMightWatch() {
        _uiState.value = _uiState.value.copy(showMightWatchSheet = true)
    }

    fun closeMightWatch() {
        _uiState.value = _uiState.value.copy(showMightWatchSheet = false)
    }

    fun removeFromMightWatch(movie: MoviePickerCandidateUi) {
        _uiState.value = _uiState.value.copy(
            mightWatch = _uiState.value.mightWatch.filterNot { it.stableKey() == movie.stableKey() }
        )
    }

    fun resetSession() {
        _uiState.value = _uiState.value.copy(
            queue = originalQueue.shuffled(),
            mightWatch = emptyList(),
            hiddenForSession = emptySet(),
            lastRemoved = null,
            lastDecision = null,
            showUndo = false,
            showMightWatchSheet = false
        )
    }

    private fun persistIgnore(movie: MoviePickerCandidateUi) {
        screenModelScope.launch {
            discoveryManagerRepository.ignoreMovie(movie)
        }
    }

    private fun undoPersistIgnore(movie: MoviePickerCandidateUi) {
        val tmdbId = movie.tmdbId ?: return
        screenModelScope.launch {
            discoveryManagerRepository.unignoreMovie(tmdbId)
        }
    }

    private fun cacheRemoteCandidates(candidates: List<MoviePickerCandidateUi>) {
        candidates
            .asSequence()
            .filter { it.source == MoviePickerCandidateSource.REMOTE }
            .filter { it.tmdbId != null }
            .forEach { cacheRemoteCandidate(it) }
    }

    private fun cacheRemoteCandidate(movie: MoviePickerCandidateUi) {
        val tmdbId = movie.tmdbId?.toString() ?: return
        val queries = database.movieEntityQueries

        val existingId = queries.getMovieIdByTmdbId(tmdbId).executeAsOneOrNull()

        if (existingId != null) {
            queries.markMovieCached(existingId)
            return
        }

        queries.insertMovie(
            name = movie.title,
            year = movie.year ?: 0,
            letterboxdUri = null,
            imdbId = null,
            isWatched = 0,
            inWatchlist = 0,
            isCached = 1,
            posterPath = movie.posterPath,
            backdropPath = null,
            overview = movie.overview,
            runtimeMinutes = movie.runtimeMinutes?.toLong(),
            tmdbId = tmdbId,
            tagline = null,
            originalTitle = null,
            originalLanguage = null,
            budget = null,
            revenue = null,
            genres = null,
            hungarianTitle = null,
            tmdbPopularity = null,
            tmdbVoteAverage = movie.tmdbVoteAverage,
            tmdbVoteCount = null,
            collectionName = null,
            trailerKey = null,
            mpaaRating = null,
            addedDate = null,
            studios = null,
            productionCountries = null,
            spokenLanguages = null,
            similarMovies = null,
            tmdbReviews = null
        )
    }
}