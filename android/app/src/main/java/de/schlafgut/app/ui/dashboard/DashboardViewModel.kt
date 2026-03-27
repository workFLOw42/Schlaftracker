package de.schlafgut.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.schlafgut.app.data.entity.SleepEntryEntity
import de.schlafgut.app.data.repository.SleepRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HealthDataSummary(
    val steps: Int? = null,
    val restingHeartRate: Int? = null
)

data class DashboardUiState(
    val recentEntries: List<SleepEntryEntity> = emptyList(),
    val totalEntries: Int = 0,
    val averageQuality: Double? = null,
    val averageDurationMinutes: Double? = null,
    val lastNightDurationMinutes: Int? = null,
    val healthData: HealthDataSummary? = null,
    val userName: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    repository: SleepRepository
) : ViewModel() {

    private val _healthData = MutableStateFlow<HealthDataSummary?>(null)

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.getRecentEntries(5),
        repository.getEntryCount(),
        repository.getAverageQuality(),
        repository.getAverageDuration(),
        combine(
            _healthData,
            repository.getSettings().map { it?.userName }
        ) { health, name -> health to name }
    ) { recent, count, avgQuality, avgDuration, healthAndName ->
        val (health, name) = healthAndName
        DashboardUiState(
            recentEntries = recent,
            totalEntries = count,
            averageQuality = avgQuality,
            averageDurationMinutes = avgDuration,
            lastNightDurationMinutes = recent
                .firstOrNull { !it.isNap }
                ?.sleepDurationMinutes,
            healthData = health,
            userName = name
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )
}
