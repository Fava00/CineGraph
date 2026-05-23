package com.martonegyed.presentation.components.statistics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.martonegyed.presentation.analytics.StatRange
import com.martonegyed.presentation.components.common.SkeletonCard
import com.martonegyed.presentation.screens.statistics.StatEntityType
import com.martonegyed.presentation.screens.statistics.StatMetric

import com.martonegyed.presentation.screens.statistics.StatisticsState
import kotlin.collections.get
import kotlin.collections.orEmpty

@Composable
fun RangeFilterRow(
    range: StatRange,
    selectedYear: Int?,
    selectedMonth: Int?,
    availableYears: List<Int>,
    availableMonthsForSelectedYear: List<Int>,
    onRangeChange: (StatRange, Int?, Int?) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean
) {
    val colors = MaterialTheme.colorScheme
    var rangeExpanded by remember { mutableStateOf(false) }
    var yearExpanded by remember { mutableStateOf(false) }
    var monthExpanded by remember { mutableStateOf(false) }

    val months = remember {
        listOf(
            "January" to 1,
            "February" to 2,
            "March" to 3,
            "April" to 4,
            "May" to 5,
            "June" to 6,
            "July" to 7,
            "August" to 8,
            "September" to 9,
            "October" to 10,
            "November" to 11,
            "December" to 12
        )
    }

    val resolvedYear = selectedYear ?: availableYears.firstOrNull()
    val resolvedMonth = selectedMonth ?: availableMonthsForSelectedYear.firstOrNull()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            StatDropdownField(
                text = when (range) {
                    StatRange.ALL_TIME -> "All Time"
                    StatRange.YEAR -> "By Year"
                    StatRange.MONTH -> "By Month"
                },
                onClick = { rangeExpanded = true },
                compact
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
                    enabled = availableYears.isNotEmpty(),
                    onClick = {
                        rangeExpanded = false
                        onRangeChange(
                            StatRange.YEAR,
                            resolvedYear,
                            null
                        )
                    }
                )

                DropdownMenuItem(
                    text = { Text("By Month", color = colors.onSurface) },
                    enabled = availableYears.isNotEmpty(),
                    onClick = {
                        rangeExpanded = false
                        val year = resolvedYear
                        val month = availableMonthsForSelectedYear.firstOrNull()
                        onRangeChange(
                            StatRange.MONTH,
                            year,
                            month
                        )
                    }
                )
            }
        }

        when (range) {
            StatRange.ALL_TIME -> Unit

            StatRange.YEAR -> {
                Box(modifier = Modifier.fillMaxWidth()) {
                    StatDropdownField(
                        text = resolvedYear?.toString() ?: "Year",
                        onClick = { yearExpanded = true },
                        compact = compact
                    )

                    DropdownMenu(
                        expanded = yearExpanded,
                        onDismissRequest = { yearExpanded = false },
                        modifier = Modifier.heightIn(max = 320.dp),
                        containerColor = colors.surfaceVariant
                    ) {
                        availableYears.forEach { year ->
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
                            text = resolvedYear?.toString() ?: "Year",
                            onClick = { yearExpanded = true },
                            compact = compact
                        )

                        DropdownMenu(
                            expanded = yearExpanded,
                            onDismissRequest = { yearExpanded = false },
                            modifier = Modifier.heightIn(max = 320.dp),
                            containerColor = colors.surfaceVariant
                        ) {
                            availableYears.forEach { year ->
                                DropdownMenuItem(
                                    text = { Text(year.toString(), color = colors.onSurface) },
                                    onClick = {
                                        yearExpanded = false
                                        onRangeChange(
                                            StatRange.MONTH,
                                            year,
                                            null
                                        )
                                    }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        StatDropdownField(
                            text = months.firstOrNull { it.second == resolvedMonth }?.first ?: "Month",
                            onClick = { monthExpanded = true },
                            compact = compact
                        )

                        DropdownMenu(
                            expanded = monthExpanded,
                            onDismissRequest = { monthExpanded = false },
                            modifier = Modifier.heightIn(max = 320.dp),
                            containerColor = colors.surfaceVariant
                        ) {
                            months
                                .filter { it.second in availableMonthsForSelectedYear }
                                .forEach { (label, value) ->
                                    DropdownMenuItem(
                                        text = { Text(label, color = colors.onSurface) },
                                        onClick = {
                                            monthExpanded = false
                                            onRangeChange(
                                                StatRange.MONTH,
                                                resolvedYear,
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
fun RangeFilterSkeleton(compact: Boolean) {
    SkeletonCard(height = if (compact) 96.dp else 64.dp)
}

@Composable
private fun MetricAndRangeRow(
    state: StatisticsState,
    onMetricChange: (StatMetric) -> Unit,
    onRangeChange: (StatRange, Int?, Int?) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth()
    ) {
        val compact = maxWidth < 640.dp

        if (compact) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricSegmentedControl(
                    selected = state.metric,
                    onMetricChange = onMetricChange,
                    compact = true,
                    modifier = Modifier.fillMaxWidth()
                )

                RangeFilterRow(
                    range = state.range,
                    selectedYear = state.selectedYear,
                    selectedMonth = state.selectedMonth,
                    availableYears = state.availableYears,
                    availableMonthsForSelectedYear = state.availableMonthsByYear[state.selectedYear].orEmpty(),
                    onRangeChange = onRangeChange,
                    compact = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricSegmentedControl(
                    selected = state.metric,
                    onMetricChange = onMetricChange,
                    compact = false,
                    modifier = Modifier.widthIn(min = 220.dp, max = 260.dp)
                )

                RangeFilterRow(
                    range = state.range,
                    selectedYear = state.selectedYear,
                    selectedMonth = state.selectedMonth,
                    availableYears = state.availableYears,
                    availableMonthsForSelectedYear = state.availableMonthsByYear[state.selectedYear].orEmpty(),
                    onRangeChange = onRangeChange,
                    compact = false,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MetricSegmentedControl(
    selected: StatMetric,
    onMetricChange: (StatMetric) -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val items = if (compact) {
        listOf(
            Triple("Cnt", Icons.Default.BarChart, StatMetric.COUNT),
            Triple("Rate", Icons.Default.Star, StatMetric.AVG_RATING),
            Triple("Time", Icons.Default.AccessTime, StatMetric.WATCH_TIME),
            Triple("Rev", Icons.Default.AttachMoney, StatMetric.REVENUE)
        )
    } else {
        listOf(
            Triple("Count", Icons.Default.BarChart, StatMetric.COUNT),
            Triple("Rate", Icons.Default.Star, StatMetric.AVG_RATING),
            Triple("Time", Icons.Default.AccessTime, StatMetric.WATCH_TIME),
            Triple("Rev", Icons.Default.AttachMoney, StatMetric.REVENUE)
        )
    }

    val outerShape = RoundedCornerShape(12.dp)

    Surface(
        modifier = modifier.height(if (compact) 44.dp else 50.dp),
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
                    0 -> RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                    items.lastIndex -> RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
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
                                    StatMetric.WATCH_TIME -> colors.inversePrimary.copy(alpha = 0.24f)
                                    StatMetric.REVENUE -> colors.tertiary.copy(alpha = 0.24f)
                                }
                            } else {
                                Color.Transparent
                            }
                        )
                        .clickable { onMetricChange(metric) },
                    contentAlignment = Alignment.Center
                ) {
                    if (compact) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (selectedItem) {
                                    when (metric) {
                                        StatMetric.COUNT -> colors.primary
                                        StatMetric.AVG_RATING -> colors.secondary
                                        StatMetric.WATCH_TIME -> colors.inversePrimary
                                        StatMetric.REVENUE -> colors.tertiary
                                    }
                                } else colors.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = label,
                                color = if (selectedItem) colors.onSurface else colors.onSurfaceVariant,
                                fontSize = 9.sp,
                                fontWeight = if (selectedItem) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1
                            )
                        }
                    } else {
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
                                        StatMetric.WATCH_TIME -> colors.inversePrimary
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
private fun RangeSegmentedControl(
    selected: StatRange,
    onRangeChange: (StatRange, Int?, Int?) -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    val items = if (compact) {
        listOf(
            "All" to StatRange.ALL_TIME,
            "Year" to StatRange.YEAR,
            "Month" to StatRange.MONTH
        )
    } else {
        listOf(
            "All time" to StatRange.ALL_TIME,
            "By year" to StatRange.YEAR,
            "By month" to StatRange.MONTH
        )
    }

    val outerShape = RoundedCornerShape(12.dp)

    Surface(
        modifier = modifier.height(if (compact) 44.dp else 50.dp),
        shape = outerShape,
        color = colors.surface,
        border = BorderStroke(1.dp, colors.onSurfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(outerShape)
        ) {
            items.forEachIndexed { index, (label, range) ->
                val selectedItem = selected == range

                val segmentShape = when (index) {
                    0 -> RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                    items.lastIndex -> RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
                    else -> RoundedCornerShape(0.dp)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(segmentShape)
                        .background(
                            if (selectedItem) colors.primary.copy(alpha = 0.22f)
                            else Color.Transparent
                        )
                        .clickable {
                            when (range) {
                                StatRange.ALL_TIME -> onRangeChange(StatRange.ALL_TIME, null, null)
                                StatRange.YEAR -> onRangeChange(
                                    StatRange.YEAR,
                                    null,
                                    null
                                )

                                StatRange.MONTH -> onRangeChange(
                                    StatRange.MONTH,
                                    null,
                                    null
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (selectedItem) colors.onSurface else colors.onSurfaceVariant,
                        fontSize = if (compact) 10.sp else 11.sp,
                        fontWeight = if (selectedItem) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
    onClick: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(if (compact) 46.dp else 50.dp),
        shape = RoundedCornerShape(12.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.7f)),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (compact) 12.dp else 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                color = colors.onSurface,
                fontSize = if (compact) 13.sp else 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(if (compact) 20.dp else 24.dp)
            )
        }
    }
}

@Composable
fun StatisticsFilterPanel(
    state: StatisticsState,
    onEntityChange: (StatEntityType) -> Unit,
    onMetricChange: (StatMetric) -> Unit,
    onRangeChange: (StatRange, Int?, Int?) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF1F2326),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Filters",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            EntityTabs(state = state, onSelect = onEntityChange)

            MetricAndRangeRow(
                state = state,
                onMetricChange = onMetricChange,
                onRangeChange = onRangeChange
            )
        }
    }
}