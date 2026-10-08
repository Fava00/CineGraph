package com.martonegyed.data.repository.catalog

import com.martonegyed.core.util.exportDate
import com.martonegyed.data.database.CineGraphDatabase
import com.martonegyed.data.remote.TmdbApiService
import com.martonegyed.domain.model.Movie
import com.martonegyed.domain.model.MovieSearchResult
import com.martonegyed.domain.repository.MovieSearchRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalMovieSearchRepository(
    private val database: CineGraphDatabase,
    private val api: TmdbApiService,
    private val detailsRepository: SqlDelightMovieDetailsRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : MovieSearchRepository {
    override suspend fun searchMyLibrary(query: String): List<MovieSearchResult> = withContext(dispatcher) {
        val term = query.trim()
        if (term.isEmpty()) return@withContext emptyList()
        database.movieEntityQueries.searchMyLibrary(term).executeAsList().map {
            MovieSearchResult(it.id, it.tmdbId?.toIntOrNull(), it.name, it.year.toInt(),
                it.posterPath, it.overview, it.isWatched == 1L, it.inWatchlist == 1L, true)
        }
    }

    override suspend fun searchTmdb(query: String): List<MovieSearchResult> = withContext(dispatcher) {
        require(query.trim().length >= 2) { "Enter at least two characters" }
        val response = api.searchMovie(query.trim()) ?: error("TMDb search failed. Try again.")
        response.results.map { remote ->
            val linked = database.movieEntityQueries.searchLinkedTmdbMovie(remote.id.toString()).executeAsOneOrNull()
            MovieSearchResult(
                localId = linked?.id,
                tmdbId = remote.id,
                title = remote.title,
                year = remote.releaseDate?.take(4)?.toIntOrNull(),
                posterPath = remote.posterPath ?: linked?.posterPath,
                overview = remote.overview ?: linked?.overview,
                isWatched = linked?.isWatched == 1L,
                inWatchlist = linked?.inWatchlist == 1L,
                isInLibrary = linked?.isInLibrary == 1L
            )
        }
    }

    override suspend fun addToWatchlist(movie: MovieSearchResult): MovieSearchResult = withContext(dispatcher) {
        val tmdbId = movie.tmdbId?.takeIf { it > 0 }
        var localId = if (tmdbId != null) {
            database.movieEntityQueries.getMovieIdByTmdbId(tmdbId.toString()).executeAsOneOrNull()
        } else movie.localId?.takeIf {
            database.movieEntityQueries.getMovieById(it).executeAsOneOrNull() != null
        }
        if (localId == null) {
            requireNotNull(tmdbId) { "This movie is no longer in your library" }
            val details = api.getMovieDetails(tmdbId) ?: error("Could not load this movie from TMDb. Try again.")
            require(details.id == tmdbId) { "TMDb returned a different movie" }
            details.imdbId?.takeIf { it.isNotBlank() }?.let { imdbId ->
                require(database.movieEntityQueries.getMovieIdByImdbId(imdbId).executeAsOneOrNull() == null) {
                    "An imported movie with this IMDb ID already exists. Match that movie before adding it."
                }
            }
            movie.year?.let { year ->
                require(database.movieEntityQueries.findMovieByNameYear(movie.title, year.toLong()).executeAsOneOrNull() == null) {
                    "A movie with this title and year already exists locally. Review it before adding another copy."
                }
            }
            val cached = detailsRepository.cacheSearchedMovie(
                Movie(tmdbId = tmdbId, name = movie.title, year = movie.year ?: 0,
                    posterPath = movie.posterPath, overview = movie.overview, letterboxdUri = null),
                details
            )
            localId = cached.id.takeIf { it > 0 }?.toLong()
                ?: error("Could not save the TMDb movie locally")
        }
        val savedId = requireNotNull(localId)
        database.movieEntityQueries.transaction {
            database.movieEntityQueries.addMovieToWatchlist(savedId, exportDate())
        }
        val linked = database.movieEntityQueries.getMovieById(savedId).executeAsOne()
        movie.copy(localId = linked.id, isWatched = linked.isWatched == 1L,
            inWatchlist = linked.inWatchlist == 1L, isInLibrary = true,
            posterPath = linked.posterPath ?: movie.posterPath,
            overview = linked.overview ?: movie.overview)
    }
}
