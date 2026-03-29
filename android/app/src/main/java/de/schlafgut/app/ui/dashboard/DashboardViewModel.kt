package de.schlafgut.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.schlafgut.app.data.entity.HealthSnapshotEntity
import de.schlafgut.app.data.entity.SleepEntryEntity
import de.schlafgut.app.data.health.HealthDataRepository
import de.schlafgut.app.data.repository.SleepRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HealthDataSummary(
    val weightKg: Double? = null,
    val restingHeartRate: Int? = null,
    val avgNightHeartRate: Int? = null,
    val oxygenSaturation: Double? = null,
    val stepsTotal: Int? = null,
    val bodyTempCelsius: Double? = null
) {
    val hasAnyData: Boolean
        get() = weightKg != null || restingHeartRate != null || avgNightHeartRate != null ||
                oxygenSaturation != null || stepsTotal != null || bodyTempCelsius != null
}

/**
 * Nickerchen-Zusammenfassung.
 */
data class NapSummary(
    val totalNaps: Int = 0,
    val totalNights: Int = 0,
    val averageNapDurationMinutes: Double? = null
) {
    /** z.B. "Alle 3 Tage" oder null wenn keine Naps */
    val frequencyText: String?
        get() {
            if (totalNaps == 0 || totalNights == 0) return null
            val everyXDays = totalNights.toDouble() / totalNaps
            return if (everyXDays <= 1.5) "Täglich"
            else "Alle ${String.format("%.0f", everyXDays)} Tage"
        }
}

data class DashboardUiState(
    val recentEntries: List<SleepEntryEntity> = emptyList(),
    val totalEntries: Int = 0,
    val averageQuality: Double? = null,          // nur Nachtschlaf
    val averageDurationMinutes: Double? = null,  // nur Nachtschlaf
    val lastNightDurationMinutes: Int? = null,
    val healthData: HealthDataSummary? = null,
    val userName: String? = null,
    val napSummary: NapSummary = NapSummary()
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: SleepRepository,
    private val healthDataRepository: HealthDataRepository
) : ViewModel() {

    private val _healthData = MutableStateFlow<HealthDataSummary?>(null)

    init {
        loadHealthData()
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.getRecentEntries(5),
        repository.getAllEntries(),        // für Nap-Statistik
        repository.getAverageQuality(),    // filtert bereits nach !isNap
        repository.getAverageDuration(),   // filtert bereits nach !isNap
        combine(
            _healthData,
            repository.getSettings().map { it?.userName }
        ) { health, name -> health to name }
    ) { recent, allEntries, avgQuality, avgDuration, healthAndName ->
        val (health, name) = healthAndName

        val nights = allEntries.filter { !it.isNap }
        val naps = allEntries.filter { it.isNap }

        DashboardUiState(
            recentEntries = recent,
            totalEntries = nights.size,
            averageQuality = avgQuality,
            averageDurationMinutes = avgDuration,
            lastNightDurationMinutes = recent
                .firstOrNull { !it.isNap }
                ?.sleepDurationMinutes,
            healthData = health,
            userName = name,
            napSummary = NapSummary(
                totalNaps = naps.size,
                totalNights = nights.size,
                averageNapDurationMinutes = if (naps.isEmpty()) null
                else naps.map { it.sleepDurationMinutes }.average()
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    private fun loadHealthData() {
        viewModelScope.launch {
            val settings = repository.getSettingsOnce()
            if (!settings.healthConnectEnabled) return@launch

            val entries = repository.getRecentEntriesOnce(1)
            val lastEntry = entries.firstOrNull() ?: return@launch

            var snapshot = repository.getHealthSnapshot(lastEntry.id)

            val oneHourAgo = System.currentTimeMillis() - 3_600_000
            val shouldRefresh = snapshot == null || snapshot.fetchedAt < oneHourAgo

            if (shouldRefresh) {
                val fresh = healthDataRepository.fetchHealthSnapshot(
                    sleepEntryId = lastEntry.id,
                    bedTimeEpoch = lastEntry.bedTime,
                    wakeTimeEpoch = lastEntry.wakeTime
                )
                if (fresh != null) {
                    repository.saveHealthSnapshot(fresh)
                    snapshot = fresh
                }
            }

            _healthData.value = snapshot?.toSummary()
        }
    }

    private fun HealthSnapshotEntity.toSummary() = HealthDataSummary(
        weightKg = weightKg,
        restingHeartRate = restingHeartRate,
        avgNightHeartRate = avgNightHeartRate,
        oxygenSaturation = oxygenSaturation,
        stepsTotal = stepsTotal,
        bodyTempCelsius = bodyTempCelsius
    )
}
