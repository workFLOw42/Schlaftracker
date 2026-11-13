package de.schlafgut.app.ui.logger

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.schlafgut.app.data.entity.WakeWindow
import de.schlafgut.app.util.DateTimeUtil
import de.schlafgut.app.util.SleepCalculator

@Composable
internal fun SleepTypeToggle(
    isNap: Boolean,
    onIsNapChange: (Boolean) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = !isNap,
            onClick = { if (isNap) onIsNapChange(false) },
            label = { Text("\uD83C\uDF19 Nachtschlaf") }
        )
        FilterChip(
            selected = isNap,
            onClick = { if (!isNap) onIsNapChange(true) },
            label = { Text("\u2600\uFE0F Nickerchen") }
        )
    }
}

@Composable
internal fun SleepTimeCard(
    bedTime: Long,
    wakeTime: Long,
    sleepLatency: Int,
    wakeWindows: List<WakeWindow>,
    onBedTimeClick: () -> Unit,
    onWakeTimeClick: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Bettzeit", style = MaterialTheme.typography.labelLarge)
            Text(
                text = DateTimeUtil.formatDateFull(bedTime) + ", " + DateTimeUtil.formatTime(bedTime),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { onBedTimeClick() }
                    .padding(vertical = 8.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Aufwachzeit", style = MaterialTheme.typography.labelLarge)
            Text(
                text = DateTimeUtil.formatDateFull(wakeTime) + ", " + DateTimeUtil.formatTime(wakeTime),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { onWakeTimeClick() }
                    .padding(vertical = 8.dp)
            )
            val duration = SleepCalculator.calculateSleepDuration(
                bedTime, wakeTime, sleepLatency, wakeWindows
            )
            if (duration > 0) {
                Text(
                    "= ${DateTimeUtil.formatDuration(duration)} Schlaf",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
internal fun SleepLatencyCard(
    sleepLatency: Int,
    onSleepLatencyChange: (Int) -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Einschlafzeit", style = MaterialTheme.typography.labelLarge)
                Text("$sleepLatency min", style = MaterialTheme.typography.bodyMedium)
            }
            Slider(
                value = sleepLatency.toFloat(),
                onValueChange = { onSleepLatencyChange(it.toInt()) },
                valueRange = 0f..60f,
                steps = 59
            )
        }
    }
}
