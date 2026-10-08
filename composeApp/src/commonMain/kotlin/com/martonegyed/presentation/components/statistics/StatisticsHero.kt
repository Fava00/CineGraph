package com.martonegyed.presentation.components.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.martonegyed.core.util.revenueFormater
import com.martonegyed.core.util.roundToDecimals
import com.martonegyed.presentation.components.common.cards.HeroStatCard
import com.martonegyed.presentation.screens.statistics.StatisticsState

@Composable
fun StatisticsHeroColumn(
    state: StatisticsState
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HeroStatCard(
            Modifier.fillMaxWidth().weight(1f),
            Icons.Default.Movie,
            value = state.totalMovies.toString(),
            label = "Films",

            )
        HeroStatCard(
            Modifier.fillMaxWidth().weight(1f),
            Icons.Default.AccessTime,
            value = "${state.totalHours.roundToDecimals(2)}h",
            label = "Hours",

            )
        HeroStatCard(
            Modifier.fillMaxWidth().weight(1f),
            Icons.Default.Star,
            value = if (state.averageRating > 0) state.averageRating.roundToDecimals(2).toString()
            else "-",
            label = "Avg Rating",

            )
        HeroStatCard(
            Modifier.fillMaxWidth().weight(1f),
            Icons.Default.AttachMoney,
            value = revenueFormater(state.totalRevenue),
            label = "Revenue",

            )
        ViewingSummary(state)
    }
}

@Composable
fun StatisticsHeroRow(state: StatisticsState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HeroStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Movie,
                value = state.totalMovies.toString(),
                label = "Films"
            )
            HeroStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.AccessTime,
                value = "${state.totalHours.roundToDecimals(2)}h",
                label = "Hours"
            )
            HeroStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Star,
                value = if (state.averageRating > 0) state.averageRating.roundToDecimals(2).toString() else "-",
                label = "Avg Rating"
            )
            HeroStatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.AttachMoney,
                value = revenueFormater(state.totalRevenue),
                label = "Revenue"
            )
        }
        ViewingSummary(state)
    }
}

@Composable
private fun ViewingSummary(state: StatisticsState) {
    Text(
        buildString {
            append("${state.totalViewings} viewings")
            if (state.undatedViewings > 0) append(" | ${state.undatedViewings} with unknown dates")
            if (state.unknownRuntimeViewings > 0) append("\nWatch time excludes ${state.unknownRuntimeViewings} viewings with unknown runtime.")
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
