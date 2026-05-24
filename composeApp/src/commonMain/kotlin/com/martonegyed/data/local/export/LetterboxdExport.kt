package com.martonegyed.data.local.export

import com.martonegyed.data.database.CineGraphDatabase

data class LetterboxdExportBundle(
    val diaryCsv: String,
    val watchedCsv: String,
    val watchlistCsv: String,
    val ratingsCsv: String,
    val reviewsCsv: String
)

class LetterboxdExportService(
    private val database: CineGraphDatabase
) {
    private data class MovieRow(
        val id: Long,
        val name: String,
        val year: Long,
        val letterboxdUri: String?,
        val isWatched: Long,
        val inWatchlist: Long,
        val addedDate: String?
    )

    private data class LogRow(
        val movieId: Long,
        val watchedDate: String?,
        val loggedDate: String?,
        val rating: Double?,
        val review: String?,
        val isRewatch: Long
    )

    fun export(): LetterboxdExportBundle {
        val movies = database.movieEntityQueries.selectAllMovieEntities().executeAsList().map {
            MovieRow(
                id = it.id,
                name = it.name,
                year = it.year,
                letterboxdUri = it.letterboxdUri,
                isWatched = it.isWatched,
                inWatchlist = it.inWatchlist,
                addedDate = it.addedDate
            )
        }

        val logs = database.movieEntityQueries.selectAllMovieLogs().executeAsList().map {
            LogRow(
                movieId = it.movieId,
                watchedDate = it.watchedDate,
                loggedDate = it.loggedDate,
                rating = it.rating,
                review = it.review,
                isRewatch = it.isRewatch
            )
        }

        val moviesById = movies.associateBy { it.id }
        val logsByMovie = logs.groupBy { it.movieId }

        val diaryHeader = "date,name,year,letterboxd uri,rating,rewatch,tags,watched date"
        val reviewsHeader = "date,name,year,letterboxd uri,rating,rewatch,review,tags,watched date"
        val watchedHeader = "date,name,year,letterboxd uri"
        val watchlistHeader = "date,name,year,letterboxd uri"
        val ratingsHeader = "date,name,year,letterboxd uri,rating"

        val diaryRows = buildList {
            logs.forEach { log ->
                val movie = moviesById[log.movieId] ?: return@forEach
                val watchedDate = log.watchedDate ?: return@forEach

                add(
                    csvRow(
                        log.loggedDate ?: watchedDate,
                        movie.name,
                        movie.year.toString(),
                        movie.letterboxdUri.orEmpty(),
                        log.rating?.let(::formatRating).orEmpty(),
                        rewatchValue(log.isRewatch == 1L),
                        "",
                        watchedDate
                    )
                )
            }
        }

        val reviewsRows = buildList {
            logs.forEach { log ->
                val movie = moviesById[log.movieId] ?: return@forEach
                val watchedDate = log.watchedDate ?: return@forEach
                val review = log.review?.trim().orEmpty()
                if (review.isBlank()) return@forEach

                add(
                    csvRow(
                        log.loggedDate ?: watchedDate,
                        movie.name,
                        movie.year.toString(),
                        movie.letterboxdUri.orEmpty(),
                        log.rating?.let(::formatRating).orEmpty(),
                        rewatchValue(log.isRewatch == 1L),
                        review,
                        "",
                        watchedDate
                    )
                )
            }
        }

        val watchedRows = buildList {
            movies.filter { it.isWatched == 1L }.forEach { movie ->
                val firstSeenDate = logsByMovie[movie.id]
                    .orEmpty()
                    .mapNotNull { it.watchedDate ?: it.loggedDate }
                    .sorted()
                    .firstOrNull()
                    .orEmpty()

                add(
                    csvRow(
                        firstSeenDate,
                        movie.name,
                        movie.year.toString(),
                        movie.letterboxdUri.orEmpty()
                    )
                )
            }
        }

        val watchlistRows = buildList {
            movies.filter { it.inWatchlist == 1L }.forEach { movie ->
                add(
                    csvRow(
                        movie.addedDate.orEmpty(),
                        movie.name,
                        movie.year.toString(),
                        movie.letterboxdUri.orEmpty()
                    )
                )
            }
        }

        val ratingsRows = buildList {
            movies.forEach { movie ->
                val latestRatedLog = logsByMovie[movie.id]
                    .orEmpty()
                    .filter { it.rating != null }
                    .maxByOrNull { it.watchedDate ?: it.loggedDate ?: "" }
                    ?: return@forEach

                add(
                    csvRow(
                        latestRatedLog.loggedDate ?: latestRatedLog.watchedDate ?: "",
                        movie.name,
                        movie.year.toString(),
                        movie.letterboxdUri.orEmpty(),
                        formatRating(latestRatedLog.rating!!)
                    )
                )
            }
        }

        return LetterboxdExportBundle(
            diaryCsv = buildString {
                appendLine(diaryHeader)
                diaryRows.forEach(::appendLine)
            },
            watchedCsv = buildString {
                appendLine(watchedHeader)
                watchedRows.forEach(::appendLine)
            },
            watchlistCsv = buildString {
                appendLine(watchlistHeader)
                watchlistRows.forEach(::appendLine)
            },
            ratingsCsv = buildString {
                appendLine(ratingsHeader)
                ratingsRows.forEach(::appendLine)
            },
            reviewsCsv = buildString {
                appendLine(reviewsHeader)
                reviewsRows.forEach(::appendLine)
            }
        )
    }

    private fun rewatchValue(isRewatch: Boolean): String =
        if (isRewatch) "Yes" else ""

    private fun formatRating(rating: Double): String =
        if (rating % 1.0 == 0.0) rating.toInt().toString() else rating.toString()

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