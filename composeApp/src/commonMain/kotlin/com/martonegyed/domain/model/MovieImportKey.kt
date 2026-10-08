package com.martonegyed.domain.model

data class MovieImportKey(
    val name: String,
    val year: Int,
    val letterboxdUri: String?,
    val imdbId: String?
)
