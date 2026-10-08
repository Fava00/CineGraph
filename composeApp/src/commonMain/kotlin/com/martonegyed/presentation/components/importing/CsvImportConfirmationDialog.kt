package com.martonegyed.presentation.components.importing

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.martonegyed.domain.model.CsvImportAssessment
import com.martonegyed.domain.model.CsvImportType
import com.martonegyed.presentation.screens.import.CsvSourceAction
import com.martonegyed.presentation.screens.import.CsvSourceConflict

@Composable
fun CsvImportConfirmationDialog(
    assessment: CsvImportAssessment,
    busy: Boolean,
    onChoose: (CsvImportType) -> Unit,
    onCancel: () -> Unit
) {
    val suggested = assessment.filenameType?.takeIf { it in assessment.headerTypes && it in assessment.choices }
        ?: assessment.headerTypes.singleOrNull()?.takeIf { it in assessment.choices }
    var selected by remember(assessment) { mutableStateOf(suggested ?: assessment.selectedType?.takeIf { it in assessment.choices }) }
    var showDetails by remember(assessment) { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { if (!busy) onCancel() },
        title = { Text("Import file as…") },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(assessment.fileName, style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text(assessment.platform, style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(assessment.shortExplanation(), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    assessment.choices.forEach { type ->
                        ImportChoice(type.label, type.shortEffect(), type == selected, !busy,
                            suggested = type == suggested) { selected = type }
                    }
                }
                Column {
                    TextButton(onClick = { showDetails = !showDetails }, contentPadding = PaddingValues(0.dp)) {
                        Text(if (showDetails) "Hide detection details" else "Detection details")
                    }
                    if (showDetails) Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        EvidenceRow("Your selection", assessment.selectedType?.label ?: "Automatic detection")
                        EvidenceRow("Filename", assessment.filenameType?.label ?: "No type hint")
                        EvidenceRow("Columns", assessment.headerTypes.joinToString(" or ") { it.label })
                        assessment.message?.let { Text(it, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { selected?.let(onChoose) }, enabled = !busy && selected != null) {
                if (busy) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text(selected?.let { "Continue" } ?: "Choose a type")
            }
        },
        dismissButton = { TextButton(onClick = onCancel, enabled = !busy) { Text("Skip file") } }
    )
}

@Composable
fun CsvSourceConflictDialog(
    conflict: CsvSourceConflict,
    busy: Boolean,
    onAction: (CsvSourceAction) -> Unit
) {
    var action by remember(conflict) { mutableStateOf(CsvSourceAction.COMBINE) }
    AlertDialog(
        onDismissRequest = { if (!busy) onAction(CsvSourceAction.CANCEL) },
        title = { Text("${conflict.type.label} already staged") },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ImportFileCard("Already staged · ${conflict.existingRows} rows", conflict.existingFiles)
                ImportFileCard("New file · ${conflict.incomingRows} rows", conflict.incomingFiles)
                ImportChoice("Combine files", "Keep both files. Identical rows are included once.",
                    action == CsvSourceAction.COMBINE, !busy) { action = CsvSourceAction.COMBINE }
                ImportChoice("Replace staged file", "Use only the new file for this type.",
                    action == CsvSourceAction.REPLACE, !busy) { action = CsvSourceAction.REPLACE }
            }
        },
        confirmButton = {
            Button(onClick = { onAction(action) }, enabled = !busy) {
                if (busy) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                else Text(if (action == CsvSourceAction.COMBINE) "Combine files" else "Replace file")
            }
        },
        dismissButton = { TextButton(onClick = { onAction(CsvSourceAction.CANCEL) }, enabled = !busy) { Text("Cancel") } }
    )
}

@Composable
private fun ImportFileCard(label: String, names: List<String>) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            names.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun EvidenceRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
        Text(value, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun ImportChoice(title: String, description: String, selected: Boolean, enabled: Boolean,
                         suggested: Boolean = false, onSelect: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.surfaceVariant,
        contentColor = colors.onSurfaceVariant,
        border = BorderStroke(1.dp, if (selected) colors.primary else colors.outlineVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.fillMaxWidth().selectable(selected, enabled = enabled, role = Role.RadioButton, onClick = onSelect).padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = null, enabled = enabled, modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                    if (suggested) Text("Suggested", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                }
                Text(description, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
    }
}

private fun CsvImportType.shortEffect(): String = when (this) {
    CsvImportType.WATCHLIST -> "Save movies for later"
    CsvImportType.WATCHED -> "Mark movies as watched"
    CsvImportType.DIARY -> "Import viewing history"
    CsvImportType.RATINGS -> "Import personal ratings"
    CsvImportType.REVIEWS -> "Import reviews and viewing history"
    CsvImportType.LISTS -> "Import list movies"
}

private fun CsvImportAssessment.shortExplanation(): String = when {
    selectedType != null && filenameType != null && selectedType != filenameType ->
        "The filename suggests ${filenameType.label}. You selected ${selectedType.label}."
    selectedType != null && headerTypes.singleOrNull()?.let { it != selectedType } == true ->
        "The columns suggest ${headerTypes.single().label}. You selected ${selectedType.label}."
    filenameType != null && filenameType !in headerTypes -> "The filename and columns suggest different types."
    else -> "Choose how to import this file. Its columns fit ${headerTypes.joinToString(" or ") { it.label }}."
}
