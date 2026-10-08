package com.martonegyed.data.repository.catalog

import com.martonegyed.data.local.catalog.MovieMetadataWriter

import com.martonegyed.data.database.CineGraphDatabase
import com.martonegyed.data.remote.TmdbApiService
import com.martonegyed.data.remote.TmdbMovieDetailsResponse
import com.martonegyed.domain.model.Movie
import com.martonegyed.domain.model.MovieLog
import com.martonegyed.domain.model.Person
import com.martonegyed.domain.model.SimilarMovie
import com.martonegyed.domain.repository.MovieDetailsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class SqlDelightMovieDetailsRepository(
    private val database: CineGraphDatabase,
    private val tmdbService: TmdbApiService,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val metadataWriter: MovieMetadataWriter = MovieMetadataWriter(database)
) : MovieDetailsRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override suspend fun getLogs(movieId: Long): List<MovieLog> = withContext(dispatcher) {
        database.movieEntityQueries.getLogsForMovie(movieId).executeAsList().map {
            MovieLog(it.id, it.watchedDate, it.rating, it.review, it.isRewatch == 1L, it.ratedDate, it.createdAt, it.sourceType, it.stableId)
        }
    }

    override suspend fun resolveMovie(movie: Movie): Movie? = withContext(dispatcher) {
        val tmdbId = movie.tmdbId?.takeIf { it > 0 }
        val existingId = tmdbId?.let {
            database.movieEntityQueries.getMovieIdByTmdbId(it.toString()).executeAsOneOrNull()
        }
        if (existingId != null) return@withContext getMovie(existingId)
        if (movie.id <= 0) return@withContext null
        getMovie(movie.id.toLong())?.takeIf {
            tmdbId == null || it.tmdbId == null || it.tmdbId == tmdbId
        }
    }

    override fun needsEnrichment(movie: Movie): Boolean {
        val tmdbId = movie.tmdbId ?: return false
        if (tmdbId <= 0) return false

        val missingCore = movie.runtimeMinutes == null ||
                movie.genres.isNullOrEmpty() ||
                movie.actors.isNullOrEmpty() ||
                movie.crew.isNullOrEmpty()

        val missingExtended = movie.trailerKey.isNullOrBlank() ||
                movie.studios.isNullOrEmpty() ||
                movie.productionCountries.isNullOrEmpty() ||
                movie.spokenLanguages.isNullOrEmpty() ||
                movie.similarMovies.isNullOrEmpty() ||
                movie.tmdbReviews.isNullOrEmpty()

        val missingIdentity = movie.originalTitle.isNullOrBlank() ||
                movie.tagline.isNullOrBlank()

        return missingCore || missingExtended || missingIdentity
    }

    override suspend fun refreshMovie(movie: Movie): Movie = withContext(dispatcher) {
        val tmdbId = movie.tmdbId?.takeIf { it > 0 } ?: return@withContext movie
        val details = tmdbService.getMovieDetails(tmdbId) ?: return@withContext resolveMovie(movie) ?: movie
        getMovie(metadataWriter.saveSearched(movie, details)) ?: error("TMDb movie was not saved")
    }

    suspend fun attachConfirmedTmdbMatch(movieId: Long, details: TmdbMovieDetailsResponse): Movie =
        withContext(dispatcher) {
            val original = getMovie(movieId) ?: error("Local movie no longer exists")
            require(original.tmdbId == -1) { "This movie is no longer unmatched" }
            getMovie(metadataWriter.attachConfirmedMatch(movieId, details)) ?: error("TMDb match was not saved")
        }

    suspend fun cacheSearchedMovie(movie: Movie, details: TmdbMovieDetailsResponse): Movie =
        withContext(dispatcher) {
            getMovie(metadataWriter.saveSearched(movie, details)) ?: error("TMDb movie was not saved")
        }

    override suspend fun getMovie(movieId: Long): Movie? = withContext(dispatcher) {
        val row = database.movieEntityQueries
            .getMovieById(movieId)
            .executeAsOneOrNull()
            ?: return@withContext null

        val persons = database.movieEntityQueries
            .getPersonsForMovie(movieId)
            .executeAsList()

        val actors = persons
            .filter { it.job == "Actor" }
            .map {
                Person(
                    name = it.name,
                    job = it.job,
                    character = it.character,
                    profilePath = it.profilePath
                )
            }

        val crew = persons
            .filter { it.job != "Actor" }
            .map {
                Person(
                    name = it.name,
                    job = it.job,
                    character = it.character,
                    profilePath = it.profilePath
                )
            }

        Movie(
            id = row.id.toInt(),
            tmdbId = row.tmdbId?.toIntOrNull(),
            name = row.name,
            year = row.year.toInt(),
            rating = row.rating,
            watchedDate = row.watchedDate,
            addedDate = row.addedDate,
            inWatchlist = row.inWatchlist == 1L,
            isRewatch = row.isRewatch == 1L,
            posterPath = row.posterPath,
            backdropPath = row.backdropPath,
            overview = row.overview,
            tagline = row.tagline,
            runtimeMinutes = row.runtimeMinutes?.toInt(),
            originalTitle = row.originalTitle,
            originalLanguage = row.originalLanguage,
            hungarianTitle = row.hungarianTitle,
            budget = row.budget?.toInt(),
            revenue = row.revenue,
            tmdbPopularity = row.tmdbPopularity,
            tmdbVoteAverage = row.tmdbVoteAverage,
            tmdbVoteCount = row.tmdbVoteCount?.toInt(),
            collectionName = row.collectionName,
            trailerKey = row.trailerKey,
            mpaaRating = row.mpaaRating,
            imdbId = row.imdbId,
            genres = decodeJsonStringList(row.genres),
            actors = actors.ifEmpty { null },
            crew = crew.ifEmpty { null },
            studios = decodeJsonStringList(row.studios),
            productionCountries = decodeJsonStringList(row.productionCountries),
            spokenLanguages = decodeJsonStringList(row.spokenLanguages),
            similarMovies = decodeSimilarMovies(row.similarMovies),
            tmdbReviews = decodeJsonStringList(row.tmdbReviews),
            letterboxdUri = row.letterboxdUri
        )
    }

    private fun decodeJsonStringList(value: String?): List<String>? {
        val raw = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return runCatching { json.decodeFromString<List<String>>(raw) }
            .onFailure { println("Failed to decode string list JSON: ${it.message}") }
            .getOrNull()
    }

    private fun decodeSimilarMovies(value: String?): List<SimilarMovie>? {
        val raw = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return runCatching { json.decodeFromString<List<SimilarMovie>>(raw) }
            .onFailure { println("Failed to decode similar movies JSON: ${it.message}") }
            .getOrNull()
    }
}
