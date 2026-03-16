package com.martonegyed

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.martonegyed.di.appModule
import com.martonegyed.presentation.CineGraphTheme
import org.koin.compose.KoinApplication
import com.martonegyed.data.database.CineGraphDatabase
import com.martonegyed.data.local.database.DatabaseDriverFactory
import org.koin.dsl.module
import coil3.compose.setSingletonImageLoaderFactory

import com.martonegyed.presentation.screens.import.ImportScreen
import com.martonegyed.core.util.getAsyncImageLoader

@Composable
fun App(driverFactory: DatabaseDriverFactory) {
    setSingletonImageLoaderFactory { context ->
        getAsyncImageLoader(context)
    }
    KoinApplication(application = {
        modules(
            appModule,
            module {
                single { driverFactory.createDriver() }
                single { CineGraphDatabase(get()) }
            }
        )
    }) {

        CineGraphTheme {

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
                color = androidx.compose.material3.MaterialTheme.colorScheme.background
            ) {
                Navigator(screen = ImportScreen()) { navigator ->
                    SlideTransition(navigator)
                }
            }
        }
    }
}