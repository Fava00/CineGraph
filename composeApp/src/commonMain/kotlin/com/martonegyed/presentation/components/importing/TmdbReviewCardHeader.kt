package com.martonegyed.presentation.components.importing

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TmdbReviewCardHeader(
    count: Int,
    readyCount: Int,
    preparing: Boolean,
    syncing: Boolean,
    progress: String?,
    canReview: Boolean,
    canManage: Boolean,
    reviewOpen: Boolean,
    onSuggestions: () -> Unit,
    onReview: () -> Unit,
    onRefresh: () -> Unit,
    onRemoveAll: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val badgeTextColor = if (colors.primaryContainer.luminance() > 0.179f) Color.Black else Color.White
    var menuOpen by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("TMDb matches to review", modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                color = colors.onSurface)
            Spacer(Modifier.width(8.dp))
            Surface(color = colors.primaryContainer, shape = RoundedCornerShape(10.dp)) {
                Text("$count", modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    color = badgeTextColor, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Match review options", tint = colors.onSurfaceVariant)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("Refresh suggestions") },
                        leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                        enabled = canManage && !preparing,
                        onClick = { menuOpen = false; onRefresh() })
                }
            }
        }
        Text("Choose a match or link a TMDB movie.", style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant)
        if (preparing && !syncing) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(progress ?: "Preparing suggestions…", style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant)
            }
        }
        if (readyCount > 0 && !syncing) {
            Button(onClick = onSuggestions, enabled = canReview, modifier = Modifier.fillMaxWidth()) {
                Text("Review suggestions · $readyCount ready")
            }
            if (!reviewOpen) OutlinedButton(onClick = onReview, enabled = canReview, modifier = Modifier.fillMaxWidth()) {
                Text("Review movies manually")
            }
        } else if (!reviewOpen) {
            Button(onClick = onReview, enabled = canReview, modifier = Modifier.fillMaxWidth()) {
                Text("Review movies")
            }
        }
        OutlinedButton(onClick = onRemoveAll, enabled = canManage, modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.error)) {
            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Remove all unmatched")
        }
    }
}
