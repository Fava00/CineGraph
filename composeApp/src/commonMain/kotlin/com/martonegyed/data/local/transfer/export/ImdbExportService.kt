package com.martonegyed.data.local.transfer.export

import com.martonegyed.domain.model.ImdbExportBundle
import com.martonegyed.data.database.CineGraphDatabase

class ImdbExportService(
    private val database: CineGraphDatabase
) {
    private data class MovieRow(
        val id: Long,
        val name: String,
        val year: Long,
        val imdbId: String?,
        val originalTitle: String?,
        val runtimeMinutes: Long?,
        val genres: String?,
        val inWatchlist: Long,
        val addedDate: String?
    )

    fun export(): ImdbExportBundle {
        val movies = database.movieEntityQueries.getImdbExportMovies().executeAsList().map {
            MovieRow(
                id = it.id,
                name = it.name,
                year = it.year,
                imdbId = it.imdbId,
                originalTitle = it.originalTitle,
                runtimeMinutes = it.runtimeMinutes,
                genres = it.genres,
                inWatchlist = it.inWatchlist,
                addedDate = it.addedDate
            )
        }

        val currentRatings = database.movieEntityQueries.getCurrentRatings().executeAsList().associateBy { it.movieId }

        val ratingsHeader =
            "Const,Your Rating,Date Rated,Title,URL,Title Type,IMDb Rating,Runtime (mins),Year,Genres,Num Votes,Release Date,Directors"

        val watchlistHeader =
            "Position,Const,Created,Modified,Description,Title,Original Title,URL,Title Type,IMDb Rating,Runtime (mins),Year,Genres,Num Votes,Release Date,Directors,Your Rating,Date Rated"

        val ratingsRows = buildList {
            movies.forEach { movie ->
                val imdbId = movie.imdbId?.trim().orEmpty()
                if (imdbId.isBlank()) return@forEach

                val latestRatedLog = currentRatings[movie.id]
                    ?: return@forEach

                add(
                    csvRow(
                        imdbId,
                        formatRating(latestRatedLog.rating!!),
                        latestRatedLog.effectiveDate.orEmpty(),
                        movie.name,
                        imdbUrl(imdbId),
                        "movie",
                        "",
                        movie.runtimeMinutes?.toString().orEmpty(),
                        movie.year.toString(),
                        normalizeGenres(movie.genres),
                        "",
                        "",
                        ""
                    )
                )
            }
        }

        val watchlistRows = buildList {
            var position = 1

            movies
                .filter { it.inWatchlist == 1L }
                .sortedBy { it.addedDate ?: "" }
                .forEach { movie ->
                    val imdbId = movie.imdbId?.trim().orEmpty()
                    if (imdbId.isBlank()) return@forEach

                    val latestRatedLog = currentRatings[movie.id]

                    val dateRated = latestRatedLog?.effectiveDate.orEmpty()
                    val yourRating = latestRatedLog?.rating?.let(::formatRating).orEmpty()
                    val created = movie.addedDate.orEmpty()

                    add(
                        csvRow(
                            position.toString(),
                            imdbId,
                            created,
                            created,
                            "",
                            movie.name,
                            movie.originalTitle.orEmpty(),
                            imdbUrl(imdbId),
                            "movie",
                            "",
                            movie.runtimeMinutes?.toString().orEmpty(),
                            movie.year.toString(),
                            normalizeGenres(movie.genres),
                            "",
                            "",
                            "",
                            yourRating,
                            dateRated
                        )
                    )

                    position++
                }
        }

        return ImdbExportBundle(
            ratingsCsv = buildString {
                appendLine(ratingsHeader)
                ratingsRows.forEach(::appendLine)
            },
            watchlistCsv = buildString {
                appendLine(watchlistHeader)
                watchlistRows.forEach(::appendLine)
            }
        )
    }

    private fun imdbUrl(imdbId: String): String =
        "https://www.imdb.com/title/$imdbId/"

    private fun normalizeGenres(genres: String?): String =
        genres?.trim().orEmpty()

    private fun formatRating(rating: Double): String =
        (rating * 2).toInt().toString()

    private fun csvRow(vararg values: String): String =
        values.joinToString(",") { escapeCsv(it) }

    private fun escapeCsv(value: String): String {
        val needsQuotes = value.contains(",") ||
                value.contains("\"") ||
                value.contains("\n") ||
                value.contains("\r")
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuotes) "\"$escaped\"" else escaped
    }
}
