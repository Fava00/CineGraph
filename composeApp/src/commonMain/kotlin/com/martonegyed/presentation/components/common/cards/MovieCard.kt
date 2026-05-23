package com.martonegyed.presentation.components.common.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.martonegyed.core.util.MovieListDisplayModel

@Composable
fun MovieCard(
    item: MovieListDisplayModel,
    showRating: Boolean = true,
    centerTitle: Boolean = false,
    posterMaxWidth: Dp = 115.dp,
    titleFontSize: TextUnit = 14.sp,
    metaFontSize: TextUnit = 12.sp,
    ratingFontSize: TextUnit = 11.sp,
    onTap: () -> Unit
) {
    val cardShape = RoundedCornerShape(12.dp)
    val posterShape = RoundedCornerShape(10.dp)


    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = posterMaxWidth + 16.dp)
                .clickable(onClick = onTap),
            shape = cardShape,
            color = Color(0xFF181D21),
            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.06f))
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = posterMaxWidth)
                    .padding(8.dp),
                horizontalAlignment = if (centerTitle) Alignment.CenterHorizontally else Alignment.Start
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.66f)
                        .clip(posterShape)
                        .background(Color.DarkGray)
                ) {
                    if (item.posterPath != null) {
                        AsyncImage(
                            model = "https://image.tmdb.org/t/p/w500${item.posterPath}",
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            modifier = Modifier.align(Alignment.Center),
                            tint = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.heightIn(min = 8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 42.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = titleFontSize,
                            lineHeight = titleFontSize * 1.2
                        ),
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = if (centerTitle) TextAlign.Center else TextAlign.Start
                    )
                }

                Spacer(modifier = Modifier.heightIn(min = 6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (item.year > 0) item.year.toString() else "",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF9AA0A6),
                            fontSize = metaFontSize
                        ),
                        textAlign = if (centerTitle) TextAlign.Center else TextAlign.Start,
                        maxLines = 1
                    )

                    if (showRating && item.userRating != null && item.userRating > 0.0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFF00E054),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.size(4.dp))
                            Text(
                                text = item.userRating.toString(),
                                color = Color.White,
                                fontSize = ratingFontSize,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}