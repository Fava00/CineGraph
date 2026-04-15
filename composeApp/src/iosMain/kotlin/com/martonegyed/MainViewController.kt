package com.martonegyed

import androidx.compose.ui.window.ComposeUIViewController
import com.martonegyed.data.local.database.DatabaseDriverFactory

fun MainViewController() = ComposeUIViewController { App(driverFactory = DatabaseDriverFactory()) }