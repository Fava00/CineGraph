package com.martonegyed.presentation.components.yearinreview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.martonegyed.core.util.roundToDecimals
import com.martonegyed.presentation.screens.yearinreview.RankRow

private enum class RankingMetricUi {
    COUNT, RATING
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingSection(
    countRows: List<RankRow>,
    ratingRows: List<RankRow>
) {
    var metric by rememberSaveable { mutableStateOf(RankingMetricUi.COUNT) }

    val rows = when (metric) {
        RankingMetricUi.COUNT -> countRows
        RankingMetricUi.RATING -> ratingRows
    }.take(10)

    val maxCount = countRows.maxOfOrNull { it.count } ?: 0

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(1.dp))

            SingleChoiceSegmentedButtonRow {
                SegmentedButton(
                    selected = metric == RankingMetricUi.COUNT,
                    onClick = { metric = RankingMetricUi.COUNT },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text("Count") }

                SegmentedButton(
                    selected = metric == RankingMetricUi.RATING,
                    onClick = { metric = RankingMetricUi.RATING },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("Rating") }
            }
        }

        rows.forEach { row ->
            RankingBarRow(
                row = row,
                metric = metric,
                maxCount = maxCount
            )
        }
    }
}

@Composable
private fun RankingBarRow(
    row: RankRow,
    metric: RankingMetricUi,
    maxCount: Int
) {
    val colors = MaterialTheme.colorScheme

    val progress = when (metric) {
        RankingMetricUi.COUNT -> {
            if (maxCount <= 0) 0f else row.count.toFloat() / maxCount.toFloat()
        }

        RankingMetricUi.RATING -> {
            ((row.averageRating ?: 0.0) / 5.0).toFloat().coerceIn(0f, 1f)
        }
    }

    val valueText = when (metric) {
        RankingMetricUi.COUNT -> row.count.toString()
        RankingMetricUi.RATING -> row.averageRating?.roundToDecimals(2)?.toString() ?: "-"
    }

    val supportingText = when (metric) {
        RankingMetricUi.COUNT -> row.averageRating?.let { "Avg ${it.roundToDecimals(2)}" } ?: ""
        RankingMetricUi.RATING -> "${row.count} movies"
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = row.label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )

            Text(
                text = valueText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = colors.primary,
            trackColor = colors.surfaceVariant
        )

        if (supportingText.isNotBlank()) {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}