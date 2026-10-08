package com.martonegyed.domain.model

data class CsvImportAssessment(
    val fileName: String,
    val platform: String,
    val selectedType: CsvImportType?,
    val filenameType: CsvImportType?,
    val headerTypes: List<CsvImportType>,
    val choices: List<CsvImportType>,
    val resolvedType: CsvImportType?,
    val message: String?
)

fun CsvImportType.importEffect(platform: String): String = when (this) {
    CsvImportType.DIARY -> "Adds watched movies and viewing logs."
    CsvImportType.REVIEWS -> "Adds watched movies, reviews, and viewing logs."
    CsvImportType.WATCHED -> "Marks these movies as watched; unknown viewing dates stay unknown."
    CsvImportType.RATINGS -> "Imports ratings and marks these movies as watched; rating dates are separate from viewing dates."
    CsvImportType.WATCHLIST -> "Adds these movies to your watchlist; existing ratings in the file are also imported."
    CsvImportType.LISTS -> if (platform.equals("Letterboxd", true)) "Adds these movies to your watchlist." else "Imports list movies and any included personal ratings."
}
