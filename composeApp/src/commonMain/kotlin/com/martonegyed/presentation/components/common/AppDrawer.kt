package com.martonegyed.presentation.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.Navigator
import com.martonegyed.presentation.screens.collabSearch.CollabSearchScreen
import com.martonegyed.presentation.screens.calendar.CalendarScreen
import com.martonegyed.presentation.screens.import.ImportScreen
import com.martonegyed.presentation.screens.insights.InsightsScreen
import com.martonegyed.presentation.screens.moviePicker.MoviePickerScreen
import com.martonegyed.presentation.screens.movies.CollectionType
import com.martonegyed.presentation.screens.movies.MovieCollectionScreen
import com.martonegyed.presentation.screens.randompicker.RandomPickerScreen
import com.martonegyed.presentation.screens.statistics.StatisticsScreen
import com.martonegyed.presentation.screens.yearinreview.YearInReviewScreen

@Composable
fun AppDrawer(
    navigator: Navigator,
    currentScreen: Screen,
    closeDrawer: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.width(300.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.Bottom) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = "Logo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Cinegraph",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            DrawerItem(
                icon = Icons.Default.UploadFile,
                title = "Import Data",
                isSelected = currentScreen is ImportScreen,
                onClick = {
                    if (currentScreen !is ImportScreen) {

                        closeDrawer()
                        navigator.push(ImportScreen())
                    } else {
                        closeDrawer()
                    }
                }
            )

            DrawerItem(
                icon = Icons.Default.Movie,
                title = "My Library",
                isSelected = (currentScreen as? MovieCollectionScreen)?.type == CollectionType.LIBRARY,
                onClick = {
                    if ((currentScreen as? MovieCollectionScreen)?.type != CollectionType.LIBRARY) {
                        closeDrawer()
                        navigator.replaceAll(MovieCollectionScreen(CollectionType.LIBRARY))
                    } else {
                        closeDrawer()
                    }
                }
            )

            DrawerItem(
                icon = Icons.Default.Bookmark,
                title = "Watchlist",
                isSelected = (currentScreen as? MovieCollectionScreen)?.type == CollectionType.WATCHLIST,
                onClick = {
                    if ((currentScreen as? MovieCollectionScreen)?.type != CollectionType.WATCHLIST) {
                        closeDrawer()
                        navigator.replaceAll(MovieCollectionScreen(CollectionType.WATCHLIST))
                    } else {
                        closeDrawer()
                    }
                }
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )

            DrawerItem(
                Icons.Default.PieChart, "Statistics",
                isSelected = currentScreen is StatisticsScreen
            ) {
                navigateTo(navigator, closeDrawer, currentScreen, StatisticsScreen())
            }

            DrawerItem(
                Icons.Default.Insights, "Insights",
                isSelected = currentScreen is InsightsScreen,
            ) {
                navigateTo(navigator, closeDrawer, currentScreen, InsightsScreen())
                closeDrawer()
            }

            DrawerItem(
                Icons.Default.CalendarMonth, "Year in Review",
                isSelected = currentScreen is YearInReviewScreen,
            ) {
                navigateTo(navigator, closeDrawer, currentScreen, YearInReviewScreen())
                closeDrawer()
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )

            DrawerItem(
                Icons.Default.Shuffle, "Random Picker",
                isSelected = currentScreen is RandomPickerScreen,
            ) {
                navigateTo(navigator, closeDrawer, currentScreen, RandomPickerScreen())

                closeDrawer()
            }

            DrawerItem(
                Icons.Default.Search, "Movie Picker",
                isSelected = currentScreen is MoviePickerScreen,
            ) {
                navigateTo(navigator, closeDrawer, currentScreen, MoviePickerScreen())

                closeDrawer()
            }

            DrawerItem(
                Icons.Default.Event, "Cinema Calendar",
                isSelected = false
            ) {
                navigateTo(navigator, closeDrawer, currentScreen, CalendarScreen())
                closeDrawer()
            }

            DrawerItem(
                Icons.Default.Event, "Collab Search",
                isSelected = false
            ) {
                navigateTo(navigator, closeDrawer, currentScreen, CollabSearchScreen())
                closeDrawer()
            }
        }


    }
}

private fun navigateTo(
    navigator: Navigator,
    closeDrawer: () -> Unit,
    currentScreen: Screen,
    target: Screen
) {
    closeDrawer()
    if (currentScreen::class != target::class) {
        navigator.replaceAll(target)
    }
}

@Composable
private fun DrawerItem(
    icon: ImageVector,
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        label = {
            Text(
                text = title,
                color = if (isSelected) Color.Green else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        },
        selected = isSelected,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
            unselectedContainerColor = Color.Transparent
        ),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
    )
}