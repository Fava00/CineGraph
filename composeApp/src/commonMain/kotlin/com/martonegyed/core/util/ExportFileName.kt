package com.martonegyed.core.util

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
fun exportDate(): String = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()

fun exportFileName(platform: String, type: String, date: String, extension: String, username: String? = null): String {
    val safeUsername = username?.lowercase()?.replace(Regex("[^a-z0-9_-]+"), "-")?.trim('-')?.take(64)
    return listOfNotNull(platform, type, date, safeUsername?.takeIf { it.isNotBlank() })
        .joinToString("-") + ".$extension"
}
