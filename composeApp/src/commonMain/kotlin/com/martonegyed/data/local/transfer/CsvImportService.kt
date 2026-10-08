package com.martonegyed.data.local.transfer

import com.martonegyed.domain.model.CsvImportType
import com.martonegyed.domain.model.CsvImportAssessment
import com.martonegyed.domain.model.MovieLogRules
import com.martonegyed.domain.model.StagedLog
import com.martonegyed.domain.model.StagedMovie
import com.martonegyed.domain.model.CsvParseResult

class CsvImportService {
    fun inspectCsv(content: String, platform: String, fileName: String, selectedType: String?): CsvImportAssessment {
        val rows = parseCsvString(content)
        val headers = readHeaders(rows).toSet()
        val letterboxd = platform.equals("Letterboxd", true)
        require(letterboxd || platform.equals("IMDb", true)) { "Unsupported CSV platform: $platform" }
        val actualPlatform = when {
            "name" in headers -> "Letterboxd"
            "title" in headers -> "IMDb"
            else -> null
        }
        require(actualPlatform == null || actualPlatform.equals(platform, true)) {
            "$fileName contains $actualPlatform columns. Import it under $actualPlatform instead."
        }
        val baseRequired = if (letterboxd) setOf("name", "year") else setOf("title", "year", "const")
        val missing = baseRequired - headers
        require(missing.isEmpty()) { "$fileName is missing required columns: ${missing.joinToString()}." }
        val selected = selectedType?.let(CsvImportType::from)
        val supported = if (letterboxd) CsvImportType.entries.toList() else listOf(CsvImportType.RATINGS, CsvImportType.WATCHLIST)
        require(selected == null || selected in supported) { "IMDb imports support Ratings and Watchlist only." }
        val filename = filenameType(fileName)?.takeIf { it in supported }
        val candidates = if (letterboxd) when {
            "review" in headers -> listOf(CsvImportType.REVIEWS)
            "watched date" in headers || "rewatch" in headers || "tags" in headers -> listOf(CsvImportType.DIARY)
            "rating" in headers -> listOf(CsvImportType.RATINGS)
            else -> listOf(CsvImportType.WATCHED, CsvImportType.WATCHLIST)
        } else when {
            "created" in headers || "position" in headers -> listOf(CsvImportType.WATCHLIST)
            "your rating" in headers || "date rated" in headers -> listOf(CsvImportType.RATINGS)
            else -> emptyList()
        }
        require(candidates.isNotEmpty()) { "$fileName does not match an IMDb ratings or watchlist CSV. Check its columns." }
        val allowed = supported.filter { requiredColumns(letterboxd, it).all(headers::contains) }
        val messages = mutableListOf<String>()
        val filenamePlatform = fileName.lowercase().let {
            when { it.startsWith("letterboxd-") -> "Letterboxd"; it.startsWith("imdb-") -> "IMDb"; else -> null }
        }
        if (filenamePlatform != null && !filenamePlatform.equals(platform, true)) {
            messages += "The filename suggests $filenamePlatform, while these columns belong to $platform."
        }
        if (selected != null && selected !in candidates && !(selected == CsvImportType.LISTS && filename == null)) {
            messages += "You selected ${selected.label}, but the columns suggest ${candidates.joinToString(" or ") { it.label }}."
        }
        if (filename != null && selected != null && filename != selected) {
            messages += "The filename suggests ${filename.label}, while you selected ${selected.label}."
        }
        if (filename != null && filename !in candidates && filename != CsvImportType.LISTS) {
            messages += "The filename suggests ${filename.label}, but the columns suggest ${candidates.joinToString(" or ") { it.label }}."
        }
        val desired = selected ?: filename ?: candidates.singleOrNull()
        if (desired != null && desired !in allowed) {
            val absent = requiredColumns(letterboxd, desired) - headers
            messages += "${desired.label} requires missing columns: ${absent.joinToString()}."
        }
        if (desired == null) messages += "These columns fit more than one type. Choose how to import this file."
        val choices = (listOfNotNull(selected, filename) + candidates).distinct().filter { it in allowed }
        require(choices.isNotEmpty()) { "$fileName has no supported CSV layout. Check its columns." }
        return CsvImportAssessment(fileName, platform, selected, filename, candidates, choices,
            desired.takeIf { messages.isEmpty() && it in allowed }, messages.takeIf { it.isNotEmpty() }?.joinToString("\n\n"))
    }

    private fun filenameType(fileName: String): CsvImportType? {
        val name = fileName.lowercase()
        return CsvImportType.entries.firstOrNull { type ->
            name == "${type.name.lowercase()}.csv" ||
                Regex("^(letterboxd|imdb)-${type.name.lowercase()}-[0-9]{4}-[0-9]{2}-[0-9]{2}(?:-[a-z0-9_-]+)?[.]csv$").matches(name)
        }
    }

    private fun requiredColumns(letterboxd: Boolean, type: CsvImportType): Set<String> = if (letterboxd) {
        when (type) {
            CsvImportType.RATINGS -> setOf("name", "year", "rating")
            CsvImportType.DIARY -> setOf("name", "year", "watched date")
            CsvImportType.REVIEWS -> setOf("name", "year", "watched date", "review")
            else -> setOf("name", "year")
        }
    } else {
        setOf("title", "year", "const") + when (type) {
            CsvImportType.RATINGS -> setOf("your rating", "date rated")
            CsvImportType.WATCHLIST -> setOf("created")
            else -> throw IllegalArgumentException("IMDb imports support Ratings and Watchlist only.")
        }
    }

