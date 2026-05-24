package com.martonegyed.data.local.export

import com.martonegyed.data.database.CineGraphDatabase
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.ExperimentalTime

@Serializable
data class CineGraphBackup(
    val version: Int = 1,
    val exportedAt: String,
    val movies: List<MovieBackupRow>,
    val people: List<MoviePersonBackupRow>,
    val logs: List<MovieLogBackupRow>,
    val customLists: List<CustomListBackupRow>,
    val listEntries: List<ListEntryBackupRow>,
    val ignoredMovies: List<IgnoredMovieBackupRow>
)

@Serializable
data class MovieBackupRow(
    val id: Long,
    val name: String,
    val year: Long,
    val letterboxdUri: String?,
    val imdbId: String?,
    val isWatched: Long,
    val inWatchlist: Long,
    val isCached: Long,
    val posterPath: String?,
    val backdropPath: String?,
    val overview: String?,
    val runtimeMinutes: Long?,
    val tmdbId: String?,
    val tagline: String?,
    val originalTitle: String?,
    val originalLanguage: String?,
    val budget: Long?,
    val revenue: Long?,
    val genres: String?,
    val hungarianTitle: String?,
    val tmdbPopularity: Double?,
    val tmdbVoteAverage: Double?,
    val tmdbVoteCount: Long?,
    val collectionName: String?,
    val trailerKey: String?,
    val mpaaRating: String?,
    val addedDate: String?,
    val studios: String?,
    val productionCountries: String?,
    val spokenLanguages: String?,
    val similarMovies: String?,
    val tmdbReviews: String?
)

@Serializable
data class MoviePersonBackupRow(
    val id: Long,
    val movieId: Long,
    val name: String,
    val job: String,
    val character: String?,
    val profilePath: String?
)

@Serializable
data class MovieLogBackupRow(
    val id: Long,
    val movieId: Long,
    val watchedDate: String?,
    val loggedDate: String?,
    val rating: Double?,
    val review: String?,
    val isRewatch: Long,
    val sourceType: String
)

@Serializable
data class CustomListBackupRow(
    val id: Long,
    val name: String
)

@Serializable
data class ListEntryBackupRow(
    val listId: Long,
    val movieId: Long
)

@Serializable
data class IgnoredMovieBackupRow(
    val tmdbId: Long,
    val title: String,
    val year: Long?,
    val posterPath: String?,
    val overview: String?,
    val runtimeMinutes: Long?,
    val tmdbVoteAverage: Double?,
    val createdAt: String
)

