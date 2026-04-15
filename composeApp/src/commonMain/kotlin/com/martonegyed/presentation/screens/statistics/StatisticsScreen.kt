package com.martonegyed.presentation.screens.statistics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import com.martonegyed.core.util.revenueFormater
import com.martonegyed.core.util.roundToDecimals
import com.martonegyed.presentation.components.common.AppDrawer
import com.martonegyed.presentation.components.common.HeroStatCard
import com.martonegyed.presentation.screens.movies.CollectionType
import com.martonegyed.presentation.screens.movies.MovieCollectionScreen
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime


class StatisticsScreen : Screen {

    @Preview
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
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colors.primary)
                    }
                    return@Scaffold
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            HeroStatCard(
                                Modifier.weight(1f), Icons.Default.Movie,
                                state.totalMovies.toString(), "Watched"
                            )
                            HeroStatCard(
                                Modifier.weight(1f), Icons.Default.AccessTime,
                                "${state.totalHours.roundToDecimals(2)}h", "Hours"
                            )
                            HeroStatCard(
                                Modifier.weight(1f), Icons.Default.Star,
                                if (state.averageRating > 0) (state.averageRating.roundToDecimals(2)).toString() else "-",
                                "Avg Rating"
                            )
                            HeroStatCard(
                                Modifier.weight(1f),
                                Icons.Default.AttachMoney,

                                revenueFormater(state.totalRevenue),
                                "Revenue"
                            )
                        }
                    }

                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            EntityTabs(state = state, onSelect = onEntityChange)
                            MetricAndRangeRow(
                                state = state,
                                onMetricChange = onMetricChange,
                                onRangeChange = onRangeChange
                            )
                        }
                    }

                    itemsIndexed(state.rows) { index, row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clickable {
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
                                },

                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatAvatar(row = row, index = index)
                            Spacer(Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    row.name,
                                    color = colors.onBackground,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(4.dp))
                                val topRow = state.rows.firstOrNull()
                                val fraction = when (state.metric) {
                                    StatMetric.COUNT -> {
                                        val max = topRow?.count ?: 1
                                        if (max <= 0) 0f else row.count.toFloat() / max
                                    }

                                    StatMetric.AVG_RATING -> {
                                        val max = state.rows.maxOfOrNull { it.avgRating ?: 0.0 } ?: 0.0
                                        if (max <= 0.0 || row.avgRating == null) 0f
                                        else (row.avgRating.toFloat() / max.toFloat())
                                    }

                                    StatMetric.WATCH_TIME -> {
                                        val max = state.rows.maxOfOrNull { it.totalMinutes } ?: 1
                                        if (max <= 0) 0f else row.totalMinutes.toFloat() / max.toFloat()
                                    }

                                    StatMetric.REVENUE -> {
                                        val max = state.rows.maxOfOrNull { it.totalRevenue } ?: 1L
                                        if (max <= 0L) 0f else row.totalRevenue.toFloat() / max.toFloat()
                                    }
                                }.coerceIn(0f, 1f)

                                Box(
                                    modifier = Modifier
                                        .height(6.dp)
                                        .fillMaxWidth(fraction)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(
                                            when (state.metric) {
                                                StatMetric.COUNT -> colors.primary
                                                StatMetric.AVG_RATING -> colors.secondary
                                                StatMetric.WATCH_TIME -> colors.scrim
                                                StatMetric.REVENUE -> colors.tertiary
                                            }
                                        )
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            when (state.metric) {
                                StatMetric.COUNT -> Text(
                                    row.count.toString(),
                                    color = colors.onBackground,
                                    fontSize = 14.sp
                                )

                                StatMetric.AVG_RATING -> Text(
                                    row.avgRating?.roundToDecimals(2)?.toString() ?: "-",
                                    color = colors.onBackground,
                                    fontSize = 14.sp
                                )

                                StatMetric.WATCH_TIME -> Text(
                                    "${(row.totalMinutes / 60.0).roundToInt()}h",
                                    color = colors.onBackground, fontSize = 14.sp
                                )

                                StatMetric.REVENUE -> Text(
                                    if (row.totalRevenue > 0) {
                                        revenueFormater(row.totalRevenue)
                                    } else {
                                        "0"
                                    },
                                    color = colors.onBackground, fontSize = 14.sp
                                )
                            }
                        }
                    }

                }
            }
        }
    }


    @Composable
    private fun SectionHeader(title: String) {
        Text(
            title, fontSize = 16.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), letterSpacing = 0.5.sp
        )
    }

    @Composable
    private fun RatingBarChart(distribution: Map<String, Int>) {
        val maxVal = distribution.values.maxOrNull() ?: 1
        Row(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            distribution.entries.forEach { (label, count) ->
                val fraction = count.toFloat() / maxVal
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(count.toString(), fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((80 * fraction).dp.coerceAtLeast(4.dp))
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    @Composable
    private fun MonthBarChart(byMonth: Map<Int, Int>) {
        val months = listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
        val maxVal = byMonth.values.maxOrNull() ?: 1
        Row(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            (1..12).forEach { month ->
                val count = byMonth[month] ?: 0
                val fraction = count.toFloat() / maxVal
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (count > 0) Text(
                        count.toString(),
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    else Spacer(Modifier.height(12.dp))
                    Spacer(Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((60 * fraction).dp.coerceAtLeast(4.dp))
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(MaterialTheme.colorScheme.secondary)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(months[month - 1], fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    @Composable
    private fun HorizontalBarList(items: List<Pair<String, Int>>, barColor: Color) {
        val maxVal = items.firstOrNull()?.second ?: 1
        Column(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items.forEachIndexed { index, (name, count) ->
                val fraction = count.toFloat() / maxVal
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${index + 1}.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            name, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium, maxLines = 1
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(barColor)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        count.toString(), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    @Composable
    private fun EntityTabs(state: StatisticsState, onSelect: (StatEntityType) -> Unit) {
        val colors = MaterialTheme.colorScheme
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surfaceVariant)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EntityTab("Directors", StatEntityType.DIRECTORS, state.entityType, onSelect, Modifier.weight(1f))
            EntityTab("Actors", StatEntityType.ACTORS, state.entityType, onSelect, Modifier.weight(1f))
            EntityTab("Genres", StatEntityType.GENRES, state.entityType, onSelect, Modifier.weight(1f))
            EntityTab("Studios", StatEntityType.STUDIOS, state.entityType, onSelect, Modifier.weight(1f))
            EntityTab("Countries", StatEntityType.COUNTRIES, state.entityType, onSelect, Modifier.weight(1f))
        }
    }

    @Composable
    private fun EntityTab(
        label: String,
        type: StatEntityType,
        current: StatEntityType,
        onSelect: (StatEntityType) -> Unit,
        modifier: Modifier = Modifier
    ) {
        val selected = current == type
        val colors = MaterialTheme.colorScheme

        Surface(
            modifier = modifier.height(40.dp),
            color = if (selected) colors.primary.copy(alpha = 0.14f) else Color.Transparent,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(
                1.dp,
                if (selected) colors.primary else colors.outline.copy(alpha = 0.7f)
            ),
            onClick = { onSelect(type) }
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (selected) colors.onSurface else colors.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1
                )
            }
        }
    }

    @Composable
    private fun MetricAndRangeRow(
        state: StatisticsState,
        onMetricChange: (StatMetric) -> Unit,
        onRangeChange: (StatRange, Int?, Int?) -> Unit
    ) {
        val colors = MaterialTheme.colorScheme
        var rangeExpanded by remember { mutableStateOf(false) }
        var yearExpanded by remember { mutableStateOf(false) }
        var monthExpanded by remember { mutableStateOf(false) }

        val years = remember { (currentYear() downTo currentYear() - 50).toList() }
        val months = remember {
            listOf(
                "Jan" to 1,
                "Feb" to 2,
                "Mar" to 3,
                "Apr" to 4,
                "May" to 5,
                "Jun" to 6,
                "Jul" to 7,
                "Aug" to 8,
                "Sep" to 9,
                "Oct" to 10,
                "Nov" to 11,
                "Dec" to 12
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricSegmentedControl(
                    selected = state.metric,
                    onMetricChange = onMetricChange,
                    modifier = Modifier.weight(1.25f)
                )

                Box(modifier = Modifier.weight(1f)) {
                    StatDropdownField(
                        text = when (state.range) {
                            StatRange.ALL_TIME -> "All Time"
                            StatRange.YEAR -> "By Year"
                            StatRange.MONTH -> "By Month"
                        },
                        onClick = { rangeExpanded = true }
                    )

                    DropdownMenu(
                        expanded = rangeExpanded,
                        onDismissRequest = { rangeExpanded = false },
                        containerColor = colors.surfaceVariant
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Time", color = colors.onSurface) },
                            onClick = {
                                rangeExpanded = false
                                onRangeChange(StatRange.ALL_TIME, null, null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("By Year", color = colors.onSurface) },
                            onClick = {
                                rangeExpanded = false
                                onRangeChange(
                                    StatRange.YEAR,
                                    state.selectedYear ?: currentYear(),
                                    null
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("By Month", color = colors.onSurface) },
                            onClick = {
                                rangeExpanded = false
                                onRangeChange(
                                    StatRange.MONTH,
                                    state.selectedYear ?: currentYear(),
                                    state.selectedMonth ?: currentMonth()
                                )
                            }
                        )
                    }
                }
            }

            when (state.range) {
                StatRange.ALL_TIME -> Unit

                StatRange.YEAR -> {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        StatDropdownField(
                            text = (state.selectedYear ?: currentYear()).toString(),
                            onClick = { yearExpanded = true }
                        )

                        DropdownMenu(
                            expanded = yearExpanded,
                            onDismissRequest = { yearExpanded = false },
                            modifier = Modifier.heightIn(max = 320.dp),
                            containerColor = colors.surfaceVariant
                        ) {
                            years.forEach { year ->
                                DropdownMenuItem(
                                    text = { Text(year.toString(), color = colors.onSurface) },
                                    onClick = {
                                        yearExpanded = false
                                        onRangeChange(StatRange.YEAR, year, null)
                                    }
                                )
                            }
                        }
                    }
                }

                StatRange.MONTH -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            StatDropdownField(
                                text = (state.selectedYear ?: currentYear()).toString(),
                                onClick = { yearExpanded = true }
                            )

                            DropdownMenu(
                                expanded = yearExpanded,
                                onDismissRequest = { yearExpanded = false },
                                modifier = Modifier.heightIn(max = 320.dp),
                                containerColor = colors.surfaceVariant
                            ) {
                                years.forEach { year ->
                                    DropdownMenuItem(
                                        text = { Text(year.toString(), color = colors.onSurface) },
                                        onClick = {
                                            yearExpanded = false
                                            onRangeChange(
                                                StatRange.MONTH,
                                                year,
                                                state.selectedMonth ?: currentMonth()
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            StatDropdownField(
                                text = months.firstOrNull { it.second == state.selectedMonth }?.first ?: "Month",
                                onClick = { monthExpanded = true }
                            )

                            DropdownMenu(
                                expanded = monthExpanded,
                                onDismissRequest = { monthExpanded = false },
                                modifier = Modifier.heightIn(max = 320.dp),
                                containerColor = colors.surfaceVariant
                            ) {
                                months.forEach { (label, value) ->
                                    DropdownMenuItem(
                                        text = { Text(label, color = colors.onSurface) },
                                        onClick = {
                                            monthExpanded = false
                                            onRangeChange(
                                                StatRange.MONTH,
                                                state.selectedYear ?: currentYear(),
                                                value
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun MetricSegmentedControl(
        selected: StatMetric,
        onMetricChange: (StatMetric) -> Unit,
        modifier: Modifier = Modifier
    ) {
        val colors = MaterialTheme.colorScheme
        val items = listOf(
            Triple("Count", Icons.Default.BarChart, StatMetric.COUNT),
            Triple("Rate", Icons.Default.Star, StatMetric.AVG_RATING),
            Triple("Time", Icons.Default.AccessTime, StatMetric.WATCH_TIME),
            Triple("Rev", Icons.Default.AttachMoney, StatMetric.REVENUE)
        )

        val outerShape = RoundedCornerShape(12.dp)

        Surface(
            modifier = modifier.height(50.dp),
            shape = outerShape,
            color = colors.surface,
            border = BorderStroke(1.dp, colors.onSurfaceVariant.copy(alpha = 0.3f)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(outerShape)
            ) {
                items.forEachIndexed { index, (label, icon, metric) ->
                    val selectedItem = selected == metric

                    val segmentShape = when (index) {
                        0 -> RoundedCornerShape(topStart = 12.dp, bottomStart = 16.dp)
                        items.lastIndex -> RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                        else -> RoundedCornerShape(0.dp)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(segmentShape)
                            .background(
                                if (selectedItem) {
                                    when (metric) {
                                        StatMetric.COUNT -> colors.primary.copy(alpha = 0.24f)
                                        StatMetric.AVG_RATING -> colors.secondary.copy(alpha = 0.24f)
                                        StatMetric.WATCH_TIME -> colors.scrim.copy(alpha = 0.24f)
                                        StatMetric.REVENUE -> colors.tertiary.copy(alpha = 0.24f)
                                    }
                                } else Color.Transparent
                            )
                            .clickable { onMetricChange(metric) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (selectedItem) {
                                    when (metric) {
                                        StatMetric.COUNT -> colors.primary
                                        StatMetric.AVG_RATING -> colors.secondary
                                        StatMetric.WATCH_TIME -> colors.scrim
                                        StatMetric.REVENUE -> colors.tertiary
                                    }
                                } else colors.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = label,
                                color = if (selectedItem) colors.onSurface else colors.onSurfaceVariant,
                                fontSize = 10.sp,
                                fontWeight = if (selectedItem) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }

                    if (index != items.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp)
                                .background(colors.outline.copy(alpha = 0.6f))
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun StatDropdownField(
        text: String,
        onClick: () -> Unit
    ) {
        val colors = MaterialTheme.colorScheme
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.7f)),
            onClick = onClick
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = text,
                    color = colors.onSurface,
                    fontSize = 15.sp,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

    @Composable
    private fun StatAvatar(row: EntityRow, index: Int) {
        val colors = MaterialTheme.colorScheme
        val size = 40.dp
        val initials = row.initials.ifBlank { (index + 1).toString() }
        val imageUrl = row.photoPath?.let { "https://image.tmdb.org/t/p/w200$it" }

        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(999.dp))
                .background(colors.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = row.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = initials,
                    color = colors.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }


    @OptIn(ExperimentalTime::class)
    fun currentMonth(): Int {
        val now = Clock.System.now()
        val dateTime = now.toLocalDateTime(TimeZone.currentSystemDefault())
        return dateTime.month.number
    }

    @OptIn(ExperimentalTime::class)
    fun currentYear(): Int {
        val now = Clock.System.now()
        val dateTime = now.toLocalDateTime(TimeZone.currentSystemDefault())
        return dateTime.year
    }
}
