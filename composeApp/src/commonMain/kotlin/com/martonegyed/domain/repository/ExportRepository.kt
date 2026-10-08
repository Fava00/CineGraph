package com.martonegyed.domain.repository

import com.martonegyed.domain.model.ImdbExportBundle
import com.martonegyed.domain.model.LetterboxdExportBundle
import com.martonegyed.domain.model.BackupRestoreProgress

interface ExportRepository {
    suspend fun exportJsonBackup(): String
    suspend fun exportJsonBackup(output: BackupOutput)
    suspend fun restoreJsonBackup(json: String)
    suspend fun restoreJsonBackup(source: BackupSource, onProgress: (BackupRestoreProgress) -> Unit = {})
    suspend fun exportLetterboxd(): LetterboxdExportBundle
    suspend fun exportImdb(onLookupProgress: (completed: Int, total: Int) -> Unit = { _, _ -> }): ImdbExportBundle
}
