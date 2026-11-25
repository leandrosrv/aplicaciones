package com.example

import android.content.Context
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.GamificationRepository
import com.example.data.WorkoutDatabase
import com.example.data.WorkoutRepository
import com.example.data.buildWorkoutCsv
import com.example.gamification.data.AthleteRepository
import com.example.gamification.minigames.MiniGameKind
import com.example.gamification.minigames.MiniGamesDialog
import com.example.gamification.ui.AnimalAvatarSelectionScreen
import com.example.gamification.ui.AthleteScreen
import com.example.gamification.ui.AvatarCreationWizardDialog
import com.example.gamification.ui.GamificationSettingsDialog
import com.example.gamification.ui.GamificationSoundPreferences
import com.example.gamification.ui.GamificationViewModel
import com.example.gamification.ui.LevelUpSoundPlayer
import com.example.ui.UiState
import com.example.ui.WorkoutViewModel
import com.example.ui.components.PdfReportSpec
import com.example.ui.components.SessionImagePreviewDialog
import com.example.ui.components.SessionImageScope
import com.example.ui.components.SessionReportDialog
import com.example.ui.components.buildShareReportSummaryText
import com.example.ui.components.createShareablePdfUri
import com.example.ui.components.formatMmSs
import com.example.ui.components.launchSharePdfIntent
import com.example.ui.components.saveReportPdfToDownloads
import com.example.ui.components.saveReportPdfToUri
import com.example.ui.components.saveSessionBitmapToGallery
import com.example.ui.components.saveSessionBitmapToUri
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.LogSetScreen
import com.example.ui.screens.ProgressScreen
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.RegistroGymTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AppTab(
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String,
) {
    LOG(
        labelRes = R.string.nav_log,
        selectedIcon = Icons.Filled.AddCircle,
        unselectedIcon = Icons.Outlined.AddCircleOutline,
        tag = "nav_log",
    ),
    PROGRESS(
        labelRes = R.string.nav_progress,
        selectedIcon = Icons.AutoMirrored.Filled.ShowChart,
        unselectedIcon = Icons.AutoMirrored.Outlined.ShowChart,
        tag = "nav_progress",
    ),
    HISTORY(
        labelRes = R.string.nav_history,
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History,
        tag = "nav_history",
    ),
    ATHLETE(
        labelRes = R.string.nav_athlete,
        selectedIcon = Icons.Filled.AccessibilityNew,
        unselectedIcon = Icons.Outlined.AccessibilityNew,
        tag = "nav_athlete",
    ),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RegistroGymTheme {
                val viewModel: WorkoutViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            val app = checkNotNull(this[APPLICATION_KEY]) {
                                "APPLICATION_KEY missing from CreationExtras"
                            }
                            val dao = WorkoutDatabase.getInstance(app).workoutDao()
                            WorkoutViewModel(WorkoutRepository(dao))
                        }
                    },
                )
                val gamificationViewModel: GamificationViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            val app = checkNotNull(this[APPLICATION_KEY]) {
                                "APPLICATION_KEY missing from CreationExtras"
                            }
                            val db = WorkoutDatabase.getInstance(app)
                            val animScale = runCatching {
                                Settings.Global.getFloat(
                                    app.contentResolver,
                                    Settings.Global.ANIMATOR_DURATION_SCALE,
                                    1f,
                                )
                            }.getOrDefault(1f)
                            GamificationViewModel(
                                workoutRepository = WorkoutRepository(db.workoutDao()),
                                gamificationRepository = GamificationRepository(db.gamificationDao()),
                                athleteRepository = AthleteRepository(db.athleteProfileDao()),
                                systemReduceMotion = animScale == 0f,
                            )
                        }
                    },
                )
                MainWorkoutApp(
                    viewModel = viewModel,
                    gamificationViewModel = gamificationViewModel,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainWorkoutApp(
    viewModel: WorkoutViewModel,
    gamificationViewModel: GamificationViewModel,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentTab by rememberSaveable { mutableStateOf(AppTab.LOG) }
    var pendingCsvContent by remember { mutableStateOf<String?>(null) }
    var pendingImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pendingPdfSpec by remember { mutableStateOf<PdfReportSpec?>(null) }
    var showImageDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var isGeneratingPdf by remember { mutableStateOf(false) }
    var showAvatarWizard by remember { mutableStateOf(false) }
    var showAnimalSelectionScreen by rememberSaveable { mutableStateOf(false) }
    var showGamificationSettings by remember { mutableStateOf(false) }
    var showMiniGamesDialog by remember { mutableStateOf(false) }
    var selectedMiniGameKind by remember { mutableStateOf<MiniGameKind?>(null) }
    var levelUpSoundEnabled by remember {
        mutableStateOf(GamificationSoundPreferences.isLevelUpSoundEnabled(context))
    }

    val csvSaverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val content = pendingCsvContent
        pendingCsvContent = null
        if (uri != null && content != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(content.toByteArray(Charsets.UTF_8))
                }
            }.onSuccess {
                scope.launch {
                    snackbarHostState.showSnackbar(context.getString(R.string.msg_csv_exported))
                }
            }.onFailure { err ->
                scope.launch {
                    snackbarHostState.showSnackbar(err.localizedMessage ?: "Error al guardar CSV")
                }
            }
        }
    }

    val imageSaverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        val bmp = pendingImageBitmap
        pendingImageBitmap = null
        if (uri != null && bmp != null) {
            saveSessionBitmapToUri(context, bmp, uri)
                .onSuccess {
                    showImageDialog = false
                    scope.launch {
                        snackbarHostState.showSnackbar(context.getString(R.string.msg_image_saved))
                    }
                }
                .onFailure { err ->
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            err.localizedMessage ?: "No se pudo guardar la imagen"
                        )
                    }
                }
        }
    }

    val pdfSaverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val spec = pendingPdfSpec
        pendingPdfSpec = null
        if (uri != null && spec != null) {
            scope.launch {
                isGeneratingPdf = true
                val result = withContext(Dispatchers.IO) {
                    saveReportPdfToUri(context, spec, uri)
                }
                isGeneratingPdf = false
                result
                    .onSuccess {
                        showReportDialog = false
                        snackbarHostState.showSnackbar(context.getString(R.string.msg_pdf_saved))
                    }
                    .onFailure { err ->
                        snackbarHostState.showSnackbar(
                            err.localizedMessage ?: context.getString(R.string.msg_pdf_error)
                        )
                    }
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.snackbarEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.timerCompletedEvents.collect {
            triggerRestAlert(context)
            snackbarHostState.showSnackbar(context.getString(R.string.timer_finished))
        }
    }

    LaunchedEffect(gamificationViewModel) {
        gamificationViewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(gamificationViewModel) {
        gamificationViewModel.levelUpEvents.collect { event ->
            launch {
                LevelUpSoundPlayer.playLevelUpFanfare(context)
            }
            snackbarHostState.showSnackbar(
                "¡Subiste al Nivel ${event.newLevel}! ${event.rewardLabel}"
            )
        }
    }

    if (currentTab != AppTab.LOG) {
        BackHandler { currentTab = AppTab.LOG }
    }

    val setsState by viewModel.setsState.collectAsStateWithLifecycle()
    val muscleGroups by viewModel.muscleGroups.collectAsStateWithLifecycle()
    val entryMode by viewModel.entryMode.collectAsStateWithLifecycle()
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val planillaRows by viewModel.planillaRows.collectAsStateWithLifecycle()
    val isSavingPlanilla by viewModel.isSavingPlanilla.collectAsStateWithLifecycle()
    val summary by viewModel.summaryState.collectAsStateWithLifecycle()
    val summaryScope by viewModel.summaryScope.collectAsStateWithLifecycle()
    val chartGroup by viewModel.chartGroup.collectAsStateWithLifecycle()
    val chartExercise by viewModel.chartExercise.collectAsStateWithLifecycle()
    val availableChartExercises by viewModel.availableChartExercises.collectAsStateWithLifecycle()
    val chartMetric by viewModel.chartMetric.collectAsStateWithLifecycle()
    val chartGranularity by viewModel.chartGranularity.collectAsStateWithLifecycle()
    val chartModel by viewModel.chartModel.collectAsStateWithLifecycle()
    val prSummaries by viewModel.exercisePrSummaries.collectAsStateWithLifecycle()
    val historyGroupFilter by viewModel.historyGroupFilter.collectAsStateWithLifecycle()
    val historySearchQuery by viewModel.historySearchQuery.collectAsStateWithLifecycle()
    val restTimerState by viewModel.restTimerState.collectAsStateWithLifecycle()
    val gameSnapshot by gamificationViewModel.snapshot.collectAsStateWithLifecycle()
    val isSavingSettings by gamificationViewModel.isSavingSettings.collectAsStateWithLifecycle()
    val manualAvatarAction by gamificationViewModel.manualAction.collectAsStateWithLifecycle()
    val lastMiniGameOutcome by gamificationViewModel.lastMiniGameOutcome.collectAsStateWithLifecycle()

    LaunchedEffect(restTimerState.isTimerRunning) {
        gamificationViewModel.setRestTimerActive(restTimerState.isTimerRunning)
    }

    // Automatically open animal & gender selection screen on first visit to Mi Atleta
    LaunchedEffect(currentTab, gameSnapshot.enabled, gameSnapshot.onboardingCompleted) {
        if (currentTab == AppTab.ATHLETE && gameSnapshot.enabled && !gameSnapshot.onboardingCompleted) {
            showAnimalSelectionScreen = true
        }
    }

    val allSets = remember(setsState) {
        (setsState as? UiState.Success)?.data.orEmpty()
    }

    val recentExercisesForGroup = remember(allSets, formState.grupo) {
        allSets.filter { it.grupo.equals(formState.grupo, ignoreCase = true) }
            .map { it.ejercicio }
            .distinct()
            .take(8)
    }

    val openImageDialog = {
        if (allSets.isEmpty()) {
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.msg_image_empty))
            }
        } else {
            showImageDialog = true
        }
        Unit
    }

    val openReportDialog = {
        if (allSets.isEmpty()) {
            scope.launch {
                snackbarHostState.showSnackbar(context.getString(R.string.msg_pdf_empty))
            }
        } else {
            showReportDialog = true
        }
        Unit
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                    actions = {
                        if (restTimerState.isTimerRunning || restTimerState.isStopwatchRunning) {
                            Surface(
                                onClick = { currentTab = AppTab.LOG },
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(end = 6.dp),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        imageVector = if (restTimerState.isTimerRunning) {
                                            Icons.Filled.HourglassBottom
                                        } else {
                                            Icons.Filled.Timer
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                    Text(
                                        text = if (restTimerState.isTimerRunning) {
                                            formatMmSs(restTimerState.timerRemainingMillis)
                                        } else {
                                            formatMmSs(restTimerState.stopwatchElapsedMillis)
                                        },
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                }
                            }
                        }
                        if (gameSnapshot.enabled && restTimerState.isTimerRunning) {
                            IconButton(
                                onClick = {
                                    selectedMiniGameKind = null
                                    showMiniGamesDialog = true
                                },
                                modifier = Modifier.testTag("topbar_minigame_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SportsEsports,
                                    contentDescription = "Minijuego de pausa",
                                )
                            }
                        }
                        IconButton(
                            onClick = openImageDialog,
                            enabled = allSets.isNotEmpty(),
                            modifier = Modifier.testTag("topbar_save_image_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Image,
                                contentDescription = stringResource(R.string.btn_save_image),
                            )
                        }
                        IconButton(
                            onClick = openReportDialog,
                            enabled = allSets.isNotEmpty(),
                            modifier = Modifier.testTag("topbar_pdf_report_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PictureAsPdf,
                                contentDescription = stringResource(R.string.btn_pdf_report),
                            )
                        }
                        IconButton(
                            onClick = { showGamificationSettings = true },
                            modifier = Modifier.testTag("topbar_settings_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Ajustes de Mi Atleta",
                            )
                        }
                    },
                )
            },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar {
                        AppTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            val showDot = tab == AppTab.ATHLETE &&
                                gameSnapshot.enabled &&
                                gameSnapshot.biologicalClock.needsTrainingToday
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (showDot) Badge()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = stringResource(tab.labelRes),
                                        )
                                    }
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag(tab.tag),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (useNavRail) {
                    NavigationRail {
                        AppTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            val showDot = tab == AppTab.ATHLETE &&
                                gameSnapshot.enabled &&
                                gameSnapshot.biologicalClock.needsTrainingToday
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (showDot) Badge()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = stringResource(tab.labelRes),
                                        )
                                    }
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag(tab.tag),
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (currentTab) {
                        AppTab.LOG -> LogSetScreen(
                            entryMode = entryMode,
                            formState = formState,
                            planillaRows = planillaRows,
                            isSavingPlanilla = isSavingPlanilla,
                            allSets = allSets,
                            muscleGroups = muscleGroups,
                            recentExercisesForGroup = recentExercisesForGroup,
                            summary = summary,
                            summaryScope = summaryScope,
                            restTimerState = restTimerState,
                            canSaveImage = allSets.isNotEmpty(),
                            onEntryModeChange = viewModel::setEntryMode,
                            onOpenSaveImage = openImageDialog,
                            onOpenPdfReport = openReportDialog,
                            onSummaryScopeChange = viewModel::setSummaryScope,
                            onFechaChange = viewModel::updateFecha,
                            onGrupoChange = viewModel::updateGrupo,
                            onEjercicioChange = viewModel::updateEjercicio,
                            onSerieChange = viewModel::updateSerie,
                            onPesoChange = viewModel::updatePeso,
                            onNudgePeso = viewModel::nudgePeso,
                            onRepsChange = viewModel::updateReps,
                            onNudgeReps = viewModel::nudgeReps,
                            onRirChange = viewModel::updateRir,
                            onPesoCorporalChange = viewModel::updatePesoCorporal,
                            onObsChange = viewModel::updateObs,
                            onSubmitSet = viewModel::submitNewSet,
                            onAddCustomGroup = viewModel::addCustomGroup,
                            onAddPlanillaRow = viewModel::addPlanillaRow,
                            onDuplicatePlanillaRow = viewModel::duplicatePlanillaRow,
                            onRemovePlanillaRow = viewModel::removePlanillaRow,
                            onUpdatePlanillaRow = viewModel::updatePlanillaRow,
                            onLoadPreviousSession = viewModel::loadPreviousSessionIntoPlanilla,
                            onSubmitPlanilla = viewModel::submitPlanillaSets,
                            onTimerModeChange = viewModel::setTimerMode,
                            onSelectTimerPreset = viewModel::selectTimerPreset,
                            onAdjustTimer = viewModel::adjustTimerSeconds,
                            onToggleTimer = viewModel::toggleTimerRunning,
                            onResetTimer = viewModel::resetTimer,
                            onToggleStopwatch = viewModel::toggleStopwatchRunning,
                            onLapStopwatch = viewModel::recordStopwatchLap,
                            onResetStopwatch = viewModel::resetStopwatch,
                            onOpenMiniGames = if (gameSnapshot.enabled) {
                                {
                                    selectedMiniGameKind = null
                                    showMiniGamesDialog = true
                                }
                            } else null,
                            gamificationSnapshot = gameSnapshot,
                            onOpenAthleteTab = { currentTab = AppTab.ATHLETE },
                        )

                        AppTab.PROGRESS -> ProgressScreen(
                            muscleGroups = muscleGroups,
                            selectedGroup = chartGroup,
                            availableExercises = availableChartExercises,
                            selectedExercise = chartExercise,
                            selectedMetric = chartMetric,
                            selectedGranularity = chartGranularity,
                            chartModel = chartModel,
                            prSummaries = prSummaries,
                            canExportReport = allSets.isNotEmpty(),
                            onGroupChange = viewModel::setChartGroup,
                            onExerciseChange = viewModel::setChartExercise,
                            onMetricChange = viewModel::setChartMetric,
                            onGranularityChange = viewModel::setChartGranularity,
                            onOpenPdfReport = openReportDialog,
                        )

                        AppTab.HISTORY -> HistoryScreen(
                            setsState = setsState,
                            muscleGroups = muscleGroups,
                            selectedGroupFilter = historyGroupFilter,
                            searchQuery = historySearchQuery,
                            onGroupFilterChange = viewModel::setHistoryGroupFilter,
                            onSearchQueryChange = viewModel::setHistorySearchQuery,
                            onDeleteSet = viewModel::deleteSet,
                            onUpdateSet = viewModel::updateExistingSet,
                            onSaveImage = openImageDialog,
                            onOpenPdfReport = openReportDialog,
                            onExportCsv = { sets ->
                                if (sets.isEmpty()) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(context.getString(R.string.msg_csv_empty))
                                    }
                                } else {
                                    pendingCsvContent = buildWorkoutCsv(sets)
                                    csvSaverLauncher.launch("registro_gimnasio.csv")
                                }
                            },
                            onClearAll = viewModel::clearAllSets,
                        )

                        AppTab.ATHLETE -> {
                            if (showAnimalSelectionScreen && gameSnapshot.enabled) {
                                AnimalAvatarSelectionScreen(
                                    currentAppearance = gameSnapshot.appearance,
                                    level = gameSnapshot.levelProgress.level,
                                    backgroundTier = gameSnapshot.unlockedBackgroundTier,
                                    reduceMotion = gameSnapshot.reduceMotion,
                                    isSaving = isSavingSettings,
                                    onSaveSelection = { animal, gender, sportStyle ->
                                        gamificationViewModel.saveAnimalAndGenderSelection(
                                            animal = animal,
                                            gender = gender,
                                            sportStyle = sportStyle,
                                            onSaved = { showAnimalSelectionScreen = false },
                                        )
                                    },
                                    onBack = { showAnimalSelectionScreen = false },
                                )
                            } else {
                                AthleteScreen(
                                    snapshot = gameSnapshot,
                                    manualAction = manualAvatarAction,
                                    onToggleGamification = gamificationViewModel::setGamificationEnabled,
                                    onToggleReduceMotion = gamificationViewModel::setReduceMotionOverride,
                                    onOpenAnimalSelection = { showAnimalSelectionScreen = true },
                                    onOpenWizard = { showAvatarWizard = true },
                                    onOpenSettings = { showGamificationSettings = true },
                                    onTriggerAction = gamificationViewModel::triggerAvatarAction,
                                    onRefreshClock = gamificationViewModel::refreshBiologicalClockNow,
                                    onOpenMiniGame = { kind ->
                                        selectedMiniGameKind = kind
                                        showMiniGamesDialog = true
                                    },
                                    onNavigateToLog = { currentTab = AppTab.LOG },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showGamificationSettings) {
        GamificationSettingsDialog(
            snapshot = gameSnapshot,
            isSaving = isSavingSettings,
            levelUpSoundEnabled = levelUpSoundEnabled,
            onToggleLevelUpSound = { enabled ->
                levelUpSoundEnabled = enabled
                GamificationSoundPreferences.setLevelUpSoundEnabled(context, enabled)
            },
            onTestLevelUpSound = {
                scope.launch {
                    LevelUpSoundPlayer.playLevelUpFanfare(context)
                }
            },
            onSaveSettings = { enabled, reduceMotion, appearance ->
                gamificationViewModel.saveSettings(
                    gamificationEnabled = enabled,
                    reduceMotion = reduceMotion,
                    appearance = appearance,
                    onSaved = { showGamificationSettings = false },
                )
            },
            onOpenAnimalSelection = {
                showGamificationSettings = false
                currentTab = AppTab.ATHLETE
                showAnimalSelectionScreen = true
            },
            onOpenFullWizard = {
                showGamificationSettings = false
                showAvatarWizard = true
            },
            onResetGameProgressOnly = gamificationViewModel::resetGamificationProgressOnly,
            onDismiss = { showGamificationSettings = false },
        )
    }

    if (showAvatarWizard) {
        AvatarCreationWizardDialog(
            initialAppearance = gameSnapshot.appearance,
            level = gameSnapshot.levelProgress.level,
            reduceMotion = gameSnapshot.reduceMotion,
            onDismiss = { showAvatarWizard = false },
            onSave = { newAppearance ->
                gamificationViewModel.saveAppearance(
                    appearance = newAppearance,
                    markOnboardingComplete = true,
                    onSaved = { showAvatarWizard = false },
                )
            },
        )
    }

    if (showMiniGamesDialog && gameSnapshot.enabled) {
        MiniGamesDialog(
            appearance = gameSnapshot.appearance,
            restTimerState = restTimerState,
            dailyXpUsed = gameSnapshot.minigameXpEarnedToday,
            dailyXpCap = gameSnapshot.minigameDailyXpCap,
            lastOutcome = lastMiniGameOutcome,
            initialGame = selectedMiniGameKind,
            onGameFinished = { won -> gamificationViewModel.recordMiniGameResult(won) },
            onClearOutcome = gamificationViewModel::clearLastMiniGameOutcome,
            onDismiss = {
                gamificationViewModel.clearLastMiniGameOutcome()
                showMiniGamesDialog = false
                selectedMiniGameKind = null
            },
        )
    }

    if (showImageDialog) {
        SessionImagePreviewDialog(
            allSets = allSets,
            initialScope = SessionImageScope.LATEST,
            onDismiss = { showImageDialog = false },
            onSaveToGallery = { bitmap, fileName ->
                saveSessionBitmapToGallery(context, bitmap, fileName)
                    .onSuccess {
                        showImageDialog = false
                        scope.launch {
                            snackbarHostState.showSnackbar(context.getString(R.string.msg_image_saved))
                        }
                    }
                    .onFailure { err ->
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                err.localizedMessage ?: "No se pudo guardar en la galería"
                            )
                        }
                    }
            },
            onSaveWithPicker = { bitmap, fileName ->
                pendingImageBitmap = bitmap
                imageSaverLauncher.launch(fileName)
            },
        )
    }

    if (showReportDialog) {
        SessionReportDialog(
            allSets = allSets,
            muscleGroups = muscleGroups,
            initialScope = SessionImageScope.LATEST,
            initialChartGroup = chartGroup,
            initialMetric = chartMetric,
            initialGranularity = chartGranularity,
            isProcessing = isGeneratingPdf,
            onDismiss = { showReportDialog = false },
            onSharePdf = { spec, fileName ->
                scope.launch {
                    isGeneratingPdf = true
                    val uriResult = withContext(Dispatchers.IO) {
                        createShareablePdfUri(context, spec, fileName)
                    }
                    isGeneratingPdf = false
                    uriResult
                        .onSuccess { uri ->
                            showReportDialog = false
                            val subject = "Informe Registro Gym · ${spec.scopeSubtitle}"
                            val summaryText = buildShareReportSummaryText(spec)
                            launchSharePdfIntent(
                                context = context,
                                pdfUri = uri,
                                subject = subject,
                                bodyText = summaryText,
                            )
                        }
                        .onFailure { err ->
                            snackbarHostState.showSnackbar(
                                err.localizedMessage ?: context.getString(R.string.msg_pdf_error)
                            )
                        }
                }
            },
            onSaveToDownloads = { spec, fileName ->
                scope.launch {
                    isGeneratingPdf = true
                    val saveResult = withContext(Dispatchers.IO) {
                        saveReportPdfToDownloads(context, spec, fileName)
                    }
                    isGeneratingPdf = false
                    saveResult
                        .onSuccess {
                            showReportDialog = false
                            snackbarHostState.showSnackbar(context.getString(R.string.msg_pdf_saved))
                        }
                        .onFailure { err ->
                            snackbarHostState.showSnackbar(
                                err.localizedMessage ?: context.getString(R.string.msg_pdf_error)
                            )
                        }
                }
            },
            onSaveWithPicker = { spec, fileName ->
                pendingPdfSpec = spec
                pdfSaverLauncher.launch(fileName)
            },
        )
    }
}

private fun triggerRestAlert(context: Context) {
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator?.vibrate(
                VibrationEffect.createOneShot(350L, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(350L, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(350L)
            }
        }
    }
    runCatching {
        val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 300)
        Handler(Looper.getMainLooper()).postDelayed(
            { runCatching { tone.release() } },
            450L,
        )
    }
}
