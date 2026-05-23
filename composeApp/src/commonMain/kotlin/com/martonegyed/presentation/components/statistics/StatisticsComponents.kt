package com.martonegyed.presentation.components.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.martonegyed.core.util.revenueFormater
import com.martonegyed.core.util.roundToDecimals
import com.martonegyed.presentation.components.common.PersonAvatar
import com.martonegyed.presentation.screens.statistics.EntityRow
import com.martonegyed.presentation.screens.statistics.StatMetric
import kotlin.math.roundToInt

@Composable
fun StatisticsResultCard(
    row: EntityRow,
    index: Int,
    metric: StatMetric,
    rows: List<EntityRow>,
    avatarSize: Dp,
    maxBarWidth: Dp,
    onClick: () -> Unit
) {
    val fraction = metricFraction(row, metric, rows)
    val valueText = metricValue(row, metric)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1F2326),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PersonAvatar(
                name = row.name,
                photoPath = row.photoPath,
                size = avatarSize,
                borderColor = null,
                fallbackText = row.initials.ifBlank { (index + 1).toString() }
            )

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = row.name,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(max = maxBarWidth)
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFF2C3136))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color(0xFF00E054))
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Text(
                        text = valueText,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun metricFraction(
    row: EntityRow,
    metric: StatMetric,
    rows: List<EntityRow>
): Float {
    return when (metric) {
        StatMetric.COUNT -> {
            val max = rows.maxOfOrNull { it.count } ?: 1
            if (max <= 0) 0f else row.count.toFloat() / max
        }

        StatMetric.AVG_RATING -> {
            val max = rows.maxOfOrNull { it.avgRating ?: 0.0 } ?: 0.0
            if (max <= 0.0 || row.avgRating == null) 0f else (row.avgRating / max).toFloat()
        }

        StatMetric.WATCH_TIME -> {
            val max = rows.maxOfOrNull { it.totalMinutes } ?: 1
            if (max <= 0) 0f else row.totalMinutes.toFloat() / max
        }

        StatMetric.REVENUE -> {
            val max = rows.maxOfOrNull { it.totalRevenue } ?: 1L
            if (max <= 0L) 0f else row.totalRevenue.toFloat() / max.toFloat()
        }
    }.coerceIn(0f, 1f)
}

private fun metricValue(row: EntityRow, metric: StatMetric): String {
    return when (metric) {
        StatMetric.COUNT -> row.count.toString()
        StatMetric.AVG_RATING -> row.avgRating?.roundToDecimals(2)?.toString() ?: "-"
        StatMetric.WATCH_TIME -> "${(row.totalMinutes / 60.0).roundToInt()}h"
        StatMetric.REVENUE -> if (row.totalRevenue > 0) revenueFormater(row.totalRevenue) else "0"
    }
}

