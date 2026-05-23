package com.martonegyed.presentation.screens.insights.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.Navigator
import com.martonegyed.presentation.analytics.StatRange
import com.martonegyed.presentation.components.common.DonutChart
import com.martonegyed.presentation.components.common.DonutChartItem
import com.martonegyed.presentation.components.common.SkeletonCard
import com.martonegyed.presentation.components.insights.DecadeLegendRow
import com.martonegyed.presentation.components.common.cards.SectionCard
import com.martonegyed.presentation.components.insights.decadeStartFromLabel
import com.martonegyed.presentation.screens.insights.DecadeBucket
import com.martonegyed.presentation.screens.movies.CollectionType
import com.martonegyed.presentation.screens.movies.MovieCollectionScreen


@Composable
fun DecadeInsightSection(
    buckets: List<DecadeBucket>,
    selectedRange: StatRange,
    selectedYear: Int?,
    selectedMonth: Int?,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    minorThresholdPercent: Float = 5f
) {
    val colors = MaterialTheme.colorScheme
    if (buckets.isEmpty()) {
        SectionCard(
            title = "No decade data yet",
            subtitle = "Watch more movies to see your era breakdown."
        )
        return
    }

    val total = buckets.sumOf { it.count }.coerceAtLeast(1)
    val sorted = buckets.sortedByDescending { it.count }
    val topBucket = sorted.firstOrNull()

    val palette = listOf(
        colors.primaryContainer,
        colors.inversePrimary,
        colors.primary,
        colors.error,
        colors.secondary,
        colors.tertiary
    )
    val otherColor = colors.onSurfaceVariant

    val majorBuckets: List<DecadeBucket>
    val minorBuckets: List<DecadeBucket>

    run {
        val partitioned = sorted.partition { bucket ->
            (bucket.count * 100f / total) >= minorThresholdPercent
        }

        val majors = partitioned.first.toMutableList()
        val minors = partitioned.second.toMutableList()

        if (majors.isEmpty() && sorted.isNotEmpty()) {
            majors += sorted.first()
            minors.clear()
            minors += sorted.drop(1)
        }

        majorBuckets = majors
        minorBuckets = minors
    }

    val chartItems = remember(majorBuckets, minorBuckets) {
        buildList {
            majorBuckets.forEachIndexed { index, bucket ->
                add(
                    DonutChartItem(
                        label = bucket.label,
                        value = bucket.count.toFloat(),
                        color = palette[index % palette.size]
                    )
                )
            }

            val otherCount = minorBuckets.sumOf { it.count }
            if (otherCount > 0) {
                add(
                    DonutChartItem(
                        label = "Other",
                        value = otherCount.toFloat(),
                        color = otherColor
                    )
                )
            }
        }
    }

    val legendRows = remember(majorBuckets, minorBuckets, total) {
        buildList {
            majorBuckets.forEachIndexed { index, bucket ->
                add(
                    DecadeLegendRow(
                        label = bucket.label,
                        count = bucket.count,
                        percent = (bucket.count * 100f / total).toInt(),
                        color = palette[index % palette.size],
                        decadeStart = decadeStartFromLabel(bucket.label),
                        indented = false,
                        clickable = true
                    )
                )
            }

            val otherCount = minorBuckets.sumOf { it.count }
            if (otherCount > 0) {
                add(
                    DecadeLegendRow(
                        label = "Other",
                        count = otherCount,
                        percent = (otherCount * 100f / total).toInt(),
                        color = otherColor,
                        decadeStart = null,
                        indented = false,
                        clickable = false
                    )
                )

                minorBuckets.forEach { bucket ->
                    add(
                        DecadeLegendRow(
                            label = bucket.label,
                            count = bucket.count,
                            percent = (bucket.count * 100f / total).toInt(),
                            color = otherColor.copy(alpha = 0.72f),
                            decadeStart = decadeStartFromLabel(bucket.label),
                            indented = true,
                            clickable = true
                        )
                    )
                }
            }
        }
    }

    fun openDecade(decadeStart: Int) {
        navigator.push(
            MovieCollectionScreen(
                type = CollectionType.BY_DECADE,
                decadeStart = decadeStart,
                range = selectedRange,
                year = selectedYear,
                month = selectedMonth
            )
        )
    }

    SectionCard(

        title = "Decades",
        subtitle = topBucket?.let { "${it.label} is your most watched era with ${it.count} films." }
            ?: "Your decade breakdown."
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentAlignment = Alignment.Center
            ) {
                DonutChart(
                    items = chartItems,
                    onItemClick = { item ->
                        val decadeStart = decadeStartFromLabel(item.label)
                        if (decadeStart != null) {
                            openDecade(decadeStart)
                        }
                    }
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = total.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "watched",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                legendRows.forEach { row ->
                    val rowModifier = if (row.clickable && row.decadeStart != null) {
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { openDecade(row.decadeStart) }
                            .padding(
                                start = if (row.indented) 24.dp else 0.dp,
                                top = 6.dp,
                                bottom = 6.dp
                            )
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                start = if (row.indented) 24.dp else 0.dp,
                                top = 6.dp,
                                bottom = 6.dp
                            )
                    }

                    Row(
                        modifier = rowModifier,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (row.indented) 8.dp else 10.dp)
                                .clip(CircleShape)
                                .background(row.color)
                        )

                        Spacer(Modifier.width(10.dp))

                        Text(
                            text = row.label,
                            modifier = Modifier.weight(1f),
                            color = if (row.indented) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            fontWeight = if (row.indented) FontWeight.Normal else FontWeight.Medium
                        )

                        Text(
                            text = "${row.count} • ${row.percent}%",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DecadeSectionSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SkeletonCard(height = 84.dp)
        SkeletonCard(height = 260.dp)
        SkeletonCard(height = 120.dp)
    }
}