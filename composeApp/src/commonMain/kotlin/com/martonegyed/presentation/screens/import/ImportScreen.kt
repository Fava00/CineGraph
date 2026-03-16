package com.martonegyed.presentation.screens.import

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.martonegyed.presentation.components.AppDrawer
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.launch

class ImportScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = getScreenModel<ImportScreenModel>()
        val state by screenModel.state.collectAsState()

        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()


        var selectedTabIndex by remember { mutableStateOf(0) }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                AppDrawer(
                    navigator = navigator,
                    currentScreen = this@ImportScreen,
                    closeDrawer = { scope.launch { drawerState.close() } }
                )
            }
        ) {
            val stagedCount by screenModel.stagedCount.collectAsState()
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Data Management") },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                    )
                },
                bottomBar = {
                    if (selectedTabIndex == 0 && stagedCount > 0 && state is SyncState.Idle) {
                        Surface(
                            color = Color(0xFF1F2326),
                            shadowElevation = 16.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("$stagedCount movies ready", color = Color.White, fontWeight = FontWeight.Bold)
                                    Text("Review your files before updating.", color = Color.Gray, fontSize = 12.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(onClick = { screenModel.clearStaged() }) {
                                        Text("Clear", color = Color.Red)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { screenModel.commitToDatabase() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF00E054),
                                            contentColor = Color.Black
                                        )
                                    ) {
                                        Text("Update DB", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {

                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = Color.Transparent,
                        contentColor = Color(0xFF00E054),
                        indicator = { tabPositions ->
                            if (selectedTabIndex < tabPositions.size) {
                                SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                    color = Color(0xFF00E054)
                                )
                            }
                        }
                    ) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0; screenModel.reset() },
                            text = { Text("Import", fontWeight = FontWeight.Bold) },
                            unselectedContentColor = Color.Gray
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1; screenModel.reset() },
                            text = { Text("Export", fontWeight = FontWeight.Bold) },
                            unselectedContentColor = Color.Gray
                        )
                    }


                    Box(modifier = Modifier.fillMaxSize()) {
                        when (val currentState = state) {
                            is SyncState.Idle -> {
                                if (selectedTabIndex == 0) {
                                    ImportView(screenModel)
                                } else {
                                    ExportView(screenModel)
                                }
                            }
                            is SyncState.Loading -> LoadingView(currentState)
                            is SyncState.Success -> SuccessView(currentState.message, onReset = screenModel::reset)
                            is SyncState.Error -> ErrorView(currentState.error, onReset = screenModel::reset)
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun ImportView(screenModel: ImportScreenModel) {
        val scrollState = rememberScrollState()

        val multiFilePicker = rememberFilePickerLauncher(
            type = PickerType.File(extensions = listOf("csv")),
            mode = PickerMode.Multiple(),
            title = "Select Letterboxd Export Files"
        ) { files ->
            if (files != null && files.isNotEmpty()) {
                screenModel.stageMultipleLetterboxdFiles(files)
            }
        }

        var currentImportType by remember { mutableStateOf(Pair("", "")) }

        val csvPicker = rememberFilePickerLauncher(
            type = PickerType.File(extensions = listOf("csv")),
            mode = PickerMode.Single,
            title = "Select CSV File"
        ) { file ->
            if (file != null) {
                screenModel.stageSingleCsv(file, currentImportType.second, currentImportType.first)
            }
        }

        val jsonPicker = rememberFilePickerLauncher(
            type = PickerType.File(extensions = listOf("json")),
            mode = PickerMode.Single,
            title = "Select Backup JSON"
        ) { file ->
            if (file != null) {
                screenModel.restoreBackup(file)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PlatformCard(title = "Letterboxd", icon = Icons.Default.Movie) {
                Button(
                    onClick = { multiFilePicker.launch() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E054), contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.FolderZip, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import Full Folder", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Or import individual files:", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SmallOutlinedButton("Diary") { currentImportType = "Letterboxd" to "Diary"; csvPicker.launch() }
                    SmallOutlinedButton("Watched") { currentImportType = "Letterboxd" to "Watched"; csvPicker.launch() }
                    SmallOutlinedButton("Watchlist") { currentImportType = "Letterboxd" to "Watchlist"; csvPicker.launch() }
                    SmallOutlinedButton("Ratings") { currentImportType = "Letterboxd" to "Ratings"; csvPicker.launch() }
                    SmallOutlinedButton("Reviews") { currentImportType = "Letterboxd" to "Reviews"; csvPicker.launch() }
                }
            }

            PlatformCard(title = "IMDb", icon = Icons.Default.Star) {
                Text("Import individual CSV files:", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SmallOutlinedButton("Ratings") { currentImportType = "IMDb" to "Ratings"; csvPicker.launch() }
                    SmallOutlinedButton("Watchlist") { currentImportType = "IMDb" to "Watchlist"; csvPicker.launch() }
                    SmallOutlinedButton("Lists") { currentImportType = "IMDb" to "Lists"; csvPicker.launch() }
                }
            }

            PlatformCard(title = "CineGraph Backup", icon = Icons.Default.SettingsBackupRestore) {
                Button(
                    onClick = { jsonPicker.launch() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF40bcf4), contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restore from JSON", fontWeight = FontWeight.Bold)
                }
            }
        }
    }


    @Composable
    private fun ExportView(screenModel: ImportScreenModel) {
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PlatformCard(title = "Letterboxd Format", icon = Icons.Default.Movie) {
                Button(
                    onClick = { screenModel.exportData("Letterboxd") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E054), contentColor = Color.Black)
                ) {
                    Text("Export as Letterboxd CSVs", fontWeight = FontWeight.Bold)
                }
            }

            PlatformCard(title = "IMDb Format", icon = Icons.Default.Star) {
                Button(
                    onClick = { screenModel.exportData("IMDb") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5B100), contentColor = Color.Black)
                ) {
                    Text("Export as IMDb CSVs", fontWeight = FontWeight.Bold)
                }
            }

            PlatformCard(title = "CineGraph Backup", icon = Icons.Default.SettingsBackupRestore) {
                Button(
                    onClick = { screenModel.exportData("CineGraph") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF40bcf4), contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create JSON Backup", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    @Composable
    private fun PlatformCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable () -> Unit) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFF1F2326))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
                Spacer(modifier = Modifier.height(16.dp))
                content()
            }
        }
    }

    @Composable
    private fun SmallOutlinedButton(text: String, onClick: () -> Unit) {
        OutlinedButton(
            onClick = onClick,
            contentPadding = PaddingValues(horizontal = 12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
        ) {
            Text(text, fontSize = 12.sp)
        }
    }

    @Composable
    private fun LoadingView(state: SyncState.Loading) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            CircularProgressIndicator(color = Color(0xFF00E054))
            Spacer(modifier = Modifier.height(16.dp))
            Text(state.message, color = Color.White, textAlign = TextAlign.Center)
        }
    }

    @Composable
    private fun SuccessView(message: String, onReset: () -> Unit) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00E054), modifier = Modifier.size(80.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(message, color = Color.White, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(onClick = onReset) { Text("Done", color = Color(0xFF00E054)) }
        }
    }

    @Composable
    private fun ErrorView(error: String, onReset: () -> Unit) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.Error, contentDescription = null, tint = Color.Red, modifier = Modifier.size(80.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Error: $error", color = Color.Red, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(onClick = onReset) { Text("Try Again", color = Color.White) }
        }
    }
}