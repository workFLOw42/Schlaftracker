package de.schlafgut.app.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val startDate: Long = DateTimeUtil.daysAgo(7),
    val endDate: Long = DateTimeUtil.todayEnd(),
    val averageQuality: Double = 0.0,
    val averageDurationMinutes: Double = 0.0,
    val averageInterruptions: Double = 0.0
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
        repository.getEntriesInRange(start, end)
    }.combine(_dateRange) { entries, (start, end) ->
        StatisticsUiState(
            entries = entries,
            startDate = start,
            endDate = end,
            averageQuality = if (entries.isEmpty()) 0.0
            else entries.map { it.quality }.average(),
            averageDurationMinutes = if (entries.isEmpty()) 0.0
            else entries.map { it.sleepDurationMinutes }.average(),
            averageInterruptions = if (entries.isEmpty()) 0.0
            else entries.map { it.interruptionCount }.average()
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