    private fun readHeaders(rows: List<List<String>>): List<String> {
        val header = rows.firstOrNull { row ->
            row.any { it.trim().trimStart('\uFEFF').lowercase() in setOf("name", "title") }
        } ?: throw IllegalArgumentException("CSV is missing a Name or Title column.")
        return header.map { it.trim().trimStart('\uFEFF').lowercase() }
    }

    fun parseCsv(csvContent: String, platform: String, type: String): List<StagedMovie> =
        parseCsvWithReport(csvContent, platform, type).movies

    fun parseCsvWithReport(csvContent: String, platform: String, type: String): CsvParseResult {
        val rows = parseCsvString(csvContent)
        if (rows.isEmpty()) return CsvParseResult(emptyList())
        val headerIndex = rows.indexOfFirst { row ->
            row.any { it.trim().trimStart('\uFEFF').equals("name", true) || it.trim().equals("title", true) }
        }
        require(headerIndex >= 0) { "CSV is missing a Name or Title column." }
        val headers = rows[headerIndex].map { it.trim().trimStart('\uFEFF').lowercase() }
        val source = CsvImportType.from(type)
        val letterboxd = platform.equals("letterboxd", true)
        require(letterboxd || platform.equals("imdb", true)) { "Unsupported CSV platform: $platform" }
        val missing = requiredColumns(letterboxd, source) - headers.toSet()
        require(missing.isEmpty()) { "${source.label} CSV is missing required columns: ${missing.joinToString()}." }
        var skippedTvCount = 0
        val movies = rows.drop(headerIndex + 1).mapIndexedNotNull { rowIndex, row ->
            fun value(header: String): String? = row.getOrNull(headers.indexOf(header))?.trim()?.takeIf { it.isNotEmpty() }
            val name = value(if (letterboxd) "name" else "title") ?: return@mapIndexedNotNull null
            val titleType = value("title type")?.lowercase()?.replace(Regex("[\\s_-]"), "")
            if (!letterboxd && titleType in setOf("tvseries", "tvminiseries", "tvepisode")) {
                skippedTvCount++
                return@mapIndexedNotNull null
            }
            val rating = try {
                if (letterboxd) MovieLogRules.importedRating(value("rating"))
                else MovieLogRules.importedImdbRating(value("your rating"))
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("$name (CSV row ${headerIndex + rowIndex + 2}): ${e.message}", e)
            }
            val date = MovieLogRules.dateOrNull(value(if (letterboxd) "date" else "created"))
            val dateRated = if (letterboxd) null else MovieLogRules.dateOrNull(value("date rated"))
            val collection = source == CsvImportType.WATCHLIST || source == CsvImportType.LISTS
            val watchedDate = if (letterboxd && !collection && source !in setOf(CsvImportType.WATCHED, CsvImportType.RATINGS)) {
                MovieLogRules.dateOrNull(value("watched date"))
            } else null
            val loggedDate = if (collection) null else if (letterboxd) date else dateRated ?: date
            val ratedDate = if (letterboxd) {
                date.takeIf { source == CsvImportType.RATINGS && rating != null }
            } else dateRated.takeIf { rating != null || source == CsvImportType.RATINGS }
            StagedMovie(
                name = name, year = value("year")?.toIntOrNull() ?: 0,
                letterboxdUri = if (letterboxd) value("letterboxd uri") ?: value("url") else null,
                imdbId = if (letterboxd) null else value("const"),
                originalTitle = if (letterboxd) null else value("original title"),
                imdbUrl = if (letterboxd) null else value("url"),
                addedDate = date.takeIf { collection },
                isWatched = source.marksWatched,
                inWatchlist = source == CsvImportType.WATCHLIST || (letterboxd && source == CsvImportType.LISTS),
                logs = listOf(StagedLog(source, watchedDate, loggedDate, ratedDate, rating,
                    if (letterboxd) value("review") else null,
                    letterboxd && value("rewatch").equals("yes", true)))
            )
        }
        return CsvParseResult(movies, skippedTvCount)
    }

    private fun parseCsvString(content: String): List<List<String>> {
        val lines = mutableListOf<List<String>>()
        var currentLine = mutableListOf<String>()
        val currentToken = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < content.length) {
            val c = content[i]
            if (c == '"') {
                if (inQuotes && i + 1 < content.length && content[i + 1] == '"') {
                    currentToken.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                currentLine.add(currentToken.toString().trim())
                currentToken.clear()
            } else if ((c == '\n' || c == '\r') && !inQuotes) {
                if (c == '\r' && i + 1 < content.length && content[i + 1] == '\n') {
                    i++
                }
                currentLine.add(currentToken.toString().trim())
                if (currentLine.any { it.isNotEmpty() }) {
                    lines.add(currentLine)
                }
                currentLine = mutableListOf()
                currentToken.clear()
            } else {
                currentToken.append(c)
            }
            i++
        }
        require(!inQuotes) { "CSV contains an unfinished quoted value." }
        if (currentToken.isNotEmpty() || currentLine.isNotEmpty()) {
            currentLine.add(currentToken.toString().trim())
            if (currentLine.any { it.isNotEmpty() }) lines.add(currentLine)
        }
        return lines
    }

}
