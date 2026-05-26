package com.martonegyed.presentation.screens.import

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.LinearProgressIndicator
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
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.martonegyed.data.local.DataSyncManager
import com.martonegyed.core.ui.adaptive.AdaptiveLayout
import com.martonegyed.core.ui.adaptive.AdaptiveScaffoldTokens
import com.martonegyed.core.ui.adaptive.ImportScreenTokens
import com.martonegyed.core.util.writePickedFile
import com.martonegyed.presentation.components.common.AppDrawer
import com.martonegyed.presentation.components.common.ErrorView
import com.martonegyed.presentation.components.common.LoadingView
import com.martonegyed.presentation.components.common.SuccessView
import com.martonegyed.presentation.components.importing.PlatformCard
import io.github.vinceglb.filekit.compose.rememberDirectoryPickerLauncher
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import io.github.vinceglb.filekit.core.PlatformDirectory
import io.github.vinceglb.filekit.core.PlatformFile
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.collections.emptyList

private val DesktopContentMaxWidth = 1240.dp
private val DesktopRightPaneMaxWidth = 400.dp
private val DesktopActionButtonMaxWidth = 320.dp
private val DesktopActionButtonMinWidth = 220.dp
private val DesktopCardSpacing = 18.dp


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

        var pendingSingleFile by remember { mutableStateOf<ExportPayload.SingleFile?>(null) }
        var pendingMultiFiles by remember { mutableStateOf<List<ExportFile>>(emptyList()) }
        var currentQueuedFile by remember { mutableStateOf<ExportFile?>(null) }

        val fileSaver = rememberFileSaverLauncher { file: PlatformFile? ->
            val singlePayload = pendingSingleFile
            val queuedFile = currentQueuedFile

            if (file == null) {
                pendingSingleFile = null
                pendingMultiFiles = emptyList()
                currentQueuedFile = null
                screenModel.onExportCancelled()
            } else {
                scope.launch {
                    when {
                        singlePayload != null -> {
                            writePickedFile(file, singlePayload.bytes)
                            screenModel.onExportSaved("${singlePayload.fileName} exported")
                            pendingSingleFile = null
                        }

                        queuedFile != null -> {
                            writePickedFile(file, queuedFile.bytes)

                            val remaining = pendingMultiFiles.drop(1)
                            pendingMultiFiles = remaining

                            if (remaining.isEmpty()) {
                                currentQueuedFile = null
                                screenModel.onExportSaved("5 CSV files exported")
                            } else {
                                currentQueuedFile = remaining.first()
                            }
                        }
                    }
                }
            }
        }

        LaunchedEffect(pendingSingleFile) {
            val payload = pendingSingleFile ?: return@LaunchedEffect
            fileSaver.launch(
                baseName = payload.fileName.substringBeforeLast("."),
                extension = payload.fileName.substringAfterLast(".")
            )
        }

        LaunchedEffect(currentQueuedFile) {
            val file = currentQueuedFile ?: return@LaunchedEffect
            fileSaver.launch(
                baseName = file.fileName.substringBeforeLast("."),
                extension = file.fileName.substringAfterLast(".")
            )
        }


        LaunchedEffect(Unit) {
            screenModel.exportPayload.collect { payload ->
                when (payload) {
                    is ExportPayload.SingleFile -> {
                        pendingSingleFile = payload
                    }

                    is ExportPayload.MultiFile -> {
                        pendingMultiFiles = payload.files
                        currentQueuedFile = payload.files.firstOrNull()
                    }
                }
            }
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
                        Text("Resume", color = colors.inversePrimary)
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

                }
            ) { paddingValues ->
                AdaptiveLayout(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(colors.background)
                ) { adaptive ->
                    val scaffoldTokens = adaptive.tokens.scaffold
                    val importTokens = adaptive.tokens.importScreen
                    val stagedCount by screenModel.stagedCount.collectAsState()
                    val newMoviesCount by screenModel.newMoviesCount.collectAsState()
                    val isDesktop = importTokens.useTwoPaneLayout

                    Scaffold(
                        containerColor = Color.Transparent,
                        bottomBar = {
                            if (
                                !isDesktop &&
                                selectedTabIndex == 0 &&
                                stagedCount > 0 &&
                                state is SyncState.Idle
                            ) {
                                StagedImportSummaryCard(
                                    stagedCount = stagedCount,
                                    newMoviesCount = newMoviesCount,
                                    onClear = screenModel::clearStaged,
                                    onCommit = screenModel::commitToDatabase,
                                    compactBarStyle = true,
                                    scaffoldTokens = scaffoldTokens,
                                    importTokens = importTokens
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth()
                                    .widthIn(
                                        max = if (isDesktop) {
                                            DesktopContentMaxWidth
                                        } else {
                                            scaffoldTokens.maxCenteredContentWidth
                                        }
                                    )
                            ) {
                                if (!isDesktop) {
                                    TabRow(
                                        selectedTabIndex = selectedTabIndex,
                                        containerColor = Color.Transparent,
                                        contentColor = colors.primary,
                                        indicator = { tabPositions ->
                                            if (selectedTabIndex < tabPositions.size) {
                                                SecondaryIndicator(
                                                    Modifier.tabIndicatorOffset(
                                                        tabPositions[selectedTabIndex]
                                                    ),
                                                    color = colors.primary
                                                )
                                            }
                                        }
                                    ) {
                                        Tab(
                                            selected = selectedTabIndex == 0,
                                            onClick = {
                                                selectedTabIndex = 0
                                                screenModel.reset()
                                            },
                                            text = { Text("Import", fontWeight = FontWeight.Bold) },
                                            unselectedContentColor = colors.onSurfaceVariant
                                        )
                                        Tab(
                                            selected = selectedTabIndex == 1,
                                            onClick = {
                                                selectedTabIndex = 1
                                                screenModel.reset()
                                            },
                                            text = { Text("Export", fontWeight = FontWeight.Bold) },
                                            unselectedContentColor = colors.onSurfaceVariant
                                        )
                                    }
                                }

                                Box(modifier = Modifier.fillMaxSize()) {
                                    when (val currentState = state) {
                                        is SyncState.Idle -> {
                                            if (isDesktop) {
                                                DataManagementExpandedContent(
                                                    screenModel = screenModel,
                                                    scaffoldTokens = scaffoldTokens,
                                                    importTokens = importTokens
                                                )
                                            } else {
                                                if (selectedTabIndex == 0) {
                                                    ImportView(
                                                        screenModel = screenModel,
                                                        scaffoldTokens = scaffoldTokens,
                                                        importTokens = importTokens
                                                    )
                                                } else {
                                                    ExportView(
                                                        screenModel = screenModel,
                                                        scaffoldTokens = scaffoldTokens,
                                                        importTokens = importTokens
                                                    )
                                                }
                                            }
                                        }

                                        is SyncState.Loading -> LoadingView(currentState)
                                        is SyncState.Success -> SuccessView(
                                            currentState.message,
                                            onReset = screenModel::reset
                                        )

                                        is SyncState.Error -> ErrorView(
                                            currentState.error,
                                            onReset = screenModel::reset
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

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun ImportView(
        screenModel: ImportScreenModel,
        scaffoldTokens: AdaptiveScaffoldTokens,
        importTokens: ImportScreenTokens,
        modifier: Modifier = Modifier,
        showInlineStatusCards: Boolean = true
    ) {
        val dataSyncManager: DataSyncManager = koinInject()
        val colors = MaterialTheme.colorScheme
        val phase by dataSyncManager.phase.collectAsState()
        val importedCount by dataSyncManager.importedCount.collectAsState()
        val enrichedCount by dataSyncManager.enrichedCount.collectAsState()
        val hasPending by dataSyncManager.hasPendingEnrichment.collectAsState()
        val stagedSources by screenModel.stagedSources.collectAsState()
        val scrollState = rememberScrollState()
        val isDesktop = importTokens.useTwoPaneLayout

        val multiFilePicker = rememberFilePickerLauncher(
            type = PickerType.File(extensions = listOf("csv")),
            mode = PickerMode.Multiple(),
            title = "Select Letterboxd Export Files"
        ) { files ->
            if (!files.isNullOrEmpty()) {
                screenModel.stageMultipleLetterboxdFiles(files)
            }
        }

        var currentImportType by remember { mutableStateOf("" to "") }

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
            modifier = modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .then(
                    if (isDesktop) {
                        Modifier
                    } else {
                        Modifier.padding(
                            horizontal = scaffoldTokens.horizontalPadding,
                            vertical = scaffoldTokens.verticalPadding
                        )
                    }
                ),
            verticalArrangement = Arrangement.spacedBy(scaffoldTokens.sectionSpacing)
        ) {
            if (showInlineStatusCards && phase != DataSyncManager.Phase.IDLE) {
                Surface(
                    color = colors.surfaceVariant,
                    shape = RoundedCornerShape(20.dp),
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Background sync in progress (imported: $importedCount, enriched: $enrichedCount)",
                        color = colors.onSurfaceVariant,
                        fontSize = importTokens.bodyFontSize,
                        modifier = Modifier.padding(importTokens.sectionCardPadding)
                    )
                }
            }

            if (showInlineStatusCards && hasPending && phase == DataSyncManager.Phase.IDLE) {
                PendingEnrichmentCard(
                    onContinue = { dataSyncManager.startImportAndEnrich(emptyList()) },
                    compactAction = !isDesktop
                )
            }

            PlatformCard(title = "Letterboxd", icon = Icons.Default.Movie) {
                if (isDesktop) {
                    Text(
                        "Import the letterboxd CSVs files",
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }
                
                Text(
                    "Import individual files:",
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StagedSourceChip(
                        label = "Diary",
                        isStaged = stagedSources.contains(sourceKey("Letterboxd", "Diary")),
                        onPick = {
                            currentImportType = "Letterboxd" to "Diary"
                            csvPicker.launch()
                        },
                        onRemove = { screenModel.removeStagedSource("Letterboxd", "Diary") }
                    )

                    StagedSourceChip(
                        label = "Watched",
                        isStaged = stagedSources.contains(sourceKey("Letterboxd", "Watched")),
                        onPick = {
                            currentImportType = "Letterboxd" to "Watched"
                            csvPicker.launch()
                        },
                        onRemove = { screenModel.removeStagedSource("Letterboxd", "Watched") }
                    )

                    StagedSourceChip(
                        label = "Watchlist",
                        isStaged = stagedSources.contains(sourceKey("Letterboxd", "Watchlist")),
                        onPick = {
                            currentImportType = "Letterboxd" to "Watchlist"
                            csvPicker.launch()
                        },
                        onRemove = { screenModel.removeStagedSource("Letterboxd", "Watchlist") }
                    )

                    StagedSourceChip(
                        label = "Ratings",
                        isStaged = stagedSources.contains(sourceKey("Letterboxd", "Ratings")),
                        onPick = {
                            currentImportType = "Letterboxd" to "Ratings"
                            csvPicker.launch()
                        },
                        onRemove = { screenModel.removeStagedSource("Letterboxd", "Ratings") }
                    )

                    StagedSourceChip(
                        label = "Reviews",
                        isStaged = stagedSources.contains(sourceKey("Letterboxd", "Reviews")),
                        onPick = {
                            currentImportType = "Letterboxd" to "Reviews"
                            csvPicker.launch()
                        },
                        onRemove = { screenModel.removeStagedSource("Letterboxd", "Reviews") }
                    )
                }
            }

            PlatformCard(title = "IMDb", icon = Icons.Default.Star) {
                if (isDesktop) {
                    Text(
                        "Bring in IMDb exports one category at a time.",
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                Text(
                    "Import individual CSV files:",
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StagedSourceChip(
                        label = "Ratings",
                        isStaged = stagedSources.contains(sourceKey("IMDb", "Ratings")),
                        onPick = {
                            currentImportType = "IMDb" to "Ratings"
                            csvPicker.launch()
                        },
                        onRemove = { screenModel.removeStagedSource("IMDb", "Ratings") }
                    )

                    StagedSourceChip(
                        label = "Watchlist",
                        isStaged = stagedSources.contains(sourceKey("IMDb", "Watchlist")),
                        onPick = {
                            currentImportType = "IMDb" to "Watchlist"
                            csvPicker.launch()
                        },
                        onRemove = { screenModel.removeStagedSource("IMDb", "Watchlist") }
                    )

                    StagedSourceChip(
                        label = "Lists",
                        isStaged = stagedSources.contains(sourceKey("IMDb", "Lists")),
                        onPick = {
                            currentImportType = "IMDb" to "Lists"
                            csvPicker.launch()
                        },
                        onRemove = { screenModel.removeStagedSource("IMDb", "Lists") }
                    )
                }
            }

            PlatformCard(
                title = "CineGraph Backup",
                icon = Icons.Default.SettingsBackupRestore
            ) {
                if (isDesktop) {
                    Text(
                        "Restore your complete CineGraph backup from a JSON file.",
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                Button(
                    onClick = { jsonPicker.launch() },
                    modifier = desktopAwarePrimaryActionModifier(isDesktop),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.secondary,
                        contentColor = colors.onSecondary
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restore from JSON", fontWeight = FontWeight.Bold)
                }
            }
        }
    }


    @Composable
    private fun ExportView(
        screenModel: ImportScreenModel,
        scaffoldTokens: AdaptiveScaffoldTokens,
        importTokens: ImportScreenTokens,
        modifier: Modifier = Modifier,
        scrollable: Boolean = true
    ) {
        val scrollState = rememberScrollState()
        val colors = MaterialTheme.colorScheme
        val isDesktop = importTokens.useTwoPaneLayout

        val baseModifier = modifier
            .fillMaxWidth()
            .then(
                if (isDesktop) {
                    Modifier
                } else {
                    Modifier.padding(
                        horizontal = scaffoldTokens.horizontalPadding,
                        vertical = scaffoldTokens.verticalPadding
                    )
                }
            )

        val contentModifier = if (scrollable) {
            baseModifier.verticalScroll(scrollState)
        } else {
            baseModifier
        }

        Column(
            modifier = contentModifier,
            verticalArrangement = Arrangement.spacedBy(scaffoldTokens.sectionSpacing)
        ) {
            PlatformCard(title = "Letterboxd Format", icon = Icons.Default.Movie) {
                Text(
                    "Create CSV files compatible with Letterboxd exports.",
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(14.dp))

                FilledTonalButton(
                    onClick = { screenModel.exportData("Letterboxd") },
                    modifier = desktopAwarePrimaryActionModifier(isDesktop),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Export Letterboxd CSVs", fontWeight = FontWeight.Bold)
                }
            }

            PlatformCard(title = "IMDb Format", icon = Icons.Default.Star) {
                Text(
                    "Create CSV files in IMDb-style export format.",
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(14.dp))

                FilledTonalButton(
                    onClick = { screenModel.exportData("IMDb") },
                    modifier = desktopAwarePrimaryActionModifier(isDesktop),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = colors.tertiaryContainer,
                        contentColor = colors.onTertiaryContainer
                    )
                ) {
                    Text("Export IMDb CSVs", fontWeight = FontWeight.Bold)
                }
            }

            PlatformCard(title = "CineGraph Backup", icon = Icons.Default.SettingsBackupRestore) {
                Text(
                    "Create a full JSON backup of your CineGraph data.",
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(14.dp))

                FilledTonalButton(
                    onClick = { screenModel.exportData("CineGraph") },
                    modifier = desktopAwarePrimaryActionModifier(isDesktop),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = colors.secondaryContainer,
                        contentColor = colors.onSecondaryContainer
                    )
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create JSON Backup", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    @Composable
    private fun DataManagementExpandedContent(
        screenModel: ImportScreenModel,
        scaffoldTokens: AdaptiveScaffoldTokens,
        importTokens: ImportScreenTokens
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = scaffoldTokens.horizontalPadding,
                    end = scaffoldTokens.horizontalPadding,
                    top = scaffoldTokens.verticalPadding,
                    bottom = scaffoldTokens.verticalPadding
                ),
            horizontalArrangement = Arrangement.spacedBy(importTokens.paneSpacing),
            verticalAlignment = Alignment.Top
        ) {
            ImportView(
                screenModel = screenModel,
                scaffoldTokens = scaffoldTokens,
                importTokens = importTokens,
                modifier = Modifier
                    .weight(1f),
                showInlineStatusCards = false
            )

            StatusAndExportPane(
                screenModel = screenModel,
                scaffoldTokens = scaffoldTokens,
                importTokens = importTokens,
                modifier = Modifier
                    .widthIn(max = DesktopRightPaneMaxWidth)
                    .fillMaxHeight()
            )
        }
    }

    @Composable
    private fun StatusAndExportPane(
        screenModel: ImportScreenModel,
        scaffoldTokens: AdaptiveScaffoldTokens,
        importTokens: ImportScreenTokens,
        modifier: Modifier = Modifier
    ) {
        val dataSyncManager: DataSyncManager = koinInject()
        val colors = MaterialTheme.colorScheme
        val phase by dataSyncManager.phase.collectAsState()
        val hasPending by dataSyncManager.hasPendingEnrichment.collectAsState()
        val importedCount by dataSyncManager.importedCount.collectAsState()
        val importedTotal by dataSyncManager.importedTotal.collectAsState()
        val enrichedCount by dataSyncManager.enrichedCount.collectAsState()
        val enrichedTotal by dataSyncManager.enrichedTotal.collectAsState()
        val lastMessage by dataSyncManager.lastMessage.collectAsState()

        val stagedCount by screenModel.stagedCount.collectAsState()
        val newMoviesCount by screenModel.newMoviesCount.collectAsState()
        val scrollState = rememberScrollState()

        Column(
            modifier = modifier
                .fillMaxHeight()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(DesktopCardSpacing)
        ) {
            if (stagedCount > 0) {
                StagedImportSummaryCard(
                    stagedCount = stagedCount,
                    newMoviesCount = newMoviesCount,
                    onClear = screenModel::clearStaged,
                    onCommit = screenModel::commitToDatabase,
                    compactBarStyle = false,
                    scaffoldTokens = scaffoldTokens,
                    importTokens = importTokens
                )
            }

            if (phase != DataSyncManager.Phase.IDLE) {
                DesktopSyncProgressCard(
                    phase = phase,
                    importedCount = importedCount,
                    importedTotal = importedTotal,
                    enrichedCount = enrichedCount,
                    enrichedTotal = enrichedTotal,
                    lastMessage = lastMessage,
                    onCancel = { dataSyncManager.cancelAll() }
                )
            }

            if (phase != DataSyncManager.Phase.IDLE) {
                Surface(
                    color = colors.surfaceVariant,
                    shape = RoundedCornerShape(20.dp),
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(importTokens.sectionCardPadding),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Background sync in progress",
                            color = colors.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = importTokens.titleFontSize
                        )
                        Text(
                            text = "Imported: $importedCount · Enriched: $enrichedCount",
                            color = colors.onSurfaceVariant,
                            fontSize = importTokens.bodyFontSize
                        )
                    }
                }
            }

            if (hasPending && phase == DataSyncManager.Phase.IDLE) {
                PendingEnrichmentCard(
                    onContinue = { dataSyncManager.startImportAndEnrich(emptyList()) },
                    compactAction = false
                )
            }

            ExportView(
                screenModel = screenModel,
                scaffoldTokens = scaffoldTokens,
                importTokens = importTokens,
                modifier = Modifier.fillMaxWidth(),
                scrollable = false
            )
        }
    }


    @Composable
    private fun StagedImportSummaryCard(
        stagedCount: Int,
        newMoviesCount: Int,
        onClear: () -> Unit,
        onCommit: () -> Unit,
        compactBarStyle: Boolean,
        scaffoldTokens: AdaptiveScaffoldTokens,
        importTokens: ImportScreenTokens
    ) {
        val colors = MaterialTheme.colorScheme
        val existingCount = stagedCount - newMoviesCount
        val statusText = when {
            stagedCount == 0 -> "No movies staged"
            newMoviesCount == 0 -> "Updating existing, no new movies"
            else -> "$newMoviesCount new, $existingCount existing"
        }

        Surface(
            color = colors.surfaceVariant,
            shadowElevation = if (compactBarStyle) 16.dp else 2.dp,
            shape = if (compactBarStyle) RoundedCornerShape(0.dp) else RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = scaffoldTokens.horizontalPadding.coerceAtLeast(16.dp),
                    vertical = 14.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column {
                    Text(
                        statusText,
                        color = colors.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = importTokens.bodyFontSize
                    )
                    Text(
                        "Review your files before updating.",
                        color = colors.onSurfaceVariant,
                        fontSize = importTokens.bodyFontSize
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onClear) {
                        Text("Clear", color = colors.error)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onCommit,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.onPrimary
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            "Update DB",
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun PendingEnrichmentCard(
        onContinue: () -> Unit,
        compactAction: Boolean
    ) {
        val colors = MaterialTheme.colorScheme

        Surface(
            color = colors.errorContainer,
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Some movies were not enriched with TMDb yet.",
                    color = colors.onErrorContainer,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = onContinue,
                    modifier = if (compactAction) Modifier else Modifier.widthIn(min = 180.dp, max = 240.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.error,
                        contentColor = colors.onError
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Continue enrichment", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    @Composable
    private fun DesktopSyncProgressCard(
        phase: DataSyncManager.Phase,
        importedCount: Int,
        importedTotal: Int,
        enrichedCount: Int,
        enrichedTotal: Int,
        lastMessage: String?,
        onCancel: () -> Unit
    ) {
        val colors = MaterialTheme.colorScheme
        val importedProgress = if (importedTotal > 0) {
            importedCount.toFloat() / importedTotal.toFloat()
        } else {
            0f
        }
        val enrichedProgress = if (enrichedTotal > 0) {
            enrichedCount.toFloat() / enrichedTotal.toFloat()
        } else {
            0f
        }

        Surface(
            color = colors.surfaceVariant,
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (phase) {
                            DataSyncManager.Phase.IMPORTING -> "Importing"
                            DataSyncManager.Phase.ENRICHING -> "Enriching from TMDb"
                            DataSyncManager.Phase.IDLE -> "Idle"
                        },
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )

                    TextButton(onClick = onCancel) {
                        Text("Cancel", color = colors.error)
                    }
                }

                if (importedTotal > 0) {
                    Text(
                        "Imported: $importedCount / $importedTotal",
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                    LinearProgressIndicator(
                        progress = { importedProgress },
                        modifier = Modifier.fillMaxWidth(),
                        color = colors.primary,
                        trackColor = colors.surface,
                        strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                    )
                }

                if (enrichedTotal > 0) {
                    Text(
                        "Enriched: $enrichedCount / $enrichedTotal",
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                    LinearProgressIndicator(
                        progress = { enrichedProgress },
                        modifier = Modifier.fillMaxWidth(),
                        color = colors.secondary,
                        trackColor = colors.surface,
                        strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                    )
                }

                if (!lastMessage.isNullOrBlank()) {
                    Text(
                        lastMessage,
                        color = colors.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    @Composable
    private fun StagedSourceChip(
        label: String,
        isStaged: Boolean,
        onPick: () -> Unit,
        onRemove: () -> Unit
    ) {
        val colors = MaterialTheme.colorScheme

        if (isStaged) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilledTonalButton(
                    onClick = onPick,
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = colors.secondaryContainer,
                        contentColor = colors.onSecondaryContainer
                    )
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(label, fontWeight = FontWeight.SemiBold)
                }

                FilledIconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(34.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = colors.surfaceVariant,
                        contentColor = colors.onSurfaceVariant
                    )
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove $label"
                    )
                }
            }
        } else {
            OutlinedButton(
                onClick = onPick,
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = colors.onSurface
                )
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge)
            }
        }
    }

    private fun sourceKey(platform: String, type: String): String {
        return "${platform.lowercase()}:${type.lowercase()}"
    }

    private fun desktopAwarePrimaryActionModifier(isDesktop: Boolean): Modifier {
        return if (isDesktop) {
            Modifier.widthIn(
                min = DesktopActionButtonMinWidth,
                max = DesktopActionButtonMaxWidth
            )
        } else {
            Modifier.fillMaxWidth()
        }
    }
}

