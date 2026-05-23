package com.martonegyed.presentation.screens.moviePicker

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.martonegyed.core.ui.adaptive.AdaptiveLayout
import com.martonegyed.core.util.roundToDecimals
import com.martonegyed.data.remote.TmdbGenre
import kotlin.math.roundToInt

data class MoviePickerScreen(
    val showBackButton: Boolean = false
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val colors = MaterialTheme.colorScheme
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<MoviePickerScreenModel>()
        val uiState by screenModel.uiState.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("Movie Picker") },
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
                    actions = {
                        IconButton(
                            onClick = {
                                navigator.push(DiscoveryManagerPlaceholderScreen())
                            }
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = "Discovery Manager")
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
                            onClick = {
                                navigator.push(MoviePickerResultsScreen(screenModel.buildRequest()))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00E054),
                                contentColor = Color.Black
                            )
                        ) {
                            Text("Find Movies", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        ) { padding ->
            AdaptiveLayout(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFF14181C))
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
                    item { SectionTitle("Search source") }
                    item {
                        SearchSourceChooser(
                            selected = uiState.source,
                            onSelected = screenModel::setSource
                        )
                    }

                    if (uiState.needsSearchDepth) {
                        item { SectionTitle("Search depth") }
                        item {
                            SearchDepthSection(
                                searchDepth = uiState.searchDepth,
                                onDepthChange = screenModel::setSearchDepth
                            )
                        }
                    }

                    item { Divider(colors) }
                    item { SectionTitle("I want to watch…") }
                    item {
                        WatchIntentChooser(
                            selected = uiState.watchIntent,
                            onSelected = screenModel::setWatchIntent
                        )
                    }

                    item { Divider(colors) }
                    item { SectionTitle("Genres") }
                    item {
                        if (uiState.isLoadingMeta && uiState.availableGenres.isEmpty()) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        } else {
                            GenreChooser(
                                genres = uiState.availableGenres,
                                states = uiState.genreStates,
                                onGenreTap = screenModel::cycleGenre
                            )
                        }
                    }

                    item { Divider(colors) }
                    item { SectionTitle("Runtime") }
                    item {
                        RuntimeChooser(
                            range = uiState.runtimeRange,
                            onRangeChange = screenModel::updateRuntime
                        )
                    }

                    item { Divider(colors) }
                    item { SectionTitle("Minimum rating") }
                    item {
                        MinimumRatingChooser(
                            rating = uiState.minimumRating,
                            onRatingChange = screenModel::updateMinimumRating
                        )
                    }

                    item { Divider(colors) }
                    item { SectionTitle("Decades") }
                    item {
                        DecadesChooser(
                            decades = uiState.decades,
                            selectedDecades = uiState.selectedDecades,
                            onDecadeTap = screenModel::toggleDecade
                        )
                    }

                    item { Divider(colors) }
                    item { SectionTitle("Languages") }
                    item {
                        MultiSelectDropdownSection(
                            label = "Languages",
                            options = uiState.availableLanguages,
                            selectedValues = uiState.selectedLanguages,
                            onToggle = screenModel::toggleLanguage
                        )
                    }

                    item { Divider(colors) }
                    item { SectionTitle("Production countries") }
                    item {
                        MultiSelectDropdownSection(
                            label = "Production countries",
                            options = uiState.availableCountries,
                            selectedValues = uiState.selectedCountries,
                            onToggle = screenModel::toggleCountry
                        )
                    }

                    item {
                        Text(
                            text = "TMDb-side filtering should use original language and production country filters first, then local filtering should re-check cached and library items once full movie details are present.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
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

                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@Composable
private fun Divider(colors: androidx.compose.material3.ColorScheme) {
    HorizontalDivider(color = colors.onSurface.copy(alpha = 0.12f))
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun SearchSourceChooser(
    selected: MoviePickerSearchSource,
    onSelected: (MoviePickerSearchSource) -> Unit
) {
    val options = listOf(
        MoviePickerSearchSource.MY_LIBRARY to "My Library",
        MoviePickerSearchSource.DISCOVER_NEW to "Discover New",
        MoviePickerSearchSource.BOTH to "Both"
    )

    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = selected == value,
                onClick = { onSelected(value) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun WatchIntentChooser(
    selected: MoviePickerWatchIntent,
    onSelected: (MoviePickerWatchIntent) -> Unit
) {
    val options = listOf(
        MoviePickerWatchIntent.SOMETHING_NEW to "Something new",
        MoviePickerWatchIntent.REWATCH to "A rewatch",
        MoviePickerWatchIntent.ANYTHING to "Anything"
    )

    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = selected == value,
                onClick = { onSelected(value) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                label = { Text(label) }
            )
        }
    }
}

@Composable
private fun SearchDepthSection(
    searchDepth: Int,
    onDepthChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "How many remote candidates to pull",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = searchDepth.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Slider(
            value = searchDepth.toFloat(),
            onValueChange = { onDepthChange(it.roundToInt()) },
            valueRange = 20f..500f,
            steps = 23
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GenreChooser(
    genres: List<TmdbGenre>,
    states: Map<Int, TriStateFilter>,
    onGenreTap: (Int) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        genres.forEach { genre ->
            TriStateChip(
                label = genre.name,
                state = states[genre.id] ?: TriStateFilter.NEUTRAL,
                onClick = { onGenreTap(genre.id) }
            )
        }
    }
}

@Composable
private fun RuntimeChooser(
    range: ClosedFloatingPointRange<Float>,
    onRangeChange: (ClosedFloatingPointRange<Float>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatRuntime(range.start.toInt()))
            Text(formatRuntime(range.endInclusive.toInt()))
        }

        RangeSlider(
            value = range,
            onValueChange = onRangeChange,
            valueRange = 0f..240f
        )
    }
}

@Composable
private fun MinimumRatingChooser(
    rating: Float,
    onRatingChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "TMDb minimum",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = rating.roundToDecimals(2).toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Slider(
            value = rating,
            onValueChange = onRatingChange,
            valueRange = 0f..10f,
            steps = 19
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DecadesChooser(
    decades: List<DecadeOption>,
    selectedDecades: Set<Int>,
    onDecadeTap: (Int) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        decades.forEach { decade ->
            FilterChip(
                selected = decade.startYear in selectedDecades,
                onClick = { onDecadeTap(decade.startYear) },
                label = { Text(decade.label) }
            )
        }
    }
}

@Composable
private fun TriStateChip(
    label: String,
    state: TriStateFilter,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val container = when (state) {
        TriStateFilter.NEUTRAL -> colors.surfaceVariant
        TriStateFilter.INCLUDE -> colors.primaryContainer
        TriStateFilter.EXCLUDE -> colors.errorContainer
    }
    val content = when (state) {
        TriStateFilter.NEUTRAL -> colors.onSurfaceVariant
        TriStateFilter.INCLUDE -> colors.onPrimaryContainer
        TriStateFilter.EXCLUDE -> colors.onErrorContainer
    }
    val displayLabel = when (state) {
        TriStateFilter.NEUTRAL -> label
        TriStateFilter.INCLUDE -> label
        TriStateFilter.EXCLUDE -> "Not $label"
    }

    FilterChip(
        selected = state != TriStateFilter.NEUTRAL,
        onClick = onClick,
        label = { Text(displayLabel) },
        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
            selectedContainerColor = container,
            selectedLabelColor = content,
            containerColor = colors.surfaceVariant,
            labelColor = colors.onSurfaceVariant
        )
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MultiSelectDropdownSection(
    label: String,
    options: List<PickerOption>,
    selectedValues: Set<String>,
    onToggle: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabels = options.filter { it.code in selectedValues }.map { it.label }
    val summary = when {
        selectedLabels.isEmpty() -> "Any"
        selectedLabels.size <= 2 -> selectedLabels.joinToString()
        else -> "${selectedLabels.size} selected"
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = summary,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            trailingIcon = {
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.94f)
        ) {
            options.forEach { option ->
                val selected = option.code in selectedValues
                DropdownMenuItem(
                    text = {
                        Text(if (selected) "✓ ${option.label}" else option.label)
                    },
                    onClick = { onToggle(option.code) }
                )
            }
        }
    }
}

private fun formatRuntime(minutes: Int): String {
    return if (minutes >= 240) "240+ min" else "$minutes min"
}

@OptIn(ExperimentalMaterial3Api::class)
data class DiscoveryManagerPlaceholderScreen(
    val showBackButton: Boolean = true
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Discovery Manager") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text("Discovery Manager screen placeholder")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
data class MoviePickerResultsPlaceholderScreen(
    val request: MoviePickerRequest,
    val showBackButton: Boolean = true
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Movie Picker Results") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFF14181C)),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { Text("Request handoff works", color = Color.White, fontWeight = FontWeight.Bold) }
                item { Text("Source: ${request.source}", color = Color.White) }
                item { Text("Depth: ${request.searchDepth}", color = Color.White) }
                item { Text("Intent: ${request.watchIntent}", color = Color.White) }
                item { Text("Included genres: ${request.includedGenreIds}", color = Color.White) }
                item { Text("Excluded genres: ${request.excludedGenreIds}", color = Color.White) }
                item {
                    Text(
                        "Runtime: ${request.runtimeMinutes.first}-${request.runtimeMinutes.last}",
                        color = Color.White
                    )
                }
                item { Text("Minimum rating: ${request.minimumRating}", color = Color.White) }
                item { Text("Decades: ${request.selectedDecades}", color = Color.White) }
                item { Text("Languages: ${request.languages}", color = Color.White) }
                item { Text("Countries: ${request.productionCountries}", color = Color.White) }
            }
        }
    }
}