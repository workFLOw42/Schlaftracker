package de.schlafgut.app.ui.logger

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.schlafgut.app.data.entity.HealthSnapshotEntity
import de.schlafgut.app.data.entity.MedicationEntry
import de.schlafgut.app.data.entity.SleepEntryEntity
import de.schlafgut.app.data.entity.WakeWindow
import de.schlafgut.app.data.health.HealthConnectManager
import de.schlafgut.app.data.health.HealthDataRepository
import de.schlafgut.app.data.repository.SleepRepository
import de.schlafgut.app.util.DateTimeUtil
import de.schlafgut.app.util.SleepCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class SleepLoggerUiState(
    val isNap: Boolean = false,
    val bedTime: Long = DateTimeUtil.defaultBedTime(),
    val wakeTime: Long = DateTimeUtil.defaultWakeTime(),
    val sleepLatency: Int = 15,
    val wakeWindows: List<WakeWindow> = emptyList(),
    val quality: Int = 7,
    val notes: String = "",
    val isEditing: Boolean = false,
    val editingId: String? = null,
    val errorMessage: String? = null,
    val isSaved: Boolean = false,

    // Substanzkonsum
    val alcoholLevel: Int = 0,
    val drugLevel: Int = 0,
    val sleepAid: Boolean = false,
    val medication: Boolean = false,
    val medicationNotes: String = "",

    // Health Connect Daten
    val healthConnectEnabled: Boolean = false,
    val healthSnapshot: HealthSnapshotEntity? = null,
    val isLoadingHealth: Boolean = false,
    val healthError: String? = null,

    // Reguläre Medikamente aus den Einstellungen
    val regularMedications: List<MedicationEntry> = emptyList()
)

