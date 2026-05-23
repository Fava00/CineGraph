package com.martonegyed.presentation.screens.movies

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.martonegyed.core.util.toListDisplayModel
import com.martonegyed.core.ui.adaptive.AdaptiveLayout
import com.martonegyed.presentation.analytics.StatRange
import com.martonegyed.presentation.components.common.AppDrawer
import com.martonegyed.presentation.components.common.cards.MovieCard
import com.martonegyed.presentation.components.common.MovieListItem
import com.martonegyed.presentation.screens.details.MovieDetailScreen
import com.martonegyed.presentation.screens.statistics.StatEntityType
import kotlinx.coroutines.launch
import kotlin.math.floor

data class MovieCollectionScreen(
    val type: CollectionType = CollectionType.LIBRARY,
    val entityType: StatEntityType? = null,
    val entityName: String? = null,
    val range: StatRange? = null,
    val year: Int? = null,
    val month: Int? = null,
    val decadeStart: Int? = null,
    val rating: Double? = null,
    val firstName: String? = null,
    val secondName: String? = null,
    val firstJob: String? = null,
    val secondJob: String? = null
) : Screen {
    override val key: String =
        "MovieCollectionScreen_${type.name}_" +
                (entityType?.name ?: "none") + "_" +
                (entityName ?: "none") + "_" +
                (range?.name ?: "ALL") + "_" +
                (year ?: "all") + "_" +
                (month ?: "all")

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val colors = MaterialTheme.colorScheme
        val screenModel = koinScreenModel<MovieCollectionScreenModel>()
        val isLoading by screenModel.isLoading.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val displayedMovies by screenModel.displayedMovies.collectAsState()
        val isGrid by screenModel.isGrid.collectAsState()
        val searchQuery by screenModel.searchQuery.collectAsState()
        val currentSort by screenModel.currentSort.collectAsState()
        val isAscending by screenModel.isAscending.collectAsState()
        val currentListType by screenModel.currentListType.collectAsState()


        LaunchedEffect(
            type, entityType, entityName, range, year, month,
            decadeStart, rating, firstName, secondName, firstJob, secondJob
        ) {
            when (type) {
                CollectionType.BY_ENTITY if entityType != null &&
                        entityName != null &&
                        range != null -> {
                    screenModel.initCollectionForEntity(
                        entityType = entityType,
                        entityName = entityName,
                        range = range,
                        year = year,
                        month = month
                    )
                }

                CollectionType.BY_DECADE if decadeStart != null -> {
                    screenModel.initCollectionForDecade(
                        decadeStart = decadeStart,
                        range = range ?: StatRange.ALL_TIME,
                        year = year,
                        month = month
                    )
                }

                CollectionType.BY_RATING if rating != null -> {
                    screenModel.initCollectionForRating(
                        rating = rating,
                        range = range ?: StatRange.ALL_TIME,
                        year = year,
                        month = month
                    )
                }

                CollectionType.BY_DUO if firstName != null &&
                        secondName != null -> {
                    screenModel.initCollectionForDuo(
                        firstName = firstName,
                        secondName = secondName,
                        firstJob = firstJob,
                        secondJob = secondJob,
                        range = range ?: StatRange.ALL_TIME,
                        year = year,
                        month = month
                    )
                }

                else -> screenModel.initCollection(type)
            }
        }

        var isSearching by remember { mutableStateOf(false) }
        var showSortSheet by remember { mutableStateOf(false) }

        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        fun rangeLabel(range: StatRange?, year: Int?, month: Int?): String {
            val monthNames = listOf(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
            )
            return when (range) {
                StatRange.YEAR -> "${year ?: "Year"}."
                StatRange.MONTH -> {
                    val y = year ?: "Year"
                    val m = month?.takeIf { it in 1..12 }?.let { monthNames[it - 1] } ?: "Month"
                    "$y. $m"
                }

                else -> "All time"
            }
        }

        val titlePrefix = when (type) {
            CollectionType.BY_ENTITY
                if entityName != null ->
                entityName

            CollectionType.BY_DUO
                if firstName != null && secondName != null ->
                "$firstName & $secondName"

            CollectionType.BY_DECADE
                if decadeStart != null ->
                "${decadeStart}s"

            CollectionType.BY_RATING
                if rating != null ->
                "★ $rating"

            else -> null
        }

        val titleText = if (titlePrefix != null) {
            "$titlePrefix - ${rangeLabel(range, year, month)}"
        } else {
            type.title
        }

        if (isLoading && displayedMovies.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colors.inversePrimary)
            }
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    AppDrawer(
                        navigator = navigator,
                        currentScreen = this,
                        closeDrawer = { scope.launch { drawerState.close() } })
                }
            ) {

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                if (isSearching) {
                                    TextField(
                                        value = searchQuery,
                                        onValueChange = { screenModel.updateSearchQuery(it) },
                                        placeholder = {
                                            Text(
                                                "Search movies...",
                                                color = colors.onSurfaceVariant,
                                                maxLines = 2
                                            )
                                        },
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent
                                        ),
                                        singleLine = true
                                    )
                                } else {
                                    Text(titleText, maxLines = 2)
                                }
                            },
                            navigationIcon = {
                                if (type == CollectionType.LIBRARY || type == CollectionType.WATCHLIST) {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                                    }
                                } else {
                                    IconButton(onClick = { navigator.pop() }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                    }
                                }
                            },
                            actions = {
                                IconButton(onClick = {
                                    isSearching = !isSearching
                                    if (!isSearching) screenModel.updateSearchQuery("")
                                }) {
                                    Icon(if (isSearching) Icons.Default.Close else Icons.Default.Search, null)
                                }
                                IconButton(onClick = { showSortSheet = true }) {
                                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                                }
                                IconButton(onClick = { screenModel.toggleGrid() }) {
                                    Icon(
                                        if (isGrid) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                                        null
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background)
                        )
                    }
                ) { padding ->
                    AdaptiveLayout(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .background(Color(0xFF14181c))
                    ) { adaptive ->

                        val scaffoldTokens = adaptive.tokens.scaffold
                        val collectionTokens = adaptive.tokens.movieCollection
                        val listPosterWidth = when {
                            adaptive.window.isExpanded -> 78.dp
                            adaptive.window.isMedium -> 70.dp
                            else -> 64.dp
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = scaffoldTokens.maxCenteredContentWidth)
                        ) {
                            if (type == CollectionType.BY_ENTITY) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.25f))
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Show",
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = collectionTokens.metaFontSize
                                        )
                                    )

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        FilterChip(
                                            selected = currentListType == MovieListType.WATCHED,
                                            onClick = { screenModel.switchListType(MovieListType.WATCHED) },
                                            label = { Text("Watched") }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        FilterChip(
                                            selected = currentListType == MovieListType.WATCHLIST,
                                            onClick = { screenModel.switchListType(MovieListType.WATCHLIST) },
                                            label = { Text("Watchlist") }
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.2f))
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "${displayedMovies.size} movies",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = collectionTokens.countFontSize
                                    )
                                )
                            }

                            if (isGrid) {
                                BoxWithConstraints(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    val columns = if (adaptive.window.isCompact) 3 else floor(
                                        maxWidth / (collectionTokens.minGridItemWidth + collectionTokens.gridSpacing)
                                    ).toInt().coerceAtLeast(3)

                                    val totalSpacing = collectionTokens.gridSpacing * (columns - 1)
                                    val availableWidth = maxWidth - totalSpacing
                                    val itemWidth = availableWidth / columns

                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(columns),
                                        contentPadding = PaddingValues(
                                            horizontal = 0.dp,
                                            vertical = collectionTokens.gridSpacing
                                        ),
                                        horizontalArrangement = Arrangement.spacedBy(
                                            collectionTokens.gridSpacing,
                                            Alignment.CenterHorizontally
                                        ),
                                        verticalArrangement = Arrangement.spacedBy(collectionTokens.gridSpacing),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(displayedMovies) { row ->
                                            val item = row.toListDisplayModel(currentListType)
                                            val movie = row.toMovie(
                                                preferWatchlistDate = currentListType == MovieListType.WATCHLIST
                                            )

                                            Box(
                                                modifier = Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.TopCenter
                                            ) {
                                                MovieCard(
                                                    item = item,
                                                    posterMaxWidth = itemWidth - 16.dp,
                                                    titleFontSize = collectionTokens.titleFontSize,
                                                    metaFontSize = collectionTokens.metaFontSize,
                                                    onTap = { navigator.push(MovieDetailScreen(movie)) }
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(
                                        horizontal = scaffoldTokens.horizontalPadding,
                                        vertical = collectionTokens.gridSpacing
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(displayedMovies) { row ->
                                        val item = row.toListDisplayModel(currentListType)
                                        val movie = row.toMovie(
                                            preferWatchlistDate = currentListType == MovieListType.WATCHLIST
                                        )
                                        MovieListItem(
                                            item = item,
                                            posterWidth = listPosterWidth,
                                            titleFontSize = when {
                                                adaptive.window.isExpanded -> 18.sp
                                                adaptive.window.isMedium -> 17.sp
                                                else -> 16.sp
                                            },
                                            metaFontSize = collectionTokens.metaFontSize,
                                            supportingFontSize = collectionTokens.countFontSize,
                                            onTap = {
                                                navigator.push(MovieDetailScreen(movie))
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        if (showSortSheet) {
                            val sheetState = rememberModalBottomSheetState(
                                skipPartiallyExpanded = true,
                            )
                            ModalBottomSheet(
                                sheetState = sheetState,
                                onDismissRequest = { showSortSheet = false },
                                containerColor = colors.secondary,
                                dragHandle = {
                                    BottomSheetDefaults.DragHandle(color = colors.onSurfaceVariant)
                                },
                                modifier = Modifier.padding(
                                    horizontal = if (adaptive.window.isCompact) 30.dp else 0.dp
                                )
                            ) {
                                Surface(
                                    color = colors.secondary,
                                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 420.dp),
                                        contentPadding = PaddingValues(bottom = 32.dp)
                                    ) {
                                        item {
                                            Text(
                                                text = "Sort By",
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.onSecondary
                                                ),
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                            )
                                        }

                                        item {
                                            HorizontalDivider(color = colors.onSecondary.copy(alpha = 0.2f))
                                        }

                                        item {
                                            SortOptionItem("Date Watched", SortOption.DATE_WATCHED, currentSort) {
                                                screenModel.updateSort(SortOption.DATE_WATCHED)
                                            }
                                        }
                                        item {
                                            SortOptionItem("Release Year", SortOption.RELEASE_YEAR, currentSort) {
                                                screenModel.updateSort(SortOption.RELEASE_YEAR)
                                            }
                                        }
                                        item {
                                            SortOptionItem("Rating", SortOption.RATING, currentSort) {
                                                screenModel.updateSort(SortOption.RATING)
                                            }
                                        }
                                        item {
                                            SortOptionItem("Name", SortOption.NAME, currentSort) {
                                                screenModel.updateSort(SortOption.NAME)
                                            }
                                        }

                                        item {
                                            HorizontalDivider(
                                                color = colors.onSecondary.copy(alpha = 0.2f),
                                                modifier = Modifier.padding(vertical = 8.dp)
                                            )
                                        }

                                        item {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 32.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    "Ascending Order",
                                                    color = colors.onSecondary,
                                                    fontSize = 16.sp
                                                )

                                                Switch(
                                                    checked = isAscending,
                                                    onCheckedChange = { checked ->
                                                        screenModel.toggleAscending(checked)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }


            }
        }


    }

    @Composable
    private fun SortOptionItem(label: String, option: SortOption, currentSort: SortOption, onClick: () -> Unit) {
        val isSelected = currentSort == option
        val colors = MaterialTheme.colorScheme
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) colors.background else colors.onSecondary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                color = if (isSelected) colors.onBackground else colors.onBackground.copy(alpha = 0.7f),
                fontSize = 16.sp
            )
        }
    }
}