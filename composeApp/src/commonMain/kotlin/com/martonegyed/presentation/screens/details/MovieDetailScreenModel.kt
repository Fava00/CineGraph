package com.martonegyed.presentation.screens.details

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.martonegyed.core.AppLogger
import com.martonegyed.data.database.CineGraphDatabase
import com.martonegyed.data.remote.TmdbApiService
import com.martonegyed.domain.model.Movie
import com.martonegyed.domain.model.Person
import com.martonegyed.domain.model.SimilarMovie
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

data class MovieLog(
    val id: Long,
    val watchedDate: String?,
    val rating: Double?,
    val review: String?,
    val isRewatch: Boolean
)

class MovieDetailScreenModel(
    private val database: CineGraphDatabase,
    private val tmdbService: TmdbApiService
) : ScreenModel {

    private val _movie = MutableStateFlow<Movie?>(null)
    val movie = _movie.asStateFlow()

    private val _logs = MutableStateFlow<List<MovieLog>>(emptyList())
    val logs = _logs.asStateFlow()

    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog = _showDeleteDialog.asStateFlow()

    private val _isEnriching = MutableStateFlow(false)
    val isEnriching = _isEnriching.asStateFlow()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun init(initialMovie: Movie) {
        screenModelScope.launch {
            val fullMovie = loadFullMovie(initialMovie.id.toLong()) ?: initialMovie
            _movie.value = fullMovie
            loadLogs(initialMovie.id.toLong())
            enrichIfNeeded(fullMovie)
        }
    }

    private fun loadLogs(movieId: Long) {
        screenModelScope.launch {
            val rawLogs = database.movieEntityQueries
                .getLogsForMovie(movieId)
                .executeAsList()
            _logs.value = rawLogs.map {
                MovieLog(
                    id = it.id,
                    watchedDate = it.watchedDate,
                    rating = it.rating,
                    review = it.review,
                    isRewatch = it.isRewatch == 1L
                )
            }
        }
    }

    private fun enrichIfNeeded(movie: Movie) {
        if (!needsTmdbEnrichment(movie)) return
        enrichMovie(movie)
    }

    private fun needsTmdbEnrichment(movie: Movie): Boolean {
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

    fun refreshDetails() {
        _movie.value?.let { movie ->
            enrichMovie(movie)
        }
    }

    private fun enrichMovie(movie: Movie) {
        val tmdbId = movie.tmdbId ?: return
        if (tmdbId <= 0) return

        screenModelScope.launch {
            _isEnriching.value = true
            try {
                val details = tmdbService.getMovieDetails(tmdbId) ?: return@launch

                val enrichedSimilarMovies =
                    details.similar?.results
                        ?.take(10)
                        ?.map { t ->
                            SimilarMovie(
                                tmdbId = t.id,
                                name = t.title,
                                year = t.releaseDate?.take(4)?.toIntOrNull(),
                                posterPath = t.posterPath,
                                originalTitle = t.title,
                                originalLanguage = t.originalLanguage,
                                backdropPath = t.backdropPath,
                                overview = t.overview,
                                tmdbVoteAverage = t.voteAverage,
                                tmdbVoteCount = t.voteCount
                            )
                        }
                        ?.takeIf { it.isNotEmpty() }

                val enrichedReviews =
                    details.reviews?.results
                        ?.take(5)
                        ?.map { "${it.author}: ${it.content}" }
                        ?.takeIf { it.isNotEmpty() }

                val enriched = movie.copy(
                    runtimeMinutes = details.runtime ?: movie.runtimeMinutes,
                    tagline = details.tagline ?: movie.tagline,
                    originalTitle = details.originalTitle ?: movie.originalTitle,
                    originalLanguage = details.originalLanguage ?: movie.originalLanguage,
                    overview = details.overview ?: movie.overview,
                    revenue = details.revenue ?: movie.revenue,
                    budget = details.budget?.toInt() ?: movie.budget,
                    imdbId = details.imdbId ?: movie.imdbId,
                    collectionName = details.collection?.name ?: movie.collectionName,
                    trailerKey = details.trailerKey ?: movie.trailerKey,
                    mpaaRating = details.mpaaRating ?: movie.mpaaRating,
                    hungarianTitle = details.hungarianTitle ?: movie.hungarianTitle,
                    tmdbVoteAverage = details.voteAverage ?: movie.tmdbVoteAverage,
                    tmdbVoteCount = details.voteCount ?: movie.tmdbVoteCount,
                    tmdbPopularity = details.popularity ?: movie.tmdbPopularity,
                    genres = details.genres.map { it.name }.takeIf { it.isNotEmpty() } ?: movie.genres,
                    studios = details.studios.map { it.name }.takeIf { it.isNotEmpty() } ?: movie.studios,
                    productionCountries = details.productionCountries.map { it.name }
                        .takeIf { it.isNotEmpty() } ?: movie.productionCountries,
                    spokenLanguages = details.spokenLanguages.map { it.englishName }
                        .takeIf { it.isNotEmpty() } ?: movie.spokenLanguages,
                    actors = details.credits?.cast
                        ?.map {
                            Person(
                                name = it.name,
                                character = it.character,
                                profilePath = it.profilePath,
                                job = "Actor"
                            )
                        }
                        ?.takeIf { it.isNotEmpty() }
                        ?: movie.actors,
                    crew = details.credits?.crew
                        ?.map {
                            Person(
                                name = it.name,
                                job = it.job,
                                profilePath = it.profilePath
                            )
                        }
                        ?.takeIf { it.isNotEmpty() }
                        ?: movie.crew,
                    similarMovies = enrichedSimilarMovies ?: movie.similarMovies,
                    tmdbReviews = enrichedReviews ?: movie.tmdbReviews,
                    posterPath = details.posterPath ?: movie.posterPath,
                    backdropPath = details.backdropPath ?: movie.backdropPath,
                )

                _movie.value = enriched

                database.movieEntityQueries.updateMovieWithTmdb(
                    posterPath = enriched.posterPath,
                    backdropPath = enriched.backdropPath,
                    overview = enriched.overview,
                    runtimeMinutes = enriched.runtimeMinutes?.toLong(),
                    tmdbId = enriched.tmdbId?.toString(),
                    tagline = enriched.tagline,
                    originalTitle = enriched.originalTitle,
                    originalLanguage = enriched.originalLanguage,
                    budget = enriched.budget?.toLong(),
                    revenue = enriched.revenue,
                    genres = enriched.genres?.let(json::encodeToString),
                    hungarianTitle = enriched.hungarianTitle,
                    tmdbPopularity = enriched.tmdbPopularity,
                    tmdbVoteAverage = enriched.tmdbVoteAverage,
                    tmdbVoteCount = enriched.tmdbVoteCount?.toLong(),
                    collectionName = enriched.collectionName,
                    trailerKey = enriched.trailerKey,
                    mpaaRating = enriched.mpaaRating,
                    studios = enriched.studios?.let(json::encodeToString),
                    productionCountries = enriched.productionCountries?.let(json::encodeToString),
                    spokenLanguages = enriched.spokenLanguages?.let(json::encodeToString),
                    similarMovies = enriched.similarMovies?.let(json::encodeToString),
                    tmdbReviews = enriched.tmdbReviews?.let(json::encodeToString),
                    id = enriched.id.toLong()
                )

                database.movieEntityQueries.deletePersonsForMovie(enriched.id.toLong())

                enriched.actors.orEmpty().forEach { person ->
                    database.movieEntityQueries.insertMoviePerson(
                        movieId = enriched.id.toLong(),
                        name = person.name.orEmpty(),
                        job = person.job ?: "Actor",
                        character = person.character,
                        profilePath = person.profilePath
                    )
                }

                enriched.crew.orEmpty().forEach { person ->
                    database.movieEntityQueries.insertMoviePerson(
                        movieId = enriched.id.toLong(),
                        name = person.name.orEmpty(),
                        job = person.job ?: "",
                        character = person.character,
                        profilePath = person.profilePath
                    )
                }
            } catch (e: Exception) {
                AppLogger.exception(
                    tag = "MovieDetailScreenModel",
                    throwable = e,
                    message = "enrich Movie, ${e.message}"
                )
            } finally {
                _isEnriching.value = false
            }
        }
    }

    fun requestDelete() {
        _showDeleteDialog.value = true
    }

    fun dismissDeleteDialog() {
        _showDeleteDialog.value = false
    }

    private suspend fun loadFullMovie(movieId: Long): Movie? {
        val row = database.movieEntityQueries
            .getMovieById(movieId)
            .executeAsOneOrNull()
            ?: return null

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

        return Movie(
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
