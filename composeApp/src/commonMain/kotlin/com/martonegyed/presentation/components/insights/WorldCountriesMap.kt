package com.martonegyed.presentation.components.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

@Immutable
data class WorldMapCountryVisual(
    val alpha2Code: String,
    val name: String,
    val count: Int,
    val fillColor: Color
)

private data class WorldMapBounds(
    val minLon: Double,
    val maxLon: Double,
    val minLat: Double,
    val maxLat: Double
)

private data class ProjectedCountryShape(
    val selectionKey: String,
    val alpha2Code: String?,
    val name: String,
    val count: Int,
    val fillColor: Color,
    val polygons: List<List<Offset>>
)

@Composable
fun WorldCountriesMap(
    countries: List<WorldMapCountryVisual>,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 2.1f,
    onCountrySelected: (WorldMapCountryVisual?) -> Unit = {}
) {
    val geoCountries = remember { worldCountryShapes }
    val colors = MaterialTheme.colorScheme

    val bounds = remember(geoCountries) {
        val allPoints = geoCountries.flatMap { it.polygons }.flatten()
        WorldMapBounds(
            minLon = allPoints.minOfOrNull { it.first } ?: -180.0,
            maxLon = allPoints.maxOfOrNull { it.first } ?: 180.0,
            minLat = allPoints.minOfOrNull { it.second } ?: -90.0,
            maxLat = allPoints.maxOfOrNull { it.second } ?: 90.0
        )
    }

    val countriesByCode = remember(countries) {
        countries.associateBy { it.alpha2Code.uppercase() }
    }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var zoom by remember { mutableStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var selectedCountryKey by remember { mutableStateOf<String?>(null) }

    val projectedShapes = remember(geoCountries, countriesByCode, bounds, canvasSize, zoom, pan) {
        if (canvasSize.width == 0 || canvasSize.height == 0) {
            emptyList()
        } else {
            buildProjectedShapes(
                geoCountries = geoCountries,
                countriesByCode = countriesByCode,
                bounds = bounds,
                canvasSize = Size(canvasSize.width.toFloat(), canvasSize.height.toFloat()),
                zoom = zoom,
                pan = pan,
                defaultFill = colors.surface.copy(alpha = 0.55f)
            )
        }
    }

    val selectedCountry = remember(projectedShapes, selectedCountryKey) {
        projectedShapes.firstOrNull { it.selectionKey == selectedCountryKey }
            ?.takeIf { it.alpha2Code != null }
            ?.let {
                WorldMapCountryVisual(
                    alpha2Code = it.alpha2Code!!,
                    name = it.name,
                    count = it.count,
                    fillColor = it.fillColor
                )
            }
    }

    Box(
        modifier = modifier
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surfaceVariant.copy(alpha = 0.25f))
            .onSizeChanged { canvasSize = it }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)

                            if (event.type == PointerEventType.Scroll) {
                                val change = event.changes.firstOrNull() ?: continue
                                val scrollDelta = change.scrollDelta.y

                                if (scrollDelta != 0f) {
                                    val oldZoom = zoom
                                    val zoomFactor = if (scrollDelta > 0f) 0.9f else 1.1f
                                    val newZoom = (oldZoom * zoomFactor).coerceIn(1f, 8f)

                                    if (newZoom != oldZoom) {
                                        val pointer = change.position
                                        val canvasCenter = Offset(
                                            canvasSize.width / 2f,
                                            canvasSize.height / 2f
                                        )

                                        val contentBefore = (pointer - canvasCenter - pan) / oldZoom
                                        val newPan = pointer - canvasCenter - (contentBefore * newZoom)

                                        zoom = newZoom
                                        pan = clampPan(
                                            pan = newPan,
                                            zoom = newZoom,
                                            canvasSize = Size(
                                                canvasSize.width.toFloat(),
                                                canvasSize.height.toFloat()
                                            )
                                        )
                                    }

                                    event.changes.forEach { it.consume() }
                                }
                            }
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, panChange, zoomChange, _ ->
                        val newZoom = (zoom * zoomChange).coerceIn(1f, 8f)
                        zoom = newZoom
                        pan = clampPan(
                            pan = pan + panChange,
                            zoom = newZoom,
                            canvasSize = Size(size.width.toFloat(), size.height.toFloat())
                        )
                    }
                }
                .pointerInput(projectedShapes) {
                    detectTapGestures(
                        onDoubleTap = {
                            zoom = 1f
                            pan = Offset.Zero
                        },
                        onTap = { tapOffset ->
                            val hit = projectedShapes
                                .asReversed()
                                .firstOrNull { shape ->
                                    shape.polygons.any { polygon -> pointInPolygon(tapOffset, polygon) }
                                }

                            selectedCountryKey = hit?.selectionKey
                            onCountrySelected(
                                hit?.takeIf { it.alpha2Code != null }?.let {
                                    WorldMapCountryVisual(
                                        alpha2Code = it.alpha2Code!!,
                                        name = it.name,
                                        count = it.count,
                                        fillColor = it.fillColor
                                    )
                                }
                            )
                        }
                    )
                }
        ) {
            projectedShapes.forEach { country ->
                country.polygons.forEach { polygon ->
                    if (polygon.size < 3) return@forEach

                    val path = Path().apply {
                        moveTo(polygon.first().x, polygon.first().y)
                        polygon.drop(1).forEach { point ->
                            lineTo(point.x, point.y)
                        }
                        close()
                    }

                    drawPath(path = path, color = country.fillColor)
                    drawPath(
                        path = path,
                        color = if (country.selectionKey == selectedCountryKey) {
                            colors.primary
                        } else {
                            colors.outlineVariant.copy(alpha = 0.5f)
                        },
                        style = Stroke(
                            width = if (country.selectionKey == selectedCountryKey) {
                                1.6.dp.toPx()
                            } else {
                                0.8.dp.toPx()
                            }
                        )
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
        ) {
            MapZoomButton(
                label = "+",
                onClick = {
                    val newZoom = (zoom + 0.75f).coerceAtMost(8f)
                    zoom = newZoom
                    pan = clampPan(
                        pan = pan,
                        zoom = newZoom,
                        canvasSize = Size(canvasSize.width.toFloat(), canvasSize.height.toFloat())
                    )
                }
            )
            MapZoomButton(
                label = "−",
                onClick = {
                    val newZoom = (zoom - 0.75f).coerceAtLeast(1f)
                    zoom = newZoom
                    pan = clampPan(
                        pan = pan,
                        zoom = newZoom,
                        canvasSize = Size(canvasSize.width.toFloat(), canvasSize.height.toFloat())
                    )
                }
            )
            MapZoomButton(
                label = "↺",
                onClick = {
                    zoom = 1f
                    pan = Offset.Zero
                }
            )
        }

        selectedCountry?.let { country ->
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                shape = RoundedCornerShape(14.dp),
                color = colors.surface.copy(alpha = 0.96f),
                tonalElevation = 2.dp,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Text(
                        text = "${countryFlagEmoji(country.name)} ${country.name}",
                        color = colors.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = if (country.count == 1) "1 film" else "${country.count} films",
                        color = colors.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MapZoomButton(
    label: String,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier
            .padding(bottom = 8.dp)
            .size(34.dp),
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = colors.surface.copy(alpha = 0.92f),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = colors.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

private fun buildProjectedShapes(
    geoCountries: List<GeoCountryShape>,
    countriesByCode: Map<String, WorldMapCountryVisual>,
    bounds: WorldMapBounds,
    canvasSize: Size,
    zoom: Float,
    pan: Offset,
    defaultFill: Color
): List<ProjectedCountryShape> {
    val lonSpan = (bounds.maxLon - bounds.minLon).toFloat()
    val latSpan = (bounds.maxLat - bounds.minLat).toFloat()

    if (lonSpan <= 0f || latSpan <= 0f || canvasSize.width <= 0f || canvasSize.height <= 0f) {
        return emptyList()
    }

    val scale = minOf(
        canvasSize.width / lonSpan,
        canvasSize.height / latSpan
    )

    val mapWidth = lonSpan * scale
    val mapHeight = latSpan * scale
    val xOffset = (canvasSize.width - mapWidth) / 2f
    val yOffset = (canvasSize.height - mapHeight) / 2f
    val centerX = canvasSize.width / 2f
    val centerY = canvasSize.height / 2f

    fun project(lon: Double, lat: Double): Offset {
        val baseX = xOffset + ((lon - bounds.minLon).toFloat() * scale)
        val baseY = yOffset + ((bounds.maxLat - lat).toFloat() * scale)

        val transformedX = ((baseX - centerX) * zoom) + centerX + pan.x
        val transformedY = ((baseY - centerY) * zoom) + centerY + pan.y
        return Offset(transformedX, transformedY)
    }

    return geoCountries.map { country ->
        val alpha2Code = countryAlpha2Code(country.name)?.uppercase()
        val visual = alpha2Code?.let { countriesByCode[it] }
        val selectionKey = alpha2Code ?: normalizeCountryName(country.name)

        ProjectedCountryShape(
            selectionKey = selectionKey,
            alpha2Code = alpha2Code,
            name = visual?.name ?: country.name,
            count = visual?.count ?: 0,
            fillColor = visual?.fillColor ?: defaultFill,
            polygons = country.polygons.map { polygon ->
                polygon.map { (lon, lat) -> project(lon, lat) }
            }
        )
    }
}

private fun clampPan(
    pan: Offset,
    zoom: Float,
    canvasSize: Size
): Offset {
    if (canvasSize.width <= 0f || canvasSize.height <= 0f) return Offset.Zero
    if (zoom <= 1f) return Offset.Zero

    val maxPanX = ((canvasSize.width * zoom) - canvasSize.width) / 2f + 24f
    val maxPanY = ((canvasSize.height * zoom) - canvasSize.height) / 2f + 24f

    return Offset(
        x = pan.x.coerceIn(-maxPanX, maxPanX),
        y = pan.y.coerceIn(-maxPanY, maxPanY)
    )
}

private fun pointInPolygon(point: Offset, polygon: List<Offset>): Boolean {
    if (polygon.size < 3) return false

    var inside = false
    var j = polygon.lastIndex

    for (i in polygon.indices) {
        val xi = polygon[i].x
        val yi = polygon[i].y
        val xj = polygon[j].x
        val yj = polygon[j].y

        val intersects = ((yi > point.y) != (yj > point.y)) &&
                (point.x < (xj - xi) * (point.y - yi) / ((yj - yi).takeIf { abs(it) > 0.000001f } ?: 0.000001f) + xi)

        if (intersects) inside = !inside
        j = i
    }

    return inside
}