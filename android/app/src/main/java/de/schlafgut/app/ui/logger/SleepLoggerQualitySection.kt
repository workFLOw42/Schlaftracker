package de.schlafgut.app.ui.logger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.schlafgut.app.ui.theme.qualityColor

@Composable
internal fun SleepQualityCard(
    quality: Int,
    onQualityChange: (Int) -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Schlafqualität", style = MaterialTheme.typography.labelLarge)
                Text(
                    "$quality/10",
                    style = MaterialTheme.typography.bodyMedium,
                    color = qualityColor(quality)
                )
            }
            Slider(
                value = quality.toFloat(),
                onValueChange = { onQualityChange(it.toInt()) },
                valueRange = 0f..10f,
                steps = 9,
                colors = SliderDefaults.colors(
                    thumbColor = qualityColor(quality),
                    activeTrackColor = qualityColor(quality)
                )
            )
        }
    }
}
