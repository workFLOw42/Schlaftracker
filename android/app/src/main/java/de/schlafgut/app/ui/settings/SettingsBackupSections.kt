package de.schlafgut.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import de.schlafgut.app.ui.theme.DangerRed
import de.schlafgut.app.ui.theme.DangerRedBg

@Composable
internal fun SettingsLocalBackupSection(
    onExport: () -> Unit,
    onImport: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Lokales Backup", style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.FileDownload, contentDescription = null)
                Text("  Backup exportieren (JSON)")
            }
            OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.FileUpload, contentDescription = null)
                Text("  Backup importieren (JSON)")
            }
        }
    }
}

@Composable
internal fun SettingsDriveBackupSection(
    driveConnected: Boolean,
    isDriveBackupRunning: Boolean,
    signInError: String?,
    onConnect: () -> Unit,
    onUpload: () -> Unit,
    onRestore: () -> Unit,
    onDisconnect: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Cloud,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("  Google Drive Backup", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                "Passwort-verschlüsseltes Backup (AES-256, geräteübergreifend)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            signInError?.let { error ->
                Card(colors = CardDefaults.cardColors(containerColor = DangerRedBg)) {
                    Text(
                        error,
                        color = DangerRed,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            if (!driveConnected) {
                OutlinedButton(onClick = onConnect, modifier = Modifier.fillMaxWidth()) {
                    Text("Mit Google Drive verbinden")
                }
            } else {
                Text(
                    "Mit Google Drive verbunden",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (isDriveBackupRunning) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text("  Backup läuft...", modifier = Modifier.padding(start = 8.dp))
                    }
                } else {
                    OutlinedButton(onClick = onUpload, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Text("  Backup erstellen")
                    }
                    OutlinedButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null)
                        Text("  Backup wiederherstellen")
                    }
                    TextButton(onClick = onDisconnect, modifier = Modifier.fillMaxWidth()) {
                        Text("Trennen")
                    }
                }
            }
        }
    }
}

@Composable
internal fun BackupPasswordDialog(
    mode: PasswordDialogMode,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val isUpload = mode == PasswordDialogMode.UPLOAD
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isUpload) "Backup-Passwort festlegen" else "Backup-Passwort eingeben")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (isUpload) {
                        "Dieses Passwort wird benötigt, um das Backup auf einem anderen Gerät wiederherzustellen."
                    } else {
                        "Gib das Passwort ein, mit dem das Backup erstellt wurde."
                    },
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        error = null
                    },
                    label = { Text("Passwort") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (isUpload) {
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            error = null
                        },
                        label = { Text("Passwort bestätigen") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                error?.let { Text(it, color = DangerRed, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        password.length < 6 -> error = "Mindestens 6 Zeichen"
                        isUpload && password != confirmPassword -> error = "Passwörter stimmen nicht überein"
                        else -> onConfirm(password)
                    }
                }
            ) {
                Text(if (isUpload) "Backup erstellen" else "Wiederherstellen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}
