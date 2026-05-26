package com.martonegyed.data.local

class CsvImportService {

    fun parseCsv(csvContent: String, platform: String, type: String): List<Map<String, Any>> {

        val rows = parseCsvString(csvContent)

        if (rows.isEmpty()) {
            return emptyList()
        }

        var headerIndex = 0
        for (i in rows.indices) {
            val rowLower = rows[i].map { it.lowercase().trim() }
            if (rowLower.contains("name") || rowLower.contains("title")) {
                headerIndex = i
                break
            }
        }


        if (headerIndex >= rows.size) {
            return emptyList()
        }

        val headers = rows[headerIndex].map { it.lowercase().trim() }
        val dataRows = rows.drop(headerIndex + 1)



        val result = when (platform.lowercase()) {
            "letterboxd" -> parseLetterboxd(headers, dataRows, type.lowercase())
            "imdb" -> parseImdb(headers, dataRows, type.lowercase())
            else -> {
                emptyList()
            }
        }

        return result
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
        if (currentToken.isNotEmpty() || currentLine.isNotEmpty()) {
            currentLine.add(currentToken.toString().trim())
            if (currentLine.any { it.isNotEmpty() }) lines.add(currentLine)
        }
        return lines
    }

    private fun parseLetterboxd(
        headers: List<String>,
        rows: List<List<String>>,
        type: String
    ): List<Map<String, Any>> {
        val parsedMovies = mutableListOf<Map<String, Any>>()

        val nameIdx = headers.indexOf("name")
        val yearIdx = headers.indexOf("year")
        val ratingIdx = headers.indexOf("rating")
        val watchedDateIdx = headers.indexOf("watched date")
        val loggedDateIdx = headers.indexOf("date")
        val rewatchIdx = headers.indexOf("rewatch")
        val reviewIdx = headers.indexOf("review")
        val uriIdx = headers.indexOf("letterboxd uri").takeIf { it >= 0 } ?: headers.indexOf("url")


        for (row in rows) {

            if (nameIdx == -1 || row.size <= nameIdx) {
                continue
            }

            val map = mutableMapOf<String, Any>()
            map["name"] = row[nameIdx]
            map["year"] = if (yearIdx != -1 && row.size > yearIdx) row[yearIdx].toIntOrNull() ?: 0 else 0

            if (uriIdx != -1 && row.size > uriIdx) map["letterboxdUri"] = row[uriIdx]
            if (ratingIdx != -1 && row.size > ratingIdx) map["rating"] = row[ratingIdx].toDoubleOrNull() ?: 0.0

            val watchedDate = if (watchedDateIdx != -1 && row.size > watchedDateIdx) {
                row[watchedDateIdx].takeIf { it.isNotBlank() }
            } else null

            val loggedDate = if (loggedDateIdx != -1 && row.size > loggedDateIdx) {
                row[loggedDateIdx].takeIf { it.isNotBlank() }
            } else null

            when (type) {
                "watchlist", "lists" -> {
                    if (!loggedDate.isNullOrBlank()) {
                        map["addedDate"] = loggedDate
                    }
                    map["inWatchlist"] = true
                }

                "diary", "reviews" -> {
                    if (!watchedDate.isNullOrBlank()) {
                        map["watchedDate"] = watchedDate
                    }
                    if (!loggedDate.isNullOrBlank()) {
                        map["loggedDate"] = loggedDate
                    }
                    map["sourceType"] = type.uppercase()
                }

                "watched", "ratings" -> {
                    if (!loggedDate.isNullOrBlank()) {
                        map["loggedDate"] = loggedDate
                    }
                    map["sourceType"] = type.uppercase()
                }

                else -> {
                    if (!watchedDate.isNullOrBlank()) {
                        map["watchedDate"] = watchedDate
                    }
                    if (!loggedDate.isNullOrBlank()) {
                        map["loggedDate"] = loggedDate
                    }
                    map["sourceType"] = type.uppercase()
                }
            }

            if (rewatchIdx != -1 && row.size > rewatchIdx) {
                map["isRewatch"] = row[rewatchIdx].equals("yes", ignoreCase = true)
            }
            if (reviewIdx != -1 && row.size > reviewIdx && row[reviewIdx].isNotBlank()) {
                map["userReview"] = row[reviewIdx]
            }

            parsedMovies.add(map)
        }
        return parsedMovies
    }

    private fun parseImdb(
        headers: List<String>,
        rows: List<List<String>>,
        type: String
    ): List<Map<String, Any>> {
        val parsedMovies = mutableListOf<Map<String, Any>>()

        val titleIdx = headers.indexOf("title")
        val originalTitleIdx = headers.indexOf("original title")
        val yearIdx = headers.indexOf("year")
        val constIdx = headers.indexOf("const")
        val urlIdx = headers.indexOf("url")
        val userRatingIdx = headers.indexOf("your rating")
        val dateRatedIdx = headers.indexOf("date rated")
        val createdIdx = headers.indexOf("created")

        for (row in rows) {
            if (titleIdx == -1 || row.size <= titleIdx) continue

            val map = mutableMapOf<String, Any>()
            map["name"] = row[titleIdx].trim()
            map["year"] = if (yearIdx != -1 && row.size > yearIdx) {
                row[yearIdx].trim().toIntOrNull() ?: 0
            } else 0

            if (originalTitleIdx != -1 && row.size > originalTitleIdx && row[originalTitleIdx].isNotBlank()) {
                map["originalTitle"] = row[originalTitleIdx].trim()
            }

            if (constIdx != -1 && row.size > constIdx && row[constIdx].isNotBlank()) {
                map["imdbId"] = row[constIdx].trim()
            }

            if (urlIdx != -1 && row.size > urlIdx && row[urlIdx].isNotBlank()) {
                map["imdbUrl"] = row[urlIdx].trim()
            }

            if (userRatingIdx != -1 && row.size > userRatingIdx && row[userRatingIdx].isNotBlank()) {
                map["rating"] = row[userRatingIdx].trim().toDoubleOrNull() ?: 0.0
            }

            val dateRated = if (dateRatedIdx != -1 && row.size > dateRatedIdx) {
                row[dateRatedIdx].trim().takeIf { it.isNotBlank() }
            } else null

            val created = if (createdIdx != -1 && row.size > createdIdx) {
                row[createdIdx].trim().takeIf { it.isNotBlank() }
            } else null

            when (type) {
                "watchlist" -> {
                    if (!created.isNullOrBlank()) {
                        map["addedDate"] = created
                    }
                    map["inWatchlist"] = true
                    map["sourceType"] = "WATCHLIST"
                }

                "ratings" -> {
                    if (!dateRated.isNullOrBlank()) {
                        map["loggedDate"] = dateRated
                        map["watchedDate"] = dateRated
                    } else if (!created.isNullOrBlank()) {
                        map["loggedDate"] = created
                    }
                    map["sourceType"] = "RATINGS"
                }

                "lists" -> {
                    if (!created.isNullOrBlank()) {
                        map["addedDate"] = created
                    }
                    map["sourceType"] = "LISTS"
                }

                else -> {
                    if (!dateRated.isNullOrBlank()) {
                        map["loggedDate"] = dateRated
                    } else if (!created.isNullOrBlank()) {
                        map["loggedDate"] = created
                    }
                    map["sourceType"] = type.uppercase()
                }
            }

            parsedMovies.add(map)
        }

        return parsedMovies
    }
}