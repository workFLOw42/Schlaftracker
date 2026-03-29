package de.schlafgut.app.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.schlafgut.app.data.backup.DriveBackupManager
import de.schlafgut.app.data.entity.HealthSnapshotEntity
import de.schlafgut.app.data.entity.MedicationEntry
import de.schlafgut.app.data.entity.UserSettingsEntity
import de.schlafgut.app.data.export.JsonImportExport
import de.schlafgut.app.data.health.HealthConnectManager
import de.schlafgut.app.data.repository.SleepRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val userName: String = "",
    val defaultSleepLatency: Int = 15,
    val appLockEnabled: Boolean = false,
    val healthConnectEnabled: Boolean = false,
    val healthConnectAvailability: HealthConnectManager.Availability = HealthConnectManager.Availability.NOT_SUPPORTED,
    val onboardingCompleted: Boolean = false,
    val showClearDataDialog: Boolean = false,
    val successMessage: String? = null,

    // Feste Medikamente
    val regularMedications: List<MedicationEntry> = emptyList(),

    // Health Connect Profil-Daten
    val latestWeight: Double? = null,
    val latestRestingHr: Int? = null,
    val latestSteps: Int? = null,
    val latestSpO2: Double? = null,

    // Google Drive Backup
    val driveAccountEmail: String? = null,
    val isDriveBackupRunning: Boolean = false,
    val showPasswordDialog: Boolean = false,
    val passwordDialogMode: PasswordDialogMode = PasswordDialogMode.UPLOAD
)

