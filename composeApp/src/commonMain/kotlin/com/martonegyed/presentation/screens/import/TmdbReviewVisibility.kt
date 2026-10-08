package com.martonegyed.presentation.screens.import

import com.martonegyed.domain.repository.ImportPhase

internal class TmdbReviewVisibility {
    private var availableBeforeSync = false

    fun update(phase: ImportPhase, unmatchedCount: Int): Boolean {
        if (phase == ImportPhase.IDLE) availableBeforeSync = unmatchedCount > 0
        return unmatchedCount > 0 && (phase == ImportPhase.IDLE || availableBeforeSync)
    }
}
