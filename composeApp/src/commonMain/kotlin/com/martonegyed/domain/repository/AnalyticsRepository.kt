package com.martonegyed.domain.repository

import com.martonegyed.domain.model.AnalyticsSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface AnalyticsRepository {
    suspend fun getSnapshot(forceRefresh: Boolean = false): AnalyticsSnapshot
    fun getCachedSnapshot(): AnalyticsSnapshot?
    suspend fun clearCache()
    fun observeSnapshots(): Flow<AnalyticsSnapshot> = flow { emit(getSnapshot()) }
}
