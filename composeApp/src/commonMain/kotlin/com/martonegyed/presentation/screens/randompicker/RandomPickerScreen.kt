package com.martonegyed.presentation.screens.randompicker

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.LocalMovies
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.martonegyed.core.ui.adaptive.AdaptiveLayout
import com.martonegyed.core.util.MovieListDisplayModel
import com.martonegyed.domain.model.Movie
import com.martonegyed.presentation.components.common.AppDrawer
import com.martonegyed.presentation.components.common.cards.MovieCard
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RandomPickerScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val screenModel = koinScreenModel<RandomPickerScreenModel>()
        val uiState by screenModel.state.collectAsState()

        var previewMovie by remember { mutableStateOf<Movie?>(null) }
        var isPicking by remember { mutableStateOf(false) }

        val rotation = remember { Animatable(0f) }
        val scale = remember { Animatable(1f) }

        suspend fun runPickAnimation() {
            if (isPicking) return
            if (uiState.watchlist.isEmpty()) return

            isPicking = true
            val finalMovie = uiState.watchlist.random()

            coroutineScope {
                launch {
                    rotation.snapTo(0f)
                    rotation.animateTo(
                        targetValue = 1080f,
                        animationSpec = tween(
                            durationMillis = 1100,
                            easing = FastOutSlowInEasing
                        )
                    )
                    rotation.snapTo(0f)
                }

                launch {
                    scale.snapTo(1f)
                    scale.animateTo(0.92f, tween(120))
                    scale.animateTo(
                        1.06f,
                        spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                    scale.animateTo(1f, tween(180))
                }

                launch {
                    repeat(12) {
                        previewMovie = uiState.watchlist.random()
                        delay(75)
                    }
                    previewMovie = finalMovie
                }
            }

            screenModel.setPickedMovie(finalMovie)
            previewMovie = null
            isPicking = false
        }

        ShakeToPickEffect(
            enabled = uiState.watchlist.isNotEmpty() && !isPicking,
            onShake = {
                scope.launch { runPickAnimation() }
            }
        )

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                AppDrawer(
                    navigator = navigator,
                    currentScreen = this@RandomPickerScreen,
                    closeDrawer = { scope.launch { drawerState.close() } }
                )
            }
        ) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = "Random Picker",
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        }
                    )
                }
            ) { padding ->
                AdaptiveLayout(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator()
                            return@AdaptiveLayout
                        }

                        Column(
                            modifier = Modifier
                                .widthIn(max = 500.dp)
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Box(
                                modifier = Modifier.graphicsLayer {
                                    rotationZ = rotation.value
                                    scaleX = scale.value
                                    scaleY = scale.value
                                },
                                contentAlignment = Alignment.Center
                            ) {
                                RandomPickerCard(
                                    movie = previewMovie ?: uiState.pickedMovie
                                )
                            }

                            Text(
                                text = when {
                                    uiState.watchlist.isEmpty() -> "Your watchlist is empty."
                                    uiState.pickedMovie == null -> "Tap the button to get a random movie."
                                    else -> "Not feeling it? Pick again."
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = { scope.launch { runPickAnimation() } },
                                enabled = uiState.watchlist.isNotEmpty() && !isPicking,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                contentPadding = PaddingValues(vertical = 16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Casino,
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (isPicking) "Picking..." else "Pick from Watchlist",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            if (uiState.watchlist.isNotEmpty()) {
                                Text(
                                    text = "On Android, you can also shake the phone.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RandomPickerCard(
    movie: Movie?,
    onMovieClick: (Long) -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme

    if (movie == null) {
        ElevatedCard(
            modifier = Modifier.width(220.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = colors.primaryContainer
                    ) {
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .padding(18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalMovies,
                                contentDescription = null,
                                tint = colors.onPrimaryContainer,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Text(
                        text = "No movie picked yet",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "Let fate choose tonight’s watchlist winner.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .width(220.dp)
                .clip(RoundedCornerShape(28.dp))
        ) {
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
                showRating = true,
                centerTitle = true,
                posterMaxWidth = 500.dp,
                onTap = { onMovieClick(movie.id.toLong()) }
            )
        }
    }
}