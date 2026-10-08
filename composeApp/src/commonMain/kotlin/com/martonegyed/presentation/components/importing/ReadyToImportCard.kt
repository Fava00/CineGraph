package com.martonegyed.presentation.components.importing

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.martonegyed.presentation.screens.import.StagedSourceSummary

@Composable
fun ReadyToImportCard(sources: List<StagedSourceSummary>) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.surfaceVariant,
        contentColor = colors.onSurfaceVariant,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                "Ready to import",
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface
            )
            sources.forEachIndexed { index, source ->
                if (index > 0) HorizontalDivider(color = colors.outlineVariant)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            source.name,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface
                        )
                        Surface(
                            color = colors.surface,
                            contentColor = colors.onSurface,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "${source.count} rows",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    source.fileNames.forEach { fileName ->
                        Text(fileName, style = MaterialTheme.typography.bodySmall)
                    }
                    if (source.skippedTvCount > 0) {
                        Text("${source.skippedTvCount} TV series / episode rows skipped",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
