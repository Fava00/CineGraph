package com.martonegyed.data.local.catalog

import com.martonegyed.data.database.CineGraphDatabase
import com.martonegyed.data.remote.TmdbMovieDetailsResponse
import com.martonegyed.domain.model.Movie
import com.martonegyed.domain.model.SimilarMovie
import kotlinx.serialization.json.Json

class MovieMetadataWriter(private val database: CineGraphDatabase) {
    private val json = Json { encodeDefaults = true }

    fun markUnmatched(movieId: Long) {
        database.movieEntityQueries.markMovieUnmatched(movieId)
    }

    fun enrichExisting(movieId: Long, details: TmdbMovieDetailsResponse): Long =
        save(details, movieId = movieId, requireExisting = true)

    fun attachConfirmedMatch(movieId: Long, details: TmdbMovieDetailsResponse): Long =
        save(details, movieId = movieId, requireExisting = true, requireUnmatched = true)

    fun saveSearched(movie: Movie, details: TmdbMovieDetailsResponse): Long {
        require(movie.tmdbId == details.id) { "TMDb returned a different movie" }
        return save(details, movieId = movie.id.takeIf { it > 0 }?.toLong(), candidate = movie)
    }

    private fun save(
        details: TmdbMovieDetailsResponse,
        movieId: Long? = null,
        candidate: Movie? = null,
        requireExisting: Boolean = false,
        requireUnmatched: Boolean = false
    ): Long = database.movieEntityQueries.transactionWithResult {
        val queries = database.movieEntityQueries
        val tmdbId = details.id.takeIf { it > 0 } ?: error("TMDb returned an invalid movie ID")
        val linkedId = queries.getMovieIdByTmdbId(tmdbId.toString()).executeAsOneOrNull()
        val preferred = movieId?.let { queries.getMovieById(it).executeAsOneOrNull() }
        if (requireExisting) requireNotNull(preferred) { "Local movie no longer exists" }
        if (requireUnmatched) {
            require(preferred?.tmdbId == "-1") { "This movie is no longer unmatched" }
        }
        if (requireExisting) {
            require(linkedId == null || linkedId == movieId) {
                "This TMDb movie is already linked to another library movie"
            }
        }
        val targetId = if (requireExisting) movieId else linkedId ?: preferred?.takeIf {
            it.tmdbId == null || it.tmdbId == tmdbId.toString()
        }?.id
        val target = targetId?.let { queries.getMovieById(it).executeAsOneOrNull() }
        require(target?.tmdbId == null || target.tmdbId == "-1" || target.tmdbId == tmdbId.toString()) {
            "This movie is linked to a different TMDb movie"
        }

        val fetchedImdb = details.imdbId?.trim()?.takeIf { it.isNotEmpty() }
        val storedImdb = target?.imdbId?.trim()?.takeIf { it.isNotEmpty() }
        require(fetchedImdb == null || storedImdb == null || fetchedImdb == storedImdb) {
            "TMDb IMDb ID differs from the imported IMDb ID; review required"
        }
        fetchedImdb?.let { imdb ->
            val owner = queries.getMovieIdByImdbId(imdb).executeAsOneOrNull()
            require(owner == null || owner == targetId) {
                "An imported movie with this IMDb ID already exists. Match that movie before adding it."
            }
        }

        val savedId = targetId ?: run {
            val name = requireNotNull(candidate?.name?.takeIf { it.isNotBlank() }) { "Movie title is required" }
            queries.insertCachedMovieIdentity(name, candidate.year.toLong(), tmdbId.toString())
            queries.getLastInsertId().executeAsOne()
        }
        val existing = target ?: queries.getMovieById(savedId).executeAsOne()
        val similar = details.similar?.results?.take(10)?.map { movie ->
            SimilarMovie(
                tmdbId = movie.id,
                name = movie.title,
                year = movie.releaseDate?.take(4)?.toIntOrNull(),
                posterPath = movie.posterPath,
                originalTitle = movie.title,
                originalLanguage = movie.originalLanguage,
                backdropPath = movie.backdropPath,
                overview = movie.overview,
                tmdbVoteAverage = movie.voteAverage,
                tmdbVoteCount = movie.voteCount
            )
        }?.takeIf { it.isNotEmpty() }
        val reviews = details.reviews?.results?.take(5)?.mapNotNull { review ->
            val author = review.author.trim()
            val content = review.content.trim()
            if (author.isBlank() || content.isBlank()) null else "$author: $content"
        }?.takeIf { it.isNotEmpty() }

        queries.updateMovieWithTmdb(
            imdbId = fetchedImdb ?: existing.imdbId,
            posterPath = details.posterPath ?: existing.posterPath ?: candidate?.posterPath,
            backdropPath = details.backdropPath ?: existing.backdropPath ?: candidate?.backdropPath,
            overview = details.overview ?: existing.overview ?: candidate?.overview,
            runtimeMinutes = details.runtime?.toLong() ?: existing.runtimeMinutes,
            tmdbId = tmdbId.toString(),
            tagline = details.tagline ?: existing.tagline,
            originalTitle = details.originalTitle ?: existing.originalTitle,
            originalLanguage = details.originalLanguage ?: existing.originalLanguage,
            budget = details.budget ?: existing.budget,
            revenue = details.revenue ?: existing.revenue,
            genres = details.genres.map { it.name }.takeIf { it.isNotEmpty() }
                ?.let(json::encodeToString) ?: existing.genres,
            hungarianTitle = details.hungarianTitle ?: existing.hungarianTitle,
            tmdbPopularity = details.popularity ?: existing.tmdbPopularity,
            tmdbVoteAverage = details.voteAverage ?: existing.tmdbVoteAverage,
            tmdbVoteCount = details.voteCount?.toLong() ?: existing.tmdbVoteCount,
            collectionName = details.collection?.name ?: existing.collectionName,
            trailerKey = details.trailerKey ?: existing.trailerKey,
            mpaaRating = details.mpaaRating ?: existing.mpaaRating,
            studios = details.studios.map { it.name }.takeIf { it.isNotEmpty() }
                ?.let(json::encodeToString) ?: existing.studios,
            productionCountries = details.productionCountries.map { it.name }.takeIf { it.isNotEmpty() }
                ?.let(json::encodeToString) ?: existing.productionCountries,
            spokenLanguages = details.spokenLanguages.map { it.englishName }.takeIf { it.isNotEmpty() }
                ?.let(json::encodeToString) ?: existing.spokenLanguages,
            similarMovies = similar?.let(json::encodeToString) ?: existing.similarMovies,
            tmdbReviews = reviews?.let(json::encodeToString) ?: existing.tmdbReviews,
            id = savedId
        )

        val cast = details.credits?.cast.orEmpty()
        val crew = details.credits?.crew.orEmpty()
        if (cast.isNotEmpty() || crew.isNotEmpty()) {
            val previous = queries.getPersonsForMovie(savedId).executeAsList()
            queries.deletePersonsForMovie(savedId)
            if (cast.isNotEmpty()) {
                cast.forEach { actor ->
                    queries.insertMoviePerson(savedId, actor.name, "Actor", actor.character, actor.profilePath)
                }
            } else {
                previous.filter { it.job == "Actor" }.forEach { person ->
                    queries.insertMoviePerson(savedId, person.name, person.job, person.character, person.profilePath)
                }
            }
            if (crew.isNotEmpty()) {
                crew.forEach { member ->
                    queries.insertMoviePerson(savedId, member.name, member.job, null, member.profilePath)
                }
            } else {
                previous.filter { it.job != "Actor" }.forEach { person ->
                    queries.insertMoviePerson(savedId, person.name, person.job, person.character, person.profilePath)
                }
            }
        }
        savedId
    }
}
