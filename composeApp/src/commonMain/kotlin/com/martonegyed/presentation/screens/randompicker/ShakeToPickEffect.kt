package com.martonegyed.presentation.screens.randompicker

import androidx.compose.runtime.Composable

@Composable
expect fun ShakeToPickEffect(
    enabled: Boolean,
    onShake: () -> Unit
)