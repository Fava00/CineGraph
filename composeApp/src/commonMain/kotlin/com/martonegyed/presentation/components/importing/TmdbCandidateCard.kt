package com.martonegyed.presentation.components.importing

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.martonegyed.domain.model.TmdbMatchCandidate

@Composable
fun TmdbCandidateCard(candidate: TmdbMatchCandidate, enabled: Boolean, actionLabel: String = "Link this movie", onLink: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(modifier = Modifier.width(72.dp).height(108.dp), shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant) {
                if (candidate.posterPath != null) AsyncImage(
                    model = "https://image.tmdb.org/t/p/w185${candidate.posterPath}",
                    contentDescription = "Poster for ${candidate.title}",
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                ) else Box(Modifier.padding(8.dp)) { Text("No poster", style = MaterialTheme.typography.labelSmall) }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(candidate.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("${candidate.year ?: "Year unknown"}", style = MaterialTheme.typography.bodySmall)
                candidate.runtimeMinutes?.let { Text("$it min", style = MaterialTheme.typography.bodySmall) }
                Text(if (candidate.directors.isEmpty()) "Director unavailable" else candidate.directors.joinToString(", "),
                    style = MaterialTheme.typography.bodySmall)
                candidate.originalTitle?.takeIf { it != candidate.title }?.let {
                    Text("Original: $it", style = MaterialTheme.typography.bodySmall)
                }
                candidate.overview?.takeIf { it.isNotBlank() }?.let {
                    Text(it.take(180), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onLink, enabled = enabled) { Text(actionLabel) }
            }
        }
    }
}
