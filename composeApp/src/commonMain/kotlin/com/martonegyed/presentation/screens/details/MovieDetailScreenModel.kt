package com.martonegyed.presentation.screens.details

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
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

    private val json = Json { ignoreUnknownKeys = true }

    fun init(initialMovie: Movie) {
        _movie.value = initialMovie
        loadLogs(initialMovie.id.toLong())
        enrichIfNeeded(initialMovie)
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
        if (movie.runtimeMinutes != null && !movie.actors.isNullOrEmpty()) return
        enrichMovie(movie)
    }

    fun refreshDetails() {
        _movie.value?.let { movie ->
            enrichMovie(movie)
        }
    }

    private fun enrichMovie(movie: Movie) {
        val tmdbId = movie.tmdbId ?: return

        screenModelScope.launch {
            _isEnriching.value = true
            try {
                val details = tmdbService.getMovieDetails(tmdbId) ?: return@launch
                val similarMovies = movie.similarMovies
                val tmdbReviews = movie.tmdbReviews

                val enriched = movie.copy(
                    runtimeMinutes = details.runtime ?: movie.runtimeMinutes,
                    tagline = details.tagline ?: movie.tagline,
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
                    genres = details.genres.map { it.name }.ifEmpty { movie.genres },
                    studios = details.studios.map { it.name }.ifEmpty { movie.studios },
                    productionCountries = details.productionCountries.map { it.name }
                        .ifEmpty { movie.productionCountries },
                    spokenLanguages = details.spokenLanguages.map { it.englishName }.ifEmpty { movie.spokenLanguages },
                    actors = details.credits?.cast?.map {
                        Person(name = it.name, character = it.character, profilePath = it.profilePath, job = "Actor")
                    } ?: movie.actors,
                    crew = details.credits?.crew?.map {
                        Person(name = it.name, job = it.job, profilePath = it.profilePath)
                    } ?: movie.crew,
                    similarMovies = details.similar?.results?.take(10)?.map { t ->
                        SimilarMovie(
                            tmdbId = t.id,
                            name = t.title,
                            year = t.releaseDate?.take(4)?.toIntOrNull(),
                            posterPath = t.posterPath,
                            overview = t.overview
                        )
                    } ?: similarMovies,
                    tmdbReviews = details.reviews?.results?.take(5)
                        ?.map { "${it.author}: ${it.content}" }
                        ?: tmdbReviews,
                    posterPath = details.posterPath ?: movie.posterPath,
                    backdropPath = details.backdropPath ?: movie.backdropPath,
                )

                _movie.value = enriched
            } catch (e: Exception) {
                println("Detail enrichment failed: ${e.message}")
            } finally {
                _isEnriching.value = false
            }
        }
    }

    fun requestDelete() {
        _showDeleteDialog.value = true
    }

    fun dismissDeleteDialog(){
        _showDeleteDialog.value = false
    }
}
