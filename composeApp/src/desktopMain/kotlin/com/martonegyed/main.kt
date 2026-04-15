package com.martonegyed

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.martonegyed.data.local.database.DatabaseDriverFactory

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Cinegraph",
    ) {
        App(driverFactory = DatabaseDriverFactory())
    }
}