package com.martonegyed.presentation.screens.insights.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.Navigator
import com.martonegyed.presentation.analytics.StatRange
import com.martonegyed.presentation.components.common.SkeletonCard
import com.martonegyed.presentation.components.common.cards.SectionCard
import com.martonegyed.presentation.components.insights.DuoRankCard
import com.martonegyed.presentation.screens.insights.DuoRow
import com.martonegyed.presentation.screens.insights.DuoType

import com.martonegyed.presentation.screens.movies.CollectionType
import com.martonegyed.presentation.screens.movies.MovieCollectionScreen

@Composable
fun DuosInsightSection(
    rows: List<DuoRow>,
    selectedDuoType: DuoType,
    selectedRange: StatRange,
    selectedYear: Int?,
    selectedMonth: Int?,
    navigator: Navigator,
    onDuoTypeSelect: (DuoType) -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = colors.surfaceVariant
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DuoType.entries.forEach { type ->
                    val isSelected = type == selectedDuoType
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) colors.surface else Color.Transparent,
                        shadowElevation = if (isSelected) 2.dp else 0.dp,
                        onClick = { onDuoTypeSelect(type) }
                    ) {
                        Text(
                            text = type.label,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            textAlign = TextAlign.Center,
                            color = if (isSelected) colors.onSurface else colors.onSurfaceVariant,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }


        if (rows.isEmpty()) {
            SectionCard(
                title = "No duos yet",
                subtitle = "Watch more movies to see repeated collaborations here."
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                rows.forEachIndexed { index, row ->
                    val firstJob = when (selectedDuoType) {
                        DuoType.ACTOR_ACTOR -> "Actor"
                        DuoType.ACTOR_DIRECTOR -> "Actor"
                    }
                    val secondJob = when (selectedDuoType) {
                        DuoType.ACTOR_ACTOR -> "Actor"
                        DuoType.ACTOR_DIRECTOR -> "Director"
                    }
                    DuoRankCard(
                        rank = index + 1,
                        row = row,
                        onClick = {
                            navigator.push(
                                MovieCollectionScreen(
                                    type = CollectionType.BY_DUO,
                                    firstName = row.leftName,
                                    secondName = row.rightName,
                                    firstJob = firstJob,
                                    secondJob = secondJob,
                                    range = selectedRange,
                                    year = selectedYear,
                                    month = selectedMonth
                                )
                            )
                        }

                    )
                }
            }
        }
    }
}

@Composable
fun DuosSectionSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SkeletonCard(height = 56.dp)
        repeat(5) {
            SkeletonCard(height = 72.dp)
        }
    }
}