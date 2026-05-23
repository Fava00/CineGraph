package com.martonegyed.presentation.components.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt

data class DonutChartItem(
    val label: String,
    val value: Float,
    val color: Color
)

@Composable
fun DonutChart(
    items: List<DonutChartItem>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 26.dp,
    onItemClick: ((DonutChartItem) -> Unit)? = null
) {
    val total = items.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(1f)
    val animatedProgress = remember(items) { Animatable(0f) }
    val strokePx = with(LocalDensity.current) { strokeWidth.toPx() }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(items) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
        )
    }

    Canvas(
        modifier = modifier
            .size(220.dp)
            .onSizeChanged { canvasSize = it }
            .pointerInput(items, canvasSize, strokePx, onItemClick) {
                detectTapGestures { tapOffset ->
                    val clickHandler = onItemClick ?: return@detectTapGestures
                    if (canvasSize == IntSize.Zero) return@detectTapGestures

                    val center = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
                    val dx = tapOffset.x - center.x
                    val dy = tapOffset.y - center.y
                    val distance = sqrt(dx * dx + dy * dy)

                    val outerRadius = min(canvasSize.width, canvasSize.height) / 2f
                    val innerRadius = outerRadius - strokePx

                    if (distance !in innerRadius..outerRadius) return@detectTapGestures

                    var angle =
                        ((atan2(dy.toDouble(), dx.toDouble()) * 180.0 / PI).toFloat()) + 90f
                    if (angle < 0f) angle += 360f
                    if (angle >= 360f) angle -= 360f

                    var startAngle = 0f
                    items.forEachIndexed { index, item ->
                        val sweep = (item.value / total) * 360f
                        val endAngle = startAngle + sweep
                        val isLast = index == items.lastIndex

                        val hit = if (isLast) {
                            angle in startAngle..endAngle
                        } else {
                            angle in startAngle..<endAngle
                        }

                        if (hit) {
                            clickHandler(item)
                            return@detectTapGestures
                        }

                        startAngle = endAngle
                    }
                }
            }
    ) {
        val stroke = Stroke(
            width = strokePx,
            cap = StrokeCap.Round
        )

        val diameterOffset = stroke.width / 2
        val arcSize = Size(
            width = size.width - diameterOffset * 2,
            height = size.height - diameterOffset * 2
        )
        val topLeft = Offset(diameterOffset, diameterOffset)

        var startAngle = -90f

        items.forEach { item ->
            val fullSweep = (item.value / total) * 360f
            val animatedSweep = fullSweep * animatedProgress.value

            drawArc(
                color = item.color.copy(alpha = 0.18f),
                startAngle = startAngle,
                sweepAngle = fullSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )

            drawArc(
                color = item.color,
                startAngle = startAngle,
                sweepAngle = animatedSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )

            startAngle += fullSweep
        }
    }
}