package com.martonegyed.data.repository.discovery

import com.martonegyed.data.database.CineGraphDatabase
import com.martonegyed.data.remote.*
import com.martonegyed.domain.model.*
import com.martonegyed.domain.repository.CrossoverRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalCrossoverRepository(
    private val database: CineGraphDatabase,
    private val tmdbApiService: TmdbApiService,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : CrossoverRepository {
    override suspend fun getGenres(): List<MovieGenre> =
        tmdbApiService.getMovieGenres()?.genres.orEmpty().map { MovieGenre(it.id, it.name) }

    override suspend fun getRemoteSuggestions(role: PersonRole, query: String): List<PersonSuggestion> {
        return tmdbApiService.searchPerson(query)
            ?.results
            .orEmpty()
            .filter {
                it.knownForDepartment == null ||
                        it.knownForDepartment.equals(role.tmdbDepartment, ignoreCase = true)
            }
            .map {
                PersonSuggestion(
                    name = it.name,
                    tmdbPersonId = it.id,
                    source = SuggestionSource.TMDB
                )
            }
    }

    override suspend fun search(request: CrossoverRequest): List<CrossoverMovie> {
        val actorPeople = resolvePeople(
            selectedPeople = request.actors,
            expectedDepartment = "Acting"
        )

        val directorPeople = resolvePeople(
            selectedPeople = request.directors,
            expectedDepartment = "Directing"
        )

        require(actorPeople.size == request.actors.size) { "Could not find one or more actors." }
        require(directorPeople.size == request.directors.size) { "Could not find one or more directors." }

        val discovered = mutableListOf<TmdbMovie>()
        val selectedGenres = request.genreIds.toList()

        for (page in 1..3) {
            val response = tmdbApiService.discoverMovies(
                castIds = actorPeople.map { it.id },
                crewIds = directorPeople.map { it.id },
                includedGenreIds = selectedGenres,
                fromYear = request.startYear,
                toYear = request.endYear,
                page = page,
            ) ?: continue

            if (response.results.isEmpty()) break
            discovered += response.results
            if (page >= response.totalPages) break
        }

        val uniqueDiscovered = discovered.distinctBy { it.id }

        val filtered = uniqueDiscovered.filter { movie ->
            movieMatchesAllCriteria(
                movieId = movie.id,
                actorNames = request.actors,
                directorNames = request.directors,
                selectedGenreIds = request.genreIds
            )
        }

        return filtered.map { CrossoverMovie(it.id, it.title, it.posterPath, it.releaseDate) }
    }

    private suspend fun resolvePeople(
        selectedPeople: List<SelectedPerson>,
        expectedDepartment: String
    ): List<TmdbPerson> {
        return selectedPeople.mapNotNull { selected ->
            selected.tmdbPersonId?.let { knownId ->
                return@mapNotNull TmdbPerson(
                    id = knownId,
                    name = selected.name,
                    knownForDepartment = expectedDepartment
                )
            }

            val query = selected.name.trim()
            if (query.isBlank()) return@mapNotNull null

            val results = tmdbApiService.searchPerson(query)?.results.orEmpty()

            results.firstOrNull {
                it.name.equals(query, ignoreCase = true) &&
                        (it.knownForDepartment == null ||
                                it.knownForDepartment.equals(expectedDepartment, ignoreCase = true))
            } ?: results.firstOrNull {
                it.knownForDepartment == null ||
                        it.knownForDepartment.equals(expectedDepartment, ignoreCase = true)
            } ?: results.firstOrNull()
        }
    }

    private suspend fun movieMatchesAllCriteria(
        movieId: Int,
        actorNames: List<SelectedPerson>,
        directorNames: List<SelectedPerson>,
        selectedGenreIds: Set<Int>
    ): Boolean {
        val details = tmdbApiService.getMovieDetails(movieId) ?: return false

        val castNames = details.credits?.cast
            ?.map { it.name.trim().lowercase() }
            .orEmpty()
            .toSet()

        val directorSet = details.credits?.crew
            ?.filter { it.job.equals("Director", ignoreCase = true) }
            ?.map { it.name.trim().lowercase() }
            .orEmpty()
            .toSet()

        val requiredActors = actorNames.map { it.name.trim().lowercase() }
        val requiredDirectors = directorNames.map { it.name.trim().lowercase() }

        val actorsMatch = requiredActors.all { it in castNames }
        val directorsMatch = requiredDirectors.all { it in directorSet }

        val genresMatch = if (selectedGenreIds.isEmpty()) {
            true
        } else {
            details.genres.any { it.id in selectedGenreIds }
        }

        return actorsMatch && directorsMatch && genresMatch
    }

    override suspend fun getLocalSuggestions(
        role: PersonRole,
        query: String
    ): List<PersonSuggestion> = withContext(dispatcher) {
        database.movieEntityQueries
            .getPersonSuggestionsByJob(
                job = role.localJob,
                query = query
            )
            .executeAsList()
            .map { name ->
                PersonSuggestion(
                    name = name,
                    tmdbPersonId = null,
                    source = SuggestionSource.LOCAL
                )
            }
    }

}
