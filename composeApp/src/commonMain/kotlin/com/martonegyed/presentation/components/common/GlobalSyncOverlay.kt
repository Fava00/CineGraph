package com.martonegyed.presentation.components.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.martonegyed.domain.repository.ImportRepository
import com.martonegyed.domain.repository.ImportPhase
import org.koin.compose.koinInject

@Composable
fun GlobalSyncOverlay() {
    val importRepository: ImportRepository = koinInject()
    val phase by importRepository.phase.collectAsState()
    val importedCount by importRepository.importedCount.collectAsState()
    val importedTotal by importRepository.importedTotal.collectAsState()
    val enrichedCount by importRepository.enrichedCount.collectAsState()
    val enrichedTotal by importRepository.enrichedTotal.collectAsState()
    val message by importRepository.lastMessage.collectAsState()

    if (phase == ImportPhase.IDLE) return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()


            .padding(12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp,
            shadowElevation = 16.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = when (phase) {
                            ImportPhase.IMPORTING -> "Importing movies"
                            ImportPhase.ENRICHING -> "Enriching from TMDb"
                            else -> ""
                        },
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    TextButton(onClick = { importRepository.cancelAll() }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }

                if (importedTotal > 0) {
                    val fraction = importedCount.toFloat() / importedTotal.coerceAtLeast(1)
                    Text(
                        text = "Imported: $importedCount / $importedTotal",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    LinearProgressIndicator(
                        progress = { fraction.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surface,
                        strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                    )
                }

                if (enrichedTotal > 0) {
                    val fraction = enrichedCount.toFloat() / enrichedTotal.coerceAtLeast(1)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Processed: $enrichedCount / $enrichedTotal",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    LinearProgressIndicator(
                        progress = { fraction.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surface,
                        strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                    )
                }

                if (!message.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(message!!, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
            }
        }
    }
}
