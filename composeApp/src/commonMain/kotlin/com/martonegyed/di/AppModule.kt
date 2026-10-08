package com.martonegyed.di

import com.martonegyed.domain.repository.DiscoveryRepository
import com.martonegyed.domain.repository.CrossoverRepository
import com.martonegyed.data.repository.discovery.LocalDiscoveryRepository
import com.martonegyed.data.repository.discovery.LocalCrossoverRepository
import com.martonegyed.data.repository.library.SqlDelightMovieCollectionRepository
import com.martonegyed.data.repository.catalog.SqlDelightMovieDetailsRepository
import com.martonegyed.domain.repository.MovieCollectionRepository
import com.martonegyed.domain.repository.MovieDetailsRepository
import com.martonegyed.domain.repository.MovieLogRepository
import com.martonegyed.data.repository.library.SqlDelightMovieLogRepository
import com.martonegyed.data.local.transfer.DataSyncManager
import com.martonegyed.data.local.catalog.MovieMetadataWriter
import com.martonegyed.data.database.CineGraphDatabase
import com.martonegyed.data.local.transfer.CsvImportService
import com.martonegyed.data.repository.discovery.SqlDelightDiscoveryManagerRepository
import com.martonegyed.data.local.transfer.export.BackupExportService
import com.martonegyed.data.local.transfer.export.ImdbExportService
import com.martonegyed.data.local.transfer.export.LetterboxdExportService
import com.martonegyed.data.remote.TmdbApiService
import com.martonegyed.domain.repository.AnalyticsRepository
import com.martonegyed.domain.repository.ImportRepository
import com.martonegyed.domain.repository.ExportRepository
import com.martonegyed.data.repository.analytics.SqlDelightAnalyticsRepository
import com.martonegyed.data.repository.transfer.LocalImportRepository
import com.martonegyed.data.repository.transfer.LocalExportRepository
import com.martonegyed.data.repository.catalog.LocalTmdbMatchReviewRepository
import com.martonegyed.data.repository.catalog.LocalMovieSearchRepository
import com.martonegyed.domain.repository.TmdbMatchReviewRepository
import com.martonegyed.domain.repository.MovieSearchRepository
import com.martonegyed.presentation.screens.collabSearch.CollabSearchScreenModel
import com.martonegyed.presentation.screens.calendar.CalendarScreenModel
import com.martonegyed.presentation.screens.details.MovieDetailScreenModel
import com.martonegyed.presentation.screens.import.ImportScreenModel
import com.martonegyed.presentation.screens.search.MovieSearchScreenModel
import com.martonegyed.presentation.screens.insights.InsightsScreenModel
import com.martonegyed.domain.repository.DiscoveryManagerRepository
import com.martonegyed.presentation.screens.moviePicker.DiscoveryManagerScreenModel
import com.martonegyed.domain.model.MoviePickerRequest
import com.martonegyed.presentation.screens.moviePicker.MoviePickerResultsScreenModel
import com.martonegyed.presentation.screens.moviePicker.MoviePickerScreenModel
import com.martonegyed.presentation.screens.movies.MovieCollectionScreenModel
import com.martonegyed.presentation.screens.randompicker.RandomPickerScreenModel
import com.martonegyed.presentation.screens.statistics.StatisticsScreenModel
import com.martonegyed.presentation.screens.yearinreview.YearInReviewScreenModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
val appModule = module {
    single {
        HttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = 25_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 20_000
            }
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
    single { MovieMetadataWriter(get()) }
    single {
        val suggestions = get<TmdbMatchReviewRepository>()
        DataSyncManager(get(), get(), metadataWriter = get(), onUnmatched = suggestions::prepareSuggestions)
    }
    single<AnalyticsRepository> { SqlDelightAnalyticsRepository(get()) }
    single<ImportRepository> { LocalImportRepository(get(), get(), get()) }
    single<TmdbMatchReviewRepository> { LocalTmdbMatchReviewRepository(get(), get(), get()) }
    single<MovieSearchRepository> { LocalMovieSearchRepository(get(), get(), get()) }
    single<ExportRepository> { LocalExportRepository(get(), get(), get(), get(), api = get()) }
    single<MovieCollectionRepository> { SqlDelightMovieCollectionRepository(get()) }
    single { SqlDelightMovieDetailsRepository(get(), get(), metadataWriter = get()) }
    single<MovieDetailsRepository> { get<SqlDelightMovieDetailsRepository>() }
    single<MovieLogRepository> { SqlDelightMovieLogRepository(get(), get()) }
    single<DiscoveryRepository> { LocalDiscoveryRepository(get(), get(), get()) }
    single<CrossoverRepository> { LocalCrossoverRepository(get(), get()) }
    single<DiscoveryManagerRepository> {
        SqlDelightDiscoveryManagerRepository(get())
    }
    single { BackupExportService(get()) }
    single { LetterboxdExportService(get()) }
    single { ImdbExportService(get()) }



    factory { ImportScreenModel(get(), get(), get()) }
    factory { MovieSearchScreenModel(get()) }
    factory { MovieCollectionScreenModel(get()) }
    factory { MovieDetailScreenModel(get(), get()) }
    factory { StatisticsScreenModel(get()) }
    factory { InsightsScreenModel(get()) }
    factory { CalendarScreenModel(get()) }
    factory { CollabSearchScreenModel(get()) }
    factory { YearInReviewScreenModel(get()) }
    factory { RandomPickerScreenModel(get()) }
    factory { MoviePickerScreenModel(get()) }
    factory { (request: MoviePickerRequest) ->
        MoviePickerResultsScreenModel(request, get(), get())
    }
    factory {
        DiscoveryManagerScreenModel(repository = get())
    }
}
