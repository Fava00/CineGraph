package com.martonegyed.presentation.components.details

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.martonegyed.core.ui.adaptive.MovieDetailTokens
import com.martonegyed.domain.model.MovieLog

@Composable
fun MovieReviewsSection(
    logs: List<MovieLog>,
    detailTokens: MovieDetailTokens,
    isBusy: Boolean,
    onEdit: (MovieLog) -> Unit
) {
    val reviews = logs.withIndex().filter { !it.value.review.isNullOrBlank() }
    if (reviews.isEmpty()) return
    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Your reviews", paddingHorizontal = 0.dp)
        reviews.forEach { (index, log) ->
            key(log.stableId.ifBlank { log.id.toString() }) {
                var expanded by remember { mutableStateOf(false) }
                var overflows by remember(log.review) { mutableStateOf(false) }
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(when {
                            log.watchedDate != null -> "${if (log.isRewatch) "Rewatch" else "Viewing"} - ${log.watchedDate}"
                            log.isRatingOnly -> "Rating - ${log.ratedDate ?: "date unknown"}"
                            else -> "Viewing date unknown - Log ${index + 1}"
                        }, style = MaterialTheme.typography.titleSmall)
                        log.rating?.let {
                            Text("$it / 5 stars", color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = detailTokens.metaFontSize)
                        }
                        Text(log.review.orEmpty(), fontSize = detailTokens.bodyFontSize,
                            maxLines = if (expanded) Int.MAX_VALUE else 5,
                            overflow = TextOverflow.Ellipsis,
                            onTextLayout = { if (!expanded) overflows = it.hasVisualOverflow })
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (expanded || overflows) {
                                TextButton(onClick = { expanded = !expanded }) {
                                    Text(if (expanded) "Show less" else "Read more")
                                }
                            }
                            TextButton(onClick = { onEdit(log) }, enabled = !isBusy) { Text("Edit log") }
                        }
                    }
                }
            }
        }
    }
}
