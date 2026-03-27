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
import androidx.hilt.navigation.compose.hiltViewModel
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
            // Health Data Section - Only show if data is available
            val steps = state.healthData?.steps
            val rhr = state.healthData?.restingHeartRate
            
            if (steps != null || rhr != null) {
                item {
                    Text(
                        text = "Gesundheit",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (steps != null) {
                            SummaryCard(
                                title = "Schritte",
                                value = steps.toString(),
                                icon = "👣",
                                modifier = Modifier.weight(1f)
                            )
                        } else if (rhr != null) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                        
                        if (rhr != null) {
                            SummaryCard(
                                title = "Ruhepuls",
                                value = "$rhr bpm",
                                icon = "❤️",
                                modifier = Modifier.weight(1f)
                            )
                        } else if (steps != null) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Sleep Statistics Section
            item {
                Text(
                    text = "Schlaf-Statistik",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onViewAllClick() },
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryCard(
                        title = "Letzte Nacht",
                        value = state.lastNightDurationMinutes?.let {
                            DateTimeUtil.formatDuration(it)
                        } ?: "—",
                        icon = "🌙",
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = "Ø Qualität",
                        value = state.averageQuality?.let {
                            String.format("%.1f", it)
                        } ?: "—",
                        icon = "⭐",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onViewAllClick() },
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryCard(
                        title = "Ø Dauer",
                        value = state.averageDurationMinutes?.let {
                            DateTimeUtil.formatDuration(it.toInt())
                        } ?: "—",
                        icon = "⏱️",
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = "Einträge",
                        value = "${state.totalEntries}",
                        icon = "📋",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Recent entries header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Letzte Einträge",
                        style = MaterialTheme.typography.titleLarge
                    )
                    TextButton(onClick = onViewAllClick) {
                        Text("Alle anzeigen")
                    }
                }
            }

            if (state.recentEntries.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Text(
                            text = "Noch keine Einträge. Tippe auf +, um deinen ersten Schlaf zu erfassen!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
            }

            items(state.recentEntries, key = { it.id }) { entry ->
                SleepEntryRow(
                    entry = entry,
                    onClick = { onEntryClick(entry.id) }
                )
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}
