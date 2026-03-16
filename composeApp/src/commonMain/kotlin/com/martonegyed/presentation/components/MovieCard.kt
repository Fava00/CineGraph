package com.martonegyed.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.martonegyed.domain.model.Movie

@Composable
fun MovieCard(
    movie: Movie,
    showRating: Boolean = true,
    centerTitle: Boolean = false,
    onTap: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(115.dp)
            .clickable { onTap() },
        horizontalAlignment = if (centerTitle) Alignment.CenterHorizontally else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.66f)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.DarkGray)
        ) {
            if (movie.posterPath != null) {
                AsyncImage(
                    model = movie.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" },
                    contentDescription = movie.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    Icons.Default.Movie,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.Center),
                    tint = Color.Gray
                )
            }

            if (showRating && movie.rating != null && movie.rating!! > 0) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFF00E054), modifier = Modifier.size(10.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text(movie.rating.toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = movie.name,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (centerTitle) TextAlign.Center else TextAlign.Start
        )
        if (movie.year > 0) {
            Text(
                text = movie.year.toString(),
                style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray),
                textAlign = if (centerTitle) TextAlign.Center else TextAlign.Start
            )
        }
    }
}