package com.martonegyed.presentation.screens.import

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.martonegyed.domain.model.StagedMovie
import com.martonegyed.domain.model.CsvImportType
import com.martonegyed.domain.model.CsvImportAssessment
import com.martonegyed.domain.model.MovieImportKey
import com.martonegyed.domain.model.BackupRestoreProgress
import com.martonegyed.domain.model.TmdbMatchCandidate
import com.martonegyed.domain.model.UnmatchedMovie
import com.martonegyed.domain.model.SuggestedTmdbMatch
import com.martonegyed.domain.repository.TmdbMatchReviewRepository
import com.martonegyed.domain.repository.ImportRepository
import com.martonegyed.domain.repository.ExportRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import com.martonegyed.core.AppLogger
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.martonegyed.core.util.backupFileSource
import com.martonegyed.core.util.backupFileOutput
import com.martonegyed.core.util.exportDate
import com.martonegyed.core.util.exportFileName

data class StagedSourceSummary(val name: String, val count: Int, val fileNames: List<String>, val skippedTvCount: Int = 0)

sealed class SyncState {
    object Idle : SyncState()
    data class Loading(val message: String, val backupProgress: BackupRestoreProgress? = null) : SyncState()
    data class Success(val message: String) : SyncState()
    data class Error(val error: String) : SyncState()
}

sealed interface ExportPayload {
    data class BackupDestination(val fileName: String) : ExportPayload
    data class SingleFile(
        val fileName: String,
        val mimeType: String,
        val bytes: ByteArray
    ) : ExportPayload {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as SingleFile

            if (fileName != other.fileName) return false
            if (mimeType != other.mimeType) return false
            if (!bytes.contentEquals(other.bytes)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = fileName.hashCode()
            result = 31 * result + mimeType.hashCode()
            result = 31 * result + bytes.contentHashCode()
            return result
        }
    }

    data class MultiFile(
        val files: List<ExportFile>
    ) : ExportPayload
}

data class ExportFile(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ExportFile

        if (fileName != other.fileName) return false
        if (mimeType != other.mimeType) return false
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}

enum class CsvSourceAction { REPLACE, COMBINE, CANCEL }

data class CsvSourceConflict(
    val platform: String,
    val type: CsvImportType,
    val existingFiles: List<String>,
    val incomingFiles: List<String>,
    val existingRows: Int,
    val incomingRows: Int
)

