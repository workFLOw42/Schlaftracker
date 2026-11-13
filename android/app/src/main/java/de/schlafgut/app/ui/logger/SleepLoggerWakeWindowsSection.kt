package de.schlafgut.app.ui.logger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.schlafgut.app.data.entity.WakeWindow
import de.schlafgut.app.ui.theme.DangerRed
import de.schlafgut.app.ui.theme.WakeWindowColor
import de.schlafgut.app.util.DateTimeUtil
import de.schlafgut.app.util.SleepCalculator

@Composable
internal fun WakeWindowsCard(
    wakeWindows: List<WakeWindow>,
    onRemoveWakeWindow: (index: Int) -> Unit,
    onAddWakeWindowClick: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Wachphasen", style = MaterialTheme.typography.labelLarge)
                if (wakeWindows.isNotEmpty()) {
                    val totalMin = SleepCalculator.calculateWakeDuration(wakeWindows)
                    Text(
                        "${wakeWindows.size}x, $totalMin min",
                        style = MaterialTheme.typography.bodySmall,
                        color = WakeWindowColor
                    )
                }
            }
            wakeWindows.forEachIndexed { index, window ->
                WakeWindowItem(window = window, onRemove = { onRemoveWakeWindow(index) })
            }
            OutlinedButton(
                onClick = onAddWakeWindowClick,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("+ Wachphase hinzufügen")
            }
        }
    }
}

@Composable
private fun WakeWindowItem(
    window: WakeWindow,
    onRemove: () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${DateTimeUtil.formatTime(window.start)} \u2013 ${DateTimeUtil.formatTime(window.end)} (${window.durationMinutes} min)",
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(onClick = onRemove) {
                Text("Entfernen", color = DangerRed)
            }
        }
        window.outOfBedPeriods.forEach { oob ->
            Text(
                "   \uD83D\uDEB6 Aufgestanden: ${DateTimeUtil.formatTime(oob.start)} \u2013 ${DateTimeUtil.formatTime(oob.end)} (${oob.durationMinutes} min)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}
