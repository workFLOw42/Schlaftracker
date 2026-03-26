package de.schlafgut.app.ui.logger

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.schlafgut.app.data.entity.SleepEntryEntity
import de.schlafgut.app.data.entity.WakeWindow
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
    val isSaved: Boolean = false
)

@HiltViewModel
class SleepLoggerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SleepRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SleepLoggerUiState())
    val uiState: StateFlow<SleepLoggerUiState> = _uiState.asStateFlow()

    init {
        val entryId = savedStateHandle.get<String>("entryId")
        if (entryId != null) {
            loadEntry(entryId)
        } else {
            viewModelScope.launch {
                val settings = repository.getSettingsOnce()
                _uiState.update { it.copy(sleepLatency = settings.defaultSleepLatency) }
            }
        }
    }

    private fun loadEntry(id: String) {
        viewModelScope.launch {
            val entry = repository.getEntryById(id) ?: return@launch
            _uiState.value = SleepLoggerUiState(
                isNap = entry.isNap,
                bedTime = entry.bedTime,
                wakeTime = entry.wakeTime,
                sleepLatency = entry.sleepLatency,
                wakeWindows = entry.wakeWindows,
                quality = entry.quality,
                notes = entry.notes,
                isEditing = true,
                editingId = entry.id
            )
        }
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

    fun addWakeWindow(start: Long, end: Long) {
        val window = WakeWindow(start, end)
        val state = _uiState.value

        if (!SleepCalculator.validateWakeWindow(window, state.bedTime, state.wakeTime)) {
            _uiState.update {
                it.copy(errorMessage = "Die Wachphase muss innerhalb der Schlafzeit liegen.")
            }
            return
        }

        _uiState.update {
            it.copy(wakeWindows = (it.wakeWindows + window).sortedBy { w -> w.start })
        }
    }

    fun removeWakeWindow(index: Int) {
        _uiState.update {
            it.copy(wakeWindows = it.wakeWindows.toMutableList().apply { removeAt(index) })
        }
    }

    fun save() {
        val state = _uiState.value

        // Validate
        if (!SleepCalculator.validateTimeOrder(state.bedTime, state.wakeTime)) {
            _uiState.update {
                it.copy(errorMessage = "Die Aufwachzeit muss nach der Bettzeit liegen.")
            }
            return
        }

        if (!SleepCalculator.validatePositiveDuration(
                state.bedTime, state.wakeTime, state.sleepLatency, state.wakeWindows
            )
        ) {
            _uiState.update {
                it.copy(
                    errorMessage = "Die berechnete Schlafzeit ist negativ. " +
                        "Pr\u00fcfe Einschlafzeit und Wachphasen."
                )
            }
            return
        }

        viewModelScope.launch {
            val id = state.editingId ?: UUID.randomUUID().toString()

            // Overlap check
            val overlaps = repository.findOverlappingEntries(state.bedTime, state.wakeTime, id)
            if (overlaps.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        errorMessage = "Dieser Zeitraum \u00fcberschneidet sich mit einem " +
                            "existierenden Eintrag."
                    )
                }
                return@launch
            }

            val sleepDuration = SleepCalculator.calculateSleepDuration(
                state.bedTime, state.wakeTime, state.sleepLatency, state.wakeWindows
            )
            val wakeDuration = SleepCalculator.calculateWakeDuration(state.wakeWindows)

            val entry = SleepEntryEntity(
                id = id,
                isNap = state.isNap,
                date = state.bedTime,
                bedTime = state.bedTime,
                wakeTime = state.wakeTime,
                sleepLatency = state.sleepLatency,
                sleepDurationMinutes = sleepDuration,
                wakeDurationMinutes = wakeDuration,
                wakeWindows = state.wakeWindows,
                interruptionCount = state.wakeWindows.size,
                quality = state.quality,
                tags = emptyList(),
                notes = state.notes
            )

            repository.saveEntry(entry)
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
