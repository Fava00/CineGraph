package com.martonegyed.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val LetterboxdBlue = Color(0xFF40bcf4)
val LetterboxdDarkGrey = Color(0xFF14181c)
val LetterboxdGreen = Color(0xFF00E054)
val LetterboxdSurface = Color(0xFF1F2326)
val LetterboxdScrim = Color(0xFFB10808)
val LetterboxdTertiary = Color(0xFFE9DB14)


private val CineGraphColorScheme = darkColorScheme(
    primary = LetterboxdGreen,
    secondary = LetterboxdBlue,
    background = LetterboxdDarkGrey,
    surface = LetterboxdSurface,
    scrim = LetterboxdScrim,
    tertiary = LetterboxdTertiary
)

@Composable
fun CineGraphTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CineGraphColorScheme,
        content = content
    )
}