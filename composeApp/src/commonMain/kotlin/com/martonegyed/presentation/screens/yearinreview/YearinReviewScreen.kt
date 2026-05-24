package com.martonegyed.presentation.screens.yearinreview

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.martonegyed.core.ui.adaptive.AdaptiveLayout
import com.martonegyed.core.util.revenueFormater
import com.martonegyed.core.util.roundToDecimals
import com.martonegyed.presentation.analytics.StatRange
import com.martonegyed.presentation.components.common.AppDrawer
import com.martonegyed.presentation.components.common.cards.HeroStatCard
import com.martonegyed.presentation.components.common.cards.SectionCard
import com.martonegyed.presentation.components.yearinreview.BreakDownSection
import com.martonegyed.presentation.components.yearinreview.FirstLastMovieSection
import com.martonegyed.presentation.components.yearinreview.MilestonesSection
import com.martonegyed.presentation.components.yearinreview.MostWatchedPeopleRow
import com.martonegyed.presentation.components.yearinreview.MovieStripSection
import com.martonegyed.presentation.components.yearinreview.RankingSection
import com.martonegyed.presentation.components.yearinreview.WeekBarSection
import com.martonegyed.presentation.components.yearinreview.WeekdayBarSection
import com.martonegyed.presentation.components.yearinreview.YearInReviewSelector
import com.martonegyed.presentation.screens.details.MovieDetailScreen
import com.martonegyed.presentation.screens.insights.sections.MapInsightSection
import kotlinx.coroutines.launch


class YearInReviewScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val screenModel = koinScreenModel<YearInReviewScreenModel>()
        val uiState by screenModel.state.collectAsState()

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                AppDrawer(
                    navigator = navigator,
                    currentScreen = this@YearInReviewScreen,
                    closeDrawer = { scope.launch { drawerState.close() } }
                )
            }
        ) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            YearInReviewSelector(
                                years = uiState.availableYears,
                                selectedYear = uiState.selectedYear,
                                onYearSelected = screenModel::setYear
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        }
                    )
                }
            ) { padding ->
                AdaptiveLayout(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) { adaptive ->
                    val scaffold = adaptive.tokens.scaffold

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator()
                            return@AdaptiveLayout
                        }

                        LazyColumn(
                            modifier = Modifier
                                .widthIn(max = 1200.dp)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(
                                horizontal = scaffold.horizontalPadding,
                                vertical = scaffold.verticalPadding
                            ),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    HeroStatCard(
                                        modifier = Modifier.weight(1f),
                                        icon = Icons.Default.Movie,
                                        value = uiState.hero.filmCount.toString(),
                                        label = "Watched"
                                    )
                                    HeroStatCard(
                                        modifier = Modifier.weight(1f),
                                        icon = Icons.Default.AccessTime,
                                        value = "${uiState.hero.hoursWatched}h",
                                        label = "Hours"
                                    )
                                    HeroStatCard(
                                        modifier = Modifier.weight(1f),
                                        icon = Icons.Default.Star,
                                        value = uiState.hero.averageRating
                                            ?.roundToDecimals(2)
                                            ?.toString()
                                            ?: "-",
                                        label = "Avg Rating"
                                    )
                                    HeroStatCard(
                                        modifier = Modifier.weight(1f),
                                        icon = Icons.Default.AttachMoney,
                                        value = revenueFormater(uiState.hero.totalRevenue),
                                        label = "Revenue"
                                    )
                                }
                            }

                            item {
                                SectionCard(title = "Most watched people") {
                                    MostWatchedPeopleRow(
                                        navigator = navigator,
                                        actor = uiState.mostWatchedActor,
                                        director = uiState.mostWatchedDirector,
                                        selectedYear = uiState.selectedYear,
                                    )
                                }
                            }

                            item {
                                SectionCard(title = "Your highest rated") {
                                    MovieStripSection(
                                        movies = uiState.highestRatedByUser,
                                        onMovieClick = { movie -> navigator.push(MovieDetailScreen(movie)) }

                                    )
                                }
                            }

                            item {
                                SectionCard(title = "Highest rated on TMDB") {
                                    MovieStripSection(
                                        movies = uiState.highestRatedByTmdb,
                                        showTmdbBadge = true,
                                        onMovieClick = { movie -> navigator.push(MovieDetailScreen(movie)) }

                                    )
                                }
                            }

                            item {
                                SectionCard(title = "Lowest rated on TMDB") {
                                    MovieStripSection(
                                        movies = uiState.lowestRatedByTmdb,
                                        showTmdbBadge = true,
                                        onMovieClick = { movie -> navigator.push(MovieDetailScreen(movie)) }
                                    )
                                }
                            }

                            item {
                                if (uiState.moviesByWeek.isNotEmpty()) {
                                    SectionCard(title = "Movies by week") {
                                        WeekBarSection(rows = uiState.moviesByWeek)
                                    }
                                }
                            }

                            item {
                                if (uiState.moviesByDay.isNotEmpty()) {
                                    SectionCard(title = "Movies by day") {
                                        WeekdayBarSection(rows = uiState.moviesByDay)
                                    }
                                }
                            }

                            item {
                                if (uiState.firstMovie != null || uiState.lastMovie != null) {
                                    FirstLastMovieSection(
                                        firstMovie = uiState.firstMovie,
                                        lastMovie = uiState.lastMovie,
                                        onMovieClick = { movie -> navigator.push(MovieDetailScreen(movie)) }
                                    )
                                }
                            }

                            item {
                                if (uiState.milestones.isNotEmpty()) {
                                    SectionCard(title = "Milestones") {
                                        MilestonesSection(
                                            rows = uiState.milestones,
                                            onMovieClick = { movie -> navigator.push(MovieDetailScreen(movie)) })
                                    }
                                }
                            }

                            item {
                                SectionCard(title = "Genres") {
                                    RankingSection(
                                        countRows = uiState.genreRankingByCount,
                                        ratingRows = uiState.genreRankingByAverageRating
                                    )
                                }
                            }

                            item {
                                SectionCard(title = "Languages") {
                                    RankingSection(
                                        countRows = uiState.languageRankingByCount,
                                        ratingRows = uiState.languageRankingByAverageRating
                                    )
                                }
                            }

                            item {
                                if (uiState.newVsOld.isNotEmpty() || uiState.rewatchesVsFirstTime.isNotEmpty()) {
                                    BreakDownSection(uiState.newVsOld, uiState.rewatchesVsFirstTime)
                                }
                            }

                            item {
                                SectionCard(title = "Actors") {
                                    RankingSection(
                                        countRows = uiState.actorRankingByCount,
                                        ratingRows = uiState.actorRankingByAverageRating
                                    )
                                }
                            }

                            item {
                                SectionCard(title = "Directors") {
                                    RankingSection(
                                        countRows = uiState.directorRankingByCount,
                                        ratingRows = uiState.directorRankingByAverageRating
                                    )
                                }
                            }

                            item {
                                SectionCard(title = "Countries") {
                                    MapInsightSection(
                                        rows = uiState.mapCountries,
                                        navigator = navigator,
                                        range = StatRange.YEAR,
                                        year = uiState.selectedYear,
                                        month = null
                                    )
                                }
                            }
                        }
                    }

                }
            }
        }
    }
}




