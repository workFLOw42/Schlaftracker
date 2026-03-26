package de.schlafgut.app.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.schlafgut.app.ui.components.SummaryCard
import de.schlafgut.app.ui.components.SleepEntryRow
import de.schlafgut.app.ui.theme.TextSecondary
import de.schlafgut.app.util.DateTimeUtil

@Composable
fun DashboardScreen(
    onEntryClick: (String) -> Unit,
    onViewAllClick: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        item {
            Text(
                text = "SchlafGut",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
        }

        // Summary Cards (2x2 grid)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard(
                    title = "Letzte Nacht",
                    value = state.lastNightDurationMinutes?.let {
                        DateTimeUtil.formatDuration(it)
                    } ?: "—",
                    icon = "\uD83C\uDF19",
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "\u00D8 Qualit\u00E4t",
                    value = state.averageQuality?.let {
                        String.format("%.1f", it)
                    } ?: "—",
                    icon = "\u2B50",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard(
                    title = "\u00D8 Dauer",
                    value = state.averageDurationMinutes?.let {
                        DateTimeUtil.formatDuration(it.toInt())
                    } ?: "—",
                    icon = "\u23F1",
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Eintr\u00E4ge",
                    value = "${state.totalEntries}",
                    icon = "\uD83D\uDCCB",
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
                    text = "Letzte Eintr\u00E4ge",
                    style = MaterialTheme.typography.titleLarge
                )
                if (state.totalEntries > 5) {
                    TextButton(onClick = onViewAllClick) {
                        Text("Alle anzeigen")
                    }
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
                        text = "Noch keine Eintr\u00E4ge. Tippe auf +, um deinen ersten Schlaf zu erfassen!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
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

        // Bottom spacer for FAB
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}
