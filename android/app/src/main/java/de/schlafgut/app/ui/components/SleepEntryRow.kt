package de.schlafgut.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import de.schlafgut.app.data.entity.SleepEntryEntity
import de.schlafgut.app.ui.theme.NapColor
import de.schlafgut.app.ui.theme.TextSecondary
import de.schlafgut.app.ui.theme.qualityColor
import de.schlafgut.app.util.DateTimeUtil

@Composable
fun SleepEntryRow(
    entry: SleepEntryEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Quality badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(qualityColor(entry.quality).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${entry.quality}",
                    style = MaterialTheme.typography.titleMedium,
                    color = qualityColor(entry.quality)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = DateTimeUtil.formatDateFull(entry.date),
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (entry.isNap) {
                        Text(
                            text = "\u2600\uFE0F Nickerchen",
                            style = MaterialTheme.typography.bodySmall,
                            color = NapColor
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = DateTimeUtil.formatDuration(entry.sleepDurationMinutes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "${DateTimeUtil.formatTime(entry.bedTime)} \u2013 ${DateTimeUtil.formatTime(entry.wakeTime)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    if (entry.interruptionCount > 0) {
                        Text(
                            text = "${entry.interruptionCount}x wach",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                // Inline sleep timeline
                SleepTimeline(
                    entry = entry,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
