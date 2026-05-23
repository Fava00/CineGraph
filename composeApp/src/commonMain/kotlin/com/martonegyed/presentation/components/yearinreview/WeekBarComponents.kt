package com.martonegyed.presentation.components.yearinreview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.martonegyed.presentation.screens.yearinreview.WeekCountRow
import com.martonegyed.presentation.screens.yearinreview.WeekdayCountRow

@Composable
fun WeekdayBarSection(rows: List<WeekdayCountRow>) {
    val colors = MaterialTheme.colorScheme
    val maxCount = (rows.maxOfOrNull { it.count } ?: 1).coerceAtLeast(1)
    val maxIndex = rows.indexOfFirst { it.count == maxCount }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        rows.forEachIndexed { index, row ->
            val fraction = (row.count.toFloat() / maxCount.toFloat()).coerceIn(0.06f, 1f)
            val isHighlight = index == maxIndex
            val barColor = if (isHighlight) colors.primary else colors.secondary

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height((130f * fraction).dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(barColor)
                )

                Text(
                    text = row.label.take(1),
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun WeekBarSection(rows: List<WeekCountRow>) {
    val colors = MaterialTheme.colorScheme
    val maxCount = (rows.maxOfOrNull { it.count } ?: 1).coerceAtLeast(1)
    val highlightWeek = rows.maxByOrNull { it.count }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                rows.forEach { row ->
                    val fraction = (row.count.toFloat() / maxCount.toFloat()).coerceIn(0.06f, 1f)
                    val isHighlight = row.week == highlightWeek?.week

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(fraction)
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(if (isHighlight) colors.primary else colors.secondary)
                    )
                }
            }

            highlightWeek?.let { peak ->
                val totalWeeks = rows.size.coerceAtLeast(1)
                val index = rows.indexOfFirst { it.week == peak.week }
                val xFraction = (index.toFloat() + 0.5f) / totalWeeks.toFloat()

                Text(
                    text = "W${peak.week}\n(${peak.count})",
                    color = colors.primary,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth(xFraction)
                        .wrapContentWidth(Alignment.End)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("Jan", "Apr", "Jul", "Oct", "Dec").forEach { month ->
                Text(
                    text = month,
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}