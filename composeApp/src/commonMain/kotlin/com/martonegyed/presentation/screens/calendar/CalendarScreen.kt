package com.martonegyed.presentation.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.martonegyed.core.ui.adaptive.AdaptiveLayout
import com.martonegyed.presentation.components.common.AppDrawer
import com.martonegyed.presentation.screens.details.MovieDetailScreen
import kotlinx.coroutines.launch

class CalendarScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val screenModel = koinScreenModel<CalendarScreenModel>()
        val uiState by screenModel.state.collectAsState()
        val colors = MaterialTheme.colorScheme


        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                AppDrawer(
                    navigator = navigator,
                    currentScreen = this@CalendarScreen,
                    closeDrawer = { scope.launch { drawerState.close() } }
                )
            }
        ) {
            Scaffold(
                containerColor = colors.background,
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Cinema Calendar",
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = colors.surface
                        )
                    )
                }
            ) { padding ->
                AdaptiveLayout(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(colors.background)
                ) { adaptive ->

                    val showTwoPane = adaptive.window.isExpanded
                    val calendarGridMaxWidth = when {
                        adaptive.window.isExpanded -> 560.dp
                        adaptive.window.isMedium -> 520.dp
                        else -> 380.dp
                    }

                    val scaffold = adaptive.tokens.scaffold
                    val contentMaxWidth = when {
                        adaptive.window.isExpanded -> 1200.dp
                        adaptive.window.isMedium -> 920.dp
                        else -> scaffold.maxCenteredContentWidth
                    }

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        if (uiState.isLoading) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                            return@AdaptiveLayout
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = contentMaxWidth),
                            contentPadding = PaddingValues(
                                horizontal = if (adaptive.window.isCompact) 8.dp else scaffold.horizontalPadding,
                                vertical = if (adaptive.window.isCompact) 8.dp else scaffold.verticalPadding
                            ),
                            verticalArrangement = Arrangement.spacedBy(
                                if (adaptive.window.isCompact) 10.dp else scaffold.sectionSpacing
                            )
                        ) {
                            item {
                                MonthHeaderCard(
                                    monthLabel = uiState.monthLabel,
                                    monthMovieCount = uiState.monthMovieCount,
                                    compact = !showTwoPane,
                                    onPrevious = screenModel::goToPreviousMonth,
                                    onNext = screenModel::goToNextMonth
                                )
                            }

                            item {
                                if (showTwoPane) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {


                                        Box(
                                            modifier = Modifier.width(calendarGridMaxWidth)
                                        ) {
                                            Column {
                                                WeekdayHeader()
                                                CalendarMonthGrid(
                                                    days = uiState.days,
                                                    onDayClick = screenModel::selectDate,
                                                    compact = false
                                                )
                                            }
                                        }


                                        Box(
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            SelectedDaySection(
                                                selectedDateLabel = uiState.selectedDateLabel,
                                                movies = uiState.selectedMovies,
                                                onMovieClick = { navigator.push(MovieDetailScreen(it.toMovie())) },
                                                compact = false
                                            )
                                        }
                                    }
                                } else {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .widthIn(max = calendarGridMaxWidth)
                                                .align(Alignment.CenterHorizontally)
                                        ) {
                                            CalendarMonthGrid(
                                                days = uiState.days,
                                                onDayClick = screenModel::selectDate,
                                                compact = adaptive.window.isCompact
                                            )
                                        }

                                        SelectedDaySection(
                                            selectedDateLabel = uiState.selectedDateLabel,
                                            movies = uiState.selectedMovies,
                                            onMovieClick = { navigator.push(MovieDetailScreen(it.toMovie())) },
                                            compact = adaptive.window.isCompact
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

@Composable
private fun MonthHeaderCard(
    monthLabel: String,
    monthMovieCount: Int,
    compact: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (compact) 16.dp else 24.dp),
        color = colors.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (compact) 8.dp else 18.dp,
                    vertical = if (compact) 8.dp else 16.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPrevious,
                modifier = Modifier.size(if (compact) 32.dp else 40.dp)
            ) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = "Previous month",
                    modifier = Modifier.size(if (compact) 18.dp else 24.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = if (compact) Alignment.Start else Alignment.CenterHorizontally
            ) {
                Text(
                    text = monthLabel,
                    style = if (compact) {
                        MaterialTheme.typography.titleMedium
                    } else {
                        MaterialTheme.typography.titleLarge
                    },
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )

                Text(
                    text = "$monthMovieCount watched",
                    color = colors.onSurfaceVariant,
                    style = if (compact) {
                        MaterialTheme.typography.bodySmall
                    } else {
                        MaterialTheme.typography.bodyMedium
                    },
                    maxLines = 1
                )
            }

            IconButton(
                onClick = onNext,
                modifier = Modifier.size(if (compact) 32.dp else 40.dp)
            ) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Next month",
                    modifier = Modifier.size(if (compact) 18.dp else 24.dp)
                )
            }
        }
    }
}

