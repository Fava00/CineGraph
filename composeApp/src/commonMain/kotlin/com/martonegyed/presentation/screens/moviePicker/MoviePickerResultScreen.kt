package com.martonegyed.presentation.screens.moviePicker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.martonegyed.core.util.roundToDecimals
import com.martonegyed.domain.model.toMovie
import com.martonegyed.presentation.screens.details.MovieDetailScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.parameter.parametersOf

data class MoviePickerResultsScreen(
    val request: MoviePickerRequest
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<MoviePickerResultsScreenModel> {
            parametersOf(request)
        }
        val uiState by screenModel.uiState.collectAsState()

        LaunchedEffect(uiState.queue.isEmpty(), uiState.mightWatch.size) {
            if (!uiState.isLoading && uiState.queue.isEmpty() && uiState.mightWatch.isNotEmpty()) {
                screenModel.openMightWatch()
            }
        }

        Scaffold(
            containerColor = Color(0xFF14181C),
            topBar = {
                TopAppBar(
                    title = { Text("Movie Picker") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    actions = {
                        AssistChip(
                            onClick = { screenModel.openMightWatch() },
                            label = { Text("${uiState.mightWatch.size}") },
                            leadingIcon = {
                                Icon(Icons.Default.Movie, contentDescription = null)
                            }
                        )

                        IconButton(onClick = { screenModel.resetSession() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset")
                        }

                        IconButton(
                            onClick = { navigator.push(DiscoveryManagerScreen()) }
                        ) {
                            Icon(Icons.Default.Storage, contentDescription = "Discovery manager")
                        }
                    }
                )
            }
        ) { padding ->
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.errorMessage != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "Unknown error",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                else -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        MoviePickerSwipeDeck(
                            modifier = Modifier
                                .widthIn(max = 460.dp)
                                .fillMaxWidth()
                                .fillMaxHeight(),
                            uiState = uiState,
                            onPass = { screenModel.onPassTop() },
                            onSave = { screenModel.onSaveTop() },
                            onIgnore = { screenModel.onIgnoreTop() },
                            onUndo = { screenModel.undoLastSwipe() },
                            onDismissUndo = { screenModel.dismissUndo() },
                            onOpenMightWatch = { screenModel.openMightWatch() },
                            onOpenMovie = { movie ->
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

        if (uiState.showMightWatchSheet) {
            ModalBottomSheet(
                onDismissRequest = { screenModel.closeMightWatch() },
                containerColor = Color(0xFF1C2228)
            ) {
                MightWatchSheet(
                    movies = uiState.mightWatch,
                    onRemove = { screenModel.removeFromMightWatch(it) },
                    onClose = { screenModel.closeMightWatch() }
                )
            }
        }
    }
}

@Composable
private fun MoviePickerSwipeDeck(
    modifier: Modifier,
    uiState: MoviePickerDeckUiState,
    onPass: () -> Unit,
    onSave: () -> Unit,
    onIgnore: () -> Unit,
    onUndo: () -> Unit,
    onDismissUndo: () -> Unit,
    onOpenMightWatch: () -> Unit,
    onOpenMovie: (MoviePickerCandidateUi) -> Unit
) {
    val currentMovie = uiState.queue.lastOrNull()
    val nextMovie = uiState.queue.dropLast(1).lastOrNull()

    LaunchedEffect(uiState.showUndo, uiState.lastRemoved?.stableKey()) {
        if (uiState.showUndo && uiState.lastRemoved != null) {
            delay(3_000)
            onDismissUndo()
        }
    }

    Box(
        modifier = modifier
            .background(Color(0xFF14181C))
            .padding(horizontal = 16.dp)
    ) {
        if (currentMovie == null) {
            EmptyResultsState(
                mightWatchCount = uiState.mightWatch.size,
                onOpenMightWatch = onOpenMightWatch
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${uiState.queue.size} movies remaining",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray
                    )

                    Text(
                        text = "Local ${uiState.totalLocalCandidates} • Remote ${uiState.totalRemoteCandidates}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(8f),
                    contentAlignment = Alignment.Center
                ) {
                    val cardWidth = minOf(maxWidth * 0.92f, maxHeight * (2f / 3f))

                    Box(
                        modifier = Modifier.width(cardWidth),
                        contentAlignment = Alignment.Center
                    ) {
                        if (nextMovie != null) {
                            DeckBackgroundCard(
                                movie = nextMovie,
                                modifier = Modifier.fillMaxWidth(0.93f)
                            )
                        }

                        SwipeableMovieCard(
                            movie = currentMovie,
                            modifier = Modifier.fillMaxWidth(),
                            onSwipeLeft = onPass,
                            onSwipeRight = onSave,
                            onSwipeDown = onIgnore,
                            onOpenDetails = { onOpenMovie(currentMovie) }
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(2f)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        DeckActionButton(
                            icon = Icons.Default.Close,
                            color = Color.Red,
                            onClick = onPass
                        )

                        DeckActionButton(
                            icon = Icons.Default.VisibilityOff,
                            color = Color.Gray,
                            onClick = onIgnore
                        )

                        DeckActionButton(
                            icon = Icons.Default.Check,
                            color = Color(0xFF00E054),
                            onClick = onSave
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = uiState.showUndo && uiState.lastRemoved != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .navigationBarsPadding(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                color = Color(0xFF21262D),
                shape = RoundedCornerShape(14.dp),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (uiState.lastDecision) {
                            MoviePickerSwipeDecision.SAVE -> "Saved '${uiState.lastRemoved?.title}'"
                            MoviePickerSwipeDecision.IGNORE -> "Ignored '${uiState.lastRemoved?.title}'"
                            else -> "Passed '${uiState.lastRemoved?.title}'"
                        },
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White
                    )

                    TextButton(onClick = onUndo) {
                        Text("UNDO", color = Color(0xFF00E054))
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyResultsState(
    mightWatchCount: Int,
    onOpenMightWatch: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Movie,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "You’ve gone through all the matches",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (mightWatchCount > 0) {
                "$mightWatchCount movies in your Might Watch pile"
            } else {
                "No saved picks yet"
            },
            color = Color.LightGray
        )
        if (mightWatchCount > 0) {
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onOpenMightWatch) {
                Text("Open Might Watch", color = Color(0xFF00E054))
            }
        }
    }
}

@Composable
private fun DeckBackgroundCard(
    movie: MoviePickerCandidateUi,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(2f / 3f)
            .graphicsLayer {
                scaleX = 0.92f
                scaleY = 0.92f
                alpha = 0.45f
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF222A31))
    ) {
        MoviePosterCardContent(movie = movie)
    }
}

@Composable
private fun SwipeableMovieCard(
    movie: MoviePickerCandidateUi,
    modifier: Modifier = Modifier,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onSwipeDown: () -> Unit,
    onOpenDetails: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val offsetX = remember(movie.stableKey()) { Animatable(0f) }
    val offsetY = remember(movie.stableKey()) { Animatable(0f) }

    BoxWithConstraints(
        modifier = modifier
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val swipeXThreshold = widthPx * 0.25f
        val swipeDownThreshold = heightPx * 0.18f

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .graphicsLayer {
                    translationX = offsetX.value
                    translationY = offsetY.value
                    rotationZ = offsetX.value / 45f
                }
                .pointerInput(movie.stableKey()) {
                    detectDragGestures(
                        onDragCancel = {
                            scope.launch {
                                offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                                offsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                            }
                        },
                        onDragEnd = {
                            scope.launch {
                                when {
                                    offsetX.value > swipeXThreshold -> {
                                        offsetX.animateTo(widthPx * 1.2f)
                                        onSwipeRight()
                                    }

                                    offsetX.value < -swipeXThreshold -> {
                                        offsetX.animateTo(-widthPx * 1.2f)
                                        onSwipeLeft()
                                    }

                                    offsetY.value > swipeDownThreshold -> {
                                        offsetY.animateTo(heightPx * 1.1f)
                                        onSwipeDown()
                                    }

                                    else -> {
                                        launch {
                                            offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                                        }
                                        launch {
                                            offsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                                        }
                                    }
                                }
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                offsetX.snapTo(offsetX.value + dragAmount.x)
                                offsetY.snapTo(offsetY.value + dragAmount.y)
                            }
                        }
                    )
                },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF222A31)),
            onClick = onOpenDetails
        ) {
            Box(Modifier.fillMaxSize()) {
                MoviePosterCardContent(movie = movie)
            }
        }
    }
}

@Composable
private fun SwipeLabel(
    text: String,
    color: Color,
    alpha: Float,
    modifier: Modifier = Modifier
) {
    if (alpha <= 0f) return

    Text(
        text = text,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        color = color.copy(alpha = alpha),
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun MoviePosterCardContent(movie: MoviePickerCandidateUi) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (movie.posterPath != null) {
            AsyncImage(
                model = "https://image.tmdb.org/t/p/w500${movie.posterPath}",
                contentDescription = movie.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF2C3440)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(56.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.60f))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = movie.title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = buildString {
                        movie.year?.let { append(it) }
                        movie.runtimeMinutes?.let {
                            if (isNotEmpty()) append(" • ")
                            append("${it}m")
                        }
                        movie.tmdbVoteAverage?.let {
                            if (isNotEmpty()) append(" • ")
                            append(it.roundToDecimals(1).toString())
                        }
                    },
                    color = Color.LightGray,
                    style = MaterialTheme.typography.bodyMedium
                )

                if (!movie.overview.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = movie.overview,
                        color = Color.White.copy(alpha = 0.92f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun DeckActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.size(64.dp),
        shape = CircleShape,
        color = Color(0xFF222A31),
        shadowElevation = 6.dp,
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
private fun MightWatchSheet(
    movies: List<MoviePickerCandidateUi>,
    onRemove: (MoviePickerCandidateUi) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Might Watch (${movies.size})",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White
        )

        Spacer(Modifier.height(12.dp))

        if (movies.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 36.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nothing saved yet",
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(movies, key = { it.stableKey() }) { movie ->
                    Surface(
                        color = Color(0xFF2C3440),
                        shape = RoundedCornerShape(14.dp)
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
                                    .clip(RoundedCornerShape(8.dp))
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
                                    Icon(Icons.Default.Movie, contentDescription = null, tint = Color.Gray)
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
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

                            IconButton(onClick = { onRemove(movie) }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }

        TextButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Done", color = Color(0xFF00E054))
        }
    }
}