package com.martonegyed.data.local.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import com.martonegyed.data.database.CineGraphDatabase

internal object DesktopDatabaseInitializer {
    fun initialize(driver: SqlDriver, beforeMigration: () -> Unit) {
        driver.execute(null, "PRAGMA foreign_keys = ON", 0)
        val version = driver.executeQuery(null, "PRAGMA user_version", { cursor ->
            cursor.next()
            QueryResult.Value(cursor.getLong(0)!!)
        }, 0).value
        val target = CineGraphDatabase.Schema.version
        require(version <= target) { "This database was created by a newer CineGraph version." }
        val tables = driver.executeQuery(null,
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'", { cursor ->
                val result = mutableSetOf<String>()
                while (cursor.next().value) result += cursor.getString(0)!!
                QueryResult.Value(result)
            }, 0).value
        if (tables.isEmpty()) {
            require(version == 0L) { "Database schema is missing; refusing to recreate a versioned database." }
            CineGraphDatabase(driver).transaction {
                CineGraphDatabase.Schema.create(driver)
                driver.execute(null, "PRAGMA user_version = $target", 0)
            }
            return
        }
        if (version == target) return
        require(version in 0L until target) { "Unsupported CineGraph database version: $version" }
        if (version <= 1L) {
            require(tables == legacyColumns.keys) {
                "Unsupported legacy table layout. Original database is unchanged."
            }
            legacyColumns.forEach { (table, expected) ->
                require(table in tables) { "Unsupported legacy schema: missing $table. Original database is unchanged." }
                val columns = driver.executeQuery(null, "PRAGMA table_info($table)", { cursor ->
                    val result = mutableSetOf<String>()
                    while (cursor.next().value) result += cursor.getString(1)!!
                    QueryResult.Value(result)
                }, 0).value
                require(columns == expected) { "Unsupported legacy schema in $table. Original database is unchanged." }
            }
        }
        beforeMigration()
        CineGraphDatabase(driver).transaction {
            CineGraphDatabase.Schema.migrate(driver, maxOf(1L, version), target)
            val violation = driver.executeQuery(null, "PRAGMA foreign_key_check", { cursor ->
                QueryResult.Value(cursor.next().value)
            }, 0).value
            check(!violation) { "Migration failed integrity validation." }
            driver.execute(null, "PRAGMA user_version = $target", 0)
        }
    }

    private val legacyColumns = mapOf(
        "MovieEntity" to "id name year letterboxdUri imdbId isWatched inWatchlist isCached posterPath backdropPath overview runtimeMinutes tmdbId tagline originalTitle originalLanguage budget revenue genres hungarianTitle tmdbPopularity tmdbVoteAverage tmdbVoteCount collectionName trailerKey mpaaRating addedDate studios productionCountries spokenLanguages similarMovies tmdbReviews",
        "MoviePersonEntity" to "id movieId name job character profilePath",
        "MovieLogEntity" to "id movieId watchedDate loggedDate rating review isRewatch sourceType",
        "CustomList" to "id name",
        "ListEntry" to "listId movieId",
        "DiscoveryIgnoredEntity" to "tmdbId title year posterPath overview runtimeMinutes tmdbVoteAverage createdAt"
    ).mapValues { (_, columns) -> columns.split(' ').toSet() }
}
