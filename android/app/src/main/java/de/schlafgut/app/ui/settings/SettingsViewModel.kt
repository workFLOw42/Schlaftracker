package de.schlafgut.app.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val successMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SleepRepository,
    private val healthConnectManager: HealthConnectManager
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
                        onboardingCompleted = s.onboardingCompleted
                    )
                }
            }
        }
    }

    fun setUserName(name: String) = _uiState.update { it.copy(userName = name) }
    fun setDefaultLatency(minutes: Int) = _uiState.update { it.copy(defaultSleepLatency = minutes) }
    fun setAppLockEnabled(enabled: Boolean) = _uiState.update { it.copy(appLockEnabled = enabled) }
    fun setHealthConnectEnabled(enabled: Boolean) = _uiState.update { it.copy(healthConnectEnabled = enabled) }
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
                    onboardingCompleted = true
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

    fun getHealthConnectInstallIntent(): Intent = healthConnectManager.getInstallIntent()

    val healthConnectPermissions get() = healthConnectManager.permissions
    val healthConnectPermissionContract get() = healthConnectManager.requestPermissionContract
}
