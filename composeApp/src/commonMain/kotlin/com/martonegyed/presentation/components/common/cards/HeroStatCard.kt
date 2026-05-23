package com.martonegyed.presentation.components.common.cards

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HeroStatCard(
    modifier: Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    twoPane: Boolean = false,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val compact = maxWidth < 90.dp
            val veryCompact = maxWidth < 78.dp

            val horizontalPadding = when {
                veryCompact -> 4.dp
                compact -> 6.dp
                else -> 10.dp
            }

            val verticalPadding = when {
                veryCompact -> 6.dp
                compact -> 7.dp
                else -> 10.dp
            }

            val iconSize = when {
                veryCompact -> 16
                compact -> 18
                twoPane -> 20
                else -> 18
            }

            val valueFontSize = when {
                veryCompact -> 11
                compact -> 12
                twoPane -> 18
                else -> 15
            }

            val labelFontSize = when {
                veryCompact -> 10
                compact -> 12
                twoPane -> 12
                else -> 10
            }

            val labelText = when {
                veryCompact && label == "Avg Rating" -> "Rating"
                veryCompact && label == "Revenue" -> "Rev"
                else -> label
            }

            if (twoPane) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontalPadding, vertical = verticalPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InsideContent(icon, value, labelText, valueFontSize, labelFontSize, iconSize)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = horizontalPadding, vertical = verticalPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    InsideContent(icon, value, labelText, valueFontSize, labelFontSize, iconSize)
                }
            }
        }
    }
}

@Composable
fun InsideContent(icon: ImageVector, value: String, label: String, valueSize: Int, labelSize: Int, iconSize: Int) {
    Icon(
        icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(iconSize.dp)
    )
    Spacer(Modifier.height(2.dp))

    Text(
        text = value,
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = valueSize.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )

    Spacer(Modifier.height(2.dp))

    Text(
        text = label,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = labelSize.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}