package com.martonegyed.presentation.screens.movies

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
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
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.martonegyed.presentation.components.AppDrawer
import com.martonegyed.presentation.components.MovieCard
import com.martonegyed.presentation.screens.details.MovieDetailScreen
import kotlinx.coroutines.launch

class AllMoviesScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = getScreenModel<AllMoviesScreenModel>()

        val displayedMovies by screenModel.displayedMovies.collectAsState()
        val isGrid by screenModel.isGrid.collectAsState()
        val searchQuery by screenModel.searchQuery.collectAsState()
        val currentSort by screenModel.currentSort.collectAsState()
        val isAscending by screenModel.isAscending.collectAsState()

        var isSearching by remember { mutableStateOf(false) }
        var showSortSheet by remember { mutableStateOf(false) }

        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                AppDrawer(navigator = navigator, currentScreen = this, closeDrawer = { scope.launch { drawerState.close() } })
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
                                    placeholder = { Text("Search movies...", color = Color.Gray) },
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    singleLine = true
                                )
                            } else {
                                Text("My Library")
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
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
                                Icon(Icons.Default.Sort, contentDescription = "Sort")
                            }
                            IconButton(onClick = { screenModel.toggleGrid() }) {
                                Icon(if (isGrid) Icons.Default.ViewList else Icons.Default.GridView, null)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF14181c))
                    )
                }
            ) { padding ->
                Column(modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFF14181c))) {
                    Box(modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.2f)).padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text("${displayedMovies.size} movies", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
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
                        // TODO: List view
                    }
                }
            }

            if (showSortSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showSortSheet = false },
                    containerColor = Color(0xFF2c3440),
                    dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
                    ) {
                        Text(
                            text = "Sort By",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.White),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

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

                        HorizontalDivider(color = Color.White.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Ascending Order", color = Color.White, fontSize = 16.sp)
                            Switch(
                                checked = isAscending,
                                onCheckedChange = {
                                    screenModel.toggleAscending(it)
                                    showSortSheet = false
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E054), checkedTrackColor = Color(0xFF00E054).copy(alpha = 0.5f))
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SortOptionItem(label: String, option: SortOption, currentSort: SortOption, onClick: () -> Unit) {
        val isSelected = currentSort == option
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) Color(0xFF00E054) else Color.Gray
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp
            )
        }
    }
}