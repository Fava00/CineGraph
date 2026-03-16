package com.martonegyed.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.martonegyed.presentation.screens.import.ImportScreen
import com.martonegyed.presentation.screens.movies.AllMoviesScreen

@Composable
fun AppDrawer(
    navigator: Navigator,
    currentScreen: Screen,
    closeDrawer: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = Color(0xFF14181c),
        modifier = Modifier.width(300.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1F2326))
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.Bottom) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = "Logo",
                    tint = Color(0xFF00E054),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Letterboxd Stats",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color.White,
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
            isSelected = currentScreen is AllMoviesScreen,
            onClick = {
                if (currentScreen !is AllMoviesScreen) {
                    closeDrawer()
                    navigator.push(AllMoviesScreen())
                } else {
                    closeDrawer()
                }
            }
        )
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
                tint = if (isSelected) Color(0xFF00E054) else Color.Gray
            )
        },
        label = {
            Text(
                text = title,
                color = if (isSelected) Color.Green else Color.Gray,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        },
        selected = isSelected,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = Color.White.copy(alpha = 0.05f),
            unselectedContainerColor = Color.Transparent
        ),
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
    )
}