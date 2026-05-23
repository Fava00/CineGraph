package com.martonegyed.presentation.components.statistics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.martonegyed.presentation.screens.statistics.StatEntityType
import com.martonegyed.presentation.screens.statistics.StatisticsState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EntityTabs(
    state: StatisticsState,
    onSelect: (StatEntityType) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth()
    ) {
        val useScrollableTabs = maxWidth < 500.dp

        if (useScrollableTabs) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF181C20))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EntityTab("Directors", StatEntityType.DIRECTORS, state.entityType, onSelect)
                EntityTab("Actors", StatEntityType.ACTORS, state.entityType, onSelect)
                EntityTab("Genres", StatEntityType.GENRES, state.entityType, onSelect)
                EntityTab("Studios", StatEntityType.STUDIOS, state.entityType, onSelect)
                EntityTab("Countries", StatEntityType.COUNTRIES, state.entityType, onSelect)
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF181C20))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EntityTab("Directors", StatEntityType.DIRECTORS, state.entityType, onSelect, Modifier.weight(1f))
                EntityTab("Actors", StatEntityType.ACTORS, state.entityType, onSelect, Modifier.weight(1f))
                EntityTab("Genres", StatEntityType.GENRES, state.entityType, onSelect, Modifier.weight(1f))
                EntityTab("Studios", StatEntityType.STUDIOS, state.entityType, onSelect, Modifier.weight(1f))
                EntityTab("Countries", StatEntityType.COUNTRIES, state.entityType, onSelect, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun EntityTab(
    label: String,
    type: StatEntityType,
    current: StatEntityType,
    onSelect: (StatEntityType) -> Unit,
    modifier: Modifier = Modifier
) {
    val selected = current == type

    Surface(
        modifier = modifier
            .height(40.dp)
            .widthIn(min = 92.dp),
        color = if (selected) Color(0xFF184D2A) else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (selected) Color(0xFF00E054) else Color(0xFF5A5F66)
        ),
        onClick = { onSelect(type) }
    ) {
        Box(
            modifier = Modifier
                .wrapContentWidth()
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (selected) Color.White else Color(0xFFC0C0C0),
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1
            )
        }
    }
}