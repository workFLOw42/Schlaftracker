package de.schlafgut.app.ui.logger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.schlafgut.app.data.entity.MedicationEntry

private val substanceLevelLabels = mapOf(
    0 to "Nein",
    1 to "Wenig",
    2 to "Mittel",
    3 to "Viel"
)

/** Feste Breite für alle Dropdowns, damit sie gleich groß sind */
private val DROPDOWN_WIDTH = 130.dp

/** Gebündelte Callbacks für die Substanzen-&-Medikamente-Karte. */
internal class SubstanceCallbacks(
    val onAlcoholLevelChange: (Int) -> Unit,
    val onDrugLevelChange: (Int) -> Unit,
    val onSleepAidChange: (Boolean) -> Unit,
    val onMedicationChange: (Boolean) -> Unit,
    val onMedicationNotesChange: (String) -> Unit
)

@Composable
internal fun SubstancesCard(
    alcoholLevel: Int,
    drugLevel: Int,
    sleepAid: Boolean,
    medication: Boolean,
    medicationNotes: String,
    regularMedications: List<MedicationEntry>,
    callbacks: SubstanceCallbacks
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Substanzen & Medikamente", style = MaterialTheme.typography.labelLarge)

            SubstanceLevelDropdown(
                label = "\uD83C\uDF77 Alkohol",
                currentLevel = alcoholLevel,
                onLevelChange = { callbacks.onAlcoholLevelChange(it) }
            )
            SubstanceLevelDropdown(
                label = "\uD83C\uDF3F Drogenkonsum",
                currentLevel = drugLevel,
                onLevelChange = { callbacks.onDrugLevelChange(it) }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("\uD83D\uDC8A Schlafmittel", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = sleepAid, onCheckedChange = { callbacks.onSleepAidChange(it) })
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("\uD83D\uDC89 Medikamente", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = medication, onCheckedChange = { callbacks.onMedicationChange(it) })
            }
            if (medication) {
                OutlinedTextField(
                    value = medicationNotes,
                    onValueChange = { callbacks.onMedicationNotesChange(it) },
                    label = { Text("Welche Medikamente?") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    minLines = 2
                )
            }

            // Reguläre Medikamente aus den Einstellungen anzeigen
            if (regularMedications.isNotEmpty()) {
                RegularMedicationsList(regularMedications)
            }
        }
    }
}

@Composable
private fun RegularMedicationsList(regularMedications: List<MedicationEntry>) {
    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
    Text(
        "Feste Medikamente (aus Einstellungen)",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    regularMedications.forEach { med ->
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("\uD83D\uDC8A ${med.name}", style = MaterialTheme.typography.bodyMedium)
            if (med.dosage.isNotBlank()) {
                Text(
                    " – ${med.dosage}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubstanceLevelDropdown(
    label: String,
    currentLevel: Int,
    onLevelChange: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.width(DROPDOWN_WIDTH)
        ) {
            @Suppress("DEPRECATION")
            OutlinedTextField(
                value = substanceLevelLabels[currentLevel] ?: "Nein",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .width(DROPDOWN_WIDTH),
                textStyle = MaterialTheme.typography.bodySmall,
                singleLine = true
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                substanceLevelLabels.forEach { (level, text) ->
                    DropdownMenuItem(
                        text = { Text(text) },
                        onClick = {
                            onLevelChange(level)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
