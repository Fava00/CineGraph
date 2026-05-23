package com.martonegyed.core.ui

fun formatWatchedDate(raw: String): String {
    val parts = raw.split("-")
    if (parts.size != 3) return raw
    val month = when (parts[1]) {
        "01" -> "Jan"; "02" -> "Feb"; "03" -> "Mar"; "04" -> "Apr"
        "05" -> "May"; "06" -> "Jun"; "07" -> "Jul"; "08" -> "Aug"
        "09" -> "Sep"; "10" -> "Oct"; "11" -> "Nov"; "12" -> "Dec"
        else -> parts[1]
    }
    return "$month ${parts[2].trimStart('0')}"
}