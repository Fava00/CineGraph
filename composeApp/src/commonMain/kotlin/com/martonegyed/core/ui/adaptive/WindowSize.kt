package com.martonegyed.core.ui.adaptive

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowWidthSizeClass {
    Compact,
    Medium,
    Expanded
}

@Immutable
data class AdaptiveWindowInfo(
    val maxWidth: Dp,
    val widthSizeClass: WindowWidthSizeClass
) {
    val isCompact: Boolean get() = widthSizeClass == WindowWidthSizeClass.Compact
    val isMedium: Boolean get() = widthSizeClass == WindowWidthSizeClass.Medium
    val isExpanded: Boolean get() = widthSizeClass == WindowWidthSizeClass.Expanded
}

fun calculateAdaptiveWindowInfo(maxWidth: Dp): AdaptiveWindowInfo {
    val widthSizeClass = when {
        maxWidth < 700.dp -> WindowWidthSizeClass.Compact
        maxWidth < 1100.dp -> WindowWidthSizeClass.Medium
        else -> WindowWidthSizeClass.Expanded
    }

    return AdaptiveWindowInfo(
        maxWidth = maxWidth,
        widthSizeClass = widthSizeClass
    )
}