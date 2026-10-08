package com.martonegyed.domain.model

data class LetterboxdExportBundle(
    val diaryCsv: String,
    val watchedCsv: String,
    val watchlistCsv: String,
    val ratingsCsv: String,
    val reviewsCsv: String,
    val imdbFilmsCsv: String
)


data class ImdbExportBundle(
    val ratingsCsv: String,
    val watchlistCsv: String
)

