package de.schlafgut.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.schlafgut.app.data.entity.MedicationEntry
import de.schlafgut.app.ui.theme.DangerRed

@Composable
internal fun SettingsProfileSection(
    userName: String,
    defaultSleepLatency: Int,
    onUserNameChange: (String) -> Unit,
    onDefaultLatencyChange: (Int) -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Profil", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = userName,
                onValueChange = { onUserNameChange(it) },
                label = { Text("Name") },
                placeholder = { Text("Dein Name (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Text(
                "Standard-Einschlafzeit: $defaultSleepLatency min",
                style = MaterialTheme.typography.labelLarge
            )
            Slider(
                value = defaultSleepLatency.toFloat(),
                onValueChange = { onDefaultLatencyChange(it.toInt()) },
                valueRange = 0f..120f,
                steps = 23
            )
            Text(
                "Wird automatisch bei neuen Einträgen verwendet",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun SettingsMedicationsSection(
    medications: List<MedicationEntry>,
    pendingMedName: String,
    pendingMedDosage: String,
    onPendingMedNameChange: (String) -> Unit,
    onPendingMedDosageChange: (String) -> Unit,
    onAddMedication: (name: String, dosage: String) -> Unit,
    onRemoveMedication: (index: Int) -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("\uD83D\uDC8A Feste Medikamente", style = MaterialTheme.typography.titleMedium)
            Text(
                "Medikamente die du regelmäßig nimmst",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            medications.forEachIndexed { index, med ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(med.name, style = MaterialTheme.typography.bodyMedium)
                        if (med.dosage.isNotBlank()) {
                            Text(
                                med.dosage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = { onRemoveMedication(index) }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Entfernen",
                            tint = DangerRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                if (index < medications.lastIndex) HorizontalDivider()
            }
            // Textfelder im ViewModel-State → werden bei "Einstellungen speichern" mitgespeichert
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = pendingMedName,
                    onValueChange = { onPendingMedNameChange(it) },
                    label = { Text("Medikament") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = pendingMedDosage,
                    onValueChange = { onPendingMedDosageChange(it) },
                    label = { Text("Dosis") },
                    modifier = Modifier.weight(0.6f),
                    singleLine = true
                )
            }
            OutlinedButton(
                onClick = {
                    if (pendingMedName.isNotBlank()) {
                        onAddMedication(pendingMedName.trim(), pendingMedDosage.trim())
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = pendingMedName.isNotBlank()
            ) {
                Text("+ Medikament hinzufügen")
            }
        }
    }
}

@Composable
internal fun SettingsSecuritySection(
    appLockEnabled: Boolean,
    onAppLockEnabledChange: (Boolean) -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Sicherheit", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("App-Schutz", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "Fingerabdruck, PIN oder Muster",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = appLockEnabled,
                    onCheckedChange = { onAppLockEnabledChange(it) }
                )
            }
        }
    }
}