class BackupExportService(
    private val database: CineGraphDatabase,
    private val json: Json = Json {
        prettyPrint = true
        encodeDefaults = true
        explicitNulls = false
    }
) {
    @OptIn(ExperimentalTime::class)
    fun exportJsonBackup(): String {
        val backup = CineGraphBackup(
            exportedAt = kotlin.time.Clock.System.now().toString(),
            movies = database.movieEntityQueries.selectAllMovieEntities().executeAsList().map {
                MovieBackupRow(
                    id = it.id,
                    name = it.name,
                    year = it.year,
                    letterboxdUri = it.letterboxdUri,
                    imdbId = it.imdbId,
                    isWatched = it.isWatched,
                    inWatchlist = it.inWatchlist,
                    isCached = it.isCached,
                    posterPath = it.posterPath,
                    backdropPath = it.backdropPath,
                    overview = it.overview,
                    runtimeMinutes = it.runtimeMinutes,
                    tmdbId = it.tmdbId,
                    tagline = it.tagline,
                    originalTitle = it.originalTitle,
                    originalLanguage = it.originalLanguage,
                    budget = it.budget,
                    revenue = it.revenue,
                    genres = it.genres,
                    hungarianTitle = it.hungarianTitle,
                    tmdbPopularity = it.tmdbPopularity,
                    tmdbVoteAverage = it.tmdbVoteAverage,
                    tmdbVoteCount = it.tmdbVoteCount,
                    collectionName = it.collectionName,
                    trailerKey = it.trailerKey,
                    mpaaRating = it.mpaaRating,
                    addedDate = it.addedDate,
                    studios = it.studios,
                    productionCountries = it.productionCountries,
                    spokenLanguages = it.spokenLanguages,
                    similarMovies = it.similarMovies,
                    tmdbReviews = it.tmdbReviews
                )
            },
            people = database.movieEntityQueries.selectAllMoviePersons().executeAsList().map {
                MoviePersonBackupRow(
                    id = it.id,
                    movieId = it.movieId,
                    name = it.name,
                    job = it.job,
                    character = it.character,
                    profilePath = it.profilePath
                )
            },
            logs = database.movieEntityQueries.selectAllMovieLogs().executeAsList().map {
                MovieLogBackupRow(
                    id = it.id,
                    movieId = it.movieId,
                    watchedDate = it.watchedDate,
                    loggedDate = it.loggedDate,
                    rating = it.rating,
                    review = it.review,
                    isRewatch = it.isRewatch,
                    sourceType = it.sourceType
                )
            },
            customLists = database.movieEntityQueries.selectAllCustomLists().executeAsList().map {
                CustomListBackupRow(
                    id = it.id,
                    name = it.name
                )
            },
            listEntries = database.movieEntityQueries.selectAllListEntries().executeAsList().map {
                ListEntryBackupRow(
                    listId = it.listId,
                    movieId = it.movieId
                )
            },
            ignoredMovies = database.movieEntityQueries.selectAllIgnoredMovies().executeAsList().map {
                IgnoredMovieBackupRow(
                    tmdbId = it.tmdbId,
                    title = it.title,
                    year = it.year,
                    posterPath = it.posterPath,
                    overview = it.overview,
                    runtimeMinutes = it.runtimeMinutes,
                    tmdbVoteAverage = it.tmdbVoteAverage,
                    createdAt = it.createdAt
                )
            }
        )

        return json.encodeToString(CineGraphBackup.serializer(), backup)
    }

    fun restoreJsonBackup(jsonString: String) {
        val backup = json.decodeFromString(CineGraphBackup.serializer(), jsonString)

        database.transaction {
            database.movieEntityQueries.deleteAllData()

            backup.movies.forEach { movie ->
                database.movieEntityQueries.insertMovie(
                    name = movie.name,
                    year = movie.year,
                    letterboxdUri = movie.letterboxdUri,
                    imdbId = movie.imdbId,
                    isWatched = movie.isWatched,
                    inWatchlist = movie.inWatchlist,
                    isCached = movie.isCached,
                    posterPath = movie.posterPath,
                    backdropPath = movie.backdropPath,
                    overview = movie.overview,
                    runtimeMinutes = movie.runtimeMinutes,
                    tmdbId = movie.tmdbId,
                    tagline = movie.tagline,
                    originalTitle = movie.originalTitle,
                    originalLanguage = movie.originalLanguage,
                    budget = movie.budget,
                    revenue = movie.revenue,
                    genres = movie.genres,
                    hungarianTitle = movie.hungarianTitle,
                    tmdbPopularity = movie.tmdbPopularity,
                    tmdbVoteAverage = movie.tmdbVoteAverage,
                    tmdbVoteCount = movie.tmdbVoteCount,
                    collectionName = movie.collectionName,
                    trailerKey = movie.trailerKey,
                    mpaaRating = movie.mpaaRating,
                    addedDate = movie.addedDate,
                    studios = movie.studios,
                    productionCountries = movie.productionCountries,
                    spokenLanguages = movie.spokenLanguages,
                    similarMovies = movie.similarMovies,
                    tmdbReviews = movie.tmdbReviews
                )
            }

            backup.people.forEach { person ->
                database.movieEntityQueries.insertMoviePerson(
                    movieId = person.movieId,
                    name = person.name,
                    job = person.job,
                    character = person.character,
                    profilePath = person.profilePath
                )
            }

            backup.logs.forEach { log ->
                database.movieEntityQueries.insertMovieLog(
                    movieId = log.movieId,
                    watchedDate = log.watchedDate,
                    loggedDate = log.loggedDate,
                    rating = log.rating,
                    review = log.review,
                    isRewatch = log.isRewatch,
                    sourceType = log.sourceType
                )
            }
        }
    }
}

