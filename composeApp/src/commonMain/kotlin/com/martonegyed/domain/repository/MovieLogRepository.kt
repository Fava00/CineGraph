package com.martonegyed.domain.repository

import com.martonegyed.domain.model.ManualMovieLog

interface MovieLogRepository {
    suspend fun addViewing(movieId: Long, log: ManualMovieLog)
    suspend fun rateMovie(movieId: Long, rating: Double)
    suspend fun updateLog(movieId: Long, logId: Long, log: ManualMovieLog)
    suspend fun deleteLog(movieId: Long, logId: Long)
    suspend fun removeMovie(movieId: Long)
}
