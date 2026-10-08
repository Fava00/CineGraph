package com.martonegyed.data.repository.catalog

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.martonegyed.data.database.CineGraphDatabase
import com.martonegyed.data.remote.TmdbApiService
import com.martonegyed.domain.model.TmdbMatchCandidate
import com.martonegyed.domain.model.UnmatchedMovie
import com.martonegyed.domain.model.tmdbMovieIdFromUrl
import com.martonegyed.domain.model.rankTmdbCandidates
import com.martonegyed.domain.model.SuggestedTmdbMatch
import com.martonegyed.domain.model.SuggestionPreparation
import com.martonegyed.data.local.catalog.matchesTmdbTitle
import com.martonegyed.data.local.catalog.searchTmdbMatchCandidates
import com.martonegyed.data.remote.TmdbMovieDetailsResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import com.martonegyed.domain.repository.TmdbMatchReviewRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class LocalTmdbMatchReviewRepository(
    private val database: CineGraphDatabase,
    private val api: TmdbApiService,
    private val detailsRepository: SqlDelightMovieDetailsRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : TmdbMatchReviewRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val preparationScope = CoroutineScope(SupervisorJob() + dispatcher)
    private val requests = Channel<Unit>(Channel.CONFLATED)
    private val _preparation = MutableStateFlow(SuggestionPreparation())
    override val suggestionPreparation = _preparation.asStateFlow()

    init {
        preparationScope.launch {
            for (request in requests) {
                try {
                    database.tmdbSuggestionCacheQueries.deleteResolvedSuggestions()
                    val pending = unmatchedMovies().filter { !hasFreshCache(it) }
                    if (pending.isEmpty()) continue
                    _preparation.value = SuggestionPreparation(true, 0, pending.size)
                    pending.chunked(4).forEach { batch ->
                        coroutineScope {
                            batch.map { movie -> async {
                                try { suggestionCandidates(movie); false
                                } catch (e: CancellationException) { throw e
                                } catch (e: Exception) { saveCache(movie, "error-v3", emptyList()); true }
                            } }.awaitAll().forEach { failed ->
                                _preparation.value = _preparation.value.copy(
                                    completed = _preparation.value.completed + 1,
                                    failed = _preparation.value.failed + if (failed) 1 else 0)
                            }
                        }
                    }
                } catch (e: CancellationException) { throw e
                } catch (e: Exception) {
                    _preparation.value = _preparation.value.copy(failed = _preparation.value.failed + 1)
                } finally { _preparation.value = _preparation.value.copy(running = false) }
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun now() = Clock.System.now().toEpochMilliseconds()

    private fun hasFreshCache(movie: UnmatchedMovie): Boolean {
        val cached = database.tmdbSuggestionCacheQueries.getSuggestionCache(movie.id).executeAsOneOrNull() ?: return false
        if (cached.name != movie.name || cached.year != movie.year.toLong()) return false
        return cached.state == "dismissed" || (cached.state in setOf("ready-v3", "none-v3", "error-v3") && now() - cached.updatedAt <
            if (cached.state == "error-v3") 10 * 60_000L else 7 * 24 * 60 * 60_000L)
    }

    private fun saveCache(movie: UnmatchedMovie, state: String, candidates: List<TmdbMatchCandidate>) {
        database.transaction {
            val current = database.movieEntityQueries.getMovieById(movie.id).executeAsOneOrNull()
            val cache = database.tmdbSuggestionCacheQueries.getSuggestionCache(movie.id).executeAsOneOrNull()
            if (current?.tmdbId != "-1" || current.name != movie.name || current.year != movie.year.toLong() ||
                (cache?.state == "dismissed" && cache.name == movie.name && cache.year == movie.year.toLong())) return@transaction
            database.tmdbSuggestionCacheQueries.putSuggestionCache(movie.id, movie.name, movie.year.toLong(), state,
                json.encodeToString(candidates), cache?.searchJson, now())
        }
    }

    override fun prepareSuggestions() { requests.trySend(Unit) }

    override suspend fun refreshSuggestions() = withContext(dispatcher) {
        database.tmdbSuggestionCacheQueries.clearUnmatchedSuggestionCache()
        prepareSuggestions()
    }

    override suspend fun dismissSuggestions(movieIds: List<Long>) = withContext(dispatcher) {
        database.transaction { movieIds.forEach { database.tmdbSuggestionCacheQueries.dismissSuggestion(it) } }
    }

    override fun observePreparedSuggestions(): Flow<List<SuggestedTmdbMatch>> =
        database.tmdbSuggestionCacheQueries.getReadySuggestions().asFlow().mapToList(dispatcher).map { cached ->
            val movies = unmatchedMovies().associateBy { it.id }
            cached.mapNotNull { row ->
                movies[row.movieId]?.let { movie ->
                    val candidates = json.decodeFromString<List<TmdbMatchCandidate>>(row.candidatesJson)
                    candidates.takeIf { it.isNotEmpty() }?.let { SuggestedTmdbMatch(movie, it) }
                }
            }
        }
    private fun mapUnmatched(row: com.martonegyed.data.database.GetUnmatchedTmdbMovies) =
        UnmatchedMovie(
            row.id, row.name, row.year.toInt(), row.imdbId,
            row.isWatched == 1L, row.inWatchlist == 1L,
            row.logCount.toInt(), row.listCount.toInt(), row.letterboxdUri
        )

    override fun observeUnmatchedMovies(): Flow<List<UnmatchedMovie>> =
        database.movieEntityQueries.getUnmatchedTmdbMovies()
            .asFlow().mapToList(dispatcher).map { rows -> rows.map(::mapUnmatched) }

    override suspend fun unmatchedMovies(): List<UnmatchedMovie> = withContext(dispatcher) {
        database.movieEntityQueries.getUnmatchedTmdbMovies().executeAsList().map(::mapUnmatched)
    }

    override suspend fun removeUnmatchedMovie(movieId: Long) = withContext(dispatcher) {
        database.movieEntityQueries.transaction {
            require(database.movieEntityQueries.getMovieById(movieId).executeAsOneOrNull()?.tmdbId == "-1") {
                "This movie is no longer unmatched"
            }
            database.movieEntityQueries.deleteUnmatchedMovie(movieId)
        }
    }

    override suspend fun searchCandidates(query: String, movie: UnmatchedMovie?): List<TmdbMatchCandidate> {
        require(query.isNotBlank()) { "Enter a title to search TMDb" }
        val response = api.searchMovie(query.trim()) ?: error("TMDb search failed. Try again later.")
        val ranked = response.results.distinctBy { it.id }.sortedWith(
            compareByDescending<com.martonegyed.data.remote.TmdbMovie> { matchesTmdbTitle(movie?.name ?: query, it) }
                .thenBy { if (movie != null && movie.year > 0) kotlin.math.abs((it.releaseDate?.take(4)?.toIntOrNull() ?: 0) - movie.year) else 0 }
                .thenByDescending { it.voteCount ?: 0 }
        ).take(10)
        val candidates = loadCandidates(ranked)
        return if (movie != null) rankTmdbCandidates(movie, candidates) else candidates
    }

    override suspend fun suggestionCandidates(movie: UnmatchedMovie): List<TmdbMatchCandidate> = withContext(dispatcher) {
        val cache = database.tmdbSuggestionCacheQueries.getSuggestionCache(movie.id).executeAsOneOrNull()
        if (hasFreshCache(movie)) return@withContext if (cache?.state == "ready-v3")
            json.decodeFromString<List<TmdbMatchCandidate>>(cache.candidatesJson) else emptyList()
        val searched = if (cache?.state == "seed-v3" && cache.name == movie.name && cache.year == movie.year.toLong() && cache.searchJson != null)
            json.decodeFromString<List<com.martonegyed.data.remote.TmdbMovie>>(cache.searchJson)
        else searchTmdbMatchCandidates(api, movie.name, movie.year)
        val results = searched.distinctBy { it.id }.filter { item ->
            matchesTmdbTitle(movie.name, item) && (movie.year <= 0 ||
                item.releaseDate?.take(4)?.toIntOrNull()?.let { kotlin.math.abs(it - movie.year) <= 2 } == true)
        }
        val candidates = if (results.size < 2) emptyList() else rankTmdbCandidates(movie, loadCandidates(results, requireDetails = true))
        saveCache(movie, if (candidates.isEmpty()) "none-v3" else "ready-v3", candidates)
        candidates
    }

    private suspend fun loadCandidates(items: List<com.martonegyed.data.remote.TmdbMovie>, requireDetails: Boolean = false): List<TmdbMatchCandidate> =
        items.chunked(4).flatMap { batch ->
            coroutineScope {
                batch.map { item -> async {
                    val details = api.getMovieDetails(item.id)?.takeIf { it.id == item.id }
                    check(!requireDetails || details != null) { "Could not load candidate details" }
                    TmdbMatchCandidate(item.id, item.title, item.releaseDate?.take(4)?.toIntOrNull(), item.overview,
                        details?.posterPath ?: item.posterPath,
                        details?.credits?.crew?.filter { it.job == "Director" }?.map { it.name }?.distinct().orEmpty(),
                        item.originalTitle, details?.runtime, details?.popularity, details?.voteCount ?: item.voteCount, item.alternativeTitles)
                } }.awaitAll()
            }
        }

    private fun TmdbMovieDetailsResponse.asCandidate() = TmdbMatchCandidate(
        id, title, releaseDate?.take(4)?.toIntOrNull(), overview, posterPath,
        credits?.crew?.filter { it.job == "Director" }?.map { it.name }?.distinct().orEmpty(), originalTitle,
        runtime, popularity, voteCount
    )

    override suspend fun candidateFromUrl(url: String): TmdbMatchCandidate {
        val id = tmdbMovieIdFromUrl(url)
        val details = api.getMovieDetails(id) ?: error("Could not load this TMDB movie. Check the URL and try again.")
        require(details.id == id && details.title.isNotBlank()) { "TMDB did not return the requested movie." }
        return details.asCandidate()
    }

    override suspend fun removeUnmatchedMovies(movieIds: List<Long>): Int = withContext(dispatcher) {
        var removed = 0
        database.movieEntityQueries.transaction {
            movieIds.distinct().forEach { id ->
                if (database.movieEntityQueries.getMovieById(id).executeAsOneOrNull()?.tmdbId == "-1") {
                    database.movieEntityQueries.deleteUnmatchedMovie(id)
                    removed++
                }
            }
        }
        removed
    }

    override suspend fun confirmMatch(movieId: Long, tmdbId: Int) = withContext(dispatcher) {
        require(tmdbId > 0) { "Invalid TMDb movie ID" }
        val fetched = api.getMovieDetails(tmdbId) ?: error("Could not load TMDb movie details")
        require(fetched.id == tmdbId) { "TMDB returned a different movie." }
        val original = detailsRepository.getMovie(movieId) ?: error("Local movie no longer exists")
        require(original.tmdbId == -1) { "This movie is no longer unmatched" }
        val originalImdb = original.imdbId?.takeIf { it.isNotBlank() }
        val fetchedImdb = fetched.imdbId?.takeIf { it.isNotBlank() }
        require(originalImdb == null || fetchedImdb == null || originalImdb == fetchedImdb) {
            "IMDb IDs differ ($originalImdb vs $fetchedImdb). Review the match before changing the movie."
        }
        require(fetchedImdb == null || database.movieEntityQueries.getMovieIdByImdbId(fetchedImdb).executeAsOneOrNull()?.let { it == movieId } != false) {
            "This IMDb ID is already linked to another library movie"
        }
        detailsRepository.attachConfirmedTmdbMatch(movieId, fetched)
        Unit
    }
}
