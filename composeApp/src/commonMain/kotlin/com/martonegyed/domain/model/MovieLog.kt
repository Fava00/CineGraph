package com.martonegyed.domain.model

data class MovieLog(
    val id: Long,
    val watchedDate: String?,
    val rating: Double?,
    val review: String?,
    val isRewatch: Boolean,
    val ratedDate: String? = null,
    val createdAt: String? = null,
    val sourceType: String? = null,
    val stableId: String = ""
) {
    val isRatingOnly: Boolean
        get() = watchedDate == null && sourceType?.uppercase() in setOf("RATINGS", "MANUAL_RATING")
}

