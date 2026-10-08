package com.martonegyed.domain.repository

import com.martonegyed.domain.model.MovieCollectionRow
import com.martonegyed.domain.model.MovieFilterType
import com.martonegyed.domain.model.MovieListType
import kotlinx.coroutines.flow.Flow

interface MovieCollectionRepository {
    fun observeWatchedMovies(): Flow<List<MovieCollectionRow>>
    fun observeWatchlistMovies(): Flow<List<MovieCollectionRow>>
    fun observeCachedMovies(): Flow<List<MovieCollectionRow>>

    suspend fun getMovies(listType: MovieListType): List<MovieCollectionRow>

    suspend fun getMoviesByPerson(
        listType: MovieListType,
        personName: String,
        job: String?,
        startDate: String?,
        endDate: String?
    ): List<MovieCollectionRow>

    suspend fun getMoviesByFilter(
        listType: MovieListType,
        filterType: MovieFilterType,
        filterName: String,
        startDate: String?,
        endDate: String?
    ): List<MovieCollectionRow>

    suspend fun getMoviesByDuo(
        listType: MovieListType,
        firstName: String,
        secondName: String,
        firstJob: String?,
        secondJob: String?,
        startDate: String?,
        endDate: String?
    ): List<MovieCollectionRow>
}
