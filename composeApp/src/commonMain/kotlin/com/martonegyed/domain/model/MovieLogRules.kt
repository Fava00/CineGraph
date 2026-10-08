package com.martonegyed.domain.model

import kotlinx.datetime.LocalDate

object MovieLogRules {
    fun dateOrNull(value: String?): String? {
        val text = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return LocalDate.parse(text).toString()
    }

    fun ratingOrNull(value: Double?): Double? {
        if (value == null || value == 0.0) return null
        require(value.isFinite() && value in 0.5..5.0 && value * 2 % 1.0 == 0.0) {
            "Ratings must use half-star increments from 0.5 to 5."
        }
        return value
    }

    fun importedRating(text: String?): Double? {
        val value = text?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val number = requireNotNull(value.toDoubleOrNull()) { "Invalid rating: $value" }
        return ratingOrNull(number)
    }

    fun importedImdbRating(text: String?): Double? {
        val value = text?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val number = requireNotNull(value.toDoubleOrNull()) { "Invalid IMDb Your Rating: $value" }
        if (number == 0.0) return null
        require(number.isFinite() && number in 1.0..10.0 && number % 1.0 == 0.0) {
            "IMDb Your Rating must be a whole number from 1 to 10 (received $value)."
        }
        return number / 2.0
    }
}
