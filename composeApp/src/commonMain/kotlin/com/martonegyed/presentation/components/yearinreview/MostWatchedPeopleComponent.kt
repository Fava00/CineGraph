package com.martonegyed.presentation.components.yearinreview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.Navigator
import coil3.compose.AsyncImage
import com.martonegyed.presentation.analytics.AnalyticsSharedModels
import com.martonegyed.presentation.analytics.StatRange
import com.martonegyed.presentation.components.common.openPersonCollection
import com.martonegyed.presentation.screens.statistics.StatEntityType


@Composable
fun MostWatchedPeopleRow(
    navigator: Navigator,
    actor: AnalyticsSharedModels.AnalyticsEntityRow?,
    director: AnalyticsSharedModels.AnalyticsEntityRow?,
    selectedYear: Int?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MostWatchedPersonCard(
            modifier = Modifier.weight(1f),
            navigator = navigator,
            title = "Most watched actor",
            row = actor,
            entityType = StatEntityType.ACTORS,
            selectedYear = selectedYear,
        )

        MostWatchedPersonCard(
            modifier = Modifier.weight(1f),
            navigator = navigator,
            title = "Most watched director",
            row = director,
            entityType = StatEntityType.DIRECTORS,
            selectedYear = selectedYear,
        )
    }
}

@Composable
private fun MostWatchedPersonCard(
    modifier: Modifier = Modifier,
    navigator: Navigator,
    title: String,
    row: AnalyticsSharedModels.AnalyticsEntityRow?,
    entityType: StatEntityType,
    selectedYear: Int?,
) {
    val colors = MaterialTheme.colorScheme
    val imageUrl = row?.photoPath
        ?.takeIf { it.isNotBlank() }
        ?.let { "https://image.tmdb.org/t/p/w200$it" }

    val initials = row?.name
        .orEmpty()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "?" }

    ElevatedCard(
        modifier = modifier.fillMaxWidth()
            .clickable(onClick = {
                row?.name?.let {
                    openPersonCollection(
                        navigator = navigator,
                        personName = it,
                        entityType = entityType,
                        range = StatRange.YEAR,
                        selectedYear = selectedYear,
                    )
                }
            })
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Surface(
                modifier = Modifier.size(76.dp),
                shape = CircleShape,
                color = colors.surfaceVariant
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = row.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Text(
                text = row?.name ?: "-",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text("${row?.count ?: 0} movies")

        }
    }
}
