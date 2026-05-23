package com.martonegyed.presentation.screens.insights.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.Navigator
import com.martonegyed.presentation.analytics.StatRange
import com.martonegyed.presentation.components.common.SkeletonCard
import com.martonegyed.presentation.components.common.cards.SectionCard
import com.martonegyed.presentation.screens.insights.RatingBucket
import com.martonegyed.presentation.screens.movies.CollectionType
import com.martonegyed.presentation.screens.movies.MovieCollectionScreen

import kotlin.math.roundToInt

@Composable
fun RatingsInsightSection(
    distribution: List<RatingBucket>,
    compact: Boolean,
    medium: Boolean,
    selectedRange: StatRange,
    selectedYear: Int?,
    selectedMonth: Int?,
    navigator: Navigator
) {
    val colors = MaterialTheme.colorScheme

    if (distribution.isEmpty()) {
        SectionCard(
            title = "Rating Distribution",
            subtitle = "No ratings yet.",
        )
        return
    }

    val maxCount = distribution.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    val chartHeight = when {
        medium -> 300.dp
        compact -> 220.dp
        else -> 340.dp
    }

    var tooltipIndex by remember { mutableStateOf<Int?>(null) }
    var overlayBounds by remember { mutableStateOf(Rect.Zero) }
    var overlaySize by remember { mutableStateOf(IntSize.Zero) }
    var tooltipSize by remember { mutableStateOf(IntSize.Zero) }
    val barBounds = remember(distribution.size) { MutableList(distribution.size) { Rect.Zero } }

    val density = LocalDensity.current
    val labelSpace = if (compact) 22.dp else 24.dp
    val minBarHeight = 6.dp

    val chartHeightPx = with(density) { chartHeight.toPx() }
    val minBarHeightPx = with(density) { minBarHeight.toPx() }
    val tooltipGapPx = with(density) { 8.dp.toPx() }

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Tap a bar to open movies, hold to see count",
            color = colors.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth()
        )

        SectionCard(title = "Rating Distribution") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight + labelSpace + 48.dp)
                    .onGloballyPositioned { coords ->
                        overlayBounds = coords.boundsInWindow()
                        overlaySize = coords.size
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart),
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    distribution.forEachIndexed { index, bucket ->
                        val fraction =
                            if (maxCount == 0) 0f else bucket.count.toFloat() / maxCount.toFloat()

                        val fillHeight = (chartHeight * fraction).coerceAtLeast(minBarHeight)

                        val barColor = when {
                            bucket.label.toDoubleOrNull()?.let { it >= 4.0 } == true -> colors.primary
                            bucket.label.toDoubleOrNull()?.let { it <= 2.5 } == true -> colors.tertiary
                            else -> colors.secondary
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(chartHeight)
                                    .onGloballyPositioned { coords ->
                                        barBounds[index] = coords.boundsInWindow()
                                    }
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.surfaceVariant.copy(alpha = 0.35f))
                                    .combinedClickable(
                                        onClick = {
                                            tooltipIndex = null
                                            if (bucket.count > 0) {
                                                navigator.push(
                                                    MovieCollectionScreen(
                                                        type = CollectionType.BY_RATING,
                                                        rating = bucket.label.toDoubleOrNull(),
                                                        range = selectedRange,
                                                        year = selectedYear,
                                                        month = selectedMonth
                                                    )
                                                )
                                            }
                                        },
                                        onLongClick = {
                                            tooltipIndex = if (tooltipIndex == index) null else index
                                        }
                                    ),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(fillHeight)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(barColor)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = bucket.label.removeSuffix(".0"),
                                color = colors.onSurfaceVariant,
                                fontSize = if (compact) 11.sp else 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                tooltipIndex?.let { index ->
                    val bucket = distribution.getOrNull(index) ?: return@let
                    val rect = barBounds[index]

                    if (rect != Rect.Zero && overlayBounds != Rect.Zero && overlaySize != IntSize.Zero) {
                        val fraction =
                            if (maxCount == 0) 0f else bucket.count.toFloat() / maxCount.toFloat()

                        val fillHeightPx = maxOf(chartHeightPx * fraction, minBarHeightPx)

                        val barCenterXInOverlay =
                            ((rect.left + rect.right) / 2f) - overlayBounds.left

                        val filledBarTopInOverlay =
                            (rect.bottom - overlayBounds.top) - fillHeightPx

                        val tooltipX = (
                                barCenterXInOverlay - tooltipSize.width / 2f
                                ).roundToInt().coerceIn(
                                0,
                                (overlaySize.width - tooltipSize.width).coerceAtLeast(0)
                            )

                        val tooltipY = (
                                filledBarTopInOverlay - tooltipSize.height - tooltipGapPx
                                ).roundToInt().coerceAtLeast(0)

                        Surface(
                            color = Color.Black.copy(alpha = 0.92f),
                            shape = RoundedCornerShape(12.dp),
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .absoluteOffset { IntOffset(tooltipX, tooltipY) }
                                .onGloballyPositioned { tooltipSize = it.size }
                        ) {
                            Text(
                                text = "${bucket.count} movie" + if (bucket.count == 1) "" else "s",
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RatingsSectionSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SkeletonCard(height = 220.dp)
        SkeletonCard(height = 120.dp)
    }
}