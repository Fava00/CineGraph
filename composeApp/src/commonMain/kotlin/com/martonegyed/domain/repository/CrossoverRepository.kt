package com.martonegyed.domain.repository

import com.martonegyed.domain.model.*

interface CrossoverRepository {
    suspend fun getGenres(): List<MovieGenre>
    suspend fun getLocalSuggestions(role: PersonRole, query: String): List<PersonSuggestion>
    suspend fun getRemoteSuggestions(role: PersonRole, query: String): List<PersonSuggestion>
    suspend fun search(request: CrossoverRequest): List<CrossoverMovie>
}
