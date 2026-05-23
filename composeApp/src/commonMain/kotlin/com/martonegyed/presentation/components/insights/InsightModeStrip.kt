package com.martonegyed.presentation.components.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.martonegyed.presentation.screens.insights.InsightMode

@Composable
fun InsightModeStrip(
    selected: InsightMode,
    onSelect: (InsightMode) -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            InsightMode.entries.forEach { mode ->
                val isSelected = mode == selected
                val tint = if (isSelected) colors.primary else colors.onSurfaceVariant

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(mode) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = mode.icon,
                        contentDescription = mode.label,
                        tint = tint,
                        modifier = Modifier.size(26.dp)
                    )

                    Text(
                        text = mode.label,
                        color = tint,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Box(
                        modifier = Modifier
                            .height(3.dp)
                            .width(44.dp)
                            .background(
                                color = if (isSelected) colors.primary else Color.Transparent,
                                shape = RoundedCornerShape(999.dp)
                            )
                    )
                }
            }
        }

        HorizontalDivider(color = colors.outlineVariant)
    }
}