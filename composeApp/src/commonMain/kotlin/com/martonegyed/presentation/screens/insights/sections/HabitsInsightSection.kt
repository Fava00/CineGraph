package com.martonegyed.presentation.screens.insights.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.martonegyed.presentation.components.common.SkeletonCard
import com.martonegyed.presentation.components.common.cards.SectionCard
import com.martonegyed.presentation.screens.insights.HabitsSummary


@Composable
fun HabitsInsightSection(
    summary: HabitsSummary
) {
    if (summary.totalWatches == 0) {
        SectionCard(
            title = "No habits yet",
            subtitle = "Watch more movies to build your viewing patterns."
        )
        return
    }

    val colors = MaterialTheme.colorScheme
    val maxWeekdayCount = summary.weekdayBuckets.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SectionCard(
            title = "Habits",
            subtitle = "A quick look at when and how you watch."
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HabitMetricCard(
                        title = "Watched",
                        value = summary.totalWatches.toString(),
                        subtitle = "In this range",
                        modifier = Modifier.weight(1f)
                    )
                    HabitMetricCard(
                        title = "Rewatches",
                        value = summary.rewatchCount.toString(),
                        subtitle = "${summary.rewatchPercent}%",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HabitMetricCard(
                        title = "Favorite day",
                        value = summary.favoriteWeekdayLabel,
                        subtitle = "${summary.favoriteWeekdayCount} watches",
                        modifier = Modifier.weight(1f)
                    )
                    HabitMetricCard(
                        title = "Weekend share",
                        value = "${summary.weekendPercent}%",
                        subtitle = "${summary.weekendCount} logged watches",
                        modifier = Modifier.weight(1f)
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Weekday activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onSurface
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        summary.weekdayBuckets.forEach { bucket ->
                            val fraction = bucket.count.toFloat() / maxWeekdayCount.toFloat()

                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                Text(
                                    text = bucket.count.toString(),
                                    color = colors.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall
                                )

                                Spacer(Modifier.height(8.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.surfaceVariant.copy(alpha = 0.35f)),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    if (bucket.count > 0) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .fillMaxHeight(fraction.coerceIn(0f, 1f))
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(colors.primary)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(8.dp))

                                Text(
                                    text = bucket.label,
                                    color = colors.onSurface,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    Text(
                        text = "Based on watched dates in the selected range.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun HabitMetricCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = colors.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
fun HabitsSectionSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SkeletonCard(
                modifier = Modifier.weight(1f),
                height = 110.dp
            )
            SkeletonCard(
                modifier = Modifier.weight(1f),
                height = 110.dp
            )
        }
        SkeletonCard(height = 140.dp)
        SkeletonCard(height = 240.dp)
    }
}