@HiltViewModel
class SleepLoggerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SleepRepository,
    private val healthConnectManager: HealthConnectManager,
    private val healthDataRepository: HealthDataRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SleepLoggerUiState())
    val uiState: StateFlow<SleepLoggerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = repository.getSettingsOnce()
            _uiState.update {
                it.copy(
                    healthConnectEnabled = settings.healthConnectEnabled,
                    sleepLatency = if (it.isEditing) it.sleepLatency else settings.defaultSleepLatency,
                    regularMedications = settings.regularMedications
                )
            }
        }

        val entryId = savedStateHandle.get<String>("entryId")
        if (entryId != null) {
            loadEntry(entryId)
        }
    }

    private fun loadEntry(id: String) {
        viewModelScope.launch {
            val entry = repository.getEntryById(id) ?: return@launch
            _uiState.update {
                it.copy(
                    isNap = entry.isNap,
                    bedTime = entry.bedTime,
                    wakeTime = entry.wakeTime,
                    sleepLatency = entry.sleepLatency,
                    wakeWindows = entry.wakeWindows,
                    quality = entry.quality,
                    notes = entry.notes,
                    isEditing = true,
                    editingId = entry.id,
                    alcoholLevel = entry.alcoholLevel,
                    drugLevel = entry.drugLevel,
                    sleepAid = entry.sleepAid,
                    medication = entry.medication,
                    medicationNotes = entry.medicationNotes
                )
            }
            val snapshot = repository.getHealthSnapshot(id)
            _uiState.update { it.copy(healthSnapshot = snapshot) }
        }
    }

    /** Health-Daten von Health Connect für den aktuellen Zeitraum laden/aktualisieren. */
    fun refreshHealthData() {
        val state = _uiState.value
        if (!state.healthConnectEnabled) return
        if (healthConnectManager.checkAvailability() != HealthConnectManager.Availability.AVAILABLE) return

        _uiState.update { it.copy(isLoadingHealth = true, healthError = null) }

        viewModelScope.launch {
            try {
                val settings = repository.getSettingsOnce()
                val entryId = state.editingId ?: "temp_${System.currentTimeMillis()}"
                val snapshot = healthDataRepository.fetchHealthSnapshot(
                    sleepEntryId = entryId,
                    bedTimeEpoch = state.bedTime,
                    wakeTimeEpoch = state.wakeTime,
                    settings = settings
                )
                _uiState.update {
                    it.copy(
                        healthSnapshot = snapshot,
                        isLoadingHealth = false,
                        healthError = if (snapshot == null) "Keine Health-Daten für diesen Zeitraum gefunden" else null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingHealth = false,
                        healthError = "Fehler beim Laden: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearHealthData() {
        _uiState.update { it.copy(healthSnapshot = null, healthError = null) }
    }

    fun setIsNap(isNap: Boolean) {
        _uiState.update { state ->
            if (isNap) {
                state.copy(
                    isNap = true,
                    bedTime = DateTimeUtil.defaultNapBedTime(),
                    wakeTime = DateTimeUtil.defaultNapWakeTime(),
                    sleepLatency = 5,
                    wakeWindows = emptyList()
                )
            } else {
                state.copy(
                    isNap = false,
                    bedTime = DateTimeUtil.defaultBedTime(),
                    wakeTime = DateTimeUtil.defaultWakeTime(),
                    sleepLatency = 15,
                    wakeWindows = emptyList()
                )
            }
        }
    }

    fun setBedTime(time: Long) = _uiState.update { it.copy(bedTime = time) }
    fun setWakeTime(time: Long) = _uiState.update { it.copy(wakeTime = time) }
    fun setSleepLatency(minutes: Int) = _uiState.update { it.copy(sleepLatency = minutes) }
    fun setQuality(quality: Int) = _uiState.update { it.copy(quality = quality) }
    fun setNotes(notes: String) = _uiState.update { it.copy(notes = notes) }
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    fun setAlcoholLevel(level: Int) = _uiState.update { it.copy(alcoholLevel = level) }
    fun setDrugLevel(level: Int) = _uiState.update { it.copy(drugLevel = level) }
    fun setSleepAid(enabled: Boolean) = _uiState.update { it.copy(sleepAid = enabled) }
    fun setMedication(enabled: Boolean) = _uiState.update {
        it.copy(medication = enabled, medicationNotes = if (!enabled) "" else it.medicationNotes)
    }
    fun setMedicationNotes(notes: String) = _uiState.update { it.copy(medicationNotes = notes) }

    fun addWakeWindow(wakeWindow: WakeWindow) {
        val state = _uiState.value
        if (!SleepCalculator.validateWakeWindow(wakeWindow, state.bedTime, state.wakeTime)) {
            _uiState.update { it.copy(errorMessage = "Die Wachphase muss innerhalb der Schlafzeit liegen.") }
            return
        }
        _uiState.update { it.copy(wakeWindows = (it.wakeWindows + wakeWindow).sortedBy { w -> w.start }) }
    }

    fun removeWakeWindow(index: Int) {
        _uiState.update { it.copy(wakeWindows = it.wakeWindows.toMutableList().apply { removeAt(index) }) }
    }

    fun save() {
        val state = _uiState.value

        if (!SleepCalculator.validateTimeOrder(state.bedTime, state.wakeTime)) {
            _uiState.update { it.copy(errorMessage = "Die Aufwachzeit muss nach der Bettzeit liegen.") }
            return
        }
        if (!SleepCalculator.validatePositiveDuration(state.bedTime, state.wakeTime, state.sleepLatency, state.wakeWindows)) {
            _uiState.update { it.copy(errorMessage = "Die berechnete Schlafzeit ist negativ. Prüfe Einschlafzeit und Wachphasen.") }
            return
        }

        viewModelScope.launch {
            val id = state.editingId ?: UUID.randomUUID().toString()

            val overlaps = repository.findOverlappingEntries(state.bedTime, state.wakeTime, id)
            if (overlaps.isNotEmpty()) {
                _uiState.update { it.copy(errorMessage = "Dieser Zeitraum überschneidet sich mit einem existierenden Eintrag.") }
                return@launch
            }

            val sleepDuration = SleepCalculator.calculateSleepDuration(state.bedTime, state.wakeTime, state.sleepLatency, state.wakeWindows)
            val wakeDuration = SleepCalculator.calculateWakeDuration(state.wakeWindows)

            val entry = SleepEntryEntity(
                id = id, isNap = state.isNap, date = state.bedTime,
                bedTime = state.bedTime, wakeTime = state.wakeTime,
                sleepLatency = state.sleepLatency, sleepDurationMinutes = sleepDuration,
                wakeDurationMinutes = wakeDuration, wakeWindows = state.wakeWindows,
                interruptionCount = state.wakeWindows.size, quality = state.quality,
                tags = emptyList(), notes = state.notes,
                alcoholLevel = state.alcoholLevel, drugLevel = state.drugLevel,
                sleepAid = state.sleepAid, medication = state.medication,
                medicationNotes = state.medicationNotes
            )

            repository.saveEntry(entry)
            state.healthSnapshot?.let { snapshot ->
                repository.saveHealthSnapshot(snapshot.copy(sleepEntryId = id))
            }
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    fun delete() {
        val id = _uiState.value.editingId ?: return
        viewModelScope.launch {
            repository.deleteEntry(id)
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
