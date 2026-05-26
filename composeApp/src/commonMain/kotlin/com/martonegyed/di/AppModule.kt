package com.martonegyed.di

import com.martonegyed.data.local.DataSyncManager
import com.martonegyed.data.database.CineGraphDatabase
import com.martonegyed.data.local.CsvImportService
import com.martonegyed.data.local.SqlDelightDiscoveryManagerRepository
import com.martonegyed.data.local.export.BackupExportService
import com.martonegyed.data.local.export.ImdbExportService
import com.martonegyed.data.local.export.LetterboxdExportService
import com.martonegyed.data.remote.TmdbApiService
import com.martonegyed.presentation.analytics.AnalyticsRepository
import com.martonegyed.presentation.screens.collabSearch.CollabSearchScreenModel
import com.martonegyed.presentation.screens.calendar.CalendarScreenModel
import com.martonegyed.presentation.screens.details.MovieDetailScreenModel
import com.martonegyed.presentation.screens.import.ImportScreenModel
import com.martonegyed.presentation.screens.insights.InsightsScreenModel
import com.martonegyed.presentation.screens.moviePicker.DiscoveryManagerRepository
import com.martonegyed.presentation.screens.moviePicker.DiscoveryManagerScreenModel
import com.martonegyed.presentation.screens.moviePicker.MoviePickerRequest
import com.martonegyed.presentation.screens.moviePicker.MoviePickerResultsScreenModel
import com.martonegyed.presentation.screens.moviePicker.MoviePickerScreenModel
import com.martonegyed.presentation.screens.movies.MovieCollectionScreenModel
import com.martonegyed.presentation.screens.randompicker.RandomPickerScreenModel
import com.martonegyed.presentation.screens.statistics.StatisticsScreenModel
import com.martonegyed.presentation.screens.yearinreview.YearInReviewScreenModel
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
    single { AnalyticsRepository(get()) }
    single<DiscoveryManagerRepository> {
        SqlDelightDiscoveryManagerRepository(get())
    }
    single { BackupExportService(get()) }
    single { LetterboxdExportService(get()) }
    single { ImdbExportService(get()) }



    factory { ImportScreenModel(get(), get(), get(), get(), get(), get(), get()) }
    factory { MovieCollectionScreenModel(get()) }
    factory { MovieDetailScreenModel(get(), get()) }
    factory { StatisticsScreenModel(get()) }
    factory { InsightsScreenModel(get()) }
    factory { CalendarScreenModel(get()) }
    factory { CollabSearchScreenModel(get(), get()) }
    factory { YearInReviewScreenModel(get()) }
    factory { RandomPickerScreenModel(get()) }
    factory { MoviePickerScreenModel(get()) }
    factory { (request: MoviePickerRequest) ->
        MoviePickerResultsScreenModel(request, get(), get(), get())
    }
    factory {
        DiscoveryManagerScreenModel(repository = get())
    }
}