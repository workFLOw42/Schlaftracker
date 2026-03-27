package de.schlafgut.app.ui.statistics

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.schlafgut.app.data.export.CsvExporter
import de.schlafgut.app.data.export.PdfExporter
import de.schlafgut.app.ui.components.DurationChart
import de.schlafgut.app.ui.components.InterruptionsChart
import de.schlafgut.app.ui.components.QualityChart
import de.schlafgut.app.ui.components.SleepTimeline24h
import de.schlafgut.app.util.DateTimeUtil
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    // Date pickers
    if (showStartDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.startDate
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC).toLocalDate()
                        viewModel.setDateRange(
                            DateTimeUtil.localDateToEpoch(date),
                            state.endDate
                        )
                    }
                    showStartDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Abbrechen") }
            }
        ) { DatePicker(state = pickerState) }
    }

    if (showEndDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.endDate
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC).toLocalDate()
                        viewModel.setDateRange(
                            state.startDate,
                            DateTimeUtil.localDateToEpoch(date)
                        )
                    }
                    showEndDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) { Text("Abbrechen") }
            }
        ) { DatePicker(state = pickerState) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Statistik",
            style = MaterialTheme.typography.headlineLarge
        )

        // Date range filter
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Zeitraum", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(onClick = { showStartDatePicker = true }) {
                        Text(DateTimeUtil.formatDateShort(state.startDate))
                    }
                    Text(
                        "\u2013",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    OutlinedButton(onClick = { showEndDatePicker = true }) {
                        Text(DateTimeUtil.formatDateShort(state.endDate))
                    }
                }
            }
        }

        // Summary
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "${state.entries.size} Eintr\u00E4ge im Zeitraum",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "\u00D8 Dauer: ${DateTimeUtil.formatDuration(state.averageDurationMinutes.toInt())}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "\u00D8 Qualit\u00E4t: ${String.format("%.1f", state.averageQuality)}/10",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "\u00D8 Unterbrechungen: ${String.format("%.1f", state.averageInterruptions)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 24h Timeline
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "24h-Timeline",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                SleepTimeline24h(entries = state.entries)
            }
        }

        // Charts
        DurationChart(entries = state.entries)
        QualityChart(entries = state.entries)
        InterruptionsChart(entries = state.entries)

        // Export buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val startStr = DateTimeUtil.formatDateShort(state.startDate)
                    val endStr = DateTimeUtil.formatDateShort(state.endDate)
                    val uri = CsvExporter.export(
                        context, state.entries,
                        startDate = startStr, endDate = endStr
                    )
                    if (uri != null) {
                        context.startActivity(
                            CsvExporter.createShareIntent(uri).let {
                                Intent.createChooser(it, "CSV exportieren")
                            }
                        )
                    } else {
                        Toast.makeText(context, "Keine Daten zum Exportieren", Toast.LENGTH_SHORT)
                            .show()
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null)
                Text("  CSV")
            }
            OutlinedButton(
                onClick = {
                    val startStr = DateTimeUtil.formatDateShort(state.startDate)
                    val endStr = DateTimeUtil.formatDateShort(state.endDate)
                    val uri = PdfExporter.export(
                        context, state.entries,
                        startDate = startStr, endDate = endStr
                    )
                    if (uri != null) {
                        context.startActivity(
                            PdfExporter.createShareIntent(uri).let {
                                Intent.createChooser(it, "PDF exportieren")
                            }
                        )
                    } else {
                        Toast.makeText(context, "Keine Daten zum Exportieren", Toast.LENGTH_SHORT)
                            .show()
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                Text("  PDF")
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}
