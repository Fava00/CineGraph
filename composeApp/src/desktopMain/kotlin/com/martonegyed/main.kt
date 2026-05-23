package com.martonegyed

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.martonegyed.data.local.database.DatabaseDriverFactory
import java.awt.Dimension

fun main() = application {
    val windowState = rememberWindowState(
        width = 1180.dp,
        height = 820.dp
    )
    Window(
        onCloseRequest = ::exitApplication,
        title = "Cinegraph",
        state = windowState
    ) {
        window.minimumSize = Dimension(600, 700)

        App(driverFactory = DatabaseDriverFactory())
    }
}