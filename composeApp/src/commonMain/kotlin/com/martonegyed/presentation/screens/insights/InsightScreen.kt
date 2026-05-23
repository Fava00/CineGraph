package com.martonegyed.presentation.screens.insights

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.martonegyed.core.ui.adaptive.AdaptiveLayout
import com.martonegyed.presentation.components.common.AppDrawer
import com.martonegyed.presentation.components.insights.InsightModeStrip
import com.martonegyed.presentation.components.statistics.RangeFilterRow
import com.martonegyed.presentation.components.statistics.RangeFilterSkeleton
import com.martonegyed.presentation.screens.insights.sections.DecadeInsightSection
import com.martonegyed.presentation.screens.insights.sections.DecadeSectionSkeleton
import com.martonegyed.presentation.screens.insights.sections.DuosInsightSection
import com.martonegyed.presentation.screens.insights.sections.DuosSectionSkeleton
import com.martonegyed.presentation.screens.insights.sections.HabitsInsightSection
import com.martonegyed.presentation.screens.insights.sections.HabitsSectionSkeleton
import com.martonegyed.presentation.screens.insights.sections.MapInsightSection
import com.martonegyed.presentation.screens.insights.sections.MapSectionSkeleton
import com.martonegyed.presentation.screens.insights.sections.RatingsInsightSection
import com.martonegyed.presentation.screens.insights.sections.RatingsSectionSkeleton
import kotlinx.coroutines.launch

enum class InsightMode(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    RATINGS("Ratings", Icons.Default.StarOutline),
    DECADE("Decade", Icons.Default.Schedule),
    DUOS("Duos", Icons.Default.Groups),
    HABITS("Habits", Icons.Default.CalendarMonth),
    MAP("Map", Icons.Default.Map)
}

enum class InsightRange(val label: String) {
    ALL_TIME("All Time"),
    YEAR("By Year"),
    MONTH("By Month")
}

enum class DuoType(val label: String) {
    ACTOR_ACTOR("Actor × Actor"),
    ACTOR_DIRECTOR("Actor × Director")
}

data class RatingBucket(
    val label: String,
    val count: Int
)

data class DuoRow(
    val leftName: String,
    val rightName: String,
    val count: Int,
    val leftPhotoPath: String? = null,
    val rightPhotoPath: String? = null
)

class InsightsScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val screenModel = koinScreenModel<InsightsScreenModel>()
        val uiState by screenModel.state.collectAsState()

        var rangeExpanded by remember { mutableStateOf(false) }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                AppDrawer(
                    navigator = navigator,
                    currentScreen = this@InsightsScreen,
                    closeDrawer = { scope.launch { drawerState.close() } }
                )
            }
        ) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Insights",
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            ) { padding ->
                AdaptiveLayout(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) { adaptive ->

                    val scaffold = adaptive.tokens.scaffold
                    val contentMaxWidth = when {
                        adaptive.window.isExpanded -> 1200.dp
                        adaptive.window.isMedium -> 920.dp
                        else -> scaffold.maxCenteredContentWidth
                    }
                    val isCompact = adaptive.window.isCompact

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = contentMaxWidth),
                            contentPadding = PaddingValues(
                                horizontal = scaffold.horizontalPadding,
                                vertical = scaffold.verticalPadding
                            ),
                            verticalArrangement = Arrangement.spacedBy(scaffold.sectionSpacing)
                        ) {
                            item {
                                InsightModeStrip(
                                    selected = uiState.selectedMode,
                                    onSelect = { screenModel.setMode(it) }
                                )
                            }

                            item {
                                if (uiState.isRangeLoading) {
                                    RangeFilterSkeleton(compact = isCompact)
                                } else {
                                    RangeFilterRow(
                                        range = uiState.selectedRange,
                                        selectedYear = uiState.selectedYear,
                                        selectedMonth = uiState.selectedMonth,
                                        availableYears = uiState.availableYears,
                                        availableMonthsForSelectedYear = uiState.availableMonthsByYear[uiState.selectedYear].orEmpty(),
                                        onRangeChange = screenModel::setRange,
                                        compact = isCompact
                                    )
                                }
                            }

                            item {
                                when (uiState.selectedMode) {
                                    InsightMode.RATINGS -> {
                                        if (uiState.isSectionLoading) {
                                            RatingsSectionSkeleton()
                                        } else {
                                            RatingsInsightSection(
                                                distribution = uiState.ratingDistribution,
                                                compact = adaptive.window.isCompact,
                                                medium = adaptive.window.isMedium,
                                                selectedRange = uiState.selectedRange,
                                                selectedYear = uiState.selectedYear,
                                                selectedMonth = uiState.selectedMonth,
                                                navigator = navigator
                                            )
                                        }
                                    }

                                    InsightMode.DUOS -> {
                                        if (uiState.isSectionLoading) {
                                            DuosSectionSkeleton()
                                        } else {
                                            DuosInsightSection(
                                                rows = uiState.topDuos,
                                                selectedDuoType = uiState.selectedDuoType,
                                                selectedRange = uiState.selectedRange,
                                                selectedYear = uiState.selectedYear,
                                                selectedMonth = uiState.selectedMonth,
                                                navigator = navigator,
                                                onDuoTypeSelect = { screenModel.setDuoType(it) }
                                            )
                                        }
                                    }

                                    InsightMode.DECADE -> {
                                        if (uiState.isSectionLoading) {
                                            DecadeSectionSkeleton()
                                        } else {
                                            DecadeInsightSection(
                                                buckets = uiState.decadeBuckets,
                                                navigator = navigator,
                                                selectedRange = uiState.selectedRange,
                                                selectedYear = uiState.selectedYear,
                                                selectedMonth = uiState.selectedMonth,
                                            )
                                        }
                                    }

                                    InsightMode.HABITS -> {
                                        if (uiState.isSectionLoading) {
                                            HabitsSectionSkeleton()
                                        } else {
                                            HabitsInsightSection(summary = uiState.habitsSummary)
                                        }
                                    }

                                    InsightMode.MAP -> {
                                        if (uiState.isSectionLoading) {
                                            MapSectionSkeleton()
                                        } else {
                                            MapInsightSection(
                                                rows = uiState.mapCountries,
                                                navigator = navigator,
                                                range = uiState.selectedRange,
                                                year = uiState.selectedYear,
                                                month = uiState.selectedMonth
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
    }
}