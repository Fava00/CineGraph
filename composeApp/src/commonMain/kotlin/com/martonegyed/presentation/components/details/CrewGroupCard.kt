package com.martonegyed.presentation.components.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.Navigator
import coil3.compose.AsyncImage
import com.martonegyed.domain.model.Person
import com.martonegyed.core.ui.adaptive.MovieDetailTokens
import com.martonegyed.presentation.components.common.PersonAvatar

@Composable
fun CrewGroupCard(
    title: String,
    people: List<Person>,
    detailTokens: MovieDetailTokens,
    navigator: Navigator,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceVariant.copy(alpha = 0.35f),
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                color = colors.inversePrimary,
                fontSize = detailTokens.metaFontSize,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(10.dp))

            people.forEachIndexed { index, person ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            /*TODO:
                               onDismiss()
                            openPersonCollection(
                                navigator = navigator,
                                personName = person.name,
                                entityType = StatEntityType.CREW
                            )*/
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PersonAvatar(
                        name = person.name ?: "Unknown",
                        photoPath = person.profilePath,
                        size = 42.dp,
                        borderColor = null
                    )

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = person.name ?: "Unknown",
                            color = colors.onSurface,
                            fontSize = detailTokens.bodyFontSize,
                            fontWeight = FontWeight.SemiBold
                        )

                        val secondary = listOfNotNull(
                            person.character?.takeIf { it.isNotBlank() },
                            person.job?.takeIf { it.isNotBlank() }
                        ).joinToString(" • ")

                        if (secondary.isNotEmpty()) {
                            Text(
                                text = secondary,
                                color = colors.onSurfaceVariant,
                                fontSize = detailTokens.metaFontSize
                            )
                        }
                    }
                }

                if (index != people.lastIndex) {
                    HorizontalDivider(color = colors.onSurface.copy(alpha = 0.08f))
                }
            }
        }
    }
}