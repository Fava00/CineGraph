package com.martonegyed.presentation.components.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.Navigator
import com.martonegyed.domain.model.Person
import com.martonegyed.core.ui.adaptive.MovieDetailTokens

@Composable
fun FullCrewSheet(
    directors: List<Person>,
    crew: List<Person>,
    detailTokens: MovieDetailTokens,
    navigator: Navigator,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    val groupedCrew: Map<String, List<Person>> = remember(crew) {
        crew
            .filter { !it.job.isNullOrBlank() && !it.name.isNullOrBlank() }
            .groupBy { it.job!!.trim() }
            .entries
            .sortedBy { it.key }
            .associate { it.key to it.value }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Full Crew",
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurface,
                modifier = Modifier.weight(1f)
            )

            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (directors.isNotEmpty()) {
                item {
                    CrewGroupCard(
                        title = "Director",
                        people = directors,
                        detailTokens = detailTokens,
                        navigator = navigator,
                        onDismiss = onDismiss
                    )
                }
            }

            groupedCrew.forEach { (job, people) ->
                item(key = job) {
                    CrewGroupCard(
                        title = job,
                        people = people.distinctBy { it.name },
                        detailTokens = detailTokens,
                        navigator = navigator,
                        onDismiss = onDismiss
                    )
                }
            }
        }
    }
}