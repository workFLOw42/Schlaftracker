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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.schlafgut.app.ui.components.DateTimePickerDialog
import de.schlafgut.app.ui.components.WakeWindowDialog
import de.schlafgut.app.ui.theme.DangerRed
import de.schlafgut.app.ui.theme.DangerRedBg
import de.schlafgut.app.ui.theme.TextSecondary
import de.schlafgut.app.ui.theme.WakeWindowColor
import de.schlafgut.app.ui.theme.qualityColor
import de.schlafgut.app.util.DateTimeUtil
import de.schlafgut.app.util.SleepCalculator

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

    // Picker dialogs
    if (showBedTimePicker) {
        DateTimePickerDialog(
            title = "Bettzeit w\u00E4hlen",
            initialEpochMillis = state.bedTime,
            onConfirm = {
                viewModel.setBedTime(it)
                showBedTimePicker = false
            },
            onDismiss = { showBedTimePicker = false }
        )
    }

    if (showWakeTimePicker) {
        DateTimePickerDialog(
            title = "Aufwachzeit w\u00E4hlen",
            initialEpochMillis = state.wakeTime,
            onConfirm = {
                viewModel.setWakeTime(it)
                showWakeTimePicker = false
            },
            onDismiss = { showWakeTimePicker = false }
        )
    }

    if (showWakeWindowDialog) {
        WakeWindowDialog(
            bedTimeEpoch = state.bedTime,
            wakeTimeEpoch = state.wakeTime,
            onConfirm = { start, end ->
                viewModel.addWakeWindow(start, end)
                showWakeWindowDialog = false
            },
            onDismiss = { showWakeWindowDialog = false }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
        TopAppBar(
            title = {
                Text(
                    if (state.isEditing) "Eintrag bearbeiten" else "Schlaf eintragen"
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zur\u00fcck")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Error message
            state.errorMessage?.let { error ->
                Card(colors = CardDefaults.cardColors(containerColor = DangerRedBg)) {
                    Text(
                        text = error,
                        color = DangerRed,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Type toggle
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !state.isNap,
                    onClick = { if (state.isNap) viewModel.setIsNap(false) },
                    label = { Text("\uD83C\uDF19 Nachtschlaf") }
                )
                FilterChip(
                    selected = state.isNap,
                    onClick = { if (!state.isNap) viewModel.setIsNap(true) },
                    label = { Text("\u2600\uFE0F Nickerchen") }
                )
            }

            // Bed time / Wake time
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Bettzeit", style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = DateTimeUtil.formatDateFull(state.bedTime) + ", " +
                            DateTimeUtil.formatTime(state.bedTime),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { showBedTimePicker = true }
                            .padding(vertical = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Aufwachzeit", style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = DateTimeUtil.formatDateFull(state.wakeTime) + ", " +
                            DateTimeUtil.formatTime(state.wakeTime),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { showWakeTimePicker = true }
                            .padding(vertical = 8.dp)
                    )

                    // Calculated duration
                    val duration = SleepCalculator.calculateSleepDuration(
                        state.bedTime, state.wakeTime, state.sleepLatency, state.wakeWindows
                    )
                    if (duration > 0) {
                        Text(
                            text = "= ${DateTimeUtil.formatDuration(duration)} Schlaf",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // Sleep latency slider
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Einschlafzeit", style = MaterialTheme.typography.labelLarge)
                        Text(
                            "${state.sleepLatency} min",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Slider(
                        value = state.sleepLatency.toFloat(),
                        onValueChange = { viewModel.setSleepLatency(it.toInt()) },
                        valueRange = 0f..60f,
                        steps = 59
                    )
                }
            }

            // Wake windows
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Wachphasen", style = MaterialTheme.typography.labelLarge)
                        if (state.wakeWindows.isNotEmpty()) {
                            val totalMin = SleepCalculator.calculateWakeDuration(state.wakeWindows)
                            Text(
                                "${state.wakeWindows.size}x, ${totalMin} min",
                                style = MaterialTheme.typography.bodySmall,
                                color = WakeWindowColor
                            )
                        }
                    }

                    state.wakeWindows.forEachIndexed { index, window ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${DateTimeUtil.formatTime(window.start)} \u2013 ${
                                    DateTimeUtil.formatTime(window.end)
                                } (${window.durationMinutes} min)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            TextButton(onClick = { viewModel.removeWakeWindow(index) }) {
                                Text("Entfernen", color = DangerRed)
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { showWakeWindowDialog = true },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("+ Wachphase hinzuf\u00fcgen")
                    }
                }
            }

            // Quality slider
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Schlafqualit\u00E4t", style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = "${state.quality}/10",
                            style = MaterialTheme.typography.titleMedium,
                            color = qualityColor(state.quality)
                        )
                    }
                    Slider(
                        value = state.quality.toFloat(),
                        onValueChange = { viewModel.setQuality(it.toInt()) },
                        valueRange = 1f..10f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = qualityColor(state.quality),
                            activeTrackColor = qualityColor(state.quality)
                        )
                    )
                }
            }

            // Notes
            OutlinedTextField(
                value = state.notes,
                onValueChange = { viewModel.setNotes(it) },
                label = { Text("Notizen") },
                placeholder = { Text("Traumfetzen, Stress, sp\u00E4tes Essen...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            // Save
            Button(
                onClick = {
                    viewModel.clearError()
                    viewModel.save()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Speichern")
            }

            // Delete
            if (state.isEditing) {
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Text("  Eintrag l\u00F6schen")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eintrag l\u00F6schen?") },
            text = { Text("Dieser Eintrag wird unwiderruflich gel\u00F6scht.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.delete()
                }) {
                    Text("L\u00F6schen", color = DangerRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}
