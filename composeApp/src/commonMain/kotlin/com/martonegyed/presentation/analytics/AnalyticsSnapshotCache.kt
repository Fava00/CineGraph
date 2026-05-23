package com.martonegyed.presentation.analytics

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

object AnalyticsSnapshotCache {
    var snapshot: AnalyticsSnapshot? = null
    var lastUpdatedMillis: Long? = null

    @OptIn(ExperimentalTime::class)
    fun isFresh(maxAgeMillis: Long = 5 * 60 * 1000): Boolean {
        val ts = lastUpdatedMillis ?: return false
        return Clock.System.now().toEpochMilliseconds() - ts <= maxAgeMillis
    }

    fun clear() {
        snapshot = null
        lastUpdatedMillis = null
    }
}