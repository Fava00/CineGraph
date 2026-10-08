package com.martonegyed.domain.repository

import com.martonegyed.domain.model.*

interface DiscoveryRepository {
    suspend fun getGenres(): List<MovieGenre>
    suspend fun getCandidates(request: MoviePickerRequest): List<DiscoveryCandidate>
}
