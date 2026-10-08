package com.martonegyed.data.local.transfer.export

import com.martonegyed.data.database.CineGraphDatabase
import com.martonegyed.domain.repository.BackupSource
import com.martonegyed.domain.repository.BackupOutput
import com.martonegyed.domain.model.BackupRestoreProgress
import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
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
    val tmdbReviews: String?,
    val stableId: String? = null,
    val libraryStableId: String? = null
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
    val sourceType: String,
    val stableId: String? = null,
    val ratedDate: String? = null,
    val createdAt: String? = null
)

@Serializable
data class CustomListBackupRow(
    val id: Long,
    val name: String,
    val stableId: String? = null
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
            version = 3,
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
                    tmdbReviews = it.tmdbReviews,
                    stableId = it.stableId,
                    libraryStableId = it.libraryStableId
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
                    sourceType = it.sourceType,
                    stableId = it.stableId,
                    ratedDate = it.ratedDate,
                    createdAt = it.createdAt
                )
            },
            customLists = database.movieEntityQueries.selectAllCustomLists().executeAsList().map {
                CustomListBackupRow(
                    id = it.id,
                    name = it.name,
                    stableId = it.stableId
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

    @OptIn(ExperimentalTime::class)
    fun exportJsonBackup(output: BackupOutput, checkActive: () -> Unit = {}) {
        val compactJson = Json { encodeDefaults = true; explicitNulls = false }
        fun write(value: String) = output.write(value.encodeToByteArray())
        fun <T> paged(fetch: (Long) -> List<T>, id: (T) -> Long): Sequence<T> = sequence {
            var afterId = 0L
            while (true) {
                checkActive()
                val batch = fetch(afterId)
                if (batch.isEmpty()) break
                yieldAll(batch)
                afterId = id(batch.last())
            }
        }
        fun <T> writeArray(name: String, serializer: KSerializer<T>, rows: Sequence<T>) {
            write("\"$name\":[")
            var first = true
            for (row in rows) {
                if (!first) write(",")
                write(compactJson.encodeToString(serializer, row))
                first = false
            }
            write("]")
        }

        write("{\"version\":3,\"exportedAt\":")
        write(compactJson.encodeToString(kotlin.time.Clock.System.now().toString()))
        write(",")
        writeArray("movies", MovieBackupRow.serializer(),
            paged({ database.movieEntityQueries.selectBackupMovies(it).executeAsList() }, { it.id }).map {
                MovieBackupRow(
                    id = it.id, name = it.name, year = it.year, letterboxdUri = it.letterboxdUri,
                    imdbId = it.imdbId, isWatched = it.isWatched, inWatchlist = it.inWatchlist,
                    isCached = it.isCached, posterPath = it.posterPath, backdropPath = it.backdropPath,
                    overview = it.overview, runtimeMinutes = it.runtimeMinutes, tmdbId = it.tmdbId,
                    tagline = it.tagline, originalTitle = it.originalTitle, originalLanguage = it.originalLanguage,
                    budget = it.budget, revenue = it.revenue, genres = it.genres,
                    hungarianTitle = it.hungarianTitle, tmdbPopularity = it.tmdbPopularity,
                    tmdbVoteAverage = it.tmdbVoteAverage, tmdbVoteCount = it.tmdbVoteCount,
                    collectionName = it.collectionName, trailerKey = it.trailerKey, mpaaRating = it.mpaaRating,
                    addedDate = it.addedDate, studios = it.studios,
                    productionCountries = it.productionCountries, spokenLanguages = it.spokenLanguages,
                    similarMovies = it.similarMovies, tmdbReviews = it.tmdbReviews,
                    stableId = it.stableId, libraryStableId = it.libraryStableId
                )
            })
        write(",")
        writeArray("people", MoviePersonBackupRow.serializer(),
            paged({ database.movieEntityQueries.selectBackupPeople(it).executeAsList() }, { it.id }).map {
                MoviePersonBackupRow(it.id, it.movieId, it.name, it.job, it.character, it.profilePath)
            })
        write(",")
        writeArray("logs", MovieLogBackupRow.serializer(),
            paged({ database.movieEntityQueries.selectBackupLogs(it).executeAsList() }, { it.id }).map {
                MovieLogBackupRow(it.id, it.movieId, it.watchedDate, it.loggedDate, it.rating,
                    it.review, it.isRewatch, it.sourceType, it.stableId, it.ratedDate, it.createdAt)
            })
        write(",")
        writeArray("customLists", CustomListBackupRow.serializer(),
            paged({ database.movieEntityQueries.selectBackupLists(it).executeAsList() }, { it.id }).map {
                CustomListBackupRow(it.id, it.name, it.stableId)
            })
        write(",")
        val entries = sequence {
            var afterListId = 0L
            var afterMovieId = 0L
            while (true) {
                checkActive()
                val batch = database.movieEntityQueries.selectBackupEntries(
                    afterListId = afterListId, afterMovieId = afterMovieId
                )
                    .executeAsList()
                if (batch.isEmpty()) break
                yieldAll(batch)
                afterListId = batch.last().listId
                afterMovieId = batch.last().movieId
            }
        }
        writeArray("listEntries", ListEntryBackupRow.serializer(),
            entries.map { ListEntryBackupRow(it.listId, it.movieId) })
        write(",")
        writeArray("ignoredMovies", IgnoredMovieBackupRow.serializer(),
            paged({ database.movieEntityQueries.selectBackupIgnored(it).executeAsList() }, { it.tmdbId }).map {
                IgnoredMovieBackupRow(it.tmdbId, it.title, it.year, it.posterPath, it.overview,
                    it.runtimeMinutes, it.tmdbVoteAverage, it.createdAt)
            })
        write("}")
    }

    fun restoreJsonBackup(jsonString: String) {
        val backup = json.decodeFromString(CineGraphBackup.serializer(), jsonString)

        validateBackup(backup)

        database.transaction {
            database.movieEntityQueries.clearForBackupRestore()

            backup.movies.forEach { movie -> restoreMovieRow(movie, backup.version) }

            backup.people.forEach { person -> restorePersonRow(person) }

            backup.logs.forEach { log -> restoreLogRow(log, backup.version) }

            backup.customLists.forEach { list -> restoreListRow(list) }
            backup.listEntries.forEach { entry -> restoreEntryRow(entry) }
            backup.ignoredMovies.forEach { movie -> restoreIgnoredRow(movie) }
        }
    }

    fun restoreJsonBackup(
        source: BackupSource,
        checkActive: () -> Unit = {},
        onProgress: (BackupRestoreProgress) -> Unit = {}
    ) {
        fun scan(tables: Set<String>, onEntry: (String) -> Unit = {}, row: (String, String) -> Unit): Int {
            checkActive()
            val input = source.open()
            try {
                return BackupRecordReader(input, checkActive).read(tables, onEntry) { table, value ->
                    checkActive()
                    row(table, value)
                }
            } finally { input.close() }
        }
        onProgress(BackupRestoreProgress(BackupRestoreProgress.Phase.COUNTING))
        var total = 0
        var movieCount = 0
        val version = scan(emptySet(), onEntry = { table ->
            total++
            if (table == "movies") movieCount++
        }) { _, _ -> }
        var completed = 0
        fun report(force: Boolean = false) {
            if (force || completed % 250 == 0) {
                onProgress(BackupRestoreProgress(BackupRestoreProgress.Phase.RESTORING, completed, total, movieCount))
            }
        }
        report(force = true)
        val movies = mutableSetOf<Long>()
        val lists = mutableSetOf<Long>()
        val ignored = mutableSetOf<Long>()
        val stableIdPattern = Regex("[0-9a-f]{32}")
        fun identity(id: String?, optional: Boolean = false) {
            require((optional || version == 1 || id != null) && (id == null || id.matches(stableIdPattern))) {
                "Missing or malformed stable identifier in backup"
            }
        }
        database.transaction {
            database.movieEntityQueries.clearForBackupRestore()
            val loadedVersion = scan(setOf("movies", "customLists")) { table, value ->
                when (table) {
                    "movies" -> {
                        val movie = json.decodeFromString<MovieBackupRow>(value)
                        require(movie.id > 0 && movies.add(movie.id)) { "Invalid or duplicate movie ID" }
                        identity(movie.stableId)
                        identity(movie.libraryStableId, optional = true)
                        require(version == 1 || movie.libraryStableId != null ||
                            (movie.isWatched == 0L && movie.inWatchlist == 0L && movie.addedDate == null)) {
                            "Personal movie state has no library identifier"
                        }
                        restoreMovieRow(movie, version)
                    }
                    "customLists" -> {
                        val list = json.decodeFromString<CustomListBackupRow>(value)
                        require(list.id > 0 && lists.add(list.id)) { "Invalid or duplicate list ID" }
                        identity(list.stableId)
                        restoreListRow(list)
                    }
                }
                completed++
                report()
            }
            require(loadedVersion == version) { "Backup changed while reading" }
            val relatedVersion = scan(setOf("people", "logs", "listEntries", "ignoredMovies")) { table, value ->
                when (table) {
                    "people" -> {
                        val person = json.decodeFromString<MoviePersonBackupRow>(value)
                        require(person.id > 0 && person.movieId in movies) { "Invalid credit or missing movie" }
                        restorePersonRow(person)
                    }
                    "logs" -> {
                        val log = json.decodeFromString<MovieLogBackupRow>(value)
                        require(log.id > 0 && log.movieId in movies) { "Invalid log or missing movie" }
                        identity(log.stableId)
                        restoreLogRow(log, version)
                    }
                    "listEntries" -> {
                        val entry = json.decodeFromString<ListEntryBackupRow>(value)
                        require(entry.movieId in movies && entry.listId in lists) { "Missing list or movie" }
                        restoreEntryRow(entry)
                    }
                    "ignoredMovies" -> {
                        val movie = json.decodeFromString<IgnoredMovieBackupRow>(value)
                        require(movie.tmdbId > 0 && ignored.add(movie.tmdbId)) { "Invalid or duplicate ignored movie" }
                        restoreIgnoredRow(movie)
                    }
                }
                completed++
                report()
            }
            require(relatedVersion == version) { "Backup changed while reading" }
            checkActive()
        }
        report(force = true)
    }

    private fun restoreMovieRow(movie: MovieBackupRow, version: Int) {
        database.movieEntityQueries.restoreMovie(
            id = movie.id,
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
        movie.stableId?.let { database.movieEntityQueries.restoreMovieIdentity(it, movie.id) }
        movie.libraryStableId?.let { database.movieEntityQueries.restoreLibraryIdentity(it, movie.id) }
        if (version >= 2 && movie.libraryStableId == null) {
            database.movieEntityQueries.deleteLibraryState(movie.id)
        }
    }

    private fun restorePersonRow(person: MoviePersonBackupRow) {
        database.movieEntityQueries.restoreMoviePerson(
            id = person.id,
            movieId = person.movieId,
            name = person.name,
            job = person.job,
            character = person.character,
            profilePath = person.profilePath
        )
    }

    private fun restoreLogRow(log: MovieLogBackupRow, version: Int) {
        database.movieEntityQueries.restoreMovieLog(
            id = log.id,
            movieId = log.movieId,
            watchedDate = log.watchedDate?.takeUnless { version < 3 && log.sourceType.uppercase() == "RATINGS" && it == log.loggedDate },
            loggedDate = log.loggedDate,
            rating = log.rating?.takeUnless { it == 0.0 },
            review = log.review,
            isRewatch = log.isRewatch,
            sourceType = log.sourceType
        )
        log.stableId?.let { database.movieEntityQueries.restoreLogIdentity(it, log.id) }
        database.movieEntityQueries.restoreLogDates(
            ratedDate = if (version < 3 && log.sourceType.uppercase() == "RATINGS") log.loggedDate?.ifBlank { null } else log.ratedDate,
            createdAt = log.createdAt,
            id = log.id
        )
    }

    private fun restoreListRow(list: CustomListBackupRow) {
        database.movieEntityQueries.restoreCustomList(list.id, list.name)
        list.stableId?.let { database.movieEntityQueries.restoreListIdentity(it, list.id) }
    }

    private fun restoreEntryRow(entry: ListEntryBackupRow) {
        database.movieEntityQueries.restoreListEntry(entry.listId, entry.movieId)
    }

    private fun restoreIgnoredRow(movie: IgnoredMovieBackupRow) {
        database.movieEntityQueries.insertOrReplaceIgnoredMovie(
            movie.tmdbId, movie.title, movie.year, movie.posterPath, movie.overview,
            movie.runtimeMinutes, movie.tmdbVoteAverage, movie.createdAt
        )
    }

    private fun validateBackup(backup: CineGraphBackup) {
        require(backup.version in 1..3) { "Unsupported CineGraph backup version: ${backup.version}" }
        fun validateStableIds(ids: List<String?>, entity: String, required: Boolean = true) {
            val present = ids.filterNotNull()
            require((!required || backup.version == 1 || present.size == ids.size) &&
                present.all { it.matches(Regex("[0-9a-f]{32}")) } && present.distinct().size == present.size) {
                "Missing, malformed, or duplicate stable $entity identifiers in backup"
            }
        }
        validateStableIds(backup.movies.map { it.stableId }, "movie")
        validateStableIds(backup.movies.map { it.libraryStableId }, "library", required = false)
        require(backup.version == 1 || backup.movies.all {
            it.libraryStableId != null || (it.isWatched == 0L && it.inWatchlist == 0L && it.addedDate == null)
        }) { "Backup contains personal movie state without a library identifier" }
        validateStableIds(backup.logs.map { it.stableId }, "log")
        validateStableIds(backup.customLists.map { it.stableId }, "list")
        fun validateIds(ids: List<Long>, entity: String) {
            require(ids.all { it > 0 } && ids.distinct().size == ids.size) {
                "Invalid or duplicate $entity identifiers in backup"
            }
        }
        validateIds(backup.movies.map { it.id }, "movie")
        validateIds(backup.people.map { it.id }, "person")
        validateIds(backup.logs.map { it.id }, "log")
        validateIds(backup.customLists.map { it.id }, "list")
        validateIds(backup.ignoredMovies.map { it.tmdbId }, "ignored movie")
        val movieIds = backup.movies.map { it.id }.toSet()
        val listIds = backup.customLists.map { it.id }.toSet()
        require(backup.people.all { it.movieId in movieIds } && backup.logs.all { it.movieId in movieIds }) {
            "Backup contains credits or logs referring to missing movies"
        }
        require(backup.listEntries.all { it.movieId in movieIds && it.listId in listIds }) {
            "Backup contains list entries referring to missing movies or lists"
        }
        require(backup.listEntries.distinct().size == backup.listEntries.size) {
            "Backup contains duplicate list entries"
        }
    }
}

