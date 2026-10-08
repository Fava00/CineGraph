package com.martonegyed.data.local.catalog

import com.martonegyed.data.database.CineGraphDatabase
import com.martonegyed.data.remote.TmdbMovie
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

internal class TmdbSuggestionSearchCache(private val database: CineGraphDatabase) {
    @OptIn(ExperimentalTime::class)
    fun record(movieId: Long, name: String, year: Int, results: List<TmdbMovie>) {
        database.transaction {
            val current = database.tmdbSuggestionCacheQueries.getSuggestionCache(movieId).executeAsOneOrNull()
            if (current?.name == name && current.year == year.toLong() && current.state in setOf("ready-v3", "none-v3", "dismissed")) return@transaction
            database.tmdbSuggestionCacheQueries.putSuggestionCache(movieId, name, year.toLong(), "seed-v3", "[]",
                Json.encodeToString(results.distinctBy { it.id }), Clock.System.now().toEpochMilliseconds())
        }
    }
}
