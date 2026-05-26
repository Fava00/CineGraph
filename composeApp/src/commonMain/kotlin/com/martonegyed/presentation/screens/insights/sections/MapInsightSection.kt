package com.martonegyed.presentation.screens.insights.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.Navigator
import com.martonegyed.presentation.analytics.AnalyticsSharedModels
import com.martonegyed.presentation.analytics.StatRange
import com.martonegyed.presentation.components.common.CollectionEntityType
import com.martonegyed.presentation.components.common.SkeletonCard
import com.martonegyed.presentation.components.common.cards.SectionCard
import com.martonegyed.presentation.components.insights.WorldCountriesMap
import com.martonegyed.presentation.components.insights.WorldMapCountryVisual
import com.martonegyed.presentation.components.insights.countryAlpha2Code
import com.martonegyed.presentation.components.insights.countryFlagEmoji
import com.martonegyed.presentation.screens.movies.CollectionType
import com.martonegyed.presentation.screens.movies.MovieCollectionScreen

@Composable
fun MapInsightSection(
    rows: List<AnalyticsSharedModels.MapCountryRow>,
    navigator: Navigator,
    range: StatRange,
    year: Int?,
    month: Int?
) {
    if (rows.isEmpty()) {
        SectionCard(
            title = "No country data yet",
            subtitle = "Movies with production countries will light up your map."
        )
        return
    }

    val colors = MaterialTheme.colorScheme
    val topCountry = rows.firstOrNull()
    val maxCount = rows.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1

    val countryVisuals = remember(rows, colors) {
        buildCountryVisuals(
            rows = rows,
            highlightColor = colors.primary,
            baseColor = colors.surfaceVariant
        )
    }

    var selectedCountry by remember(rows) { mutableStateOf<WorldMapCountryVisual?>(null) }

    SectionCard(
        title = "Map",
        subtitle = topCountry?.let { "${it.name} leads with ${it.count} films." }
            ?: "Your watched countries."
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MapStatCard(
                    label = "Countries",
                    value = rows.size.toString(),
                    modifier = Modifier.weight(1f)
                )
                MapStatCard(
                    label = "Max Count",
                    value = maxCount.toString(),
                    modifier = Modifier.weight(1f)
                )
            }

            WorldCountriesMap(
                countries = countryVisuals,
                modifier = Modifier.fillMaxWidth(),
                aspectRatio = 2.1f,
                onCountrySelected = { selectedCountry = it }
            )

            selectedCountry?.takeIf { it.count > 0 }?.let { country ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surfaceVariant.copy(alpha = 0.4f),
                    onClick = {
                        navigator.push(
                            MovieCollectionScreen(
                                type = CollectionType.BY_ENTITY,
                                entityType = CollectionEntityType.COUNTRIES,
                                entityName = country.name,
                                range = range,
                                year = year,
                                month = month
                            )
                        )
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "${countryFlagEmoji(country.name)} ${country.name}",
                                color = colors.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = if (country.count == 1) {
                                    "Tap to open 1 film"
                                } else {
                                    "Tap to open ${country.count} films"
                                },
                                color = colors.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }

                        Text(
                            text = country.count.toString(),
                            color = colors.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            MapLegend(
                startColor = lerpForLegend(colors.surfaceVariant, colors.primary, 0.30f),
                endColor = colors.primary,
                maxCount = maxCount
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                rows.take(10).forEachIndexed { index, row ->
                    val fraction = row.count.toFloat() / maxCount.toFloat()

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = colors.surfaceVariant.copy(alpha = 0.45f),
                        onClick = {
                            navigator.push(
                                MovieCollectionScreen(
                                    type = CollectionType.BY_ENTITY,
                                    entityType = CollectionEntityType.COUNTRIES,
                                    entityName = row.name,
                                    range = range,
                                    year = year,
                                    month = month
                                )
                            )
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "#${index + 1}",
                                    color = colors.onSurfaceVariant,
                                    fontSize = 13.sp,
                                    modifier = Modifier.width(34.dp)
                                )
                                Text(
                                    text = "${countryFlagEmoji(row.name)} ${row.name}",
                                    modifier = Modifier.weight(1f),
                                    color = colors.onSurface,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = row.count.toString(),
                                    color = colors.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(colors.surfaceVariant)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction)
                                        .background(colors.primary)
                                        .padding(vertical = 3.dp)
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
private fun MapStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                color = colors.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label.uppercase(),
                color = colors.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun MapLegend(
    startColor: Color,
    endColor: Color,
    maxCount: Int
) {
    val colors = MaterialTheme.colorScheme

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(999.dp))
                .background(Brush.horizontalGradient(listOf(startColor, endColor)))
                .padding(vertical = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "1 film",
                color = colors.onSurfaceVariant,
                fontSize = 11.sp
            )
            Text(
                text = if (maxCount == 1) "1 film" else "$maxCount films",
                color = colors.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

private fun buildCountryVisuals(
    rows: List<AnalyticsSharedModels.MapCountryRow>,
    highlightColor: Color,
    baseColor: Color
): List<WorldMapCountryVisual> {
    val mergedRows = rows
        .mapNotNull { row ->
            countryAlpha2Code(row.name)?.uppercase()?.let { alpha2 ->
                alpha2 to row
            }
        }
        .groupBy(
            keySelector = { it.first },
            valueTransform = { it.second }
        )
        .map { (alpha2Code, groupedRows) ->
            val representativeName = preferredCountryDisplayName(
                alpha2Code = alpha2Code,
                fallback = groupedRows.maxByOrNull { it.count }?.name ?: alpha2Code
            )

            Triple(
                alpha2Code,
                representativeName,
                groupedRows.sumOf { it.count }
            )
        }
        .sortedByDescending { it.third }

    val totalItems = mergedRows.size.coerceAtLeast(1)
    val distinctTopCount = 15
    val topZoneStart = 1.0f
    val topZoneEnd = 0.5f
    val tailZoneStart = 0.5f
    val tailZoneEnd = 0.2f

    return mergedRows.mapIndexed { index, (alpha2Code, name, count) ->
        val intensity = when {
            totalItems <= distinctTopCount -> {
                val progress = index.toFloat() / totalItems.toFloat()
                topZoneStart - (progress * (topZoneStart - tailZoneEnd))
            }

            index < distinctTopCount -> {
                val progress = index.toFloat() / distinctTopCount.toFloat()
                topZoneStart - (progress * (topZoneStart - topZoneEnd))
            }

            else -> {
                val tailDenominator = (totalItems - distinctTopCount).coerceAtLeast(1)
                val progress = (index - distinctTopCount).toFloat() / tailDenominator.toFloat()
                tailZoneStart - (progress * (tailZoneStart - tailZoneEnd))
            }
        }.coerceIn(0.18f, 1.0f)

        WorldMapCountryVisual(
            alpha2Code = alpha2Code,
            name = name,
            count = count,
            fillColor = lerpForLegend(baseColor, highlightColor, intensity)
        )
    }
}

private fun preferredCountryDisplayName(
    alpha2Code: String,
    fallback: String
): String {
    return when (alpha2Code.uppercase()) {
        "US" -> "United States"
        "GB" -> "United Kingdom"
        else -> fallback
    }
}

private fun lerpForLegend(start: Color, end: Color, fraction: Float): Color {
    val t = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * t,
        green = start.green + (end.green - start.green) * t,
        blue = start.blue + (end.blue - start.blue) * t,
        alpha = start.alpha + (end.alpha - start.alpha) * t
    )
}

@Composable
fun MapSectionSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SkeletonCard(height = 260.dp)
        repeat(4) {
            SkeletonCard(height = 68.dp)
        }
    }
}