package com.martonegyed.presentation.components.importing

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.martonegyed.domain.model.SuggestedTmdbMatch

@Composable
fun SuggestedTmdbMatchesDialog(
    suggestions: List<SuggestedTmdbMatch>, loading: Boolean, applying: Boolean,
    progress: String?, onChoose: (Long, Int?) -> Unit, onAccept: (Long) -> Unit,
    onAcceptAll: () -> Unit, onClose: () -> Unit, canApply: Boolean = true
) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.widthIn(max = 840.dp).fillMaxWidth(.95f).fillMaxHeight(.9f),
            shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Suggested TMDB matches", style = MaterialTheme.typography.headlineSmall)
                Text("Check each suggestion or confirm all selected matches. Unchecked entries stay in manual review.",
                    style = MaterialTheme.typography.bodyMedium)
                Text("Ranked by title, closest year, feature-length runtime, then popularity. These are suggestions, not verified identities.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!canApply) Text("Finish enrichment before accepting matches.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                else if (loading) Text("Ready matches can be accepted while other suggestions load.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (loading || applying) LinearProgressIndicator(Modifier.fillMaxWidth())
                progress?.let { Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 4) }
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(suggestions, key = { it.movie.id }) { row ->
                        var expanded by remember(row.movie.id) { mutableStateOf(false) }
                        val candidate = row.candidates.firstOrNull { it.id == row.selectedId } ?: row.candidates.first()
                        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = row.selectedId != null, enabled = !applying,
                                        onCheckedChange = { onChoose(row.movie.id, if (it) candidate.id else null) })
                                    Text("${row.movie.name} (${row.movie.year})", modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.titleSmall)
                                }
                                TmdbCandidateCard(candidate, canApply && !applying && row.selectedId != null,
                                    actionLabel = "Accept this match") { onAccept(row.movie.id) }
                                TextButton(onClick = { expanded = !expanded }, enabled = !applying) {
                                    Text(if (expanded) "Hide alternatives" else "Other candidates (${row.candidates.size - 1})")
                                }
                                if (expanded) row.candidates.filter { it.id != candidate.id }.forEach { alternative ->
                                    TmdbCandidateCard(alternative, !applying, actionLabel = "Choose this match") {
                                        onChoose(row.movie.id, alternative.id)
                                        expanded = false
                                    }
                                }
                            }
                        }
                    }
                }
                val count = suggestions.count { it.selectedId != null }
                if (loading) Text("Confirmation applies only to the ready entries currently selected.", style = MaterialTheme.typography.bodySmall)
                Button(onClick = onAcceptAll, enabled = count > 0 && canApply && !applying, modifier = Modifier.fillMaxWidth()) {
                    Text("Confirm $count selected matches")
                }
                TextButton(onClick = onClose, enabled = !applying, modifier = Modifier.align(Alignment.End)) {
                    Text("Leave these entries for manual review")
                }
            }
        }
    }
}
