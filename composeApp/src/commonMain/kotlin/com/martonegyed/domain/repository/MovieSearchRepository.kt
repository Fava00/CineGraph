package com.martonegyed.domain.repository

import com.martonegyed.domain.model.MovieSearchResult

interface MovieSearchRepository {
    suspend fun searchMyLibrary(query: String): List<MovieSearchResult>
    suspend fun searchTmdb(query: String): List<MovieSearchResult>
    suspend fun addToWatchlist(movie: MovieSearchResult): MovieSearchResult
}
