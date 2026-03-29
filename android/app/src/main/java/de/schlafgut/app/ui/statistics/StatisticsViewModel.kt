package de.schlafgut.app.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.schlafgut.app.data.entity.HealthSnapshotEntity
import de.schlafgut.app.data.entity.SleepEntryEntity
import de.schlafgut.app.data.repository.SleepRepository
import de.schlafgut.app.util.DateTimeUtil
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class StatisticsUiState(
    val entries: List<SleepEntryEntity> = emptyList(),
    val healthSnapshots: List<HealthSnapshotEntity> = emptyList(),
    val startDate: Long = DateTimeUtil.daysAgo(7),
    val endDate: Long = DateTimeUtil.todayEnd(),

    // Nachtschlaf-Statistiken (ohne Nickerchen)
    val nightCount: Int = 0,
    val averageQuality: Double = 0.0,
    val averageDurationMinutes: Double = 0.0,
    val averageInterruptions: Double = 0.0,

    // Nickerchen-Statistiken
    val napCount: Int = 0,
    val averageNapDurationMinutes: Double = 0.0,

    // Health-Statistiken
    val averageWeight: Double? = null,
    val averageRestingHr: Double? = null,
    val averageSteps: Double? = null,

    // Substanz-Statistiken
    val entriesWithAlcohol: Int = 0,
    val entriesWithDrugs: Int = 0,
    val entriesWithSleepAid: Int = 0,
    val avgQualityWithAlcohol: Double? = null,
    val avgQualityWithoutAlcohol: Double? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val repository: SleepRepository
) : ViewModel() {

    private val _dateRange = MutableStateFlow(
        DateTimeUtil.daysAgo(7) to DateTimeUtil.todayEnd()
    )

    val uiState: StateFlow<StatisticsUiState> = _dateRange.flatMapLatest { (start, end) ->
        combine(
            repository.getEntriesInRange(start, end),
            repository.getHealthSnapshotsInRange(start, end)
        ) { entries, snapshots -> Triple(entries, snapshots, start to end) }
    }.combine(_dateRange) { (entries, snapshots, _), (start, end) ->

        val nights = entries.filter { !it.isNap }
        val naps = entries.filter { it.isNap }

        // Health-Durchschnitte
        val weights = snapshots.mapNotNull { it.weightKg }
        val restingHrs = snapshots.mapNotNull { it.restingHeartRate }
        val steps = snapshots.mapNotNull { it.stepsTotal }

        // Substanz-Auswertung (nur Nachtschlaf)
        val nightsWithAlcohol = nights.filter { it.alcoholLevel > 0 }
        val nightsWithoutAlcohol = nights.filter { it.alcoholLevel == 0 }

        StatisticsUiState(
            entries = entries,
            healthSnapshots = snapshots,
            startDate = start,
            endDate = end,

            // Nachtschlaf
            nightCount = nights.size,
            averageQuality = if (nights.isEmpty()) 0.0
            else nights.map { it.quality }.average(),
            averageDurationMinutes = if (nights.isEmpty()) 0.0
            else nights.map { it.sleepDurationMinutes }.average(),
            averageInterruptions = if (nights.isEmpty()) 0.0
            else nights.map { it.interruptionCount }.average(),

            // Nickerchen
            napCount = naps.size,
            averageNapDurationMinutes = if (naps.isEmpty()) 0.0
            else naps.map { it.sleepDurationMinutes }.average(),

            // Health
            averageWeight = if (weights.isEmpty()) null else weights.average(),
            averageRestingHr = if (restingHrs.isEmpty()) null else restingHrs.map { it.toDouble() }.average(),
            averageSteps = if (steps.isEmpty()) null else steps.map { it.toDouble() }.average(),

            // Substanzen (Nachtschlaf)
            entriesWithAlcohol = nightsWithAlcohol.size,
            entriesWithDrugs = nights.count { it.drugLevel > 0 },
            entriesWithSleepAid = nights.count { it.sleepAid },
            avgQualityWithAlcohol = if (nightsWithAlcohol.isEmpty()) null
            else nightsWithAlcohol.map { it.quality }.average(),
            avgQualityWithoutAlcohol = if (nightsWithoutAlcohol.isEmpty()) null
            else nightsWithoutAlcohol.map { it.quality }.average()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatisticsUiState()
    )

    fun setDateRange(startDate: Long, endDate: Long) {
        _dateRange.update { startDate to endDate }
    }
}
