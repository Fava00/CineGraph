package com.martonegyed.presentation.screens.movies

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import com.martonegyed.presentation.components.common.AppDrawer
import com.martonegyed.presentation.components.common.MovieCard
import com.martonegyed.presentation.components.common.MovieListItem
import com.martonegyed.presentation.screens.details.MovieDetailScreen
import com.martonegyed.presentation.screens.statistics.StatEntityType
import com.martonegyed.presentation.screens.statistics.StatRange
import kotlinx.coroutines.launch

data class MovieCollectionScreen(
    val type: CollectionType = CollectionType.LIBRARY,
    val entityType: StatEntityType? = null,
    val entityName: String? = null,
    val range: StatRange? = null,
    val year: Int? = null,
    val month: Int? = null
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


        LaunchedEffect(type, entityType, entityName, range, year, month) {
            if (type == CollectionType.BY_ENTITY &&
                entityType != null && entityName != null && range != null
            ) {
                screenModel.initCollectionForEntity(
                    entityType = entityType,
                    entityName = entityName,
                    range = range,
                    year = year,
                    month = month
                )
            } else {
                screenModel.initCollection(type)
            }
        }

        var isSearching by remember { mutableStateOf(false) }
        var showSortSheet by remember { mutableStateOf(false) }

        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        val titleText = when {
            type == CollectionType.BY_ENTITY && entityName != null -> {
                val rangeLabel = when (range) {
                    StatRange.ALL_TIME, null -> "All time"
                    StatRange.YEAR -> year?.toString() ?: "Year"
                    StatRange.MONTH -> {
                        val y = year?.toString() ?: "Year"
                        val m = month?.toString() ?: "Month"
                        "$y · $m"
                    }
                }
                "$entityName · $rangeLabel"
            }

            else -> type.title
        }
        if (isLoading && displayedMovies.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colors.scrim)
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
                                        placeholder = { Text("Search movies...", color = colors.onSurfaceVariant) },
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent
                                        ),
                                        singleLine = true
                                    )
                                } else {
                                    Text(titleText)
                                }
                            },
                            navigationIcon = {
                                if (type == CollectionType.BY_ENTITY) {
                                    IconButton(onClick = { navigator.pop() }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                    }
                                } else {
                                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                        Icon(Icons.Default.Menu, contentDescription = "Menu")
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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .background(colors.background.copy(blue = 0.15f)),
                    ) {
                        if (type == CollectionType.BY_ENTITY) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.background.copy(alpha = 0.25f))
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Show",
                                    color = colors.onBackground,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Row {
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
                                .background(colors.background.copy(alpha = 0.2f))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                "${displayedMovies.size} movies",
                                color = colors.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        if (isGrid) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                contentPadding = PaddingValues(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(displayedMovies) { movie ->
                                    MovieCard(
                                        movie = movie,
                                        onTap = { navigator.push(MovieDetailScreen(movie)) }
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(displayedMovies) { movie ->
                                    MovieListItem(
                                        movie = movie,
                                        onTap = { navigator.push(MovieDetailScreen(movie)) }
                                    )
                                }
                            }
                        }
                    }
                }

                if (showSortSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showSortSheet = false },
                        containerColor = colors.onSurfaceVariant,
                        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.onSurfaceVariant) }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
                        ) {
                            Text(
                                text = "Sort By",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = colors.onBackground
                                ),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            HorizontalDivider(color = colors.onBackground.copy(alpha = 0.2f))

                            SortOptionItem("Date Watched", SortOption.DATE_WATCHED, currentSort) {
                                screenModel.updateSort(SortOption.DATE_WATCHED)
                                showSortSheet = false
                            }
                            SortOptionItem("Release Year", SortOption.RELEASE_YEAR, currentSort) {
                                screenModel.updateSort(SortOption.RELEASE_YEAR)
                                showSortSheet = false
                            }
                            SortOptionItem("Rating", SortOption.RATING, currentSort) {
                                screenModel.updateSort(SortOption.RATING)
                                showSortSheet = false
                            }
                            SortOptionItem("Name", SortOption.NAME, currentSort) {
                                screenModel.updateSort(SortOption.NAME)
                                showSortSheet = false
                            }

                            HorizontalDivider(
                                color = colors.onBackground.copy(alpha = 0.2f),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Ascending Order", color = colors.onBackground, fontSize = 16.sp)
                                Switch(
                                    checked = isAscending,
                                    onCheckedChange = {
                                        screenModel.toggleAscending(it)
                                        showSortSheet = false
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = colors.scrim,
                                        checkedTrackColor = colors.scrim.copy(alpha = 0.5f)
                                    )
                                )
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
                tint = if (isSelected) colors.scrim else colors.onSurfaceVariant
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