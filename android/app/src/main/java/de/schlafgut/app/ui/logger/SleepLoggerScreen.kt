package de.schlafgut.app.ui.logger

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.schlafgut.app.data.entity.HealthSnapshotEntity
import de.schlafgut.app.ui.components.DateTimePickerDialog
import de.schlafgut.app.ui.components.WakeWindowDialog
import de.schlafgut.app.ui.theme.DangerRed
import de.schlafgut.app.ui.theme.DangerRedBg
import de.schlafgut.app.ui.theme.WakeWindowColor
import de.schlafgut.app.ui.theme.qualityColor
import de.schlafgut.app.util.DateTimeUtil
import de.schlafgut.app.util.SleepCalculator

private val substanceLevelLabels = mapOf(
    0 to "Nein",
    1 to "Wenig",
    2 to "Mittel",
    3 to "Viel"
)

/** Feste Breite für alle Dropdowns, damit sie gleich groß sind */
private val DROPDOWN_WIDTH = 130.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepLoggerScreen(
    onNavigateBack: () -> Unit,
    viewModel: SleepLoggerViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showBedTimePicker by remember { mutableStateOf(false) }
    var showWakeTimePicker by remember { mutableStateOf(false) }
    var showWakeWindowDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onNavigateBack()
    }

    if (showBedTimePicker) {
        DateTimePickerDialog(
            title = "Bettzeit wählen",
            initialEpochMillis = state.bedTime,
            onConfirm = { viewModel.setBedTime(it); showBedTimePicker = false },
            onDismiss = { showBedTimePicker = false }
        )
    }
    if (showWakeTimePicker) {
        DateTimePickerDialog(
            title = "Aufwachzeit wählen",
            initialEpochMillis = state.wakeTime,
            onConfirm = { viewModel.setWakeTime(it); showWakeTimePicker = false },
            onDismiss = { showWakeTimePicker = false }
        )
    }
    if (showWakeWindowDialog) {
        WakeWindowDialog(
            bedTimeEpoch = state.bedTime,
            wakeTimeEpoch = state.wakeTime,
            onConfirm = { viewModel.addWakeWindow(it); showWakeWindowDialog = false },
            onDismiss = { showWakeWindowDialog = false }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(if (state.isEditing) "Eintrag bearbeiten" else "Schlaf eintragen") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error
            state.errorMessage?.let { error ->
                Card(colors = CardDefaults.cardColors(containerColor = DangerRedBg)) {
                    Text(error, color = DangerRed, style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp))
                }
            }

            // Type toggle
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !state.isNap, onClick = { if (state.isNap) viewModel.setIsNap(false) },
                    label = { Text("\uD83C\uDF19 Nachtschlaf") })
                FilterChip(selected = state.isNap, onClick = { if (!state.isNap) viewModel.setIsNap(true) },
                    label = { Text("\u2600\uFE0F Nickerchen") })
            }

            // Bed time / Wake time
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Bettzeit", style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = DateTimeUtil.formatDateFull(state.bedTime) + ", " + DateTimeUtil.formatTime(state.bedTime),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { showBedTimePicker = true }.padding(vertical = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Aufwachzeit", style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = DateTimeUtil.formatDateFull(state.wakeTime) + ", " + DateTimeUtil.formatTime(state.wakeTime),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { showWakeTimePicker = true }.padding(vertical = 8.dp)
                    )
                    val duration = SleepCalculator.calculateSleepDuration(
                        state.bedTime, state.wakeTime, state.sleepLatency, state.wakeWindows)
                    if (duration > 0) {
                        Text("= ${DateTimeUtil.formatDuration(duration)} Schlaf",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            // Sleep latency
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Einschlafzeit", style = MaterialTheme.typography.labelLarge)
                        Text("${state.sleepLatency} min", style = MaterialTheme.typography.bodyMedium)
                    }
                    Slider(value = state.sleepLatency.toFloat(), onValueChange = { viewModel.setSleepLatency(it.toInt()) },
                        valueRange = 0f..60f, steps = 59)
                }
            }

            // Wake windows
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Wachphasen", style = MaterialTheme.typography.labelLarge)
                        if (state.wakeWindows.isNotEmpty()) {
                            val totalMin = SleepCalculator.calculateWakeDuration(state.wakeWindows)
                            Text("${state.wakeWindows.size}x, $totalMin min",
                                style = MaterialTheme.typography.bodySmall, color = WakeWindowColor)
                        }
                    }
                    state.wakeWindows.forEachIndexed { index, window ->
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Text("${DateTimeUtil.formatTime(window.start)} \u2013 ${DateTimeUtil.formatTime(window.end)} (${window.durationMinutes} min)",
                                    style = MaterialTheme.typography.bodyMedium)
                                TextButton(onClick = { viewModel.removeWakeWindow(index) }) {
                                    Text("Entfernen", color = DangerRed)
                                }
                            }
                            window.outOfBedPeriods.forEach { oob ->
                                Text("   \uD83D\uDEB6 Aufgestanden: ${DateTimeUtil.formatTime(oob.start)} \u2013 ${DateTimeUtil.formatTime(oob.end)} (${oob.durationMinutes} min)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                    OutlinedButton(onClick = { showWakeWindowDialog = true }, modifier = Modifier.padding(top = 8.dp)) {
                        Text("+ Wachphase hinzufügen")
                    }
                }
            }

            // Quality
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Schlafqualität", style = MaterialTheme.typography.labelLarge)
                        Text("${state.quality}/10", style = MaterialTheme.typography.bodyMedium,
                            color = qualityColor(state.quality))
                    }
                    Slider(value = state.quality.toFloat(), onValueChange = { viewModel.setQuality(it.toInt()) },
                        valueRange = 0f..10f, steps = 9,
                        colors = SliderDefaults.colors(
                            thumbColor = qualityColor(state.quality),
                            activeTrackColor = qualityColor(state.quality)))
                }
            }

            // Health Connect Daten
            if (state.healthConnectEnabled) {
                HealthDataCard(
                    snapshot = state.healthSnapshot,
                    isLoading = state.isLoadingHealth,
                    error = state.healthError,
                    onRefresh = { viewModel.refreshHealthData() },
                    onClear = { viewModel.clearHealthData() }
                )
            }

            // Substanzen & Medikamente
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Substanzen & Medikamente", style = MaterialTheme.typography.labelLarge)

                    SubstanceLevelDropdown(label = "\uD83C\uDF77 Alkohol",
                        currentLevel = state.alcoholLevel, onLevelChange = { viewModel.setAlcoholLevel(it) })
                    SubstanceLevelDropdown(label = "\uD83C\uDF3F Drogenkonsum",
                        currentLevel = state.drugLevel, onLevelChange = { viewModel.setDrugLevel(it) })

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("\uD83D\uDC8A Schlafmittel", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = state.sleepAid, onCheckedChange = { viewModel.setSleepAid(it) })
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("\uD83D\uDC89 Medikamente", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = state.medication, onCheckedChange = { viewModel.setMedication(it) })
                    }
                    if (state.medication) {
                        OutlinedTextField(value = state.medicationNotes,
                            onValueChange = { viewModel.setMedicationNotes(it) },
                            label = { Text("Welche Medikamente?") },
                            modifier = Modifier.fillMaxWidth(), singleLine = false, minLines = 2)
                    }
                }
            }

            // Notes
            OutlinedTextField(value = state.notes, onValueChange = { viewModel.setNotes(it) },
                label = { Text("Notizen") }, modifier = Modifier.fillMaxWidth(), minLines = 3)

            // Save / Delete
            Button(onClick = { viewModel.save() }, modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium) {
                Text(if (state.isEditing) "Speichern" else "Eintragen")
            }
            if (state.isEditing) {
                TextButton(onClick = { showDeleteDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Eintrag löschen", color = DangerRed)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eintrag löschen?") },
            text = { Text("Möchtest du diesen Schlafeintrag wirklich dauerhaft entfernen?") },
            confirmButton = { TextButton(onClick = { viewModel.delete(); showDeleteDialog = false }) { Text("Löschen", color = DangerRed) } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Abbrechen") } }
        )
    }
}

@Composable
private fun HealthDataCard(
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
                            Icon(Icons.Default.Refresh, contentDescription = "Aktualisieren",
                                modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            if (snapshot != null) {
                snapshot.weightKg?.let {
                    Text("\u2696\uFE0F Gewicht: ${String.format("%.1f", it)} kg",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                snapshot.bodyTempCelsius?.let {
                    Text("\uD83C\uDF21\uFE0F Körpertemp.: ${String.format("%.1f", it)} °C",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                snapshot.restingHeartRate?.let {
                    Text("\u2764\uFE0F Ruhepuls: $it bpm",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                snapshot.avgNightHeartRate?.let {
                    Text("\uD83D\uDC93 Ø Nacht-HF: $it bpm",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                snapshot.oxygenSaturation?.let {
                    Text("\uD83E\uDEC1 SpO\u2082: ${String.format("%.0f", it)}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                snapshot.stepsTotal?.let {
                    Text("\uD83D\uDC63 Schritte: $it",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (error != null) {
                Text(error, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text("Tippe auf \uD83D\uDD04 um Health-Daten für diesen Zeitraum zu laden",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        onClick = { onLevelChange(level); expanded = false }
                    )
                }
            }
        }
    }
}