class ImportScreenModel(
    private val importRepository: ImportRepository,
    private val exportRepository: ExportRepository,
    private val matchReviewRepository: TmdbMatchReviewRepository,
) : ScreenModel {

    private data class SourcePayload(
        val platform: String,
        val type: CsvImportType,
        val items: List<StagedMovie>,
        val fileNames: List<String>,
        val skippedTvCount: Int = 0
    )

    private val _exportPayload = MutableSharedFlow<ExportPayload>(extraBufferCapacity = 1)
    val exportPayload = _exportPayload.asSharedFlow()

    private val _state = MutableStateFlow<SyncState>(SyncState.Idle)
    val state: StateFlow<SyncState> = _state.asStateFlow()

    private val _reviewOpen = MutableStateFlow(false)
    val reviewOpen = _reviewOpen.asStateFlow()
    private val _unmatchedMovies = MutableStateFlow<List<UnmatchedMovie>>(emptyList())
    val unmatchedMovies = _unmatchedMovies.asStateFlow()
    private val _reviewCardVisible = MutableStateFlow(false)
    val reviewCardVisible = _reviewCardVisible.asStateFlow()
    private val _reviewedMovie = MutableStateFlow<UnmatchedMovie?>(null)
    val reviewedMovie = _reviewedMovie.asStateFlow()
    private val _matchCandidates = MutableStateFlow<List<TmdbMatchCandidate>>(emptyList())
    val matchCandidates = _matchCandidates.asStateFlow()
    private val _matchReviewBusy = MutableStateFlow(false)
    val matchReviewBusy = _matchReviewBusy.asStateFlow()
    private val _matchReviewMessage = MutableStateFlow<String?>(null)
    val matchReviewMessage = _matchReviewMessage.asStateFlow()
    private val _suggestionsOpen = MutableStateFlow(false)
    val suggestionsOpen = _suggestionsOpen.asStateFlow()
    private val _suggestions = MutableStateFlow<List<SuggestedTmdbMatch>>(emptyList())
    val suggestions = _suggestions.asStateFlow()
    private val _suggestionProgress = MutableStateFlow<String?>(null)
    val suggestionProgress = _suggestionProgress.asStateFlow()
    private val _suggestionsLoading = MutableStateFlow(false)
    val suggestionsLoading = _suggestionsLoading.asStateFlow()
    private val _suggestionsApplying = MutableStateFlow(false)
    val suggestionsApplying = _suggestionsApplying.asStateFlow()
    private var suggestionSessionHadEntries = false
    private var automaticSuggestionsJob: Job? = null

    private fun closeResolvedSuggestionSession() {
        if (_suggestionsOpen.value && suggestionSessionHadEntries && _suggestions.value.isEmpty() &&
            !_suggestionsLoading.value && !_suggestionsApplying.value) {
            _suggestionsOpen.value = false
        }
    }

    fun openSuggestedMatches() {
        if (_suggestionsApplying.value || _suggestions.value.isEmpty()) return
        automaticSuggestionsJob?.cancel()
        suggestionSessionHadEntries = _suggestions.value.isNotEmpty()
        _suggestionsOpen.value = true
        matchReviewRepository.prepareSuggestions()
    }

    fun refreshSuggestedMatches() {
        if (_suggestionsApplying.value) return
        screenModelScope.launch { matchReviewRepository.refreshSuggestions() }
    }

    fun chooseSuggestedMatch(movieId: Long, candidateId: Int?) {
        if (_suggestionsApplying.value) return
        _suggestions.value = _suggestions.value.map { row ->
            if (row.movie.id == movieId && (candidateId == null || row.candidates.any { it.id == candidateId })) row.copy(selectedId = candidateId) else row
        }
    }

    fun closeSuggestedMatches() {
        if (_suggestionsApplying.value) return
        automaticSuggestionsJob?.cancel()
        _suggestionsOpen.value = false
        val ids = _suggestions.value.map { it.movie.id }
        screenModelScope.launch { matchReviewRepository.dismissSuggestions(ids) }
    }

    fun applySuggestedMatches(movieId: Long? = null) {
        if (_matchReviewBusy.value || importRepository.phase.value != com.martonegyed.domain.repository.ImportPhase.IDLE) return
        val selected = _suggestions.value.filter { it.selectedId != null && (movieId == null || it.movie.id == movieId) }
        val skippedIds = if (movieId == null) _suggestions.value.filter { it.selectedId == null }.map { it.movie.id } else emptyList()
        if (selected.isEmpty()) return
        _suggestionsApplying.value = true
        _matchReviewBusy.value = true
        screenModelScope.launch {
            var linked = 0
            val failures = mutableListOf<String>()
            try {
                matchReviewRepository.dismissSuggestions(skippedIds)
                _suggestions.value = _suggestions.value.filterNot { it.movie.id in skippedIds }
                selected.forEachIndexed { index, row ->
                    _suggestionProgress.value = "Linking ${index + 1} of ${selected.size}: ${row.movie.name}"
                    try {
                        matchReviewRepository.confirmMatch(row.movie.id, row.selectedId!!)
                        linked++
                        _suggestions.value = _suggestions.value.filterNot { it.movie.id == row.movie.id }
                    } catch (e: CancellationException) { throw e
                    } catch (e: Exception) { failures += "${row.movie.name}: ${e.message ?: "Could not link"}" }
                }
                _suggestionProgress.value = "Linked $linked movies. Skipped or failed entries stay in manual review." +
                    if (failures.isNotEmpty()) "\n" + failures.joinToString("\n") else ""
                _matchReviewMessage.value = _suggestionProgress.value
            } finally {
                _suggestionsApplying.value = false
                _matchReviewBusy.value = false
                closeResolvedSuggestionSession()
            }
        }
    }

    init {
        screenModelScope.launch {
            val visibility = TmdbReviewVisibility()
            combine(importRepository.phase, _unmatchedMovies) { phase, movies -> phase to movies.size }
                .collect { (phase, count) -> _reviewCardVisible.value = visibility.update(phase, count) }
        }
        screenModelScope.launch {
            matchReviewRepository.observePreparedSuggestions().collect { prepared ->
                val choices = _suggestions.value.associateBy { it.movie.id }
                _suggestions.value = prepared.map { row ->
                    choices[row.movie.id]?.let { chosen -> row.copy(selectedId = chosen.selectedId) } ?: row
                }
                if (_suggestionsOpen.value && prepared.isNotEmpty()) suggestionSessionHadEntries = true
                closeResolvedSuggestionSession()
            }
        }
        screenModelScope.launch {
            matchReviewRepository.suggestionPreparation.collect { progress ->
                _suggestionsLoading.value = progress.running
                closeResolvedSuggestionSession()
                if (!_suggestionsApplying.value) _suggestionProgress.value =
                    if (progress.running) "Preparing suggestions: ${progress.completed} / ${progress.total}"
                    else "Suggestions prepared. Other entries stay in manual review." +
                        if (progress.failed > 0) " ${progress.failed} searches failed; use Refresh suggestions to retry." else ""
            }
        }
        matchReviewRepository.prepareSuggestions()
        screenModelScope.launch {
            try {
                matchReviewRepository.observeUnmatchedMovies().collect { movies ->
                    _unmatchedMovies.value = movies
                    matchReviewRepository.prepareSuggestions()
                    if (_reviewedMovie.value?.let { selected -> movies.none { it.id == selected.id } } == true) {
                        _reviewedMovie.value = null
                        _matchCandidates.value = emptyList()
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _matchReviewMessage.value = e.message ?: "Could not load unmatched movies"
            }
        }
    }

    fun openMatchReview() {
        _reviewOpen.value = true
    }

    fun retryUnmatchedMovies() {
        if (!importRepository.retryUnmatchedMovies()) {
            _matchReviewMessage.value = "Another import or enrichment is already running."
            return
        }
        _matchReviewMessage.value = "Retrying titles and TMDB aliases within two years..."
        screenModelScope.launch {
            importRepository.phase.first { it == com.martonegyed.domain.repository.ImportPhase.IDLE }
            _matchReviewMessage.value = importRepository.lastMessage.value
        }
    }

    fun closeMatchReview() {
        _reviewOpen.value = false
        _reviewedMovie.value = null
        _matchCandidates.value = emptyList()
    }

    fun selectUnmatchedMovie(movie: UnmatchedMovie) {
        _reviewedMovie.value = movie
        _matchCandidates.value = emptyList()
        _matchReviewMessage.value = null
    }

    fun backToUnmatchedMovies() {
        _reviewedMovie.value = null
        _matchCandidates.value = emptyList()
        _matchReviewMessage.value = null
    }

    fun searchMatchCandidates(query: String) {
        screenModelScope.launch {
            _matchReviewBusy.value = true
            _matchReviewMessage.value = null
            _matchCandidates.value = emptyList()
            try {
                val selected = _reviewedMovie.value
                val results = matchReviewRepository.searchCandidates(query, selected)
                if (_reviewedMovie.value?.id != selected?.id) return@launch
                _matchCandidates.value = results
                if (_matchCandidates.value.isEmpty()) _matchReviewMessage.value = "No TMDb movie results. Try another title."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _matchReviewMessage.value = e.message ?: "TMDb search failed"
            } finally {
                _matchReviewBusy.value = false
            }
        }
    }

    fun confirmMatch(candidate: TmdbMatchCandidate) {
        val movie = _reviewedMovie.value ?: return
        screenModelScope.launch {
            _matchReviewBusy.value = true
            _matchReviewMessage.value = null
            try {
                matchReviewRepository.confirmMatch(movie.id, candidate.id)
                _reviewedMovie.value = null
                _matchCandidates.value = emptyList()
                _matchReviewMessage.value = "Matched ${movie.name} with ${candidate.title}."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _matchReviewMessage.value = e.message ?: "Could not save the TMDb match"
            } finally {
                _matchReviewBusy.value = false
            }
        }
    }

    fun loadTmdbUrl(url: String) {
        if (_matchReviewBusy.value || importRepository.phase.value != com.martonegyed.domain.repository.ImportPhase.IDLE) return
        val movie = _reviewedMovie.value ?: return
        _matchReviewBusy.value = true
        screenModelScope.launch {
            try {
                val candidate = matchReviewRepository.candidateFromUrl(url)
                if (_reviewedMovie.value?.id == movie.id) {
                    _matchCandidates.value = listOf(candidate)
                    _matchReviewMessage.value = "Check this movie, then choose Link this movie."
                }
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                _matchReviewMessage.value = e.message ?: "Could not load TMDB URL"
            } finally { _matchReviewBusy.value = false }
        }
    }

    fun removeAllUnmatchedMovies(movies: List<UnmatchedMovie>) {
        if (_matchReviewBusy.value || importRepository.phase.value != com.martonegyed.domain.repository.ImportPhase.IDLE) return
        _matchReviewBusy.value = true
        screenModelScope.launch {
            try {
                val count = matchReviewRepository.removeUnmatchedMovies(movies.map { it.id })
                backToUnmatchedMovies()
                _matchReviewMessage.value = "Removed $count unmatched entries from this library."
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                _matchReviewMessage.value = e.message ?: "Could not remove unmatched entries"
            } finally { _matchReviewBusy.value = false }
        }
    }

    fun removeUnmatchedMovie(movie: UnmatchedMovie) {
        if (importRepository.phase.value != com.martonegyed.domain.repository.ImportPhase.IDLE) {
            _matchReviewMessage.value = "Wait for enrichment to finish before removing a movie."
            return
        }
        screenModelScope.launch {
            _matchReviewBusy.value = true
            _matchReviewMessage.value = null
            try {
                matchReviewRepository.removeUnmatchedMovie(movie.id)
                if (_reviewedMovie.value?.id == movie.id) backToUnmatchedMovies()
                _matchReviewMessage.value = "Removed ${movie.name} from this library."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _matchReviewMessage.value = e.message ?: "Could not remove movie"
            } finally {
                _matchReviewBusy.value = false
            }
        }
    }

    private val _newMoviesCount = MutableStateFlow(0)
    val newMoviesCount = _newMoviesCount.asStateFlow()

    private val stagedPayloads = mutableMapOf<String, SourcePayload>()
    private val stagedMovies = mutableMapOf<String, StagedMovie>()
    private var stagingRevision = 0L
    private val _stagedCount = MutableStateFlow(0)
    val stagedCount = _stagedCount.asStateFlow()
    private val _stagedSources = MutableStateFlow<Set<String>>(emptySet())
    val stagedSources: StateFlow<Set<String>> = _stagedSources.asStateFlow()

    private fun sourceKey(platform: String, type: String): String {
        return "${platform.lowercase()}:${type.lowercase()}"
    }


    fun removeStagedSource(platform: String, type: String) {
        screenModelScope.launch {
            stagedPayloads.remove(sourceKey(platform, type))
            rebuildStagedState()
            _state.value = SyncState.Idle
        }
    }

    fun clearStaged() {
        csvSelectionRevision++
        pendingCsvs.clear()
        pendingSources.clear()
        _sourceConflict.value = null
        _csvConfirmation.value = null
        _stagedSummary.value = emptyList()
        stagingRevision++
        stagedPayloads.clear()
        stagedMovies.clear()
        _stagedSources.value = emptySet()
        _stagedCount.value = 0
        _newMoviesCount.value = 0
    }

    private data class PendingCsv(val content: String, val assessment: CsvImportAssessment)
    private val pendingCsvs = mutableListOf<PendingCsv>()
    private val pendingSources = mutableListOf<SourcePayload>()
    private val _sourceConflict = MutableStateFlow<CsvSourceConflict?>(null)
    val sourceConflict = _sourceConflict.asStateFlow()
    private val _csvConfirmation = MutableStateFlow<CsvImportAssessment?>(null)
    val csvConfirmation = _csvConfirmation.asStateFlow()
    private val _csvReadBusy = MutableStateFlow(false)
    val csvReadBusy = _csvReadBusy.asStateFlow()
    private var csvSelectionRevision = 0L
    private val _stagedSummary = MutableStateFlow<List<StagedSourceSummary>>(emptyList())
    val stagedSummary = _stagedSummary.asStateFlow()

    fun stageMultipleLetterboxdFiles(files: List<PlatformFile>) = stageCsvFiles(files, "Letterboxd", null)

    fun stageSingleCsv(file: PlatformFile, type: String, platform: String) =
        stageCsvFiles(listOf(file), platform, type)

    private fun stageCsvFiles(files: List<PlatformFile>, platform: String, selectedType: String?) {
        if (_csvReadBusy.value || pendingCsvs.isNotEmpty() || pendingSources.isNotEmpty()) {
            _state.value = SyncState.Error("Finish reviewing the selected CSV files before choosing more files.")
            return
        }
        _csvReadBusy.value = true
        val revision = ++csvSelectionRevision
        screenModelScope.launch {
            val errors = mutableListOf<String>()
            try {
                for (file in files) {
                    val fileName = file.name.lowercase()
                    if (selectedType == null && (fileName in setOf("comments.csv", "profile.csv") ||
                                fileName.startsWith("letterboxd-import-") || !fileName.endsWith(".csv"))) continue
                    _state.value = SyncState.Loading("Checking ${file.name}...")
                    try {
                        val content = file.readBytes().decodeToString()
                        val assessment = importRepository.inspectCsv(content, platform, file.name, selectedType)
                        if (revision != csvSelectionRevision) return@launch
                        val resolved = assessment.resolvedType
                        if (resolved == null) {
                            pendingCsvs.add(PendingCsv(content, assessment))
                        } else {
                            val result = importRepository.parseCsvWithReport(content, platform, resolved.label)
                            if (revision != csvSelectionRevision) return@launch
                            stageParsed(platform, resolved, result.movies, file.name, result.skippedTvCount)
                        }
                    } catch (e: IllegalArgumentException) {
                        errors += "${file.name}: ${e.message ?: "Invalid CSV format"}"
                    }
                }
                _state.value = if (errors.isEmpty()) SyncState.Idle else SyncState.Error(errors.joinToString("\n\n"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = SyncState.Error("Failed to read CSV: ${e.message}")
            } finally {
                _csvReadBusy.value = false
                publishCsvReviews()
            }
        }
    }

    private suspend fun stageParsed(platform: String, type: CsvImportType, items: List<StagedMovie>, fileName: String, skippedTvCount: Int = 0) {
        val payload = SourcePayload(platform, type, items, listOf(fileName), skippedTvCount)
        val key = sourceKey(platform, type.label)
        if (key in stagedPayloads) {
            pendingSources.add(payload)
        } else {
            stagedPayloads[key] = payload
            rebuildStagedState()
        }
    }

    private fun publishCsvReviews() {
        _sourceConflict.value = pendingSources.firstOrNull()?.let { incoming ->
            val existing = stagedPayloads[sourceKey(incoming.platform, incoming.type.label)]
            CsvSourceConflict(incoming.platform, incoming.type, existing?.fileNames.orEmpty(), incoming.fileNames,
                existing?.items?.size ?: 0, incoming.items.size)
        }
        _csvConfirmation.value = if (pendingSources.isEmpty()) pendingCsvs.firstOrNull()?.assessment else null
    }

    fun resolveSourceConflict(action: CsvSourceAction) {
        if (_csvReadBusy.value) return
        val incoming = pendingSources.firstOrNull() ?: return
        _csvReadBusy.value = true
        screenModelScope.launch {
            try {
                val key = sourceKey(incoming.platform, incoming.type.label)
                val existing = stagedPayloads[key]
                if (action != CsvSourceAction.CANCEL) {
                    stagedPayloads[key] = if (action == CsvSourceAction.COMBINE && existing != null) {
                        existing.copy(items = (existing.items + incoming.items).distinct(),
                            fileNames = existing.fileNames + incoming.fileNames,
                            skippedTvCount = existing.skippedTvCount + incoming.skippedTvCount)
                    } else incoming
                }
                pendingSources.remove(incoming)
                if (action != CsvSourceAction.CANCEL) rebuildStagedState()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = SyncState.Error("Could not update staged files: ${e.message}")
            } finally {
                _csvReadBusy.value = false
                publishCsvReviews()
            }
        }
    }

    fun confirmCsvType(type: CsvImportType) {
        if (_csvReadBusy.value) return
        val pending = pendingCsvs.firstOrNull() ?: return
        if (type !in pending.assessment.choices) return
        val revision = csvSelectionRevision
        _csvReadBusy.value = true
        screenModelScope.launch {
            try {
                val result = importRepository.parseCsvWithReport(pending.content, pending.assessment.platform, type.label)
                if (revision != csvSelectionRevision) return@launch
                stageParsed(pending.assessment.platform, type, result.movies, pending.assessment.fileName, result.skippedTvCount)
                pendingCsvs.remove(pending)
                publishCsvReviews()
                if (_state.value !is SyncState.Error) _state.value = SyncState.Idle
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = SyncState.Error("${pending.assessment.fileName}: ${e.message}")
                pendingCsvs.remove(pending)
                publishCsvReviews()
            } finally {
                _csvReadBusy.value = false
            }
        }
    }

    fun cancelCsvReview() {
        if (_csvReadBusy.value) return
        if (pendingCsvs.isNotEmpty()) pendingCsvs.removeAt(0)
        publishCsvReviews()
    }

    private suspend fun recomputeNewMoviesCount() {
        val existing = importRepository.getMovieKeys()

        val existingUris = existing.mapNotNull { it.letterboxdUri }.toHashSet()
        val existingImdbs = existing.mapNotNull { it.imdbId }.toHashSet()
        val existingNameYear = existing.map { it.name.lowercase() to it.year }.toHashSet()

        var newCount = 0

        for ((name, yearInt, uri, imdb) in stagedMovies.values) {

            val isExisting = when {
                !uri.isNullOrBlank() && existingUris.contains(uri) -> true
                !imdb.isNullOrBlank() && existingImdbs.contains(imdb) -> true
                yearInt > 0 && existingNameYear.contains(name.lowercase() to yearInt) -> true
                else -> false
            }

            if (!isExisting) newCount++
        }

        _newMoviesCount.value = newCount
    }


    private fun mergeIntoStaged(parsedData: List<StagedMovie>) {
        for (movie in parsedData) {
            val uri = movie.letterboxdUri?.let {
                if (it.contains("/film/")) "https://letterboxd.com/film/${it.substringAfter("/film/").substringBefore("/")}/" else it
            }
            val key = "${movie.name.lowercase()}_${movie.year}"
            val existing = stagedMovies[key]
            stagedMovies[key] = existing?.copy(
                isWatched = existing.isWatched || movie.isWatched,
                inWatchlist = existing.inWatchlist || movie.inWatchlist,
                letterboxdUri = existing.letterboxdUri ?: uri,
                imdbId = existing.imdbId ?: movie.imdbId,
                originalTitle = existing.originalTitle ?: movie.originalTitle,
                imdbUrl = existing.imdbUrl ?: movie.imdbUrl,
                addedDate = existing.addedDate ?: movie.addedDate,
                logs = existing.logs + movie.logs
            )
                ?: movie.copy(letterboxdUri = uri)
        }
    }

    fun commitToDatabase() {
        automaticSuggestionsJob?.cancel()
        if (_csvReadBusy.value || pendingCsvs.isNotEmpty() || pendingSources.isNotEmpty()) {
            _state.value = SyncState.Error("Finish reviewing CSV types before importing.")
            return
        }
        screenModelScope.launch {
            val revision = stagingRevision
            val stagedSnapshot = stagedMovies.values.toList()
            val previouslyUnmatched = matchReviewRepository.unmatchedMovies().map { it.id }.toSet()

            val actuallyNew = stagedSnapshot.count { staged ->
                val name = staged.name
                val yearInt = staged.year
                val uri = staged.letterboxdUri
                val imdb = staged.imdbId

                !importRepository.containsMovie(MovieImportKey(name, yearInt, uri, imdb))
            }

            if (revision != stagingRevision) {
                _state.value = SyncState.Error("Staged files changed. Review them before importing again.")
                return@launch
            }

            _state.value = if (actuallyNew == 0) {
                SyncState.Loading("Updating logs and flags for existing movies...")
            } else {
                SyncState.Loading("Adding $actuallyNew new movies and updating existing ones...")
            }

            if (!importRepository.startImportAndEnrich(stagedMovies = stagedSnapshot)) {
                _state.value = SyncState.Error("Another import is already running. Your staged files have been kept.")
                return@launch
            }
            clearStaged()
            reset()
            importRepository.phase.first { it == com.martonegyed.domain.repository.ImportPhase.IDLE }
            val newUnmatched = matchReviewRepository.unmatchedMovies().filter { it.id !in previouslyUnmatched }
            if (newUnmatched.isNotEmpty()) {
                val newIds = newUnmatched.map { it.id }.toSet()
                automaticSuggestionsJob = screenModelScope.launch {
                    combine(importRepository.phase, _suggestions) { phase, ready ->
                        phase == com.martonegyed.domain.repository.ImportPhase.IDLE && ready.any { it.movie.id in newIds }
                    }.first { it }
                    openSuggestedMatches()
                }
                matchReviewRepository.prepareSuggestions()
            }
        }
    }

    fun restoreBackup(file: PlatformFile) {
        screenModelScope.launch {
            try {
                _state.value = SyncState.Loading("Restoring CineGraph backup...")
                exportRepository.restoreJsonBackup(backupFileSource(file)) { progress ->
                    _state.value = when (progress.phase) {
                        BackupRestoreProgress.Phase.COUNTING -> SyncState.Loading("Counting backup records...")
                        BackupRestoreProgress.Phase.RESTORING -> SyncState.Loading(
                            "Restoring CineGraph backup...", progress
                        )
                    }
                }
                _state.value = SyncState.Success("Backup restored successfully!")
            } catch (e: CancellationException) {
                throw e
            } catch (t: Exception) {
                AppLogger.exception(
                    tag = "ImportScreenModel",
                    throwable = t,
                    message = "restoreBackup failed file=${file.name}"
                )
                _state.value = SyncState.Error("Restore failed: ${t.message}")
            }
        }
    }

    fun exportData(platform: String) {
        screenModelScope.launch {
            try {
                val date = exportDate()
                when (platform) {
                    "CineGraph" -> {
                        _exportPayload.emit(ExportPayload.BackupDestination(
                            exportFileName("cinegraph", "backup", date, "json")
                        ))
                    }

                    "Letterboxd" -> {
                        _state.value = SyncState.Loading("Creating Letterboxd CSV files...")
                        val bundle = exportRepository.exportLetterboxd()

                        _exportPayload.emit(
                            ExportPayload.MultiFile(
                                files = listOf(
                                    ExportFile(exportFileName("letterboxd", "diary", date, "csv"), "text/csv", bundle.diaryCsv.encodeToByteArray()),
                                    ExportFile(exportFileName("letterboxd", "watched", date, "csv"), "text/csv", bundle.watchedCsv.encodeToByteArray()),
                                    ExportFile(exportFileName("letterboxd", "watchlist", date, "csv"), "text/csv", bundle.watchlistCsv.encodeToByteArray()),
                                    ExportFile(exportFileName("letterboxd", "ratings", date, "csv"), "text/csv", bundle.ratingsCsv.encodeToByteArray()),
                                    ExportFile(exportFileName("letterboxd", "reviews", date, "csv"), "text/csv", bundle.reviewsCsv.encodeToByteArray()),
                                    ExportFile(exportFileName("letterboxd", "import-imdb-films", date, "csv"), "text/csv", bundle.imdbFilmsCsv.encodeToByteArray())
                                )
                            )
                        )
                    }

                    "IMDb" -> {
                        _state.value = SyncState.Loading("Creating IMDb CSV files...")
                        val bundle = exportRepository.exportImdb { completed, total ->
                            _state.value = SyncState.Loading(
                                if (total == 0) "Creating IMDb CSV files..."
                                else "Looking up IMDb IDs: ${completed}/${total}..."
                            )
                        }

                        _exportPayload.emit(
                            ExportPayload.MultiFile(
                                files = listOf(
                                    ExportFile(exportFileName("imdb", "ratings", date, "csv"), "text/csv", bundle.ratingsCsv.encodeToByteArray()),
                                    ExportFile(exportFileName("imdb", "watchlist", date, "csv"), "text/csv", bundle.watchlistCsv.encodeToByteArray())
                                )
                            )
                        )
                    }

                    else -> {
                        _state.value = SyncState.Error("Unsupported export type: $platform")
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Exception) {
                AppLogger.exception(
                    tag = "ImportScreenModel",
                    throwable = t,
                    message = "exportData failed platform=$platform, ${t.message}"
                )
                _state.value = SyncState.Error("Export failed: ${t.message}")
            }
        }
    }

    fun writeBackup(file: PlatformFile) {
        screenModelScope.launch {
            try {
                _state.value = SyncState.Loading("Creating JSON backup...")
                exportRepository.exportJsonBackup(backupFileOutput(file))
                _state.value = SyncState.Success("JSON backup exported")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                AppLogger.exception("ImportScreenModel", e, "writeBackup failed")
                _state.value = SyncState.Error("Backup failed: ${e.message}")
            }
        }
    }

    fun onExportSaved(message: String) {
        _state.value = SyncState.Success(message)
    }

    fun onExportCancelled() {
        _state.value = SyncState.Idle
    }

    fun reset() {
        _state.value = SyncState.Idle
    }

    private suspend fun rebuildStagedState() {
        stagingRevision++
        stagedMovies.clear()

        stagedPayloads.values.forEach { payload ->
            mergeIntoStaged(parsedData = payload.items)
        }

        _stagedSummary.value = stagedPayloads.values.map { payload ->
            StagedSourceSummary("${payload.platform} ${payload.type.label}", payload.items.size, payload.fileNames, payload.skippedTvCount)
        }
        _stagedSources.value = stagedPayloads.keys.toSet()
        _stagedCount.value = stagedMovies.size

        recomputeNewMoviesCount()
    }

}
