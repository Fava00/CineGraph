package com.martonegyed.presentation.components.yearinreview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.martonegyed.core.ui.formatWatchedDate
import com.martonegyed.core.util.MovieListDisplayModel
import com.martonegyed.presentation.components.common.cards.MovieCard
import com.martonegyed.presentation.components.common.cards.SectionCard
import com.martonegyed.presentation.screens.yearinreview.WatchedMovieRow

@Composable
fun FirstLastMovieSection(
    firstMovie: WatchedMovieRow?,
    lastMovie: WatchedMovieRow?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (firstMovie != null) {
            SectionCard(
                title = "First movie",
                modifier = Modifier.weight(1f)
            ) {
                FirstLastMovieContent(
                    label = "FIRST",
                    row = firstMovie
                )
            }
        } else {
            Spacer(Modifier.weight(1f))
        }

        if (lastMovie != null) {
            SectionCard(
                title = "Last movie",
                modifier = Modifier.weight(1f)
            ) {
                FirstLastMovieContent(
                    label = "LAST",
                    row = lastMovie
                )
            }
        } else {
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun FirstLastMovieContent(
    label: String,
    row: WatchedMovieRow,
    onMovieClick: (Long) -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = label,
            color = colors.inversePrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        MovieCard(
            item = MovieListDisplayModel(
                id = row.movie.id,
                title = row.movie.name,
                year = row.movie.year,
                posterPath = row.movie.posterPath?.takeIf { it.isNotBlank() },
                userRating = row.movie.rating,
                watchedDate = row.movie.watchedDate,
                isRewatch = row.movie.isRewatch
            ),
            showRating = false,
            centerTitle = true,
            posterMaxWidth = 120.dp,
            onTap = { onMovieClick(row.movie.id.toLong()) }
        )

        Text(
            text = formatWatchedDate(row.watchedDate),
            color = colors.secondary,
            style = MaterialTheme.typography.titleMedium
        )
    }
}