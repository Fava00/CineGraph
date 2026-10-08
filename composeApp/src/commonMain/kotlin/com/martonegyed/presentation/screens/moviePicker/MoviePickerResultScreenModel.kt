package com.martonegyed.presentation.screens.moviePicker

import com.martonegyed.domain.repository.DiscoveryRepository
import com.martonegyed.domain.repository.DiscoveryManagerRepository
import com.martonegyed.domain.model.MoviePickerRequest
import com.martonegyed.domain.model.DiscoveryCandidate
import com.martonegyed.domain.model.MoviePickerCandidateSource
import com.martonegyed.domain.model.stableKey
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.martonegyed.core.AppLogger
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

data class MoviePickerDeckUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val totalLocalCandidates: Int = 0,
    val totalRemoteCandidates: Int = 0,
    val queue: List<DiscoveryCandidate> = emptyList(),
    val mightWatch: List<DiscoveryCandidate> = emptyList(),
    val hiddenForSession: Set<String> = emptySet(),
    val lastRemoved: DiscoveryCandidate? = null,
    val lastDecision: MoviePickerSwipeDecision? = null,
    val showUndo: Boolean = false,
    val showMightWatchSheet: Boolean = false
)

class MoviePickerResultsScreenModel(
    private val request: MoviePickerRequest,
    private val repository: DiscoveryRepository,
    private val discoveryManagerRepository: DiscoveryManagerRepository,
) : ScreenModel {

    private val _uiState = MutableStateFlow(MoviePickerDeckUiState())
    val uiState: StateFlow<MoviePickerDeckUiState> = _uiState

    private var originalQueue: List<DiscoveryCandidate> = emptyList()

    private var undoDismissJob: Job? = null
    private var ignoreWriteJob: Job? = null

    init {
        load()
    }

    private fun load() {
        screenModelScope.launch {
            _uiState.value = MoviePickerDeckUiState(isLoading = true)

            try {
                val loadedResults = repository.getCandidates(request)

                originalQueue = loadedResults.shuffled()

                _uiState.value = MoviePickerDeckUiState(
                    isLoading = false,
                    totalLocalCandidates = loadedResults.count { it.source == MoviePickerCandidateSource.LOCAL },
                    totalRemoteCandidates = loadedResults.count { it.source == MoviePickerCandidateSource.REMOTE },
                    queue = originalQueue
                )
            } catch (t: Exception) {
                if (t is kotlinx.coroutines.CancellationException) throw t
                AppLogger.exception(
                    tag = "MoviePicker",
                    throwable = t,
                    message = "Failed to load movie picker results: ${t.message}"
                )
                _uiState.value = MoviePickerDeckUiState(
                    isLoading = false,
                    errorMessage = t.message ?: "Failed to load movie picker results."
                )
            }
        }
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

    fun removeFromMightWatch(movie: DiscoveryCandidate) {
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

    private fun enqueueIgnoreWrite(action: suspend () -> Unit) {
        val previous = ignoreWriteJob
        ignoreWriteJob = screenModelScope.launch {
            previous?.join()
            try {
                action()
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _uiState.value = _uiState.value.copy(errorMessage = "Could not update ignored movies. Please retry.")
            }
        }
    }

    private fun persistIgnore(movie: DiscoveryCandidate) {
        enqueueIgnoreWrite { discoveryManagerRepository.ignoreMovie(movie) }
    }

    private fun undoPersistIgnore(movie: DiscoveryCandidate) {
        val tmdbId = movie.tmdbId ?: return
        enqueueIgnoreWrite { discoveryManagerRepository.unignoreMovie(tmdbId) }
    }

}
