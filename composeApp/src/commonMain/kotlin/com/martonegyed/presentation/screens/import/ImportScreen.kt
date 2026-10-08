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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.martonegyed.domain.repository.ImportRepository
import com.martonegyed.domain.repository.ImportPhase
import com.martonegyed.core.ui.adaptive.AdaptiveLayout
import com.martonegyed.core.ui.adaptive.AdaptiveScaffoldTokens
import com.martonegyed.core.ui.adaptive.ImportScreenTokens
import com.martonegyed.core.util.writePickedFile
import com.martonegyed.presentation.components.common.AppDrawer
import com.martonegyed.presentation.components.common.ErrorView
import com.martonegyed.presentation.components.common.LoadingView
import com.martonegyed.presentation.components.common.SuccessView
import com.martonegyed.presentation.components.importing.PlatformCard
import com.martonegyed.presentation.components.importing.ReadyToImportCard
import com.martonegyed.presentation.components.importing.TmdbCandidateCard
import com.martonegyed.presentation.components.importing.TmdbReviewCardHeader
import com.martonegyed.presentation.components.importing.SuggestedTmdbMatchesDialog
import com.martonegyed.presentation.components.importing.CsvImportConfirmationDialog
import com.martonegyed.presentation.components.importing.CsvSourceConflictDialog
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
        val csvConfirmation by screenModel.csvConfirmation.collectAsState()
        val csvReadBusy by screenModel.csvReadBusy.collectAsState()
        val suggestionsOpen by screenModel.suggestionsOpen.collectAsState()
        val suggestions by screenModel.suggestions.collectAsState()
        val suggestionsLoading by screenModel.suggestionsLoading.collectAsState()
        val suggestionsApplying by screenModel.suggestionsApplying.collectAsState()
        val suggestionProgress by screenModel.suggestionProgress.collectAsState()
        val sourceConflict by screenModel.sourceConflict.collectAsState()
        sourceConflict?.let { conflict ->
            CsvSourceConflictDialog(conflict, csvReadBusy, screenModel::resolveSourceConflict)
        }
        csvConfirmation?.let { assessment ->
            CsvImportConfirmationDialog(assessment, csvReadBusy, screenModel::confirmCsvType, screenModel::cancelCsvReview)
        }
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val importRepository: ImportRepository = koinInject()
        val phase by importRepository.phase.collectAsState()
        if (suggestionsOpen) SuggestedTmdbMatchesDialog(
            suggestions, suggestionsLoading && phase == ImportPhase.IDLE, suggestionsApplying,
            suggestionProgress.takeIf { phase == ImportPhase.IDLE },
            screenModel::chooseSuggestedMatch, { screenModel.applySuggestedMatches(it) },
            { screenModel.applySuggestedMatches() }, screenModel::closeSuggestedMatches,
            canApply = phase == ImportPhase.IDLE
        )
        val hasPending by importRepository.hasPendingEnrichment.collectAsState(false)
        val promptShown by importRepository.resumePromptShown.collectAsState(false)

        var selectedTabIndex by remember { mutableStateOf(0) }

        var pendingSingleFile by remember { mutableStateOf<ExportPayload.SingleFile?>(null) }
        var pendingBackupName by remember { mutableStateOf<String?>(null) }
        var pendingMultiFiles by remember { mutableStateOf<List<ExportFile>>(emptyList()) }
        var currentQueuedFile by remember { mutableStateOf<ExportFile?>(null) }

        val fileSaver = rememberFileSaverLauncher { file: PlatformFile? ->
            val singlePayload = pendingSingleFile
            val queuedFile = currentQueuedFile

            if (file == null) {
                pendingSingleFile = null
                pendingBackupName = null
                pendingMultiFiles = emptyList()
                currentQueuedFile = null
                screenModel.onExportCancelled()
            } else {
                scope.launch {
                    when {
                        pendingBackupName != null -> {
                            pendingBackupName = null
                            screenModel.writeBackup(file)
                        }

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
                                screenModel.onExportSaved("CSV files exported")
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

        LaunchedEffect(pendingBackupName) {
            val name = pendingBackupName ?: return@LaunchedEffect
            fileSaver.launch(
                baseName = name.substringBeforeLast("."),
                extension = name.substringAfterLast(".")
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
                    is ExportPayload.BackupDestination -> {
                        pendingBackupName = payload.fileName
                    }

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

        if (hasPending && phase == ImportPhase.IDLE && !promptShown) {
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
                        importRepository.markResumePromptShown()
                        importRepository.startImportAndEnrich(stagedMovies = emptyList())
                    }) {
                        Text("Resume", color = colors.inversePrimary)
                    }
                    TextButton(onClick = {
                        importRepository.markResumePromptShown()
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
        val importRepository: ImportRepository = koinInject()
        val colors = MaterialTheme.colorScheme
        val phase by importRepository.phase.collectAsState()
        val importedCount by importRepository.importedCount.collectAsState()
        val enrichedCount by importRepository.enrichedCount.collectAsState()
        val hasPending by importRepository.hasPendingEnrichment.collectAsState()
        val lastMessage by importRepository.lastMessage.collectAsState()
        val stagedSources by screenModel.stagedSources.collectAsState()
        val stagedSummary by screenModel.stagedSummary.collectAsState()
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
            if (stagedSummary.isNotEmpty()) {
                ReadyToImportCard(stagedSummary)
            }
            if (showInlineStatusCards && hasPending && phase == ImportPhase.IDLE) {
                PendingEnrichmentCard(
                    onContinue = { importRepository.startImportAndEnrich(emptyList()) },
                    status = lastMessage,
                    compactAction = !isDesktop
                )
            }

            if (showInlineStatusCards) UnmatchedTmdbReviewCard(screenModel)

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
                    "Exports six files: reviews, watchlist, diary, ratings, watched, and an IMDb-ID companion for library films without a Letterboxd URI. The companion contains film identifiers only; ratings and history remain in the other files.",
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
        val importRepository: ImportRepository = koinInject()
        val colors = MaterialTheme.colorScheme
        val phase by importRepository.phase.collectAsState()
        val hasPending by importRepository.hasPendingEnrichment.collectAsState()
        val importedCount by importRepository.importedCount.collectAsState()
        val importedTotal by importRepository.importedTotal.collectAsState()
        val enrichedCount by importRepository.enrichedCount.collectAsState()
        val enrichedTotal by importRepository.enrichedTotal.collectAsState()
        val lastMessage by importRepository.lastMessage.collectAsState()

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

            if (phase != ImportPhase.IDLE) {
                DesktopSyncProgressCard(
                    phase = phase,
                    importedCount = importedCount,
                    importedTotal = importedTotal,
                    enrichedCount = enrichedCount,
                    enrichedTotal = enrichedTotal,
                    lastMessage = lastMessage,
                    onCancel = { importRepository.cancelAll() }
                )
            }

            if (hasPending && phase == ImportPhase.IDLE) {
                PendingEnrichmentCard(
                    onContinue = { importRepository.startImportAndEnrich(emptyList()) },
                    status = lastMessage,
                    compactAction = false
                )
            }

            UnmatchedTmdbReviewCard(screenModel)

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
    private fun UnmatchedTmdbReviewCard(screenModel: ImportScreenModel) {
        val open by screenModel.reviewOpen.collectAsState()
        val movies by screenModel.unmatchedMovies.collectAsState()
        val visible by screenModel.reviewCardVisible.collectAsState()
        val selected by screenModel.reviewedMovie.collectAsState()
        val candidates by screenModel.matchCandidates.collectAsState()
        val busy by screenModel.matchReviewBusy.collectAsState()
        val suggestions by screenModel.suggestions.collectAsState()
        val preparing by screenModel.suggestionsLoading.collectAsState()
        val preparationProgress by screenModel.suggestionProgress.collectAsState()
        val message by screenModel.matchReviewMessage.collectAsState()
        val importRepository: ImportRepository = koinInject()
        val importPhase by importRepository.phase.collectAsState()
        var query by remember(selected?.id) { mutableStateOf(selected?.name.orEmpty()) }
        var page by remember { mutableIntStateOf(0) }
        var confirmation by remember(selected?.id) { mutableStateOf<com.martonegyed.domain.model.TmdbMatchCandidate?>(null) }
        var removal by remember { mutableStateOf<com.martonegyed.domain.model.UnmatchedMovie?>(null) }
        var removalAll by remember { mutableStateOf<List<com.martonegyed.domain.model.UnmatchedMovie>?>(null) }
        var tmdbUrl by remember(selected?.id) { mutableStateOf("") }
        var browserError by remember(selected?.id) { mutableStateOf<String?>(null) }
        val uriHandler = LocalUriHandler.current

        if (movies.isEmpty() || !visible) return

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TmdbReviewCardHeader(
                    count = movies.size,
                    readyCount = suggestions.size,
                    preparing = preparing,
                    syncing = importPhase != ImportPhase.IDLE,
                    progress = preparationProgress,
                    canReview = !busy,
                    canManage = !busy && importPhase == ImportPhase.IDLE,
                    onSuggestions = screenModel::openSuggestedMatches,
                    onRefresh = screenModel::refreshSuggestedMatches,
                    onRemoveAll = { removalAll = movies.toList() },
                    onReview = screenModel::openMatchReview,
                    reviewOpen = open
                )
                if (open) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = screenModel::retryUnmatchedMovies,
                            enabled = importPhase == ImportPhase.IDLE && !busy
                        ) { Text("Retry matching") }
                        TextButton(onClick = screenModel::closeMatchReview) { Text("Close") }
                    }
                    message?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    if (selected == null) {
                        val lastPage = ((movies.size - 1).coerceAtLeast(0)) / 10
                        val visiblePage = page.coerceAtMost(lastPage)
                        val start = visiblePage * 10
                        movies.drop(start).take(10).forEach { movie ->
                            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("${movie.name} (${movie.year})", fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyLarge)
                                    val details = buildList {
                                        movie.imdbId?.let { add("IMDb $it") }
                                        if (movie.isWatched) add("Watched")
                                        if (movie.inWatchlist) add("Watchlist")
                                        if (movie.logCount > 0) add("${movie.logCount} logs")
                                        if (movie.listCount > 0) add("${movie.listCount} lists")
                                    }
                                    if (details.isNotEmpty()) Text(details.joinToString(" · "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(onClick = { screenModel.selectUnmatchedMovie(movie) },
                                            enabled = !busy && importPhase == ImportPhase.IDLE) { Text("Find match") }
                                        TextButton(onClick = { removal = movie },
                                            enabled = !busy && importPhase == ImportPhase.IDLE,
                                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                                            Text("Remove")
                                        }
                                    }
                                }
                            }
                        }
                        if (movies.size > 10) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { page = visiblePage - 1 }, enabled = visiblePage > 0) { Text("Previous") }
                                Text("Page ${visiblePage + 1} of ${(movies.size + 9) / 10}",
                                    style = MaterialTheme.typography.bodySmall)
                                TextButton(onClick = { page = visiblePage + 1 }, enabled = (visiblePage + 1) * 10 < movies.size) { Text("Next") }
                            }
                        }
                    } else {
                        Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text("${selected!!.name} (${selected!!.year})", fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyLarge)
                                selected!!.imdbId?.let { Text("IMDb $it", style = MaterialTheme.typography.bodySmall) }
                                selected!!.letterboxdUri?.takeIf {
                                    Regex("^https://(?:boxd\\.it|(?:www\\.)?letterboxd\\.com)/[^\\s]*$", RegexOption.IGNORE_CASE).matches(it)
                                }?.let { url ->
                                    TextButton(onClick = {
                                        try { uriHandler.openUri(url); browserError = null }
                                        catch (e: Exception) { browserError = "Could not open Letterboxd: ${e.message}" }
                                    }) { Text("Open on Letterboxd") }
                                }
                                browserError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                                Text("${selected!!.logCount} logs · ${selected!!.listCount} lists",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = screenModel::backToUnmatchedMovies) { Text("Back to queue") }
                                    TextButton(onClick = { removal = selected },
                                        enabled = !busy && importPhase == ImportPhase.IDLE,
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                                        Text("Remove movie")
                                    }
                                }
                            }
                        }
                        OutlinedTextField(
                            value = tmdbUrl, onValueChange = { tmdbUrl = it },
                            label = { Text("TMDB movie URL") }, singleLine = true,
                            supportingText = { Text("Follow the TMDB link on Letterboxd, then paste its movie URL here.") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedButton(onClick = { screenModel.loadTmdbUrl(tmdbUrl) },
                            enabled = !busy && importPhase == ImportPhase.IDLE && tmdbUrl.isNotBlank()) { Text("Load movie from URL") }
                        Text("Search TMDb by title", style = MaterialTheme.typography.titleSmall)
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            label = { Text("Search TMDb movie title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(onClick = { screenModel.searchMatchCandidates(query) }, enabled = !busy && importPhase == ImportPhase.IDLE && query.isNotBlank()) {
                            Text(if (busy) "Searching..." else "Search movies")
                        }
                        candidates.take(10).forEach { candidate ->
                            TmdbCandidateCard(candidate, !busy && importPhase == ImportPhase.IDLE) { confirmation = candidate }
                        }
                    }
                }
            }
        }

        confirmation?.let { candidate ->
            AlertDialog(
                onDismissRequest = { confirmation = null },
                title = { Text("Confirm TMDb match") },
                text = { Text("Link ${selected?.name} (${selected?.year}) to ${candidate.title} (${candidate.year ?: "year unknown"})? Existing ratings, reviews and logs will stay with this movie.") },
                confirmButton = {
                    TextButton(onClick = { confirmation = null; screenModel.confirmMatch(candidate) }) { Text("Link movie") }
                },
                dismissButton = { TextButton(onClick = { confirmation = null }) { Text("Cancel") } }
            )
        }
        removalAll?.let { snapshot ->
            AlertDialog(
                onDismissRequest = { removalAll = null },
                title = { Text("Remove all ${snapshot.size} unmatched entries?") },
                text = { Text("This permanently deletes these entries from this device, including ${snapshot.sumOf { it.logCount }} viewing/rating logs, reviews, watchlist status, and ${snapshot.sumOf { it.listCount }} custom-list entries. It does not verify that they are series. Matched movies are kept.") },
                confirmButton = {
                    TextButton(onClick = { removalAll = null; screenModel.removeAllUnmatchedMovies(snapshot) },
                        enabled = !busy && importPhase == ImportPhase.IDLE,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Remove permanently") }
                },
                dismissButton = { TextButton(onClick = { removalAll = null }) { Text("Keep entries") } }
            )
        }
        removal?.let { movie ->
            AlertDialog(
                onDismissRequest = { removal = null },
                title = { Text("Remove ${movie.name}?") },
                text = {
                    Text(
                        "This permanently removes the movie from this device, including ${movie.logCount} viewing/rating logs, " +
                            "watchlist status, credits, and entries in ${movie.listCount} custom lists. " +
                            "Use this only if you do not want the title in your library."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = { removal = null; screenModel.removeUnmatchedMovie(movie) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Remove permanently") }
                },
                dismissButton = { TextButton(onClick = { removal = null }) { Text("Keep movie") } }
            )
        }
    }

    @Composable
    private fun PendingEnrichmentCard(
        onContinue: () -> Unit,
        status: String?,
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

                if (status?.startsWith("Enrichment processed") == true ||
                    status?.startsWith("Error during import/enrich") == true
                ) {
                    Text(status, color = colors.onErrorContainer)
                }

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
        phase: ImportPhase,
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
                            ImportPhase.IMPORTING -> "Importing"
                            ImportPhase.ENRICHING -> "Enriching from TMDb"
                            ImportPhase.IDLE -> "Idle"
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
                        "Processed: $enrichedCount / $enrichedTotal",
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

