package com.martonegyed.presentation.components.details

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.martonegyed.domain.model.MovieLog
import com.martonegyed.domain.model.MovieLogEntryMode
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun MovieLogEntryDialog(
    title: String,
    mode: MovieLogEntryMode,
    logs: List<MovieLog>,
    isSaving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onSave: (String?, Double?, String?, String?) -> Unit,
    editingLog: MovieLog? = null
) {
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString() }
    val target = logs.filter { it.rating != null }.maxWithOrNull(compareBy(
        { it.watchedDate ?: it.ratedDate }, { it.sourceType == "MANUAL_RATING" },
        { if (it.sourceType == "MANUAL_RATING") it.createdAt?.let { value ->
            runCatching { Instant.parse(value) }.getOrNull() } else null }, { it.stableId }))
    var date by rememberSaveable { mutableStateOf(editingLog?.watchedDate ?: today) }
    var unknownDate by rememberSaveable { mutableStateOf(mode == MovieLogEntryMode.EDIT && editingLog?.watchedDate == null) }
    var score by rememberSaveable { mutableStateOf(when (mode) {
        MovieLogEntryMode.RATING -> target?.rating?.toFloat() ?: 0f
        MovieLogEntryMode.EDIT -> editingLog?.rating?.toFloat() ?: 0f
        MovieLogEntryMode.VIEWING -> 0f
    }) }
    var review by rememberSaveable { mutableStateOf(editingLog?.review.orEmpty()) }
    var ratingDate by rememberSaveable { mutableStateOf(editingLog?.ratedDate.orEmpty()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateValid = unknownDate || runCatching { LocalDate.parse(date) }.isSuccess
    val ratingDateValid = ratingDate.isBlank() || runCatching { LocalDate.parse(ratingDate) }.isSuccess

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text(when (mode) {
            MovieLogEntryMode.VIEWING -> "Log viewing"
            MovieLogEntryMode.RATING -> "Rate movie"
            MovieLogEntryMode.EDIT -> "Edit log"
        }) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (mode != MovieLogEntryMode.RATING) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Date unknown", modifier = Modifier.weight(1f))
                        Switch(checked = unknownDate, onCheckedChange = { unknownDate = it }, enabled = !isSaving)
                    }
                    if (!unknownDate) {
                        OutlinedTextField(
                            value = date,
                            onValueChange = { date = it },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isSaving,
                            singleLine = true,
                            label = { Text("Viewing date") },
                            supportingText = { Text(if (!dateValid) "Use YYYY-MM-DD" else if (date == today) "Watched today - $today" else "YYYY-MM-DD") },
                            isError = !dateValid,
                            trailingIcon = {
                                IconButton(onClick = { showDatePicker = true }, enabled = !isSaving) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = "Choose viewing date")
                                }
                            }
                        )
                    } else {
                        Text(if (editingLog?.isRatingOnly == true)
                            "Leave the viewing date unknown to keep this as a rating entry."
                        else "This viewing will count toward lifetime totals, without a date.", style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    Text(
                        "Saves a separate rating dated $today. Your viewing ratings stay unchanged. To change a viewing's rating, edit its log.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Column {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Rating", modifier = Modifier.weight(1f))
                        Text(if (score == 0f) "Unrated" else "${score.toDouble()} / 5", style = MaterialTheme.typography.titleMedium)
                    }
                    Slider(
                        value = score,
                        onValueChange = { score = (it * 2).roundToInt() / 2f },
                        valueRange = 0f..5f,
                        steps = 9,
                        enabled = !isSaving
                    )
                    Text("Half-star steps from 0.5 to 5", style = MaterialTheme.typography.bodySmall)
                }
                if (mode == MovieLogEntryMode.EDIT && unknownDate && score > 0) {
                    OutlinedTextField(
                        value = ratingDate,
                        onValueChange = { ratingDate = it },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving,
                        singleLine = true,
                        label = { Text("Rating date (optional)") },
                        supportingText = { Text(if (ratingDateValid) "YYYY-MM-DD or leave blank if unknown" else "Use YYYY-MM-DD") },
                        isError = !ratingDateValid
                    )
                }
                if (mode != MovieLogEntryMode.RATING) {
                    OutlinedTextField(
                        value = review,
                        onValueChange = { review = it },
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Review (optional)") },
                        minLines = 2,
                        maxLines = 5
                    )
                }
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(if (unknownDate || mode == MovieLogEntryMode.RATING) null else date,
                    score.toDouble().takeIf { it > 0 }, review, ratingDate.takeIf { score > 0 && it.isNotBlank() }) },
                enabled = !isSaving && (mode == MovieLogEntryMode.RATING || dateValid) &&
                    (mode != MovieLogEntryMode.RATING || score > 0) && (score == 0f || ratingDateValid)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (isSaving) "Saving" else "Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Cancel") } }
    )
    if (showDatePicker) {
        val selectedMillis = runCatching { LocalDate.parse(date).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds() }.getOrNull()
        val picker = rememberDatePickerState(initialSelectedDateMillis = selectedMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { date = Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date.toString() }
                    showDatePicker = false
                }, enabled = picker.selectedDateMillis != null) { Text("Use date") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) { DatePicker(state = picker) }
    }
}
