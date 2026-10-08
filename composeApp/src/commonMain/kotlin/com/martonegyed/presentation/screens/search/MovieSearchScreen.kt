package com.martonegyed.presentation.screens.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.martonegyed.domain.model.Movie
import com.martonegyed.domain.model.MovieSearchResult
import com.martonegyed.presentation.components.common.AppDrawer
import com.martonegyed.presentation.screens.details.MovieDetailScreen
import kotlinx.coroutines.launch

class MovieSearchScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = koinScreenModel<MovieSearchScreenModel>()
        val state by model.state.collectAsState()
        val uriHandler = LocalUriHandler.current
        val drawer = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        ModalNavigationDrawer(
            drawerState = drawer,
            drawerContent = {
                AppDrawer(navigator, this@MovieSearchScreen) { scope.launch { drawer.close() } }
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Search movies") },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawer.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        }
                    )
                }
            ) { padding ->
                Column(Modifier.fillMaxSize().padding(padding)) {
                    TabRow(selectedTabIndex = if (state.tmdbTab) 1 else 0) {
                        Tab(selected = !state.tmdbTab, onClick = { model.setTmdbTab(false) },
                            text = { Text("My library") })
                        Tab(selected = state.tmdbTab, onClick = { model.setTmdbTab(true) },
                            text = { Text("TMDb") })
                    }
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = state.query,
                            onValueChange = model::setQuery,
                            label = { Text("Movie title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(onClick = model::search,
                            enabled = !state.isSearching && state.query.trim().length >= 2,
                            modifier = Modifier.fillMaxWidth()) {
                            Text(if (state.isSearching) "Searching..." else "Search ${if (state.tmdbTab) "TMDb" else "my library"}")
                        }
                        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        state.notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                    }

                    when {
                        state.isSearching -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                        state.hasSearched && state.results.isEmpty() -> Column(
                            Modifier.fillMaxWidth().padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("No movies found", style = MaterialTheme.typography.titleMedium)
                            if (state.tmdbTab) {
                                Text("Try another title. If the film is missing from TMDb, search its website and contribute it there.")
                                TextButton(onClick = { uriHandler.openUri("https://www.themoviedb.org/search") }) {
                                    Text("Open TMDb website")
                                }
                            }
                        }
                        !state.hasSearched -> Text(
                            if (state.tmdbTab) "Search TMDb's movie catalog to add a film to your watchlist."
                            else "Search movies already in your library.",
                            modifier = Modifier.padding(24.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        else -> LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(state.results, key = { it.key }) { result ->
                                SearchResultCard(
                                    movie = result,
                                    saving = state.savingMovieKey == result.key,
                                    saveEnabled = state.savingMovieKey == null,
                                    onOpen = {
                                        navigator.push(MovieDetailScreen(Movie(
                                            id = result.localId?.toInt() ?: 0,
                                            tmdbId = result.tmdbId?.takeIf { it > 0 },
                                            name = result.title,
                                            year = result.year ?: 0,
                                            posterPath = result.posterPath,
                                            overview = result.overview,
                                            inWatchlist = result.inWatchlist,
                                            letterboxdUri = null
                                        )))
                                    },
                                    onWatchlist = { model.addToWatchlist(result) }
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
private fun SearchResultCard(
    movie: MovieSearchResult,
    saving: Boolean,
    saveEnabled: Boolean,
    onOpen: () -> Unit,
    onWatchlist: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (movie.posterPath != null) {
                AsyncImage(
                    model = "https://image.tmdb.org/t/p/w154${movie.posterPath}",
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(width = 72.dp, height = 108.dp)
                )
            } else {
                Box(Modifier.size(width = 72.dp, height = 108.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Movie, contentDescription = null)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(movie.title, fontWeight = FontWeight.SemiBold, maxLines = 2,
                    overflow = TextOverflow.Ellipsis)
                Text(movie.year?.toString() ?: "Year unknown",
                    style = MaterialTheme.typography.bodySmall)
                val status = buildList {
                    if (movie.isWatched) add("Watched")
                    if (movie.inWatchlist) add("Watchlist")
                    if (movie.isInLibrary && !movie.isWatched && !movie.inWatchlist) add("In library")
                }
                if (status.isNotEmpty()) Text(status.joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary)
                movie.overview?.takeIf { it.isNotBlank() }?.let {
                    Text(it, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall)
                }
                if ((movie.localId != null || movie.tmdbId?.let { it > 0 } == true) && !movie.inWatchlist) {
                    TextButton(onClick = onWatchlist, enabled = saveEnabled) {
                        Text(if (saving) "Adding..." else "Add to watchlist")
                    }
                }
            }
        }
    }
}
