package de.schlafgut.app.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.schlafgut.app.R
import de.schlafgut.app.ui.components.SummaryCard
import de.schlafgut.app.ui.components.SleepEntryRow
import de.schlafgut.app.util.DateTimeUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onMenuClick: () -> Unit,
    onEntryClick: (String) -> Unit,
    onViewAllClick: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_splash_logo),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp).padding(end = 8.dp),
                            tint = Color.Unspecified
                        )
                        val titleText = if (state.userName.isNullOrBlank()) "SchlafGut" else "SchlafGut, ${state.userName}"
                        Text(titleText)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menü")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Health Data Section
            val health = state.healthData
            if (health != null && health.hasAnyData) {
                item {
                    Text("Gesundheit", style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 8.dp))
                }
                val row1Left = health.stepsTotal?.let { "\uD83D\uDC63" to ("Schritte" to it.toString()) }
                val row1Right = health.restingHeartRate?.let { "\u2764\uFE0F" to ("Ruhepuls" to "$it bpm") }
                if (row1Left != null || row1Right != null) {
                    item { HealthDataRow(left = row1Left, right = row1Right) }
                }
                val row2Left = health.avgNightHeartRate?.let { "\uD83D\uDC93" to ("\u00D8 Nacht-HF" to "$it bpm") }
                val row2Right = health.oxygenSaturation?.let { "\uD83E\uDEC1" to ("SpO\u2082" to "${String.format("%.0f", it)}%") }
                if (row2Left != null || row2Right != null) {
                    item { HealthDataRow(left = row2Left, right = row2Right) }
                }
                val row3Left = health.weightKg?.let { "\u2696\uFE0F" to ("Gewicht" to "${String.format("%.1f", it)} kg") }
                val row3Right = health.bodyTempCelsius?.let { "\uD83C\uDF21\uFE0F" to ("Körpertemp." to "${String.format("%.1f", it)} \u00B0C") }
                if (row3Left != null || row3Right != null) {
                    item { HealthDataRow(left = row3Left, right = row3Right) }
                }
            }

            // Nachtschlaf-Statistik
            item {
                Text("Nachtschlaf", style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 8.dp))
            }
            item {
                Row(modifier = Modifier.fillMaxWidth().clickable { onViewAllClick() },
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryCard(title = "Letzte Nacht",
                        value = state.lastNightDurationMinutes?.let { DateTimeUtil.formatDuration(it) } ?: "\u2014",
                        icon = "\uD83C\uDF19", modifier = Modifier.weight(1f))
                    SummaryCard(title = "\u00D8 Qualität",
                        value = state.averageQuality?.let { String.format("%.1f", it) } ?: "\u2014",
                        icon = "\u2B50", modifier = Modifier.weight(1f))
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth().clickable { onViewAllClick() },
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryCard(title = "\u00D8 Dauer",
                        value = state.averageDurationMinutes?.let { DateTimeUtil.formatDuration(it.toInt()) } ?: "\u2014",
                        icon = "\u23F1\uFE0F", modifier = Modifier.weight(1f))
                    SummaryCard(title = "Nächte",
                        value = "${state.totalEntries}",
                        icon = "\uD83D\uDCCB", modifier = Modifier.weight(1f))
                }
            }

            // Nickerchen-Statistik (nur wenn Naps vorhanden)
            val nap = state.napSummary
            if (nap.totalNaps > 0) {
                item {
                    Text("\u2600\uFE0F Nickerchen", style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 8.dp))
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SummaryCard(title = "Anzahl",
                            value = "${nap.totalNaps}",
                            icon = "\uD83D\uDCCA", modifier = Modifier.weight(1f))
                        SummaryCard(title = "\u00D8 Dauer",
                            value = nap.averageNapDurationMinutes?.let { DateTimeUtil.formatDuration(it.toInt()) } ?: "\u2014",
                            icon = "\u23F0", modifier = Modifier.weight(1f))
                    }
                }
                nap.frequencyText?.let { freq ->
                    item {
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SummaryCard(title = "Häufigkeit",
                                value = freq,
                                icon = "\uD83D\uDD01", modifier = Modifier.weight(1f))
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Recent entries header
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Letzte Einträge", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = onViewAllClick) { Text("Alle anzeigen") }
                }
            }

            if (state.recentEntries.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Text("Noch keine Einträge. Tippe auf +, um deinen ersten Schlaf zu erfassen!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(24.dp))
                    }
                }
            }

            items(state.recentEntries, key = { it.id }) { entry ->
                SleepEntryRow(entry = entry, onClick = { onEntryClick(entry.id) })
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun HealthDataRow(
    left: Pair<String, Pair<String, String>>?,
    right: Pair<String, Pair<String, String>>?
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (left != null) {
            SummaryCard(title = left.second.first, value = left.second.second,
                icon = left.first, modifier = Modifier.weight(1f))
        } else if (right != null) { Spacer(modifier = Modifier.weight(1f)) }
        if (right != null) {
            SummaryCard(title = right.second.first, value = right.second.second,
                icon = right.first, modifier = Modifier.weight(1f))
        } else if (left != null) { Spacer(modifier = Modifier.weight(1f)) }
    }
}
