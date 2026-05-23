package com.martonegyed.core.ui.adaptive

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

@Immutable
data class AdaptiveUi(
    val window: AdaptiveWindowInfo,
    val tokens: AdaptiveTokens
)

fun adaptiveUiForWidth(maxWidth: Dp): AdaptiveUi {
    val windowInfo = calculateAdaptiveWindowInfo(maxWidth)
    return AdaptiveUi(
        window = windowInfo,
        tokens = adaptiveTokensFor(windowInfo)
    )
}

@Composable
fun AdaptiveLayout(
    modifier: Modifier = Modifier,
    content: @Composable (AdaptiveUi) -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val adaptiveUi = remember(maxWidth) {
            adaptiveUiForWidth(maxWidth)
        }
        content(adaptiveUi)
    }
}