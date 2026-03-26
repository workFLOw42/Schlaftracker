package de.schlafgut.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.schlafgut.app.data.entity.SleepEntryEntity
import de.schlafgut.app.data.repository.SleepRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DashboardUiState(
    val recentEntries: List<SleepEntryEntity> = emptyList(),
    val totalEntries: Int = 0,
    val averageQuality: Double? = null,
    val averageDurationMinutes: Double? = null,
    val lastNightDurationMinutes: Int? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    repository: SleepRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.getRecentEntries(5),
        repository.getEntryCount(),
        repository.getAverageQuality(),
        repository.getAverageDuration()
    ) { recent, count, avgQuality, avgDuration ->
        DashboardUiState(
            recentEntries = recent,
            totalEntries = count,
            averageQuality = avgQuality,
            averageDurationMinutes = avgDuration,
            lastNightDurationMinutes = recent
                .firstOrNull { !it.isNap }
                ?.sleepDurationMinutes
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )
}
