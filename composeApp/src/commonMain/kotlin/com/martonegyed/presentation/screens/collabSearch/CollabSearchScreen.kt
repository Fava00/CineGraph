package com.martonegyed.presentation.screens.collabSearch

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Theaters
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
import androidx.compose.ui.window.PopupProperties
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.martonegyed.data.remote.TmdbMovie
import com.martonegyed.domain.model.PersonSuggestion
import com.martonegyed.domain.model.SelectedPerson
import com.martonegyed.domain.model.SuggestionSource
import com.martonegyed.core.ui.adaptive.AdaptiveLayout
import com.martonegyed.data.remote.TmdbGenre
import kotlin.math.roundToInt

data class CollabSearchScreen(
    val showBackButton: Boolean = false
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val colors = MaterialTheme.colorScheme
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<CollabSearchScreenModel>()
        val uiState by screenModel.uiState.collectAsState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Advanced Discovery") },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (showBackButton) navigator.pop()
                            }
                        ) {
                            Icon(
                                imageVector = if (showBackButton) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Menu,
                                contentDescription = if (showBackButton) "Back" else "Menu"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.background
                    )
                )
            },
            bottomBar = {
                Surface(
                    color = colors.background,
                    tonalElevation = 6.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        Button(
                            onClick = { screenModel.search() },
                            enabled = uiState.canSearch && !uiState.isSearching,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00E054),
                                contentColor = Color.Black
                            )
                        ) {
                            if (uiState.isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.Black
                                )
                            } else {
                                Text(
                                    "SEARCH MOVIES",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        ) { padding ->
            AdaptiveLayout(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFF14181c))
            ) { adaptive ->

                val scaffold = adaptive.tokens.scaffold
                val compact = adaptive.window.isCompact

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = if (compact) 12.dp else scaffold.horizontalPadding,
                        vertical = if (compact) 10.dp else scaffold.verticalPadding
                    ),
                    verticalArrangement = Arrangement.spacedBy(if (compact) 16.dp else scaffold.sectionSpacing)
                ) {
                    item {
                        FilterSectionTitle("Cast & Crew")
                    }

                    item {
                        PersonInputSection(
                            value = uiState.actorInput,
                            placeholder = "Enter an Actor's name (e.g. Tom Cruise)",
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            suggestions = uiState.actorSuggestions,
                            expanded = uiState.showActorSuggestions,
                            onValueChange = { screenModel.updateInput(PersonRole.ACTOR, it) },
                            onAdd = { screenModel.addPerson(PersonRole.ACTOR) },
                            onSuggestionClick = { screenModel.selectSuggestion(PersonRole.ACTOR, it) },
                            onDismiss = { screenModel.dismissSuggestions(PersonRole.ACTOR) }
                        )

                    }

                    if (uiState.selectedActors.isNotEmpty()) {
                        item {
                            SelectedPeopleChips(
                                items = uiState.selectedActors,
                                onRemove = { screenModel.removePerson(PersonRole.ACTOR, it) }
                            )
                        }
                    }

                    item {
                        PersonInputSection(
                            placeholder = "Enter a Director's name (e.g. David Lynch)",
                            leadingIcon = { Icon(Icons.Default.Theaters, contentDescription = null) },
                            value = uiState.directorInput,
                            suggestions = uiState.directorSuggestions,
                            expanded = uiState.showDirectorSuggestions,
                            onValueChange = { screenModel.updateInput(PersonRole.DIRECTOR, it) },
                            onAdd = { screenModel.addPerson(PersonRole.DIRECTOR) },
                            onSuggestionClick = { screenModel.selectSuggestion(PersonRole.DIRECTOR, it) },
                            onDismiss = { screenModel.dismissSuggestions(PersonRole.DIRECTOR) })
                    }

                    if (uiState.selectedDirectors.isNotEmpty()) {
                        item {
                            SelectedPeopleChips(
                                items = uiState.selectedDirectors,
                                onRemove = { screenModel.removePerson(PersonRole.DIRECTOR, it) }
                            )
                        }
                    }

                    item {
                        HorizontalDivider(color = colors.onSurface.copy(alpha = 0.12f))
                    }

                    item {
                        FilterSectionTitle("Release Years")
                    }

                    item {
                        ReleaseYearSection(
                            minYear = uiState.minYear,
                            maxYear = uiState.maxYear,
                            selectedStartYear = uiState.selectedStartYear,
                            selectedEndYear = uiState.selectedEndYear,
                            onYearRangeChanged = screenModel::updateYearRange
                        )
                    }

                    item {
                        HorizontalDivider(color = colors.onSurface.copy(alpha = 0.12f))
                    }

                    item {
                        FilterSectionTitle("Genres")
                    }

                    item {
                        if (uiState.isLoadingGenres) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else {
                            GenreSection(
                                genres = uiState.availableGenres,
                                selectedGenreIds = uiState.selectedGenreIds,
                                onToggle = screenModel::toggleGenre
                            )
                        }
                    }

                    uiState.errorMessage?.let { message ->
                        item {
                            Text(
                                text = message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    if (uiState.results.isNotEmpty()) {
                        item {
                            HorizontalDivider(color = colors.onSurface.copy(alpha = 0.12f))
                        }

                        item {
                            Text(
                                text = "${uiState.results.size} results",
                                color = colors.onSurface,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        item {
                            DiscoveryResultsGrid(
                                movies = uiState.results,
                                compact = compact
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun ReleaseYearSection(
    minYear: Int,
    maxYear: Int,
    selectedStartYear: Int,
    selectedEndYear: Int,
    onYearRangeChanged: (Int, Int) -> Unit
) {
    var sliderValues by remember(selectedStartYear, selectedEndYear) {
        mutableStateOf(selectedStartYear.toFloat()..selectedEndYear.toFloat())
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = selectedStartYear.toString(),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = selectedEndYear.toString(),
                color = MaterialTheme.colorScheme.primary
            )
        }

        RangeSlider(
            value = sliderValues,
            onValueChange = {
                sliderValues = it
                onYearRangeChanged(
                    it.start.roundToInt(),
                    it.endInclusive.roundToInt()
                )
            },
            valueRange = minYear.toFloat()..maxYear.toFloat()
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GenreSection(
    genres: List<TmdbGenre>,
    selectedGenreIds: Set<Int>,
    onToggle: (Int) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        genres.forEach { genre ->
            FilterChip(
                selected = genre.id in selectedGenreIds,
                onClick = { onToggle(genre.id) },
                label = { Text(genre.name) }
            )
        }
    }
}

@Composable
private fun DiscoveryResultsGrid(
    movies: List<TmdbMovie>,
    compact: Boolean
) {
    val columns = if (compact) 3 else 4
    val gridHeight = if (compact) 420.dp else 620.dp

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier
            .fillMaxWidth()
            .height(gridHeight),
        userScrollEnabled = false,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(movies.take(if (compact) 12 else 16)) { movie ->
            DiscoveryMovieCard(movie)
        }
    }
}

@Composable
private fun DiscoveryMovieCard(
    movie: TmdbMovie
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.66f)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (movie.posterPath != null) {
                AsyncImage(
                    model = "https://image.tmdb.org/t/p/w300${movie.posterPath}",
                    contentDescription = movie.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.Movie,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = movie.title,
            color = colors.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )

        val year = movie.releaseDate?.take(4).orEmpty()
        if (year.isNotEmpty()) {
            Text(
                text = year,
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun PersonInputSection(
    value: String,
    suggestions: List<PersonSuggestion>,
    expanded: Boolean,
    placeholder: String,
    leadingIcon: @Composable () -> Unit,
    onValueChange: (String) -> Unit,
    onAdd: () -> Unit,
    onSuggestionClick: (PersonSuggestion) -> Unit,
    onDismiss: () -> Unit
) {
    val menuExpanded = expanded && suggestions.isNotEmpty()

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            placeholder = { Text(placeholder) },
            leadingIcon = leadingIcon,
            trailingIcon = {
                IconButton(onClick = onAdd) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        )

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = onDismiss,
            modifier = Modifier.fillMaxWidth(0.92f),
            properties = PopupProperties(focusable = false)
        ) {
            suggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(suggestion.name)
                            Text(
                                text = if (suggestion.source == SuggestionSource.LOCAL) "Local" else "TMDB",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = { onSuggestionClick(suggestion) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectedPeopleChips(
    items: List<SelectedPerson>,
    onRemove: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            InputChip(
                selected = true,
                onClick = { },
                label = { Text(item.name) },
                trailingIcon = {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove",
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onRemove(item.name) }
                    )
                }
            )
        }
    }
}