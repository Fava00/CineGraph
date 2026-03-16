package com.martonegyed.presentation.screens.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.martonegyed.domain.model.Movie
import kotlin.math.roundToInt

import androidx.compose.foundation.border

class MovieDetailScreen(private val movie: Movie) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (navigator.canPop) {
                                    navigator.pop()
                                }
                             },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(50))
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color(0xFF14181c)
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())
            ) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(250.dp)) {
                        if (movie.backdropPath != null) {
                            AsyncImage(
                                model = "https://image.tmdb.org/t/p/w780${movie.backdropPath}",
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color(0xFF14181C)),
                                        startY = 100f
                                    )
                                )
                        )
                    }
                }
                item {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp).offset(y = (-40).dp)
                    ) {

                        Box(modifier = Modifier.width(100.dp)
                            .aspectRatio(0.66f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Gray)) {

                            if (movie.posterPath != null) {
                                AsyncImage(

                                    model = "https://image.tmdb.org/t/p/w500${movie.posterPath}",
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.padding(top = 40.dp)) {
                            Text(
                                text = movie.name,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White)
                            )
                            if (!movie.originalTitle.isNullOrEmpty() && movie.originalTitle != movie.name) {
                                Text("Original: ${movie.originalTitle}", color = Color.Gray, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MetaTag(movie.year.toString())
                                if (movie.runtimeMinutes != null) MetaTag("${movie.runtimeMinutes} min")
                            }
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp)).padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        RatingColumn("YOU", movie.rating?.toString() ?: "-", Color(0xFF00E054))
                        RatingColumn(
                            label = "TMDB",
                            value = movie.tmdbVoteAverage?.let { (((it * 10.0).roundToInt()) / 10.0).toString() } ?: "-",
                            color = Color(0xFFFFB300)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        if (!movie.tagline.isNullOrEmpty()) {
                            Text("“${movie.tagline}”", fontStyle = FontStyle.Italic, color = Color.Gray, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        Text(
                            text = movie.overview ?: "No description available.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun MetaTag(text: String) {
        Box(
            modifier = Modifier.background(Color.Transparent,
                RoundedCornerShape(4.dp))
                .border(1.dp, Color.White.copy(alpha = 0.3f),
                    RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(text, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }

    @Composable
    private fun RatingColumn(label: String, value: String, color: Color) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.Star, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
        }
    }
}