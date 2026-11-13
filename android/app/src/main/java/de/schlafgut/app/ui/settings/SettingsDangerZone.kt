package de.schlafgut.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.schlafgut.app.ui.theme.DangerRed
import de.schlafgut.app.ui.theme.DangerRedBg

@Composable
internal fun SettingsDangerZone(onClearDataClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DangerRedBg),
        border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Gefahrenzone",
                style = MaterialTheme.typography.titleMedium,
                color = DangerRed
            )
            Text(
                "Alle Schlafeinträge unwiderruflich löschen",
                style = MaterialTheme.typography.bodySmall,
                color = DangerRed.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp)
            )
            OutlinedButton(
                onClick = onClearDataClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = null)
                Text("  Alle Daten löschen")
            }
        }
    }
}

@Composable
internal fun ClearDataDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Alle Daten löschen?") },
        text = { Text("Alle Schlafeinträge werden unwiderruflich gelöscht.") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Löschen", color = DangerRed) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}
