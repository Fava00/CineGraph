package com.martonegyed.domain.model

data class BackupRestoreProgress(
    val phase: Phase,
    val completed: Int = 0,
    val total: Int = 0,
    val movieCount: Int = 0
) {
    enum class Phase { COUNTING, RESTORING }
    val remaining: Int get() = (total - completed).coerceAtLeast(0)
}
