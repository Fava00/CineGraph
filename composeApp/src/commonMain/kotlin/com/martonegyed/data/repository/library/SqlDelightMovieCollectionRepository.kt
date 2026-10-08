package com.martonegyed.data.repository.library

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.martonegyed.core.util.mapCollectionRow
import com.martonegyed.data.database.CineGraphDatabase
import com.martonegyed.domain.model.MovieCollectionRow
import com.martonegyed.domain.model.MovieFilterType
import com.martonegyed.domain.model.MovieListType
import com.martonegyed.domain.repository.MovieCollectionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SqlDelightMovieCollectionRepository(
    private val database: CineGraphDatabase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : MovieCollectionRepository {
    override fun observeWatchedMovies(): Flow<List<MovieCollectionRow>> =
        database.movieEntityQueries.getWatchedCollectionRows(::mapCollectionRow)
            .asFlow().mapToList(dispatcher)

    override fun observeWatchlistMovies(): Flow<List<MovieCollectionRow>> =
        database.movieEntityQueries.getWatchlistCollectionRows(::mapCollectionRow)
            .asFlow().mapToList(dispatcher)

    override fun observeCachedMovies(): Flow<List<MovieCollectionRow>> =
        database.movieEntityQueries.getCachedCollectionRows(::mapCollectionRow)
            .asFlow().mapToList(dispatcher)

    override suspend fun getMovies(listType: MovieListType): List<MovieCollectionRow> =
        withContext(dispatcher) {
            when (listType) {
                MovieListType.WATCHED -> database.movieEntityQueries
                    .getWatchedCollectionRows(::mapCollectionRow).executeAsList()
                MovieListType.WATCHLIST -> database.movieEntityQueries
                    .getWatchlistCollectionRows(::mapCollectionRow).executeAsList()
            }
        }

    override suspend fun getMoviesByPerson(
        listType: MovieListType,
        personName: String,
        job: String?,
        startDate: String?,
        endDate: String?
    ): List<MovieCollectionRow> = withContext(dispatcher) {
        database.movieEntityQueries.getCollectionRowsByPersonAndDate(
            listType = listType.name,
            personName = personName,
            job = job,
            startDate = startDate,
            endDate = endDate,
            mapper = ::mapCollectionRow
        ).executeAsList()
    }

    override suspend fun getMoviesByFilter(
        listType: MovieListType,
        filterType: MovieFilterType,
        filterName: String,
        startDate: String?,
        endDate: String?
    ): List<MovieCollectionRow> = withContext(dispatcher) {
        database.movieEntityQueries.getCollectionRowsByFilterAndDate(
            listType = listType.name,
            filterType = filterType.name,
            filterName = filterName,
            startDate = startDate,
            endDate = endDate,
            mapper = ::mapCollectionRow
        ).executeAsList()
    }

    override suspend fun getMoviesByDuo(
        listType: MovieListType,
        firstName: String,
        secondName: String,
        firstJob: String?,
        secondJob: String?,
        startDate: String?,
        endDate: String?
    ): List<MovieCollectionRow> = withContext(dispatcher) {
        database.movieEntityQueries.getCollectionRowsByDuoAndDate(
            listType = listType.name,
            firstName = firstName,
            secondName = secondName,
            firstJob = firstJob,
            secondJob = secondJob,
            startDate = startDate,
            endDate = endDate,
            mapper = ::mapCollectionRow
        ).executeAsList()
    }
}
