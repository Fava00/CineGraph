package com.martonegyed.presentation.screens.import

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.martonegyed.data.local.DataSyncManager
import com.martonegyed.presentation.components.common.AppDrawer
import com.martonegyed.presentation.components.common.ErrorView
import com.martonegyed.presentation.components.common.LoadingView
import com.martonegyed.presentation.components.common.SuccessView
import com.martonegyed.presentation.components.importing.PlatformCard
import com.martonegyed.presentation.components.importing.SmallOutlinedButton
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.collections.emptyList

class ImportScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val colors = MaterialTheme.colorScheme
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = koinScreenModel<ImportScreenModel>()
        val state by screenModel.state.collectAsState()
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val dataSyncManager: DataSyncManager = koinInject()
        val phase by dataSyncManager.phase.collectAsState()
        val hasPending by dataSyncManager.hasPendingEnrichment.collectAsState(false)
        val promptShown by dataSyncManager.resumePromptShown.collectAsState(false)

        var selectedTabIndex by remember { mutableStateOf(0) }

        LaunchedEffect(Unit) {
            dataSyncManager.refreshPendingEnrichment()
        }

        if (hasPending && phase == DataSyncManager.Phase.IDLE && !promptShown) {
            Surface(
                color = colors.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Some movies are missing TMDb data.",
                            color = colors.onBackground,
                            fontSize = 13.sp
                        )
                        Text(
                            "Resume enrichment in the background?",
                            color = colors.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                    TextButton(onClick = {
                        dataSyncManager.resumePromptShown.value = true
                        dataSyncManager.startImportAndEnrich(stagedMovies = emptyList())
                    }) {
                        Text("Resume", color = colors.scrim)
                    }
                    TextButton(onClick = {
                        dataSyncManager.resumePromptShown.value = true
                    }) {
                        Text("Not now", color = colors.onSurfaceVariant)
                    }
                }
            }
        }

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
            val colors = MaterialTheme.colorScheme
            val stagedCount by screenModel.stagedCount.collectAsState()
            val newMoviesCount by screenModel.newMoviesCount.collectAsState()
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Data Management") },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = colors.background,
                            titleContentColor = colors.onSurface,
                            navigationIconContentColor = colors.onSurface
                        )
                    )
                },
                bottomBar = {
                    if (selectedTabIndex == 0 && stagedCount > 0 && state is SyncState.Idle) {
                        Surface(
                            color = colors.surfaceVariant,
                            shadowElevation = 16.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(5f)
                                ) {
                                    val existingCount = stagedCount - newMoviesCount
                                    val statusText = when {
                                        stagedCount == 0 -> "No movies staged"
                                        newMoviesCount == 0 -> "Updating existing, no new movies"
                                        else -> "$newMoviesCount new, $existingCount existing"
                                    }

                                    Text(
                                        statusText,
                                        color = colors.onSurface,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "Review your files before updating.",
                                        color = colors.onSurfaceVariant,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .weight(4f),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        modifier = Modifier.weight(3f),
                                        onClick = { screenModel.clearStaged() }) {
                                        Text("Clear", color = colors.error)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Button(
                                        modifier = Modifier.weight(5f),
                                        onClick = { screenModel.commitToDatabase() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = colors.primary,
                                            contentColor = colors.onPrimary
                                        ),
                                        shape = RoundedCornerShape(30)
                                    ) {
                                        Text(
                                            "Update DB",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
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
                        contentColor = colors.scrim,
                        indicator = { tabPositions ->
                            if (selectedTabIndex < tabPositions.size) {
                                SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                    color = colors.scrim
                                )
                            }
                        }
                    ) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0; screenModel.reset() },
                            text = { Text("Import", fontWeight = FontWeight.Bold) },
                            unselectedContentColor = colors.onSurfaceVariant
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1; screenModel.reset() },
                            text = { Text("Export", fontWeight = FontWeight.Bold) },
                            unselectedContentColor = colors.onSurfaceVariant
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
        val dataSyncManager: DataSyncManager = koinInject()
        val colors = MaterialTheme.colorScheme
        val phase by dataSyncManager.phase.collectAsState()
        val importedCount by dataSyncManager.importedCount.collectAsState()
        val enrichedCount by dataSyncManager.enrichedCount.collectAsState()
        val hasPending by dataSyncManager.hasPendingEnrichment.collectAsState()

        val scrollState = rememberScrollState()

        val multiFilePicker = rememberFilePickerLauncher(
            type = PickerType.File(extensions = listOf("csv")),
            mode = PickerMode.Multiple(),
            title = "Select Letterboxd Export Files"
        ) { files ->
            if (!files.isNullOrEmpty()) {
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
            if (hasPending && (phase == DataSyncManager.Phase.IDLE)) {
                Surface(
                    color = colors.error.copy(alpha = 0.7f),
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "Some movies were not enriched with TMDb yet.",
                            color = colors.error.copy(alpha = 0.25f),
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        TextButton(
                            onClick = { dataSyncManager.startImportAndEnrich(emptyList()) }
                        ) {
                            Text("Continue enrichment", color = colors.error.copy(alpha = 0.85f), fontSize = 12.sp)
                        }
                    }
                }
            }
            PlatformCard(title = "Letterboxd", icon = Icons.Default.Movie) {
                Button(
                    onClick = { multiFilePicker.launch() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.scrim,
                        contentColor = colors.background
                    )
                ) {
                    Icon(Icons.Default.FolderZip, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import Full Folder", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Or import individual files:",
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SmallOutlinedButton("Diary") { currentImportType = "Letterboxd" to "Diary"; csvPicker.launch() }
                    SmallOutlinedButton("Watched") { currentImportType = "Letterboxd" to "Watched"; csvPicker.launch() }
                    SmallOutlinedButton("Watchlist") {
                        currentImportType = "Letterboxd" to "Watchlist"; csvPicker.launch()
                    }
                    SmallOutlinedButton("Ratings") { currentImportType = "Letterboxd" to "Ratings"; csvPicker.launch() }
                    SmallOutlinedButton("Reviews") { currentImportType = "Letterboxd" to "Reviews"; csvPicker.launch() }
                }
            }

            PlatformCard(title = "IMDb", icon = Icons.Default.Star) {
                Text(
                    "Import individual CSV files:",
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.secondary,
                        contentColor = colors.background
                    )
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restore from JSON", fontWeight = FontWeight.Bold)
                }
            }
            if (hasPending && (phase == DataSyncManager.Phase.IDLE)) {
                Surface(
                    color = colors.error.copy(alpha = 0.7f),
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    TextButton(
                        onClick = { dataSyncManager.startImportAndEnrich(stagedMovies = emptyList()) }
                    ) {
                        Text(
                            "Continue abandoned enrichment",
                            color = colors.error.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                    }

                }
            }
        }
    }


    @Composable
    private fun ExportView(screenModel: ImportScreenModel) {
        val scrollState = rememberScrollState()
        val colors = MaterialTheme.colorScheme
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.scrim,
                        contentColor = colors.background
                    )
                ) {
                    Text("Export as Letterboxd CSVs", fontWeight = FontWeight.Bold)
                }
            }

            PlatformCard(title = "IMDb Format", icon = Icons.Default.Star) {
                Button(
                    onClick = { screenModel.exportData("IMDb") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.background
                    )
                ) {
                    Text("Export as IMDb CSVs", fontWeight = FontWeight.Bold)
                }
            }

            PlatformCard(title = "CineGraph Backup", icon = Icons.Default.SettingsBackupRestore) {
                Button(
                    onClick = { screenModel.exportData("CineGraph") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.secondary,
                        contentColor = colors.background
                    )
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create JSON Backup", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