enum class PasswordDialogMode { UPLOAD, RESTORE }

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SleepRepository,
    private val healthConnectManager: HealthConnectManager,
    private val driveBackupManager: DriveBackupManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        _uiState.update {
            it.copy(healthConnectAvailability = healthConnectManager.checkAvailability())
        }
        viewModelScope.launch {
            repository.getSettings().collect { settings ->
                val s = settings ?: UserSettingsEntity()
                _uiState.update {
                    it.copy(
                        userName = s.userName,
                        defaultSleepLatency = s.defaultSleepLatency,
                        appLockEnabled = s.appLockEnabled,
                        healthConnectEnabled = s.healthConnectEnabled,
                        onboardingCompleted = s.onboardingCompleted,
                        regularMedications = s.regularMedications
                    )
                }
            }
        }
        loadHealthProfileData()
    }

    private fun loadHealthProfileData() {
        viewModelScope.launch {
            val settings = repository.getSettingsOnce()
            if (!settings.healthConnectEnabled) return@launch

            val entries = repository.getRecentEntriesOnce(1)
            val lastEntry = entries.firstOrNull() ?: return@launch
            val snapshot = repository.getHealthSnapshot(lastEntry.id) ?: return@launch

            _uiState.update {
                it.copy(
                    latestWeight = snapshot.weightKg,
                    latestRestingHr = snapshot.restingHeartRate,
                    latestSteps = snapshot.stepsTotal,
                    latestSpO2 = snapshot.oxygenSaturation
                )
            }
        }
    }

    fun setUserName(name: String) = _uiState.update { it.copy(userName = name) }
    fun setDefaultLatency(minutes: Int) = _uiState.update { it.copy(defaultSleepLatency = minutes) }

    fun setAppLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val currentSettings = repository.getSettingsOnce()
            repository.saveSettings(currentSettings.copy(appLockEnabled = enabled))
        }
    }

    fun setHealthConnectEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val currentSettings = repository.getSettingsOnce()
            repository.saveSettings(currentSettings.copy(healthConnectEnabled = enabled))
            if (enabled) loadHealthProfileData()
        }
    }

    // --- Feste Medikamente ---

    fun addMedication(name: String, dosage: String) {
        val meds = _uiState.value.regularMedications + MedicationEntry(name, dosage)
        _uiState.update { it.copy(regularMedications = meds) }
    }

    fun removeMedication(index: Int) {
        val meds = _uiState.value.regularMedications.toMutableList().apply { removeAt(index) }
        _uiState.update { it.copy(regularMedications = meds) }
    }

    fun showClearDataDialog(show: Boolean) = _uiState.update { it.copy(showClearDataDialog = show) }
    fun clearSuccessMessage() = _uiState.update { it.copy(successMessage = null) }

    fun saveSettings() {
        viewModelScope.launch {
            val state = _uiState.value
            repository.saveSettings(
                UserSettingsEntity(
                    userName = state.userName,
                    defaultSleepLatency = state.defaultSleepLatency,
                    appLockEnabled = state.appLockEnabled,
                    healthConnectEnabled = state.healthConnectEnabled,
                    onboardingCompleted = true,
                    regularMedications = state.regularMedications
                )
            )
            _uiState.update { it.copy(successMessage = "Einstellungen gespeichert") }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.deleteAllEntries()
            _uiState.update {
                it.copy(
                    showClearDataDialog = false,
                    successMessage = "Alle Daten gel\u00f6scht"
                )
            }
        }
    }

    fun exportJson(context: Context) {
        viewModelScope.launch {
            val settings = repository.getSettingsOnce()
            repository.getAllEntries().collect { entries ->
                val jsonString = JsonImportExport.exportToJson(entries, settings)
                val uri = JsonImportExport.exportToFile(context, jsonString)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Backup exportieren"))
                return@collect
            }
        }
    }

    fun importJson(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val jsonString = inputStream?.bufferedReader()?.readText() ?: return@launch
                inputStream.close()

                val result = JsonImportExport.importFromJson(jsonString)
                var imported = 0
                var skipped = 0

                for (entry in result.entries) {
                    val existing = repository.getEntryById(entry.id)
                    if (existing == null) {
                        repository.saveEntry(entry)
                        imported++
                    } else {
                        skipped++
                    }
                }

                result.settings?.let { settings ->
                    val current = repository.getSettingsOnce()
                    if (current.userName.isBlank() && settings.userName.isNotBlank()) {
                        repository.saveSettings(
                            current.copy(
                                userName = settings.userName,
                                defaultSleepLatency = settings.defaultSleepLatency
                            )
                        )
                    }
                }

                _uiState.update {
                    it.copy(
                        successMessage = "$imported Eintr\u00e4ge importiert" +
                            if (skipped > 0) ", $skipped \u00fcbersprungen (Duplikate)" else ""
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(successMessage = "Import fehlgeschlagen: ${e.message}")
                }
            }
        }
    }

    // --- Google Drive Backup ---

    fun setDriveAccount(email: String) {
        _uiState.update { it.copy(driveAccountEmail = email) }
    }

    fun showPasswordDialog(mode: PasswordDialogMode) {
        _uiState.update { it.copy(showPasswordDialog = true, passwordDialogMode = mode) }
    }

    fun dismissPasswordDialog() {
        _uiState.update { it.copy(showPasswordDialog = false) }
    }

    fun uploadDriveBackup(password: String) {
        val email = _uiState.value.driveAccountEmail ?: return
        _uiState.update { it.copy(isDriveBackupRunning = true, showPasswordDialog = false) }
        viewModelScope.launch {
            val result = driveBackupManager.uploadBackup(email, password)
            _uiState.update {
                it.copy(
                    isDriveBackupRunning = false,
                    successMessage = result.getOrElse { e ->
                        "Backup fehlgeschlagen: ${e.message}"
                    }
                )
            }
        }
    }

    fun restoreDriveBackup(password: String) {
        val email = _uiState.value.driveAccountEmail ?: return
        _uiState.update { it.copy(isDriveBackupRunning = true, showPasswordDialog = false) }
        viewModelScope.launch {
            val result = driveBackupManager.downloadAndRestoreBackup(email, password)
            _uiState.update {
                it.copy(
                    isDriveBackupRunning = false,
                    successMessage = result.getOrElse { e ->
                        "Wiederherstellung fehlgeschlagen: ${e.message}"
                    }
                )
            }
        }
    }

    fun getHealthConnectInstallIntent(): Intent = healthConnectManager.getInstallIntent()

    val healthConnectPermissions get() = healthConnectManager.permissions
    val healthConnectPermissionContract get() = healthConnectManager.requestPermissionContract
}
