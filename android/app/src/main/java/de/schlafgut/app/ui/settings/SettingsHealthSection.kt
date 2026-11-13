package de.schlafgut.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.schlafgut.app.data.health.HealthConnectManager

/** Gebündelte Callbacks für die einzelnen Health-Connect-Datentyp-Schalter. */
internal class HealthToggleCallbacks(
    val onWeightChange: (Boolean) -> Unit,
    val onBodyTempChange: (Boolean) -> Unit,
    val onRestingHrChange: (Boolean) -> Unit,
    val onHeartRateChange: (Boolean) -> Unit,
    val onSpO2Change: (Boolean) -> Unit,
    val onStepsChange: (Boolean) -> Unit,
    val onSleepChange: (Boolean) -> Unit
)

@Composable
internal fun SettingsHealthSection(
    state: SettingsUiState,
    onInstallHealthConnect: () -> Unit,
    onHealthConnectEnabledChange: (Boolean) -> Unit,
    toggles: HealthToggleCallbacks
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Health Connect", style = MaterialTheme.typography.titleMedium)
            when (state.healthConnectAvailability) {
                HealthConnectManager.Availability.NOT_SUPPORTED -> Text(
                    "Health Connect wird auf diesem Gerät nicht unterstützt",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HealthConnectManager.Availability.NOT_INSTALLED -> {
                    Text(
                        "Health Connect muss installiert werden",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(onClick = onInstallHealthConnect) {
                        Text("Health Connect installieren")
                    }
                }

                HealthConnectManager.Availability.AVAILABLE -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Körperdaten lesen", style = MaterialTheme.typography.labelLarge)
                            Text(
                                "Aktiviere Health Connect und wähle Datentypen",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.healthConnectEnabled,
                            onCheckedChange = onHealthConnectEnabledChange
                        )
                    }
                    if (state.healthConnectEnabled) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            "Welche Daten einlesen?",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        HealthToggleRow("\u2696\uFE0F Gewicht", state.healthReadWeight) {
                            toggles.onWeightChange(it)
                        }
                        HealthToggleRow("\uD83C\uDF21\uFE0F Körpertemperatur", state.healthReadBodyTemp) {
                            toggles.onBodyTempChange(it)
                        }
                        HealthToggleRow("\u2764\uFE0F Ruhepuls", state.healthReadRestingHr) {
                            toggles.onRestingHrChange(it)
                        }
                        HealthToggleRow("\uD83D\uDC93 Herzfrequenz (Nacht)", state.healthReadHeartRate) {
                            toggles.onHeartRateChange(it)
                        }
                        HealthToggleRow("\uD83E\uDEC1 SpO\u2082", state.healthReadSpO2) {
                            toggles.onSpO2Change(it)
                        }
                        HealthToggleRow("\uD83D\uDC63 Schritte", state.healthReadSteps) {
                            toggles.onStepsChange(it)
                        }
                        HealthToggleRow("\uD83D\uDE34 Schlaf", state.healthReadSleep) {
                            toggles.onSleepChange(it)
                        }
                        LatestHealthValues(state)
                    }
                }
            }
        }
    }
}

@Composable
private fun LatestHealthValues(state: SettingsUiState) {
    val hasData = state.latestWeight != null ||
        state.latestRestingHr != null ||
        state.latestSteps != null ||
        state.latestSpO2 != null
    if (!hasData) return

    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
    Text("Letzte Messwerte", style = MaterialTheme.typography.labelLarge)
    state.latestWeight?.let {
        Text(
            "\u2696\uFE0F Gewicht: ${String.format("%.1f", it)} kg",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    state.latestRestingHr?.let {
        Text(
            "\u2764\uFE0F Ruhepuls: $it bpm",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    state.latestSteps?.let {
        Text(
            "\uD83D\uDC63 Schritte: $it",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    state.latestSpO2?.let {
        Text(
            "\uD83E\uDEC1 SpO\u2082: ${String.format("%.0f", it)}%",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HealthToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
