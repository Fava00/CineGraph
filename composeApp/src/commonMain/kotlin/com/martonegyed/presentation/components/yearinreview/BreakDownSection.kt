package com.martonegyed.presentation.components.yearinreview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.martonegyed.presentation.components.common.DonutChart
import com.martonegyed.presentation.components.common.DonutChartItem
import com.martonegyed.presentation.components.common.cards.SectionCard
import com.martonegyed.presentation.screens.yearinreview.PieSliceRow
import kotlin.math.roundToInt

@Composable
fun BreakDownSection(newVsOld: List<PieSliceRow>,rewatchesVsFirstTime: List<PieSliceRow>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (newVsOld.isNotEmpty()) {
            SectionCard(
                title = "New vs old",
                modifier = Modifier.weight(1f)
            ) {
                CompactBreakdownChart(rows = newVsOld)
            }
        } else {
            Spacer(Modifier.weight(1f))
        }

        if (rewatchesVsFirstTime.isNotEmpty()) {
            SectionCard(
                title = "Rewatches",
                modifier = Modifier.weight(1f)
            ) {
                CompactBreakdownChart(rows = rewatchesVsFirstTime)
            }
        } else {
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun CompactBreakdownChart(rows: List<PieSliceRow>) {
    val colors = MaterialTheme.colorScheme
    val total = rows.sumOf { it.count }.coerceAtLeast(1)

    val chartColors = listOf(
        colors.inversePrimary,
        colors.onSurfaceVariant.copy(alpha = 0.7f)
    )

    val items = rows.mapIndexed { index, row ->
        DonutChartItem(
            label = row.label,
            value = row.count.toFloat(),
            color = chartColors[index % chartColors.size]
        )
    }

    val dominant = rows.maxByOrNull { it.count }
    val dominantPercent = dominant
        ?.let { ((it.count * 100f) / total.toFloat()).roundToInt() }
        ?: 0

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            DonutChart(
                items = items,
                modifier = Modifier.size(150.dp),
                strokeWidth = 18.dp
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "$dominantPercent%",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )
                Text(
                    text = dominant?.label ?: "",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            rows.forEachIndexed { index, row ->
                val percent = ((row.count * 100f) / total.toFloat()).roundToInt()

                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surfaceVariant.copy(alpha = 0.55f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(chartColors[index % chartColors.size])
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = row.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${row.count} • $percent%",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}