package com.brokenpip3.gymbro.ui.screens

import com.brokenpip3.gymbro.backup.BackupExercise
import com.brokenpip3.gymbro.backup.BackupScope
import com.brokenpip3.gymbro.backup.GymbroBackup
import com.brokenpip3.gymbro.backup.GymbroBackupCodec
import com.brokenpip3.gymbro.backup.GymbroBackupData
import com.brokenpip3.gymbro.backup.GymbroBackupDataSource
import com.brokenpip3.gymbro.backup.GymbroBackupFileStore
import com.brokenpip3.gymbro.backup.GymbroBackupStore
import com.brokenpip3.gymbro.backup.InvalidBackupException
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsViewModelTest {
    @Test
    fun fileExportWritesScopedJsonAndReportsSuccess() =
        runTest {
            val storage = FakeSettingsFileStorage()
            val viewModel = settingsViewModel(fileStorage = storage)
            viewModel.startFileExport(BackupScope.EXERCISES)
            viewModel.onFileExportLocationSelected("content://test/backup.json")
            advanceUntilIdle()
            assertTrue(storage.files["content://test/backup.json"]!!.contains("\"scope\": \"exercises\""))
            assertEquals("Export complete", viewModel.uiState.value.message)
            assertFalse(viewModel.uiState.value.isError)
        }

    @Test
    fun fileImportOfFullScopeAsksForConfirmationBeforeReplacing() =
        runTest {
            val storage = FakeSettingsFileStorage()
            storage.files["content://test/backup.json"] = importBackupJson()
            val dataSource = FakeSettingsBackupDataSource()
            val viewModel =
                settingsViewModel(
                    fileStorage = storage,
                    dataSource = dataSource,
                )
            viewModel.onFileImportLocationSelected("content://test/backup.json")
            advanceUntilIdle()
            assertEquals(null, dataSource.replacedData)
            assertTrue(viewModel.uiState.value.isFileImportConfirmationVisible)

            viewModel.confirmFileImport()
            advanceUntilIdle()
            assertEquals("Import complete", viewModel.uiState.value.message)
            assertFalse(viewModel.uiState.value.isError)
        }

    @Test
    fun fileImportOfPartialScopeMergesWithoutConfirmation() =
        runTest {
            val storage = FakeSettingsFileStorage()
            storage.files["content://test/backup.json"] = importBackupJson(scope = "exercises")
            val dataSource = FakeSettingsBackupDataSource()
            val viewModel =
                settingsViewModel(
                    fileStorage = storage,
                    dataSource = dataSource,
                )
            viewModel.onFileImportLocationSelected("content://test/backup.json")
            advanceUntilIdle()
            assertEquals("Import complete", viewModel.uiState.value.message)
            assertFalse(viewModel.uiState.value.isError)
        }

    @Test
    fun fileImportRejectsWhenWorkoutActive() =
        runTest {
            val viewModel = settingsViewModel(isWorkoutActive = { true })
            viewModel.onFileImportLocationSelected("content://test/backup.json")
            advanceUntilIdle()
            assertEquals("Finish or discard the active workout before importing", viewModel.uiState.value.message)
            assertTrue(viewModel.uiState.value.isError)
            assertFalse(viewModel.uiState.value.isFileImportConfirmationVisible)
        }

    @Test
    fun fileImportReportsInvalidBackupError() =
        runTest {
            val storage = FakeSettingsFileStorage()
            storage.files["content://test/backup.json"] = "not json"
            val viewModel = settingsViewModel(fileStorage = storage)
            viewModel.onFileImportLocationSelected("content://test/backup.json")
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isError)
            assertEquals("Invalid Gymbro backup JSON", viewModel.uiState.value.message)
        }
}

private fun settingsViewModel(
    fileStorage: GymbroBackupFileStore = FakeSettingsFileStorage(),
    dataSource: GymbroBackupDataSource = FakeSettingsBackupDataSource(),
    isWorkoutActive: () -> Boolean = { false },
): SettingsViewModel =
    SettingsViewModel(
        backupStore = GymbroBackupStore(dataSource),
        fileStorage = fileStorage,
        isWorkoutActive = isWorkoutActive,
        coroutineScope = null,
    )

private fun importBackupJson(scope: String = "full"): String =
    GymbroBackupCodec.encode(
        GymbroBackup(
            schemaVersion = GymbroBackupCodec.SUPPORTED_SCHEMA_VERSION,
            scope = scope,
            exercises =
                listOf(
                    BackupExercise(
                        id = 1,
                        name = "Squat",
                        notes = null,
                        trackingMode = "strength",
                        createdAt = 1,
                        updatedAt = 1,
                    ),
                ),
        ),
    )

private class FakeSettingsFileStorage : GymbroBackupFileStore {
    val files = mutableMapOf<String, String>()

    override fun write(
        uri: String,
        text: String,
    ) {
        files[uri] = text
    }

    override fun read(uri: String): String {
        val text = files[uri]
        if (text == null) {
            throw InvalidBackupException("Could not open file for reading: $uri")
        }
        return text
    }
}

private class FakeSettingsBackupDataSource(
    private val data: GymbroBackupData = emptyBackupData(),
) : GymbroBackupDataSource {
    var replacedData: GymbroBackupData? = null
    var mergedData: GymbroBackupData? = null

    override suspend fun readBackupData(): GymbroBackupData = data

    override suspend fun replaceAll(data: GymbroBackupData) {
        replacedData = data
    }

    override suspend fun merge(data: GymbroBackupData) {
        mergedData = data
    }
}

private fun emptyBackupData(): GymbroBackupData =
    GymbroBackupData(
        exercises = emptyList(),
        schedules = emptyList(),
        scheduleExercises = emptyList(),
        workoutRuns = emptyList(),
        exerciseResults = emptyList(),
        setResults = emptyList(),
    )
