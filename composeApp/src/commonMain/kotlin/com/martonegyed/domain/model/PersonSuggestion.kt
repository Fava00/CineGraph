package com.martonegyed.domain.model

enum class SuggestionSource {
    LOCAL,
    TMDB
}

data class PersonSuggestion(
    val name: String,
    val tmdbPersonId: Int? = null,
    val source: SuggestionSource
)

data class SelectedPerson(
    val name: String,
    val tmdbPersonId: Int? = null
)