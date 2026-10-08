package com.martonegyed.data.repository.discovery

import com.martonegyed.data.database.CineGraphDatabase
import com.martonegyed.data.remote.TmdbApiService
import com.martonegyed.domain.model.*
import com.martonegyed.domain.repository.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalDiscoveryRepository(
    private val database: CineGraphDatabase,
    private val tmdbApiService: TmdbApiService,
    private val discoveryManagerRepository: DiscoveryManagerRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : DiscoveryRepository {
    override suspend fun getGenres(): List<MovieGenre> =
        tmdbApiService.getMovieGenres()?.genres.orEmpty().map { MovieGenre(it.id, it.name) }

    override suspend fun getCandidates(request: MoviePickerRequest): List<DiscoveryCandidate> = withContext(dispatcher) {
        val candidates = loadCandidates(request).filterNot { candidate ->
            candidate.tmdbId?.let { discoveryManagerRepository.isIgnored(it) } ?: false
        }
        database.transaction { cacheRemoteCandidates(candidates) }
        candidates
    }

    private suspend fun loadCandidates(request: MoviePickerRequest): List<DiscoveryCandidate> {
        val allLocalRows = database.movieEntityQueries.getAllMovies().executeAsList()
        val watchedIds: Set<Long> =
            database.movieEntityQueries.getWatchedMovieIds().executeAsList().toSet()

        val localLibraryTmdbIds = allLocalRows
            .mapNotNull { row -> row.tmdbId?.toIntOrNull() }
            .toSet()

        val localCandidates = when (request.source) {
            MoviePickerSearchSource.MY_LIBRARY,
            MoviePickerSearchSource.BOTH -> {
                allLocalRows
                    .asSequence()
                    .filter { row ->
                        when (request.watchIntent) {
                            MoviePickerWatchIntent.SOMETHING_NEW -> row.id !in watchedIds
                            MoviePickerWatchIntent.REWATCH -> row.id in watchedIds
                            MoviePickerWatchIntent.ANYTHING -> true
                        }
                    }
                    .filter { row ->
                        matchesCommonFilters(
                            request = request,
                            candidate = DiscoveryCandidate(
                                localMovieId = row.id,
                                tmdbId = row.tmdbId?.toIntOrNull(),
                                title = row.name,
                                year = row.year,
                                posterPath = row.posterPath,
                                overview = row.overview,
                                runtimeMinutes = row.runtimeMinutes?.toInt(),
                                tmdbVoteAverage = row.tmdbVoteAverage,
                                source = MoviePickerCandidateSource.LOCAL
                            ),
                            originalLanguage = row.originalLanguage,
                            genresJson = row.genres,
                        )
                    }
                    .map { row ->
                        DiscoveryCandidate(
                            localMovieId = row.id,
                            tmdbId = row.tmdbId?.toIntOrNull(),
                            title = row.name,
                            year = row.year,
                            posterPath = row.posterPath,
                            overview = row.overview,
                            runtimeMinutes = row.runtimeMinutes?.toInt(),
                            tmdbVoteAverage = row.tmdbVoteAverage,
                            source = MoviePickerCandidateSource.LOCAL
                        )
                    }
                    .distinctBy { it.stableKey() }
                    .toList()
            }

            MoviePickerSearchSource.DISCOVER_NEW -> emptyList()
        }

        val remoteCandidates = when (request.source) {
            MoviePickerSearchSource.DISCOVER_NEW,
            MoviePickerSearchSource.BOTH -> {
                if (request.watchIntent == MoviePickerWatchIntent.REWATCH) {
                    emptyList()
                } else {
                    loadRemoteCandidates(
                        request = request,
                        excludedTmdbIds = localLibraryTmdbIds
                    )
                }
            }

            MoviePickerSearchSource.MY_LIBRARY -> emptyList()
        }


        return when (request.source) {
            MoviePickerSearchSource.MY_LIBRARY -> localCandidates
            MoviePickerSearchSource.DISCOVER_NEW -> remoteCandidates
            MoviePickerSearchSource.BOTH -> (localCandidates + remoteCandidates)
                .distinctBy { it.stableKey() }
        }
    }

    private suspend fun loadRemoteCandidates(
        request: MoviePickerRequest,
        excludedTmdbIds: Set<Int>
    ): List<DiscoveryCandidate> {
        val results = mutableListOf<DiscoveryCandidate>()
        val seenIds = mutableSetOf<Int>()

        var page = 1
        var totalPages = Int.MAX_VALUE
        val maxCandidatesToInspect = request.searchDepth * 3

        val fromYear = request.selectedDecades.minOrNull()
        val toYear = request.selectedDecades.maxOrNull()?.plus(9)

        val minRuntime = request.runtimeMinutes.first.takeIf { it > 0 }
        val maxRuntime = request.runtimeMinutes.endInclusive.takeIf { it < 240 }

        val minVoteAverage = request.minimumRating.takeIf { it > 0f }

        while (
            results.size < request.searchDepth &&
            seenIds.size < maxCandidatesToInspect &&
            page <= totalPages
        ) {
            val response = tmdbApiService.discoverMovies(
                castIds = emptyList(),
                crewIds = emptyList(),
                includedGenreIds = request.includedGenreIds.toList(),
                excludedGenreIds = request.excludedGenreIds.toList(),
                originalLanguages = request.languages.toList(),
                fromYear = fromYear,
                toYear = toYear,
                minRuntime = minRuntime,
                maxRuntime = maxRuntime,
                minVoteAverage = minVoteAverage,
                page = page
            ) ?: break

            totalPages = response.totalPages

            for (summary in response.results) {
                if (seenIds.size >= maxCandidatesToInspect) break
                if (!seenIds.add(summary.id)) continue
                if (summary.id in excludedTmdbIds) continue
                if (results.size >= request.searchDepth) break

                val ignored = discoveryManagerRepository.isIgnored(summary.id)
                if (ignored) continue

                results += DiscoveryCandidate(
                    localMovieId = null,
                    tmdbId = summary.id,
                    title = summary.title,
                    year = summary.releaseDate?.take(4)?.toLongOrNull(),
                    posterPath = summary.posterPath,
                    overview = summary.overview,
                    runtimeMinutes = null,
                    tmdbVoteAverage = summary.voteAverage,
                    source = MoviePickerCandidateSource.REMOTE
                )
            }

            page++
        }

        return results
    }

    private fun matchesCommonFilters(
        request: MoviePickerRequest,
        candidate: DiscoveryCandidate,
        originalLanguage: String?,
        genresJson: String? = null,
        genreIds: List<Int> = emptyList(),
    ): Boolean {
        val yearOk = request.selectedDecades.isEmpty() || candidate.year?.let { year ->
            request.selectedDecades.any { decadeStart -> year in decadeStart..(decadeStart + 9) }
        } == true

        val runtimeOk = candidate.runtimeMinutes?.let { it in request.runtimeMinutes } ?: true
        val ratingOk = (candidate.tmdbVoteAverage ?: 0.0) >= request.minimumRating.toDouble()

        val languageOk = request.languages.isEmpty() ||
                originalLanguage?.lowercase() in request.languages.map { it.lowercase() }.toSet()

        val includeGenresOk = request.includedGenreIds.isEmpty() ||
                request.includedGenreIds.all { genreId ->
                    genreIds.contains(genreId) || jsonContainsGenreId(genresJson, genreId)
                }

        val excludeGenresOk = request.excludedGenreIds.none { genreId ->
            genreIds.contains(genreId) || jsonContainsGenreId(genresJson, genreId)
        }

        return yearOk &&
                runtimeOk &&
                ratingOk &&
                languageOk &&
                includeGenresOk &&
                excludeGenresOk
    }

    private fun jsonContainsGenreId(genresJson: String?, genreId: Int): Boolean {
        if (genresJson.isNullOrBlank()) return false
        return genresJson.contains("\"id\":$genreId") || genresJson.contains("\"id\": $genreId")
    }

    private fun cacheRemoteCandidates(candidates: List<DiscoveryCandidate>) {
        candidates
            .asSequence()
            .filter { it.source == MoviePickerCandidateSource.REMOTE }
            .filter { it.tmdbId != null }
            .forEach { cacheRemoteCandidate(it) }
    }

    private fun cacheRemoteCandidate(movie: DiscoveryCandidate) {
        val tmdbId = movie.tmdbId?.toString() ?: return
        val queries = database.movieEntityQueries

        val existingId = queries.getMovieIdByTmdbId(tmdbId).executeAsOneOrNull()

        if (existingId != null) {
            queries.markMovieCached(existingId)
            return
        }

        queries.insertMovie(
            name = movie.title,
            year = movie.year ?: 0,
            letterboxdUri = null,
            imdbId = null,
            isWatched = 0,
            inWatchlist = 0,
            isCached = 1,
            posterPath = movie.posterPath,
            backdropPath = null,
            overview = movie.overview,
            runtimeMinutes = movie.runtimeMinutes?.toLong(),
            tmdbId = tmdbId,
            tagline = null,
            originalTitle = null,
            originalLanguage = null,
            budget = null,
            revenue = null,
            genres = null,
            hungarianTitle = null,
            tmdbPopularity = null,
            tmdbVoteAverage = movie.tmdbVoteAverage,
            tmdbVoteCount = null,
            collectionName = null,
            trailerKey = null,
            mpaaRating = null,
            addedDate = null,
            studios = null,
            productionCountries = null,
            spokenLanguages = null,
            similarMovies = null,
            tmdbReviews = null
        )
    }
}
