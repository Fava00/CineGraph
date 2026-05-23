package com.martonegyed.presentation.components.yearinreview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.martonegyed.core.ui.formatWatchedDate
import com.martonegyed.core.util.MovieListDisplayModel
import com.martonegyed.presentation.components.common.HorizontalRow
import com.martonegyed.presentation.components.common.cards.MovieCard
import com.martonegyed.presentation.screens.yearinreview.MilestoneMovieRow

@Composable
fun MilestonesSection(
    rows: List<MilestoneMovieRow>,
    onMovieClick: (Long) -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme


    Column(
        modifier = Modifier.padding(bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        HorizontalRow(items = rows, key = { it.milestone }) { row ->
            Box {
                Column(
                    modifier = Modifier.width(132.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                        posterMaxWidth = 116.dp,
                        onTap = { onMovieClick(row.movie.id.toLong()) }
                    )

                    Text(
                        text = formatWatchedDate(row.watchedDate),
                        color = colors.secondary,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 10.dp, end = 10.dp),
                    shape = CircleShape,
                    color = colors.primary,
                    tonalElevation = 2.dp
                ) {
                    Text(
                        text = row.milestone.toString(),
                        color = colors.onPrimary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}