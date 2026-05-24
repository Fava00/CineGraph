package com.martonegyed.presentation.components.yearinreview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun YearInReviewSelector(
    years: List<Int>,
    selectedYear: Int?,
    onYearSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.clickable { expanded = true },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${selectedYear ?: ""} Year in Review".trim(),
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ) {
            years.forEach { year ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "${selectedYear ?: ""} Year in Review".trim(),
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        expanded = false
                        onYearSelected(year)
                    }
                )
            }
        }
    }
}