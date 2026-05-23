package com.martonegyed.presentation.previews

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.unit.dp
import com.martonegyed.presentation.components.common.cards.HeroStatCard
import com.martonegyed.presentation.components.statistics.StatisticsHeroColumn
import com.martonegyed.presentation.components.statistics.StatisticsHeroRow
import com.martonegyed.presentation.screens.statistics.StatisticsState

private val previewState = StatisticsState(
    totalMovies = 214,
    totalHours = 437.5,
    averageRating = 7.34,
    totalRevenue = 12_450_000_000L
)

@Preview(name = "Hero Column – small phone", device = Devices.PHONE, widthDp = 360, heightDp = 480)
@Composable
private fun HeroColumnSmallPreview() {
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                StatisticsHeroColumn(state = previewState)
            }
        }
    }
}

@Preview(name = "Hero Column – phone", device = Devices.PHONE)
@Composable
private fun HeroColumnPhonePreview() {
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                StatisticsHeroColumn(state = previewState)
            }
        }
    }
}

@Preview(name = "HeroStatCard – twoPane", widthDp = 320, heightDp = 72)
@Composable
private fun HeroStatCardTwoPanePreview() {
    MaterialTheme {
        HeroStatCard(
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Default.Star,
            value = "7.34",
            label = "Avg Rating",
            twoPane = true
        )
    }
}

@Preview(name = "HeroStatCard – very short", widthDp = 160, heightDp = 52)
@Composable
private fun HeroStatCardShortPreview() {
    MaterialTheme {
        HeroStatCard(
            modifier = Modifier.fillMaxSize(),
            icon = Icons.Default.Movie,
            value = "214",
            label = "Watched"
        )
    }
}

@Preview(name = "Hero Row – small phone", device = Devices.PHONE, widthDp = 360, heightDp = 480)
@Composable
private fun HeroRowSmallPreview() {
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                StatisticsHeroRow(state = previewState)
            }
        }
    }
}