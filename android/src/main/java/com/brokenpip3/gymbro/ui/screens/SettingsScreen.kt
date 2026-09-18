package com.brokenpip3.gymbro.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.brokenpip3.gymbro.BuildConfig
import com.brokenpip3.gymbro.GymbroApplication
import com.brokenpip3.gymbro.backup.BackupScope
import com.brokenpip3.gymbro.backup.GymbroBackupCodec
import com.brokenpip3.gymbro.backup.GymbroBackupFileStorage
import com.brokenpip3.gymbro.backup.GymbroBackupFileStore
import com.brokenpip3.gymbro.backup.GymbroBackupStore
import com.brokenpip3.gymbro.backup.InvalidBackupException
import com.brokenpip3.gymbro.data.demo.DemoDataSeeder
import com.brokenpip3.gymbro.ui.theme.ThemeMode
import com.brokenpip3.gymbro.ui.theme.ThemeSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val SOURCE_CODE_URL = "https://github.com/brokenpip3/gymbro"

data class SettingsUiState(
    val message: String? = null,
    val isBusy: Boolean = false,
    val isError: Boolean = false,
    val isFileImportConfirmationVisible: Boolean = false,
)

@Suppress("TooManyFunctions")
class SettingsViewModel(
    private val backupStore: GymbroBackupStore,
    private val fileStorage: GymbroBackupFileStore? = null,
    private val isWorkoutActive: suspend () -> Boolean = { false },
    private val demoDataSeeder: DemoDataSeeder? = null,
    coroutineScope: CoroutineScope? = null,
) : ViewModel() {
    private val scope = coroutineScope ?: viewModelScope

    private var pendingExportScope: BackupScope? = null
    private var pendingFileImportUri: String? = null

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun loadDemoData() {
        val seeder = demoDataSeeder ?: return
        _uiState.update { it.copy(isBusy = true, message = null, isError = false) }
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            runCatching { seeder.seed() }
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(isBusy = false, message = result.message, isError = false)
                    }
                }.onFailure {
                    _uiState.update {
                        it.copy(isBusy = false, message = "Unable to load demo data", isError = true)
                    }
                }
        }
    }

    fun suggestedExportFileName(scope: BackupScope): String {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return "gymbro-${scope.wireValue}-$date.json"
    }

    fun startFileExport(scope: BackupScope) {
        pendingExportScope = scope
    }

    fun onFileExportLocationSelected(uri: String) {
        val storage = fileStorage ?: return
        val exportScope = pendingExportScope
        pendingExportScope = null
        if (exportScope == null) {
            _uiState.update { it.copy(message = "Pick a scope before exporting", isError = true) }
            return
        }
        _uiState.update { it.copy(isBusy = true, message = null, isError = false) }
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            runCatching {
                storage.write(uri, backupStore.exportText(exportScope))
            }.onSuccess {
                _uiState.update {
                    it.copy(isBusy = false, message = "Export complete", isError = false)
                }
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(isBusy = false, message = exception.importErrorMessage(), isError = true)
                }
            }
        }
    }

    fun onFileImportLocationSelected(uri: String) {
        val storage = fileStorage ?: return
        _uiState.update { it.copy(isBusy = true, message = null, isError = false) }
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            runCatching {
                if (isWorkoutActive()) {
                    throw InvalidBackupException("Finish or discard the active workout before importing")
                }
                val text = storage.read(uri)
                val scope =
                    BackupScope.fromWireValueOrNull(GymbroBackupCodec.decode(text).scope)
                        ?: BackupScope.FULL
                Pair(text, scope)
            }.onSuccess { (text, scope) ->
                if (scope == BackupScope.FULL) {
                    pendingFileImportUri = uri
                    _uiState.update {
                        it.copy(isBusy = false, isFileImportConfirmationVisible = true, isError = false)
                    }
                } else {
                    _uiState.update { it.copy(isBusy = true, message = null, isError = false) }
                    runCatching { backupStore.importText(text) }
                        .onSuccess {
                            _uiState.update {
                                it.copy(isBusy = false, message = "Import complete", isError = false)
                            }
                        }.onFailure { exception ->
                            _uiState.update {
                                it.copy(
                                    isBusy = false,
                                    message = exception.importErrorMessage(),
                                    isError = true,
                                )
                            }
                        }
                }
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(isBusy = false, message = exception.importErrorMessage(), isError = true)
                }
            }
        }
    }

    fun dismissFileImportConfirmation() {
        pendingFileImportUri = null
        _uiState.update { it.copy(isFileImportConfirmationVisible = false) }
    }

    fun confirmFileImport() {
        val storage = fileStorage ?: return
        val uri = pendingFileImportUri
        pendingFileImportUri = null
        _uiState.update {
            it.copy(isBusy = true, isFileImportConfirmationVisible = false, message = null, isError = false)
        }
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            runCatching {
                backupStore.importText(storage.read(uri ?: ""))
            }.onSuccess {
                _uiState.update {
                    it.copy(isBusy = false, message = "Import complete", isError = false)
                }
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(isBusy = false, message = exception.importErrorMessage(), isError = true)
                }
            }
        }
    }
}

