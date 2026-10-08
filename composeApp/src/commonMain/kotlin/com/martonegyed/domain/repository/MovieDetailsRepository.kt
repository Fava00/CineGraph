package com.martonegyed.domain.repository

import com.martonegyed.domain.model.Movie
import com.martonegyed.domain.model.MovieLog

interface MovieDetailsRepository {
    suspend fun getMovie(movieId: Long): Movie?
    suspend fun resolveMovie(movie: Movie): Movie?
    suspend fun getLogs(movieId: Long): List<MovieLog>
    fun needsEnrichment(movie: Movie): Boolean

    suspend fun refreshMovie(movie: Movie): Movie
}
