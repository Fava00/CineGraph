package com.martonegyed.domain.repository

import com.martonegyed.domain.model.*

interface DiscoveryManagerRepository {
    suspend fun getCachedMovies(): List<DiscoveryMovie>
    suspend fun getIgnoredMovies(): List<DiscoveryMovie>
    suspend fun ignoreMovie(movie: DiscoveryCandidate)
    suspend fun unignoreMovie(tmdbId: Int)
    suspend fun isIgnored(tmdbId: Int): Boolean
}