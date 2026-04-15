package com.martonegyed.di

import com.martonegyed.data.local.DataSyncManager
import com.martonegyed.data.database.CineGraphDatabase
import com.martonegyed.data.local.CsvImportService
import com.martonegyed.data.remote.TmdbApiService
import com.martonegyed.presentation.screens.details.MovieDetailScreenModel
import com.martonegyed.presentation.screens.import.ImportScreenModel
import com.martonegyed.presentation.screens.movies.MovieCollectionScreenModel
import com.martonegyed.presentation.screens.statistics.StatisticsScreenModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
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

    single { CineGraphDatabase(get()) }
    single { CsvImportService() }
    single { TmdbApiService(get()) }
    single { DataSyncManager(get(), get()) }


    factory { ImportScreenModel(get(), get(), get(), get()) }
    factory { MovieCollectionScreenModel(get()) }
    factory { MovieDetailScreenModel(get(), get()) }
    factory { StatisticsScreenModel(get()) }
}