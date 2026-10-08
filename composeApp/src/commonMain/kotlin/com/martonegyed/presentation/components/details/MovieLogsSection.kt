package com.martonegyed.presentation.components.details

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.martonegyed.core.ui.adaptive.MovieDetailTokens
import com.martonegyed.domain.model.MovieLog

@Composable
fun MovieLogsSection(
    logs: List<MovieLog>,
    detailTokens: MovieDetailTokens,
    isBusy: Boolean,
    onEdit: (MovieLog) -> Unit,
    onDelete: (MovieLog) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var menuLogId by remember { mutableStateOf<Long?>(null) }
    Column {
        HorizontalDivider(color = colors.onBackground.copy(alpha = 0.1f), modifier = Modifier.padding(16.dp))
        SectionTitle("Your Logs", paddingHorizontal = 16.dp)
        Spacer(Modifier.height(8.dp))
        if (logs.isEmpty()) {
            Text("No logs yet.", color = colors.onSurfaceVariant, fontSize = detailTokens.metaFontSize,
                modifier = Modifier.padding(horizontal = 16.dp))
        }
        logs.forEach { log ->
            ListItem(
                headlineContent = {
                    Text(log.watchedDate ?: if (log.isRatingOnly) "Rating" else "Viewing date unknown",
                        fontWeight = FontWeight.Bold, color = colors.onBackground, fontSize = detailTokens.bodyFontSize)
                },
                supportingContent = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        log.rating?.let { rating ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = colors.inversePrimary,
                                    modifier = Modifier.size(12.dp))
                                Text(buildString {
                                    append(" $rating")
                                    if (log.watchedDate == null && log.ratedDate != null) append(" - Rated ${log.ratedDate}")
                                }, color = colors.onBackground, fontSize = detailTokens.metaFontSize)
                            }
                        }
                    }
                },
                trailingContent = {
                    Box {
                        IconButton(onClick = { menuLogId = log.id }, enabled = !isBusy) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Log actions for ${log.watchedDate ?: "undated entry"}")
                        }
                        DropdownMenu(expanded = menuLogId == log.id, onDismissRequest = { menuLogId = null }) {
                            DropdownMenuItem(text = { Text("Edit log") }, enabled = !isBusy,
                                onClick = { menuLogId = null; onEdit(log) })
                            DropdownMenuItem(text = { Text("Delete log", color = colors.error) }, enabled = !isBusy,
                                onClick = { menuLogId = null; onDelete(log) })
                        }
                    }
                },
                leadingContent = {
                    Icon(when {
                        log.isRewatch -> Icons.Default.Replay
                        log.watchedDate == null -> Icons.Default.DateRange
                        else -> Icons.Default.Visibility
                    }, contentDescription = null, tint = colors.onSurfaceVariant)
                }
            )
        }
    }
}