@Composable
private fun WeekdayHeader() {
    val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        labels.forEach { label ->
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = colors.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun CalendarMonthGrid(
    days: List<CalendarDayCell>,
    onDayClick: (String) -> Unit,
    compact: Boolean
) {
    val spacing = if (compact) 4.dp else 6.dp

    Column(
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) {
        days.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                week.forEach { day ->
                    CalendarDayCard(
                        day = day,
                        compact = compact,
                        modifier = Modifier.weight(1f),
                        onClick = { day.dateKey?.let(onDayClick) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCard(
    day: CalendarDayCell,
    compact: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val imageUrl = day.posterPath?.let { "https://image.tmdb.org/t/p/w200$it" }

    val corner = if (compact) 12.dp else 16.dp
    val innerPadding = if (compact) 6.dp else 8.dp
    val dayFont = if (compact) 11.sp else 13.sp
    val badgeFont = if (compact) 9.sp else 10.sp
    val ratio = 0.66f

    val dayNumberColor = when {
        !day.isCurrentMonth -> colors.onSurfaceVariant.copy(alpha = 0.35f)
        day.isSelected -> Color.Black
        day.isWeekend -> colors.tertiary
        day.movieCount > 0 -> Color.White
        else -> colors.onSurface
    }

    val badgeContainerColor = when {
        day.isSelected -> colors.primaryContainer.copy(alpha = 0.5f)
        day.movieCount > 0 -> Color.Black.copy(alpha = 0.62f)
        else -> colors.surfaceVariant.copy(alpha = 0.75f)
    }

    val badgeTextColor = when {
        day.isSelected -> Color.Black
        !day.isSelected && day.movieCount > 0 -> Color.White
        else -> colors.onSurfaceVariant
    }

    Surface(
        modifier = modifier
            .aspectRatio(ratio)
            .clip(RoundedCornerShape(corner))
            .clickable(enabled = day.dateKey != null, onClick = onClick),
        shape = RoundedCornerShape(corner),
        color = when {
            !day.isCurrentMonth -> colors.surfaceVariant.copy(alpha = 0.18f)
            day.isSelected -> colors.primaryContainer.copy(alpha = 0.6f)
            else -> colors.surface
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (imageUrl != null && day.isCurrentMonth) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = day.dateKey,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (day.isSelected) colors.primaryContainer.copy(alpha = 0.3f)
                            else Color.Black.copy(alpha = 0.34f)
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = day.dayLabel,
                    color = dayNumberColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = dayFont
                )

                if (day.movieCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = badgeContainerColor
                    ) {
                        Text(
                            text = if (day.movieCount == 1) "1" else day.movieCount.toString(),
                            color = badgeTextColor,
                            fontSize = badgeFont,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(
                                horizontal = if (compact) 6.dp else 8.dp,
                                vertical = if (compact) 2.dp else 4.dp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedDaySection(
    selectedDateLabel: String?,
    movies: List<CalendarMovieItem>,
    onMovieClick: (CalendarMovieItem) -> Unit,
    compact: Boolean
) {
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (compact) 18.dp else 24.dp),
        color = colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (compact) 12.dp else 18.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp)
        ) {
            Text(
                text = selectedDateLabel ?: "Select a day",
                style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            if (movies.isEmpty()) {
                Text(
                    text = "No watched movies for the selected day.",
                    color = colors.onSurfaceVariant,
                    style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium
                )
            } else {
                movies.forEach { movie ->
                    CalendarMovieRow(
                        movie = movie,
                        compact = compact,
                        onClick = { onMovieClick(movie) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarMovieRow(
    movie: CalendarMovieItem,
    compact: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val imageUrl = movie.posterPath?.let { "https://image.tmdb.org/t/p/w200$it" }

    val posterWidth = if (compact) 40.dp else 54.dp
    val posterHeight = if (compact) 60.dp else 80.dp

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(if (compact) 14.dp else 18.dp),
        color = colors.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (compact) 8.dp else 12.dp),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(posterWidth)
                    .height(posterHeight)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = movie.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = movie.year.toString(),
                        color = colors.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = movie.name,
                    color = colors.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = if (compact) 13.sp else 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = movie.year.toString(),
                    color = colors.onSurfaceVariant,
                    fontSize = if (compact) 11.sp else 13.sp
                )

                movie.rating?.let {
                    Text(
                        text = "Your rating: $it",
                        color = colors.primary,
                        fontSize = if (compact) 11.sp else 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}