@Composable
fun SettingsRoute(
    backupStore: GymbroBackupStore,
    themeSettings: ThemeSettings,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val application = context.applicationContext as GymbroApplication
    val fileStorage = remember { GymbroBackupFileStorage(context.contentResolver) }
    val demoDataSeeder = remember(application) { DemoDataSeeder(application.database) }
    val viewModel: SettingsViewModel =
        viewModel(
            factory =
                remember(backupStore) {
                    SettingsViewModelFactory(
                        backupStore = backupStore,
                        fileStorage = fileStorage,
                        isWorkoutActive = { application.workoutRepository.hasActiveWorkoutRun() },
                        demoDataSeeder = demoDataSeeder,
                    )
                },
        )
    val state by viewModel.uiState.collectAsState()
    val themeMode by themeSettings.themeMode.collectAsState()

    val exportFileLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            uri?.let { viewModel.onFileExportLocationSelected(it.toString()) }
        }
    val importFileLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { viewModel.onFileImportLocationSelected(it.toString()) }
        }

    SettingsScreen(
        state = state,
        themeMode = themeMode,
        onThemeModeSelected = themeSettings::setThemeMode,
        onConfirmFileImport = viewModel::confirmFileImport,
        onDismissFileImportConfirmation = viewModel::dismissFileImportConfirmation,
        onExportFileScopeClick = { scope ->
            viewModel.startFileExport(scope)
            exportFileLauncher.launch(viewModel.suggestedExportFileName(scope))
        },
        onImportFileClick = { importFileLauncher.launch(arrayOf("application/json")) },
        onLoadDemoData = if (BuildConfig.DEBUG) viewModel::loadDemoData else null,
        modifier = modifier,
    )
}

@Composable
@Suppress("LongParameterList")
fun SettingsScreen(
    state: SettingsUiState,
    themeMode: ThemeMode = ThemeMode.System,
    onThemeModeSelected: (ThemeMode) -> Unit = {},
    onConfirmFileImport: () -> Unit = {},
    onDismissFileImportConfirmation: () -> Unit = {},
    onExportFileScopeClick: (BackupScope) -> Unit = {},
    onImportFileClick: () -> Unit = {},
    onLoadDemoData: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(state.message) {
        state.message?.let { snackbarHostState.showSnackbar(it) }
    }

    if (state.isFileImportConfirmationVisible) {
        AlertDialog(
            onDismissRequest = onDismissFileImportConfirmation,
            title = { Text(text = "Replace local data?") },
            text = { Text(text = "Importing this backup will replace all local Gymbro data.") },
            confirmButton = {
                TextButton(onClick = onConfirmFileImport) {
                    Text(text = "Import")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissFileImportConfirmation) {
                    Text(text = "Cancel")
                }
            },
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SettingsAboutSection { url ->
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
            SettingsAppearanceSection(
                themeMode = themeMode,
                onThemeModeSelected = onThemeModeSelected,
            )
            onLoadDemoData?.let { loadDemoData ->
                SettingsDemoDataSection(
                    state = state,
                    onLoadDemoData = loadDemoData,
                )
            }
            SettingsFileBackupSection(
                state = state,
                onExportScopeClick = onExportFileScopeClick,
                onImportClick = onImportFileClick,
            )
            if (state.isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                )
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .testTag("settings-snackbar"),
            snackbar = { snackbarData ->
                Snackbar(
                    snackbarData = snackbarData,
                    containerColor =
                        if (state.isError) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.inverseSurface
                        },
                    contentColor =
                        if (state.isError) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.inverseOnSurface
                        },
                )
            },
        )
    }
}

@Composable
private fun SettingsDemoDataSection(
    state: SettingsUiState,
    onLoadDemoData: () -> Unit,
) {
    SettingsSectionTitle(text = "Developer tools")
    OutlinedButton(
        onClick = onLoadDemoData,
        enabled = !state.isBusy,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = "Load demo data")
    }
}

@Composable
private fun SettingsAboutSection(onSourceCodeClick: (String) -> Unit) {
    SettingsSectionTitle(text = "About")
    Text(
        text = "Gymbro ${BuildConfig.VERSION_NAME}",
        style = MaterialTheme.typography.titleLarge,
    )
    Text(
        text = "Private training, measurable progress.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(top = 4.dp),
    )
    Text(
        text = SOURCE_CODE_URL,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp).clickable { onSourceCodeClick(SOURCE_CODE_URL) },
    )
}

@Composable
private fun SettingsAppearanceSection(
    themeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit,
) {
    SettingsSectionTitle(text = "Appearance")
    Text(
        text = "Theme",
        style = MaterialTheme.typography.bodyMedium,
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        ThemeMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = mode == themeMode,
                onClick = { onThemeModeSelected(mode) },
                shape =
                    SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = ThemeMode.entries.size,
                    ),
                modifier = Modifier.weight(1f),
            ) {
                Text(text = mode.label)
            }
        }
    }
}

@Composable
private fun SettingsFileBackupSection(
    state: SettingsUiState,
    onExportScopeClick: (BackupScope) -> Unit,
    onImportClick: () -> Unit,
) {
    SettingsSectionTitle(text = "Backup file")
    Button(
        onClick = { onExportScopeClick(BackupScope.EXERCISES) },
        enabled = !state.isBusy,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = "Export exercises")
    }
    Button(
        onClick = { onExportScopeClick(BackupScope.EXERCISES_SCHEDULES) },
        enabled = !state.isBusy,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = "Export exercises + schedules")
    }
    Button(
        onClick = { onExportScopeClick(BackupScope.FULL) },
        enabled = !state.isBusy,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = "Export everything")
    }
    OutlinedButton(
        onClick = onImportClick,
        enabled = !state.isBusy,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = "Import from file")
    }
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
}

private class SettingsViewModelFactory(
    private val backupStore: GymbroBackupStore,
    private val fileStorage: GymbroBackupFileStore,
    private val isWorkoutActive: suspend () -> Boolean,
    private val demoDataSeeder: DemoDataSeeder,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(
                backupStore = backupStore,
                fileStorage = fileStorage,
                isWorkoutActive = isWorkoutActive,
                demoDataSeeder = demoDataSeeder,
            ) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

private fun Throwable.importErrorMessage(): String =
    if (this is InvalidBackupException) {
        message ?: "Invalid backup"
    } else {
        "Unable to import backup"
    }
