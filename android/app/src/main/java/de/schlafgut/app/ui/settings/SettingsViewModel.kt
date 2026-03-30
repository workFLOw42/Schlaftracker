package de.schlafgut.app.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.schlafgut.app.data.backup.DriveBackupManager
import de.schlafgut.app.data.entity.MedicationEntry
import de.schlafgut.app.data.entity.UserSettingsEntity
import de.schlafgut.app.data.export.JsonImportExport
import de.schlafgut.app.data.health.HealthConnectManager
import de.schlafgut.app.data.repository.SleepRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
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

    // Individuelle Health-Datentyp-Auswahl
    val healthReadWeight: Boolean = true,
    val healthReadBodyTemp: Boolean = true,
    val healthReadRestingHr: Boolean = true,
    val healthReadHeartRate: Boolean = true,
    val healthReadSpO2: Boolean = true,
    val healthReadSteps: Boolean = true,
    val healthReadSleep: Boolean = true,

    // Google Drive Backup
    val driveAccountEmail: String? = null,
    val isDriveBackupRunning: Boolean = false,
    val showPasswordDialog: Boolean = false,
    val passwordDialogMode: PasswordDialogMode = PasswordDialogMode.UPLOAD,
    val signInError: String? = null
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
                        regularMedications = s.regularMedications,
                        healthReadWeight = s.healthReadWeight,
                        healthReadBodyTemp = s.healthReadBodyTemp,
                        healthReadRestingHr = s.healthReadRestingHr,
                        healthReadHeartRate = s.healthReadHeartRate,
                        healthReadSpO2 = s.healthReadSpO2,
                        healthReadSteps = s.healthReadSteps,
                        healthReadSleep = s.healthReadSleep
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

    private fun currentSettingsFromState(): UserSettingsEntity {
        val state = _uiState.value
        return UserSettingsEntity(
            userName = state.userName,
            defaultSleepLatency = state.defaultSleepLatency,
            appLockEnabled = state.appLockEnabled,
            healthConnectEnabled = state.healthConnectEnabled,
            onboardingCompleted = true,
            regularMedications = state.regularMedications,
            healthReadWeight = state.healthReadWeight,
            healthReadBodyTemp = state.healthReadBodyTemp,
            healthReadRestingHr = state.healthReadRestingHr,
            healthReadHeartRate = state.healthReadHeartRate,
            healthReadSpO2 = state.healthReadSpO2,
            healthReadSteps = state.healthReadSteps,
            healthReadSleep = state.healthReadSleep
        )
    }

    fun setUserName(name: String) = _uiState.update { it.copy(userName = name) }
    fun setDefaultLatency(minutes: Int) = _uiState.update { it.copy(defaultSleepLatency = minutes) }

    fun setAppLockEnabled(enabled: Boolean) {
        _uiState.update { it.copy(appLockEnabled = enabled) }
        persistSettings()
    }

    fun setHealthConnectEnabled(enabled: Boolean) {
        _uiState.update { it.copy(healthConnectEnabled = enabled) }
        persistSettings()
        if (enabled) loadHealthProfileData()
    }

    // --- Individuelle Health-Datentyp-Toggles ---
    fun setHealthReadWeight(v: Boolean) { _uiState.update { it.copy(healthReadWeight = v) }; persistSettings() }
    fun setHealthReadBodyTemp(v: Boolean) { _uiState.update { it.copy(healthReadBodyTemp = v) }; persistSettings() }
    fun setHealthReadRestingHr(v: Boolean) { _uiState.update { it.copy(healthReadRestingHr = v) }; persistSettings() }
    fun setHealthReadHeartRate(v: Boolean) { _uiState.update { it.copy(healthReadHeartRate = v) }; persistSettings() }
    fun setHealthReadSpO2(v: Boolean) { _uiState.update { it.copy(healthReadSpO2 = v) }; persistSettings() }
    fun setHealthReadSteps(v: Boolean) { _uiState.update { it.copy(healthReadSteps = v) }; persistSettings() }
    fun setHealthReadSleep(v: Boolean) { _uiState.update { it.copy(healthReadSleep = v) }; persistSettings() }

    // --- Feste Medikamente ---
    fun addMedication(name: String, dosage: String) {
        val meds = _uiState.value.regularMedications + MedicationEntry(name, dosage)
        _uiState.update { it.copy(regularMedications = meds) }
        persistSettings()
    }

    fun removeMedication(index: Int) {
        val meds = _uiState.value.regularMedications.toMutableList().apply { removeAt(index) }
        _uiState.update { it.copy(regularMedications = meds) }
        persistSettings()
    }

    fun showClearDataDialog(show: Boolean) = _uiState.update { it.copy(showClearDataDialog = show) }
    fun clearSuccessMessage() = _uiState.update { it.copy(successMessage = null) }

    fun saveSettings() {
        _uiState.update { it.copy(successMessage = "Einstellungen gespeichert") }
        persistSettings()
    }

    private fun persistSettings() {
        viewModelScope.launch {
            repository.saveSettings(currentSettingsFromState())
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.deleteAllEntries()
            _uiState.update { it.copy(showClearDataDialog = false, successMessage = "Alle Daten gelöscht") }
        }
    }

    fun exportJson(context: Context) {
        viewModelScope.launch {
            val settings = repository.getSettingsOnce()
            val entries = repository.getAllEntries().first()
            val jsonString = JsonImportExport.exportToJson(entries, settings)
            val uri = JsonImportExport.exportToFile(context, jsonString)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Backup exportieren"))
        }
    }

    fun importJson(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val jsonString = inputStream?.bufferedReader()?.readText() ?: return@launch
                inputStream.close()
                val result = JsonImportExport.importFromJson(jsonString)
                var imported = 0; var skipped = 0
                for (entry in result.entries) {
                    if (repository.getEntryById(entry.id) == null) { repository.saveEntry(entry); imported++ } else { skipped++ }
                }
                result.settings?.let { s ->
                    val current = repository.getSettingsOnce()
                    if (current.userName.isBlank() && s.userName.isNotBlank()) {
                        repository.saveSettings(current.copy(userName = s.userName, defaultSleepLatency = s.defaultSleepLatency))
                    }
                }
                _uiState.update { it.copy(successMessage = "$imported Einträge importiert" + if (skipped > 0) ", $skipped übersprungen (Duplikate)" else "") }
            } catch (e: Exception) {
                _uiState.update { it.copy(successMessage = "Import fehlgeschlagen: ${e.message}") }
            }
        }
    }

    // --- Google Drive Backup ---
    fun setDriveAccount(email: String) { _uiState.update { it.copy(driveAccountEmail = email, signInError = null) } }
    fun setSignInError(message: String) { _uiState.update { it.copy(signInError = message) } }
    fun clearSignInError() { _uiState.update { it.copy(signInError = null) } }
    fun showPasswordDialog(mode: PasswordDialogMode) { _uiState.update { it.copy(showPasswordDialog = true, passwordDialogMode = mode) } }
    fun dismissPasswordDialog() { _uiState.update { it.copy(showPasswordDialog = false) } }

    fun uploadDriveBackup(password: String) {
        val email = _uiState.value.driveAccountEmail ?: return
        _uiState.update { it.copy(isDriveBackupRunning = true, showPasswordDialog = false) }
        viewModelScope.launch {
            val result = driveBackupManager.uploadBackup(email, password)
            _uiState.update { it.copy(isDriveBackupRunning = false, successMessage = result.getOrElse { e -> "Backup fehlgeschlagen: ${e.message}" }) }
        }
    }

    fun restoreDriveBackup(password: String) {
        val email = _uiState.value.driveAccountEmail ?: return
        _uiState.update { it.copy(isDriveBackupRunning = true, showPasswordDialog = false) }
        viewModelScope.launch {
            val result = driveBackupManager.downloadAndRestoreBackup(email, password)
            _uiState.update { it.copy(isDriveBackupRunning = false, successMessage = result.getOrElse { e -> "Wiederherstellung fehlgeschlagen: ${e.message}" }) }
        }
    }

    fun getHealthConnectInstallIntent(): Intent = healthConnectManager.getInstallIntent()
    val healthConnectPermissions get() = healthConnectManager.permissions
    val healthConnectPermissionContract get() = healthConnectManager.requestPermissionContract
}
