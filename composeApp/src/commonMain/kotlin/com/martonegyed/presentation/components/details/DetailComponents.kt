package com.martonegyed.presentation.components.details

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.martonegyed.core.ui.adaptive.MovieDetailTokens

@Composable
fun MetaTag(text: String, detailTokens: MovieDetailTokens) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .border(1.dp, colors.onSurfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = colors.onBackground,
            fontSize = detailTokens.metaFontSize,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SectionTitle(
    title: String,
    paddingHorizontal: Dp = 0.dp
) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            letterSpacing = 1.sp
        ),
        modifier = Modifier.padding(horizontal = paddingHorizontal)
    )
}