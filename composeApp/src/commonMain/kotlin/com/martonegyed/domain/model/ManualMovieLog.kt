package com.martonegyed.domain.model

data class ManualMovieLog(
    val watchedDate: String?,
    val rating: Double? = null,
    val review: String? = null,
    val ratedDate: String? = null
)

enum class MovieLogEntryMode { VIEWING, RATING, EDIT }
