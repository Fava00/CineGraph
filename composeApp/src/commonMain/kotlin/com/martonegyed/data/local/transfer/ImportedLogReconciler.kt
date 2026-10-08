package com.martonegyed.data.local.transfer

import com.martonegyed.data.database.CineGraphDatabase

internal fun mergeImportedReviews(existing: String?, incoming: String?): String? {
    if (existing.isNullOrBlank()) return incoming?.takeIf { it.isNotBlank() }
    if (incoming.isNullOrBlank() || existing.contains(incoming)) return existing
    if (incoming.contains(existing)) return incoming
    return listOf(existing, incoming).sorted().joinToString("\n\n---\n\n")
}

internal fun reconcileImportedLogs(database: CineGraphDatabase, movieId: Long) {
    val queries = database.movieEntityQueries
    val logs = queries.getLogsForMovie(movieId).executeAsList()
    if (logs.any { it.sourceType != "WATCHED" }) {
        logs.filter { it.sourceType == "WATCHED" && it.watchedDate == null && it.rating == null && it.review.isNullOrBlank() }
            .forEach { queries.deleteImportedLog(it.id) }
    }
    logs.filter { it.watchedDate != null }.groupBy { it.watchedDate }.values.filter { it.size > 1 }.forEach { sameDay ->
        require(sameDay.mapNotNull { it.rating }.distinct().size <= 1) {
            "Different ratings for the same movie on ${sameDay.first().watchedDate}. Resolve the conflicting source rows before importing."
        }
        val retained = sameDay.maxBy { it.stableId }
        val source = if (sameDay.any { it.sourceType == "DIARY" }) "DIARY" else retained.sourceType
        queries.updateMovieLog(retained.watchedDate, retained.loggedDate,
            sameDay.firstNotNullOfOrNull { it.rating }, sameDay.fold(null as String?) { review, log -> mergeImportedReviews(review, log.review) },
            retained.isRewatch, source, retained.id)
        queries.setLogRatingDate(sameDay.mapNotNull { it.ratedDate }.maxOrNull(), retained.id)
        sameDay.filter { it.id != retained.id }.forEach { queries.deleteImportedLog(it.id) }
    }
    val reconciled = queries.getLogsForMovie(movieId).executeAsList()
    reconciled.filter { it.sourceType == "RATINGS" && it.watchedDate == null && it.ratedDate != null }.forEach { rating ->
        val viewing = queries.getLogsForMovie(movieId).executeAsList().singleOrNull {
            it.watchedDate == rating.ratedDate && (it.rating == null || rating.rating == null || it.rating == rating.rating)
        } ?: return@forEach
        queries.updateMovieLog(viewing.watchedDate, viewing.loggedDate, viewing.rating ?: rating.rating,
            mergeImportedReviews(viewing.review, rating.review), viewing.isRewatch, viewing.sourceType, viewing.id)
        queries.setLogRatingDate(rating.ratedDate, viewing.id)
        queries.deleteImportedLog(rating.id)
    }
    queries.getLogsForMovie(movieId).executeAsList()
        .filter { it.sourceType == "RATINGS" && it.watchedDate == null && it.rating != null }
        .forEach { rating ->
            val viewing = queries.getLogsForMovie(movieId).executeAsList()
                .filter { (it.sourceType == "DIARY" || it.sourceType == "REVIEWS") && it.rating == rating.rating }
                .maxWithOrNull(compareBy({ it.watchedDate }, { it.stableId })) ?: return@forEach
            queries.updateMovieLog(viewing.watchedDate, viewing.loggedDate, viewing.rating,
                mergeImportedReviews(viewing.review, rating.review), viewing.isRewatch, viewing.sourceType, viewing.id)
            queries.deleteImportedLog(rating.id)
        }
}
