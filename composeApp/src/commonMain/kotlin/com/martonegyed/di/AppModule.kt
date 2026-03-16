package com.martonegyed.di

import com.martonegyed.data.local.CsvImportService
import com.martonegyed.data.remote.TmdbApiService
import com.martonegyed.presentation.screens.import.ImportScreenModel
import com.martonegyed.presentation.screens.movies.AllMoviesScreenModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val appModule = module {
    single {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                })
            }
        }
    }

    single { TmdbApiService(get()) }
    single { CsvImportService() }


    factory { ImportScreenModel(get(), get(), get()) }
    factory { AllMoviesScreenModel(get()) }
}