package com.martonegyed.core.util

object LanguageUtil {
    private val names = mapOf(
        "en" to "English",
        "hu" to "Hungarian",
        "fr" to "French",
        "es" to "Spanish",
        "de" to "German",
        "it" to "Italian",
        "ja" to "Japanese",
        "ko" to "Korean",
        "zh" to "Chinese",
        "hi" to "Hindi",
        "ru" to "Russian",
        "pt" to "Portuguese",
        "sv" to "Swedish",
        "da" to "Danish",
        "nl" to "Dutch",
        "pl" to "Polish",
        "tr" to "Turkish",
        "fi" to "Finnish",
        "no" to "Norwegian",
        "id" to "Indonesian",
        "th" to "Thai",
        "cs" to "Czech",
        "el" to "Greek"
    )

    fun getName(code: String): String {
        return names[code] ?: code.uppercase()
    }
}