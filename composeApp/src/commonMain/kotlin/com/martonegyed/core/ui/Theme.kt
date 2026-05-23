package com.martonegyed.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val CineBackground = Color(0xFF0E1014)
val CineSurface = Color(0xFF1A1D23)

val CineSurfaceContainer = Color(0xFFF0F0F0)
val CineSurfaceVariant = Color(0xFF252930)
val CinePrimary = Color(0xFFE8A030)
val CinePrimaryContainer = Color(0xFFF8E100)
val CineGreen = Color(0xFF21A705)
val CineSecondary = Color(0xFF5BA4CF)
val CineTertiary = Color(0xFFE0C060)
val CineError = Color(0xFFCF6679)
val CineOnPrimary = Color(0xFF1A1100)
val CineOnBackground = Color(0xFFE0DDD8)
val CineOnSurface = Color(0xFFE0DDD8)
val CineOnSurfaceVariant = Color(0xFF9A9590)

val CineOnSecondary = Color(0xFFE0DDD8)


private val CineGraphColorScheme = darkColorScheme(
    primary = CinePrimary,
    onPrimary = CineOnPrimary,
    secondary = CineSecondary,
    tertiary = CineTertiary,
    background = CineBackground,
    onBackground = CineOnBackground,
    surface = CineSurface,
    onSurface = CineOnSurface,
    surfaceVariant = CineSurfaceVariant,
    onSurfaceVariant = CineOnSurfaceVariant,
    error = CineError,
    inversePrimary = CineGreen,
    onSecondary = CineOnSecondary,
    surfaceContainer = CineSurfaceContainer,
    primaryContainer = CinePrimaryContainer,
)

@Composable
fun CineGraphTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CineGraphColorScheme,
        content = content
    )
}