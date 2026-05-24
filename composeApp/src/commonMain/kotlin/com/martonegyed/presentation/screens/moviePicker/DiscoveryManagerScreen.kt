package com.martonegyed.presentation.screens.moviePicker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.martonegyed.core.util.roundToDecimals
import com.martonegyed.domain.model.Movie
import com.martonegyed.presentation.screens.details.MovieDetailScreen

data class DiscoveryManagerScreen(
    val showBackButton: Boolean = true
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<DiscoveryManagerScreenModel>()
        val uiState by screenModel.uiState.collectAsState()

        Scaffold(
            containerColor = Color(0xFF14181C),
            topBar = {
                TopAppBar(
                    title = { Text("Discovery Manager") },
                    navigationIcon = {
                        if (showBackButton) {
                            IconButton(onClick = { navigator.pop() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { screenModel.refresh() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFF14181C))
            ) {
                PrimaryTabRow(selectedTabIndex = uiState.selectedTab.ordinal) {
                    DiscoveryManagerTab.entries.forEachIndexed { index, tab ->
                        Tab(
                            selected = uiState.selectedTab.ordinal == index,
                            onClick = { screenModel.selectTab(tab) },
                            text = {
                                Text(
                                    when (tab) {
                                        DiscoveryManagerTab.CACHED ->
                                            "Cached (${uiState.cachedMovies.size})"

                                        DiscoveryManagerTab.IGNORED ->
                                            "Ignored (${uiState.ignoredMovies.size})"
                                    }
                                )
                            }
                        )
                    }
                }

                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    uiState.selectedTab == DiscoveryManagerTab.CACHED -> {
                        DiscoveryMovieList(
                            movies = uiState.cachedMovies,
                            emptyText = "Cache is empty",
                            emptySubtext = "Movies fetched from discovery can appear here.",
                            trailingContent = {},
                            onMovieClick = { movie ->
                                navigator.push(
                                    MovieDetailScreen(
                                        movie = movie.toMovie()
                                    )
                                )
                            }
                        )
                    }

                    else -> {
                        DiscoveryMovieList(
                            movies = uiState.ignoredMovies,
                            emptyText = "No ignored movies",
                            emptySubtext = "Ignored movies from the picker will appear here.",
                            trailingContent = { movie ->
                                IconButton(
                                    onClick = { screenModel.unignore(movie) }
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Undo,
                                        contentDescription = "Unignore",
                                        tint = Color(0xFF00E054)
                                    )
                                }
                            },
                            onMovieClick = { movie ->
                                navigator.push(
                                    MovieDetailScreen(
                                        movie = movie.toMovie()
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

enum class DiscoveryManagerTab {
    CACHED,
    IGNORED
}

data class DiscoveryManagerMovieUi(
    val localMovieId: Long? = null,
    val tmdbId: Int? = null,
    val title: String,
    val year: Int? = null,
    val posterPath: String? = null,
    val tmdbVoteAverage: Double? = null
)

data class DiscoveryManagerUiState(
    val isLoading: Boolean = true,
    val selectedTab: DiscoveryManagerTab = DiscoveryManagerTab.CACHED,
    val cachedMovies: List<DiscoveryManagerMovieUi> = emptyList(),
    val ignoredMovies: List<DiscoveryManagerMovieUi> = emptyList()
)

fun DiscoveryManagerMovieUi.toMovie(): Movie {
    return Movie(
        id = (localMovieId ?: 0L).toInt(),
        tmdbId = tmdbId,
        name = title,
        year = year ?: 0,
        posterPath = posterPath,
        tmdbVoteAverage = tmdbVoteAverage,
        letterboxdUri = null,
    )
}

@Composable
private fun DiscoveryMovieList(
    movies: List<DiscoveryManagerMovieUi>,
    emptyText: String,
    emptySubtext: String,
    trailingContent: @Composable (DiscoveryManagerMovieUi) -> Unit,
    onMovieClick: (DiscoveryManagerMovieUi) -> Unit
) {
    if (movies.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(emptyText, color = Color.White)
                Spacer(Modifier.height(4.dp))
                Text(
                    emptySubtext,
                    color = Color.LightGray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(movies, key = { it.tmdbId ?: it.localMovieId ?: it.title.hashCode() }) { movie ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF222A31),
                    shape = MaterialTheme.shapes.medium,
                    onClick = { onMovieClick(movie) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(56.dp)
                                .aspectRatio(2f / 3f)
                                .clip(MaterialTheme.shapes.small)
                                .background(Color.DarkGray),
                            contentAlignment = Alignment.Center
                        ) {
                            if (movie.posterPath != null) {
                                AsyncImage(
                                    model = "https://image.tmdb.org/t/p/w200${movie.posterPath}",
                                    contentDescription = movie.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Default.Movie,
                                    contentDescription = null,
                                    tint = Color.Gray
                                )
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = movie.title,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = buildString {
                                    movie.year?.let { append(it) }
                                    movie.tmdbVoteAverage?.let {
                                        if (isNotEmpty()) append(" • ")
                                        append(it.roundToDecimals(1).toString())
                                    }
                                },
                                color = Color.LightGray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        trailingContent(movie)
                    }
                }
            }
        }
    }
}