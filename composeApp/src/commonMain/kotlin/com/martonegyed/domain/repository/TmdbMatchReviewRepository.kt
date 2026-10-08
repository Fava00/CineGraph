package com.martonegyed.domain.repository

import com.martonegyed.domain.model.TmdbMatchCandidate
import com.martonegyed.domain.model.UnmatchedMovie
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import com.martonegyed.domain.model.SuggestedTmdbMatch
import com.martonegyed.domain.model.SuggestionPreparation

interface TmdbMatchReviewRepository {
    val suggestionPreparation: StateFlow<SuggestionPreparation> get() = MutableStateFlow(SuggestionPreparation())
    fun observePreparedSuggestions(): Flow<List<SuggestedTmdbMatch>> = flowOf(emptyList())
    fun prepareSuggestions() {}
    suspend fun refreshSuggestions() {}
    suspend fun dismissSuggestions(movieIds: List<Long>) {}
    fun observeUnmatchedMovies(): Flow<List<UnmatchedMovie>>
    suspend fun unmatchedMovies(): List<UnmatchedMovie>
    suspend fun searchCandidates(query: String, movie: UnmatchedMovie? = null): List<TmdbMatchCandidate>
    suspend fun candidateFromUrl(url: String): TmdbMatchCandidate
    suspend fun suggestionCandidates(movie: UnmatchedMovie): List<TmdbMatchCandidate>
    suspend fun confirmMatch(movieId: Long, tmdbId: Int)
    suspend fun removeUnmatchedMovie(movieId: Long)
    suspend fun removeUnmatchedMovies(movieIds: List<Long>): Int
}
