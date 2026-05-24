package com.martonegyed.presentation.components.yearinreview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.martonegyed.core.util.MovieListDisplayModel
import com.martonegyed.core.util.roundToDecimals
import com.martonegyed.domain.model.Movie
import com.martonegyed.presentation.components.common.HorizontalRow
import com.martonegyed.presentation.components.common.cards.MovieCard

@Composable
fun MovieStripSection(
    movies: List<Movie>,
    showTmdbBadge: Boolean = false,
    onMovieClick: (Movie) -> Unit = {}
) {
    HorizontalRow(items = movies, key = { it.id }) { movie ->
        Box(modifier = Modifier.width(132.dp)) {
            MovieCard(
                item = MovieListDisplayModel(
                    id = movie.id,
                    title = movie.name,
                    year = movie.year,
                    posterPath = movie.posterPath?.takeIf { it.isNotBlank() },
                    userRating = movie.rating,
                    watchedDate = movie.watchedDate,
                    isRewatch = movie.isRewatch
                ),
                showRating = !showTmdbBadge,
                posterMaxWidth = 116.dp,
                onTap = { onMovieClick(movie) }
            )

            if (showTmdbBadge) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 4.dp,
                    shadowElevation = 2.dp
                ) {
                    Text(
                        text = movie.tmdbVoteAverage
                            ?.roundToDecimals(1)
                            ?.toString()
                            ?: "-",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}