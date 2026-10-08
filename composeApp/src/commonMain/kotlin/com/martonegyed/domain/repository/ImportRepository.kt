package com.martonegyed.domain.repository

import com.martonegyed.domain.model.StagedMovie
import com.martonegyed.domain.model.CsvImportAssessment
import com.martonegyed.domain.model.MovieImportKey
import com.martonegyed.domain.model.CsvParseResult
import kotlinx.coroutines.flow.StateFlow

enum class ImportPhase { IDLE, IMPORTING, ENRICHING }

interface ImportRepository {
    val phase: StateFlow<ImportPhase>
    val importedCount: StateFlow<Int>
    val importedTotal: StateFlow<Int>
    val enrichedCount: StateFlow<Int>
    val enrichedTotal: StateFlow<Int>
    val lastMessage: StateFlow<String?>
    val hasPendingEnrichment: StateFlow<Boolean>
    val resumePromptShown: StateFlow<Boolean>

    suspend fun parseCsv(content: String, platform: String, type: String): List<StagedMovie>
    suspend fun parseCsvWithReport(content: String, platform: String, type: String): CsvParseResult =
        CsvParseResult(parseCsv(content, platform, type))
    suspend fun inspectCsv(content: String, platform: String, fileName: String, selectedType: String?): CsvImportAssessment
    suspend fun getMovieKeys(): List<MovieImportKey>
    suspend fun containsMovie(key: MovieImportKey): Boolean

    fun startImportAndEnrich(stagedMovies: List<StagedMovie>): Boolean

    fun retryUnmatchedMovies(): Boolean
    fun refreshPendingEnrichment(force: Boolean = false)
    fun markResumePromptShown()
    fun cancelAll()
}
