package com.martonegyed.presentation.screens.details

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.martonegyed.core.AppLogger
import com.martonegyed.domain.model.Movie
import com.martonegyed.domain.model.MovieLog
import com.martonegyed.domain.model.ManualMovieLog
import com.martonegyed.domain.model.MovieLogEntryMode
import com.martonegyed.domain.repository.MovieDetailsRepository
import com.martonegyed.domain.repository.MovieLogRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MovieDetailScreenModel(
    private val repository: MovieDetailsRepository,
    private val logRepository: MovieLogRepository
) : ScreenModel {
    private val _movie = MutableStateFlow<Movie?>(null)
    val movie = _movie.asStateFlow()

    private val _logs = MutableStateFlow<List<MovieLog>>(emptyList())
    val logs = _logs.asStateFlow()

    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog = _showDeleteDialog.asStateFlow()

    private val _isEnriching = MutableStateFlow(false)
    val isEnriching = _isEnriching.asStateFlow()

    private val _logEntryMode = MutableStateFlow<MovieLogEntryMode?>(null)
    val logEntryMode = _logEntryMode.asStateFlow()
    private val _isSavingLog = MutableStateFlow(false)
    val isSavingLog = _isSavingLog.asStateFlow()
    private val _logError = MutableStateFlow<String?>(null)
    val logError = _logError.asStateFlow()
    private val _logMessage = MutableStateFlow<String?>(null)
    val logMessage = _logMessage.asStateFlow()
    private val _editingLog = MutableStateFlow<MovieLog?>(null)
    val editingLog = _editingLog.asStateFlow()
    private val _logToDelete = MutableStateFlow<MovieLog?>(null)
    val logToDelete = _logToDelete.asStateFlow()
    private val _actionError = MutableStateFlow<String?>(null)
    val actionError = _actionError.asStateFlow()
    private val _wasRemoved = MutableStateFlow(false)
    val wasRemoved = _wasRemoved.asStateFlow()
    private var enrichmentJob: Job? = null

    fun openLogEntry(mode: MovieLogEntryMode) {
        if (_isSavingLog.value) return
        _logError.value = null
        _editingLog.value = null
        _logEntryMode.value = mode
    }

    fun editLog(log: MovieLog) {
        if (_isSavingLog.value) return
        val existing = _logs.value.singleOrNull { it.id == log.id } ?: return
        _logError.value = null
        _editingLog.value = existing
        _logEntryMode.value = MovieLogEntryMode.EDIT
    }

    fun dismissLogEntry() {
        if (_isSavingLog.value) return
        _logEntryMode.value = null
        _editingLog.value = null
        _logError.value = null
    }

    fun dismissLogMessage() {
        _logMessage.value = null
    }

    fun saveLog(watchedDate: String?, rating: Double?, review: String?, ratedDate: String?) {
        val movie = _movie.value?.takeIf { it.id > 0 } ?: return
        val mode = _logEntryMode.value ?: return
        val editing = _editingLog.value
        if (_isSavingLog.value) return
        _isSavingLog.value = true
        _logError.value = null
        screenModelScope.launch {
            try {
                when (mode) {
                    MovieLogEntryMode.VIEWING -> logRepository.addViewing(movie.id.toLong(), ManualMovieLog(watchedDate, rating, review))
                    MovieLogEntryMode.RATING -> logRepository.rateMovie(movie.id.toLong(), requireNotNull(rating) { "Choose a rating." })
                    MovieLogEntryMode.EDIT -> logRepository.updateLog(movie.id.toLong(), requireNotNull(editing).id,
                        ManualMovieLog(watchedDate, rating, review, ratedDate))
                }
                reloadMovie(movie.id.toLong())
                _logEntryMode.value = null
                _editingLog.value = null
                _logMessage.value = when (mode) {
                    MovieLogEntryMode.VIEWING -> "Viewing saved"
                    MovieLogEntryMode.RATING -> "Rating saved"
                    MovieLogEntryMode.EDIT -> "Log updated"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _logError.value = e.message ?: "Could not save your log. Try again."
            } finally {
                _isSavingLog.value = false
            }
        }
    }

    private suspend fun reloadMovie(movieId: Long) {
        _movie.value = repository.getMovie(movieId) ?: _movie.value
        _logs.value = repository.getLogs(movieId)
    }

    fun init(initialMovie: Movie) {
        screenModelScope.launch {
            val fullMovie = repository.resolveMovie(initialMovie) ?: initialMovie
            _movie.value = fullMovie
            _logs.value = repository.getLogs(fullMovie.id.toLong())
            if (repository.needsEnrichment(fullMovie)) enrichMovie(fullMovie)
        }
    }

    fun refreshDetails() {
        if (_isSavingLog.value || _wasRemoved.value || _isEnriching.value) return
        _movie.value?.let(::enrichMovie)
    }

    private fun enrichMovie(movie: Movie) {
        val tmdbId = movie.tmdbId ?: return
        if (tmdbId <= 0) return

        enrichmentJob?.cancel()
        enrichmentJob = screenModelScope.launch {
            _isEnriching.value = true
            try {
                val refreshed = repository.refreshMovie(movie)
                reloadMovie(refreshed.id.toLong())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogger.exception(
                    tag = "MovieDetailScreenModel",
                    throwable = e,
                    message = "enrich Movie, ${e.message}"
                )
            } finally {
                _isEnriching.value = false
            }
        }
    }

    fun requestDelete() {
        if (_isSavingLog.value) return
        _actionError.value = null
        _showDeleteDialog.value = true
    }

    fun dismissDeleteDialog() {
        if (_isSavingLog.value) return
        _showDeleteDialog.value = false
        _actionError.value = null
    }

    fun requestDeleteLog(log: MovieLog) {
        if (_isSavingLog.value) return
        _actionError.value = null
        _logToDelete.value = _logs.value.singleOrNull { it.id == log.id }
    }

    fun dismissLogDeletion() {
        if (_isSavingLog.value) return
        _logToDelete.value = null
        _actionError.value = null
    }

    fun confirmDeleteLog() {
        val log = _logToDelete.value ?: return
        mutateHistory(action = { logRepository.deleteLog(it, log.id) }, afterSuccess = {
            reloadMovie(it)
            _logToDelete.value = null
            _logMessage.value = "Log deleted"
        })
    }

    fun confirmRemoveMovie() {
        if (!_showDeleteDialog.value) return
        mutateHistory(action = {
            enrichmentJob?.cancelAndJoin()
            logRepository.removeMovie(it)
        }, afterSuccess = {
            _logs.value = emptyList()
            _showDeleteDialog.value = false
            _wasRemoved.value = true
        })
    }

    private fun mutateHistory(action: suspend (Long) -> Unit, afterSuccess: suspend (Long) -> Unit) {
        val movieId = _movie.value?.id?.takeIf { it > 0 }?.toLong() ?: return
        if (_isSavingLog.value) return
        _isSavingLog.value = true
        _actionError.value = null
        screenModelScope.launch {
            try {
                action(movieId)
                afterSuccess(movieId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionError.value = e.message ?: "Could not remove this item. Try again."
            } finally {
                _isSavingLog.value = false
            }
        }
    }
}
