package com.martonegyed.presentation.screens.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import cafe.adriel.voyager.navigator.Navigator
import com.martonegyed.core.ui.adaptive.AdaptiveLayout
import com.martonegyed.core.ui.adaptive.AdaptiveScaffoldTokens
import com.martonegyed.core.ui.adaptive.StatisticsScreenTokens
import com.martonegyed.presentation.analytics.StatRange
import com.martonegyed.presentation.components.common.AppDrawer
import com.martonegyed.presentation.components.statistics.StatisticsFilterPanel
import com.martonegyed.presentation.components.statistics.StatisticsHeroColumn
import com.martonegyed.presentation.components.statistics.StatisticsHeroRow
import com.martonegyed.presentation.components.statistics.StatisticsResultCard
import com.martonegyed.presentation.screens.movies.CollectionType
import com.martonegyed.presentation.screens.movies.MovieCollectionScreen
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime


class StatisticsScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<StatisticsScreenModel>()
        val state by screenModel.state.collectAsState()
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        val colors = MaterialTheme.colorScheme

        val onEntityChange: (StatEntityType) -> Unit = { type ->
            screenModel.setEntityType(type)
        }
        val onMetricChange: (StatMetric) -> Unit = { metric ->
            screenModel.setMetric(metric)
        }
        val onRangeChange: (StatRange, Int?, Int?) -> Unit = { range, year, month ->
            screenModel.setRange(range, year, month)
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                AppDrawer(
                    navigator = navigator,
                    currentScreen = this@StatisticsScreen,
                    closeDrawer = { scope.launch { drawerState.close() } }
                )
            }
        ) {
            Scaffold(
                containerColor = colors.background,
                topBar = {
                    TopAppBar(
                        title = { Text("Statistics", color = colors.onPrimary, fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = colors.onSurface)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.primary)
                    )
                }
            ) { padding ->
                if (state.isLoading) {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = colors.primary)
                    }
                    return@Scaffold
                }

                AdaptiveLayout(
                    modifier = Modifier.fillMaxSize().padding(padding)
                ) { adaptive ->
                    val scaffoldTokens = adaptive.tokens.scaffold
                    val statsTokens = adaptive.tokens.statisticsScreen

                    if (statsTokens.useTwoPaneLayout) {
                        StatisticsExpandedContent(
                            state = state,
                            navigator = navigator,
                            statsTokens = statsTokens,
                            scaffoldTokens = scaffoldTokens,
                            onEntityChange = onEntityChange,
                            onMetricChange = onMetricChange,
                            onRangeChange = onRangeChange
                        )
                    } else {
                        StatisticsCompactContent(
                            state = state,
                            navigator = navigator,
                            statsTokens = statsTokens,
                            scaffoldTokens = scaffoldTokens,
                            onEntityChange = onEntityChange,
                            onMetricChange = onMetricChange,
                            onRangeChange = onRangeChange
                        )
                    }
                }
            }
        }
    }


    @Composable
    private fun StatisticsExpandedContent(
        state: StatisticsState,
        navigator: Navigator,
        statsTokens: StatisticsScreenTokens,
        scaffoldTokens: AdaptiveScaffoldTokens,
        onEntityChange: (StatEntityType) -> Unit,
        onMetricChange: (StatMetric) -> Unit,
        onRangeChange: (StatRange, Int?, Int?) -> Unit
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier
                    .width(240.dp)
                    .fillMaxHeight()
                    .padding(16.dp),
            ) {
                StatisticsHeroColumn(state = state)
            }
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp),
                color = Color(0xFF2C3136)
            )
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(max = 1000.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(start = 20.dp, top = 16.dp, bottom = 16.dp, end = 20.dp)
                ) {
                    item {
                        StatisticsFilterPanel(
                            state = state,
                            onEntityChange = onEntityChange,
                            onMetricChange = onMetricChange,
                            onRangeChange = onRangeChange
                        )
                    }

                    item {
                        Text(
                            text = "${state.rows.size} results",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }

                    itemsIndexed(state.rows) { index, row ->
                        StatisticsResultCard(
                            row = row,
                            index = index,
                            metric = state.metric,
                            rows = state.rows,
                            avatarSize = 52.dp,
                            maxBarWidth = 420.dp,
                            onClick = {
                                navigator.push(
                                    MovieCollectionScreen(
                                        type = CollectionType.BY_ENTITY,
                                        entityType = state.entityType,
                                        entityName = row.name,
                                        range = state.range,
                                        year = state.selectedYear,
                                        month = state.selectedMonth
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun StatisticsCompactContent(
        state: StatisticsState,
        navigator: Navigator,
        statsTokens: StatisticsScreenTokens,
        scaffoldTokens: AdaptiveScaffoldTokens,
        onEntityChange: (StatEntityType) -> Unit,
        onMetricChange: (StatMetric) -> Unit,
        onRangeChange: (StatRange, Int?, Int?) -> Unit
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = scaffoldTokens.horizontalPadding,
                vertical = scaffoldTokens.verticalPadding
            ),
            verticalArrangement = Arrangement.spacedBy(scaffoldTokens.sectionSpacing)
        ) {
            item {
                StatisticsHeroRow(state = state)
            }

            item {
                StatisticsFilterPanel(
                    state = state,
                    onEntityChange = onEntityChange,
                    onMetricChange = onMetricChange,
                    onRangeChange = onRangeChange
                )
            }

            itemsIndexed(state.rows) { index, row ->
                StatisticsResultCard(
                    row = row,
                    index = index,
                    metric = state.metric,
                    rows = state.rows,
                    avatarSize = 40.dp,
                    maxBarWidth = 420.dp,
                    onClick = {
                        navigator.push(
                            MovieCollectionScreen(
                                type = CollectionType.BY_ENTITY,
                                entityType = state.entityType,
                                entityName = row.name,
                                range = state.range,
                                year = state.selectedYear,
                                month = state.selectedMonth
                            )
                        )
                    }
                )
            }
        }
    }
}
