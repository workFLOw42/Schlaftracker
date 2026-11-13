package de.schlafgut.app.ui.logger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.schlafgut.app.data.entity.HealthSnapshotEntity
import de.schlafgut.app.ui.theme.DangerRed

@Composable
internal fun HealthDataCard(
    snapshot: HealthSnapshotEntity?,
    isLoading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onClear: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("\u2764\uFE0F Health Connect", style = MaterialTheme.typography.labelLarge)
                Row {
                    if (snapshot != null) {
                        TextButton(onClick = onClear) {
                            Text("Entfernen", style = MaterialTheme.typography.bodySmall, color = DangerRed)
                        }
                    }
                    IconButton(onClick = onRefresh, enabled = !isLoading) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Aktualisieren",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            if (snapshot != null) {
                snapshot.weightKg?.let {
                    Text(
                        "\u2696\uFE0F Gewicht: ${String.format("%.1f", it)} kg",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                snapshot.bodyTempCelsius?.let {
                    Text(
                        "\uD83C\uDF21\uFE0F Körpertemp.: ${String.format("%.1f", it)} °C",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                snapshot.restingHeartRate?.let {
                    Text(
                        "\u2764\uFE0F Ruhepuls: $it bpm",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                snapshot.avgNightHeartRate?.let {
                    Text(
                        "\uD83D\uDC93 Ø Nacht-HF: $it bpm",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                snapshot.oxygenSaturation?.let {
                    Text(
                        "\uD83E\uDEC1 SpO\u2082: ${String.format("%.0f", it)}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                snapshot.stepsTotal?.let {
                    Text(
                        "\uD83D\uDC63 Schritte: $it",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (error != null) {
                Text(
                    error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    "Tippe auf \uD83D\uDD04 um Health-Daten für diesen Zeitraum zu laden",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
