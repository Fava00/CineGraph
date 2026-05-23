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
            label = "Watched",

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
    }
}

@Composable
fun StatisticsHeroRow(state: StatisticsState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HeroStatCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Movie,
            value = state.totalMovies.toString(),
            label = "Watched"
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
}