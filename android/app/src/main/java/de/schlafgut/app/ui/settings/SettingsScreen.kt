package de.schlafgut.app.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.schlafgut.app.data.health.HealthConnectManager
import de.schlafgut.app.ui.theme.DangerRed
import de.schlafgut.app.ui.theme.DangerRedBg
import de.schlafgut.app.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // File picker for JSON import
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importJson(context, it) }
    }

    // Health Connect permission launcher
    val healthPermissionLauncher = rememberLauncherForActivityResult(
        viewModel.healthConnectPermissionContract
    ) { granted ->
        if (granted.isNotEmpty()) {
            viewModel.setHealthConnectEnabled(true)
            viewModel.saveSettings()
        } else {
            viewModel.setHealthConnectEnabled(false)
        }
    }

    // Auto-clear success message
    LaunchedEffect(state.successMessage) {
        if (state.successMessage != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearSuccessMessage()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Einstellungen",
            style = MaterialTheme.typography.headlineLarge
        )

        // Success message
        state.successMessage?.let { msg ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = msg,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // User settings
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Profil", style = MaterialTheme.typography.titleMedium)

                OutlinedTextField(
                    value = state.userName,
                    onValueChange = { viewModel.setUserName(it) },
                    label = { Text("Name") },
                    placeholder = { Text("Dein Name (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text(
                    text = "Standard-Einschlafzeit: ${state.defaultSleepLatency} min",
                    style = MaterialTheme.typography.labelLarge
                )
                Slider(
                    value = state.defaultSleepLatency.toFloat(),
                    onValueChange = { viewModel.setDefaultLatency(it.toInt()) },
                    valueRange = 0f..120f,
                    steps = 23
                )
                Text(
                    text = "Wird automatisch bei neuen Eintr\u00e4gen verwendet",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        // Security
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Sicherheit", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("App-Schutz", style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = "Fingerabdruck, PIN oder Muster",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = state.appLockEnabled,
                        onCheckedChange = { viewModel.setAppLockEnabled(it) }
                    )
                }
            }
        }

        // Health Connect
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Health Connect", style = MaterialTheme.typography.titleMedium)

                when (state.healthConnectAvailability) {
                    HealthConnectManager.Availability.NOT_SUPPORTED -> {
                        Text(
                            text = "Health Connect wird auf diesem Ger\u00e4t nicht unterst\u00fctzt",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    HealthConnectManager.Availability.NOT_INSTALLED -> {
                        Text(
                            text = "Health Connect muss installiert werden",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        OutlinedButton(
                            onClick = {
                                try {
                                    context.startActivity(
                                        viewModel.getHealthConnectInstallIntent()
                                    )
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "Play Store nicht verf\u00fcgbar",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Health Connect installieren")
                        }
                    }
                    HealthConnectManager.Availability.AVAILABLE -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "K\u00f6rperdaten lesen",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text(
                                    text = "Gewicht, Puls, Schritte, SpO2 aus anderen Apps",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Switch(
                                checked = state.healthConnectEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        healthPermissionLauncher.launch(
                                            viewModel.healthConnectPermissions
                                        )
                                    } else {
                                        viewModel.setHealthConnectEnabled(false)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Data management
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Daten", style = MaterialTheme.typography.titleMedium)

                OutlinedButton(
                    onClick = { viewModel.exportJson(context) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                    Text("  Backup exportieren (JSON)")
                }

                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null)
                    Text("  Backup importieren (JSON)")
                }
            }
        }

        // Save
        Button(
            onClick = { viewModel.saveSettings() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Einstellungen speichern")
        }

        // Danger zone
        Card(
            colors = CardDefaults.cardColors(containerColor = DangerRedBg),
            border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Gefahrenzone",
                    style = MaterialTheme.typography.titleMedium,
                    color = DangerRed
                )
                Text(
                    text = "Alle Schlafeintr\u00e4ge unwiderruflich l\u00f6schen",
                    style = MaterialTheme.typography.bodySmall,
                    color = DangerRed.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
                OutlinedButton(
                    onClick = { viewModel.showClearDataDialog(true) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                    border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null)
                    Text("  Alle Daten l\u00f6schen")
                }
            }
        }

        // About
        Text(
            text = "SchlafGut v1.0.0",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(80.dp))
    }

    // Clear data dialog
    if (state.showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showClearDataDialog(false) },
            title = { Text("Alle Daten l\u00f6schen?") },
            text = {
                Text(
                    "Alle Schlafeintr\u00e4ge werden unwiderruflich gel\u00f6scht. " +
                        "Diese Aktion kann nicht r\u00fcckg\u00e4ngig gemacht werden."
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearAllData() }) {
                    Text("L\u00f6schen", color = DangerRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showClearDataDialog(false) }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}
