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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import de.schlafgut.app.BuildConfig
import de.schlafgut.app.data.health.HealthConnectManager
import de.schlafgut.app.ui.theme.DangerRed
import de.schlafgut.app.ui.theme.DangerRedBg

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { viewModel.importJson(context, it) } }
    val healthPermissionLauncher = rememberLauncherForActivityResult(viewModel.healthConnectPermissionContract) { granted ->
        if (granted.isNotEmpty()) { viewModel.setHealthConnectEnabled(true); viewModel.saveSettings() } else { viewModel.setHealthConnectEnabled(false) }
    }
    val driveAuthLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        try {
            val authResult = Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(result.data)
            handleAuthResult(authResult, viewModel)
        } catch (e: Exception) { viewModel.setSignInError("Autorisierung fehlgeschlagen: ${e.message}") }
    }

    LaunchedEffect(state.successMessage) { if (state.successMessage != null) { kotlinx.coroutines.delay(3000); viewModel.clearSuccessMessage() } }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Einstellungen", style = MaterialTheme.typography.headlineLarge)
        state.successMessage?.let { msg -> Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) { Text(msg, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onPrimaryContainer) } }

        // ====== Profil ======
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Profil", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(value = state.userName, onValueChange = { viewModel.setUserName(it) }, label = { Text("Name") }, placeholder = { Text("Dein Name (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Text("Standard-Einschlafzeit: ${state.defaultSleepLatency} min", style = MaterialTheme.typography.labelLarge)
                Slider(value = state.defaultSleepLatency.toFloat(), onValueChange = { viewModel.setDefaultLatency(it.toInt()) }, valueRange = 0f..120f, steps = 23)
                Text("Wird automatisch bei neuen Einträgen verwendet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // ====== Feste Medikamente ======
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("\uD83D\uDC8A Feste Medikamente", style = MaterialTheme.typography.titleMedium)
                Text("Medikamente die du regelmäßig nimmst", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                state.regularMedications.forEachIndexed { index, med ->
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(med.name, style = MaterialTheme.typography.bodyMedium)
                            if (med.dosage.isNotBlank()) Text(med.dosage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { viewModel.removeMedication(index) }) { Icon(Icons.Default.Close, contentDescription = "Entfernen", tint = DangerRed, modifier = Modifier.size(20.dp)) }
                    }
                    if (index < state.regularMedications.lastIndex) HorizontalDivider()
                }
                // Textfelder im ViewModel-State → werden bei "Einstellungen speichern" mitgespeichert
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = state.pendingMedName, onValueChange = { viewModel.setPendingMedName(it) }, label = { Text("Medikament") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = state.pendingMedDosage, onValueChange = { viewModel.setPendingMedDosage(it) }, label = { Text("Dosis") }, modifier = Modifier.weight(0.6f), singleLine = true)
                }
                OutlinedButton(onClick = { if (state.pendingMedName.isNotBlank()) viewModel.addMedication(state.pendingMedName.trim(), state.pendingMedDosage.trim()) }, modifier = Modifier.fillMaxWidth(), enabled = state.pendingMedName.isNotBlank()) { Text("+ Medikament hinzufügen") }
            }
        }

        // ====== Health Connect ======
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Health Connect", style = MaterialTheme.typography.titleMedium)
                when (state.healthConnectAvailability) {
                    HealthConnectManager.Availability.NOT_SUPPORTED -> Text("Health Connect wird auf diesem Gerät nicht unterstützt", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HealthConnectManager.Availability.NOT_INSTALLED -> {
                        Text("Health Connect muss installiert werden", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(onClick = { try { context.startActivity(viewModel.getHealthConnectInstallIntent()) } catch (e: Exception) { Toast.makeText(context, "Play Store nicht verfügbar", Toast.LENGTH_SHORT).show() } }) { Text("Health Connect installieren") }
                    }
                    HealthConnectManager.Availability.AVAILABLE -> {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) { Text("Körperdaten lesen", style = MaterialTheme.typography.labelLarge); Text("Aktiviere Health Connect und wähle Datentypen", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            Switch(checked = state.healthConnectEnabled, onCheckedChange = { enabled -> if (enabled) healthPermissionLauncher.launch(viewModel.healthConnectPermissions) else viewModel.setHealthConnectEnabled(false) })
                        }
                        if (state.healthConnectEnabled) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Text("Welche Daten einlesen?", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            HealthToggleRow("\u2696\uFE0F Gewicht", state.healthReadWeight) { viewModel.setHealthReadWeight(it) }
                            HealthToggleRow("\uD83C\uDF21\uFE0F Körpertemperatur", state.healthReadBodyTemp) { viewModel.setHealthReadBodyTemp(it) }
                            HealthToggleRow("\u2764\uFE0F Ruhepuls", state.healthReadRestingHr) { viewModel.setHealthReadRestingHr(it) }
                            HealthToggleRow("\uD83D\uDC93 Herzfrequenz (Nacht)", state.healthReadHeartRate) { viewModel.setHealthReadHeartRate(it) }
                            HealthToggleRow("\uD83E\uDEC1 SpO\u2082", state.healthReadSpO2) { viewModel.setHealthReadSpO2(it) }
                            HealthToggleRow("\uD83D\uDC63 Schritte", state.healthReadSteps) { viewModel.setHealthReadSteps(it) }
                            HealthToggleRow("\uD83D\uDE34 Schlaf", state.healthReadSleep) { viewModel.setHealthReadSleep(it) }
                            val hasData = state.latestWeight != null || state.latestRestingHr != null || state.latestSteps != null || state.latestSpO2 != null
                            if (hasData) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                Text("Letzte Messwerte", style = MaterialTheme.typography.labelLarge)
                                state.latestWeight?.let { Text("\u2696\uFE0F Gewicht: ${String.format("%.1f", it)} kg", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                state.latestRestingHr?.let { Text("\u2764\uFE0F Ruhepuls: $it bpm", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                state.latestSteps?.let { Text("\uD83D\uDC63 Schritte: $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                state.latestSpO2?.let { Text("\uD83E\uDEC1 SpO\u2082: ${String.format("%.0f", it)}%", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                        }
                    }
                }
            }
        }

        // ====== Sicherheit ======
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Sicherheit", style = MaterialTheme.typography.titleMedium)
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column { Text("App-Schutz", style = MaterialTheme.typography.labelLarge); Text("Fingerabdruck, PIN oder Muster", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Switch(checked = state.appLockEnabled, onCheckedChange = { viewModel.setAppLockEnabled(it) })
                }
            }
        }

        // ====== Lokales Backup ======
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Lokales Backup", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = { viewModel.exportJson(context) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.FileDownload, contentDescription = null); Text("  Backup exportieren (JSON)") }
                OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.FileUpload, contentDescription = null); Text("  Backup importieren (JSON)") }
            }
        }

        // ====== Google Drive Backup ======
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary); Text("  Google Drive Backup", style = MaterialTheme.typography.titleMedium) }
                Text("Passwort-verschlüsseltes Backup (AES-256, geräteübergreifend)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                state.signInError?.let { error -> Card(colors = CardDefaults.cardColors(containerColor = DangerRedBg)) { Text(error, color = DangerRed, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp)) } }
                if (state.driveAccountEmail.isNullOrBlank()) {
                    OutlinedButton(onClick = {
                        viewModel.clearSignInError()
                        val authRequest = AuthorizationRequest.Builder().setRequestedScopes(listOf(Scope(DriveScopes.DRIVE_APPDATA))).build()
                        Identity.getAuthorizationClient(context).authorize(authRequest)
                            .addOnSuccessListener { authResult ->
                                if (authResult.hasResolution()) {
                                    authResult.pendingIntent?.let { pi -> driveAuthLauncher.launch(androidx.activity.result.IntentSenderRequest.Builder(pi.intentSender).build()) }
                                } else { handleAuthResult(authResult, viewModel) }
                            }
                            .addOnFailureListener { e -> viewModel.setSignInError("Autorisierung fehlgeschlagen: ${e.message}") }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Mit Google autorisieren") }
                } else {
                    Text("Autorisiert als: ${state.driveAccountEmail}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    if (state.isDriveBackupRunning) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(modifier = Modifier.size(24.dp)); Text("  Backup läuft...", modifier = Modifier.padding(start = 8.dp)) }
                    } else {
                        OutlinedButton(onClick = { viewModel.showPasswordDialog(PasswordDialogMode.UPLOAD) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.CloudUpload, contentDescription = null); Text("  Backup erstellen") }
                        OutlinedButton(onClick = { viewModel.showPasswordDialog(PasswordDialogMode.RESTORE) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.CloudDownload, contentDescription = null); Text("  Backup wiederherstellen") }
                        TextButton(onClick = { viewModel.setDriveAccount("") }, modifier = Modifier.fillMaxWidth()) { Text("Abmelden") }
                    }
                }
            }
        }

        Button(onClick = { viewModel.saveSettings() }, modifier = Modifier.fillMaxWidth()) { Text("Einstellungen speichern") }

        Card(colors = CardDefaults.cardColors(containerColor = DangerRedBg), border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.3f))) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Gefahrenzone", style = MaterialTheme.typography.titleMedium, color = DangerRed)
                Text("Alle Schlafeinträge unwiderruflich löschen", style = MaterialTheme.typography.bodySmall, color = DangerRed.copy(alpha = 0.7f), modifier = Modifier.padding(top = 4.dp))
                OutlinedButton(onClick = { viewModel.showClearDataDialog(true) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed), border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f))) { Icon(Icons.Default.DeleteForever, contentDescription = null); Text("  Alle Daten löschen") }
            }
        }
        Text("SchlafGut v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        Spacer(modifier = Modifier.height(80.dp))
    }

    if (state.showClearDataDialog) {
        AlertDialog(onDismissRequest = { viewModel.showClearDataDialog(false) }, title = { Text("Alle Daten löschen?") }, text = { Text("Alle Schlafeinträge werden unwiderruflich gelöscht.") },
            confirmButton = { TextButton(onClick = { viewModel.clearAllData() }) { Text("Löschen", color = DangerRed) } }, dismissButton = { TextButton(onClick = { viewModel.showClearDataDialog(false) }) { Text("Abbrechen") } })
    }
    if (state.showPasswordDialog) {
        BackupPasswordDialog(mode = state.passwordDialogMode, onConfirm = { pw -> when (state.passwordDialogMode) { PasswordDialogMode.UPLOAD -> viewModel.uploadDriveBackup(pw); PasswordDialogMode.RESTORE -> viewModel.restoreDriveBackup(pw) } }, onDismiss = { viewModel.dismissPasswordDialog() })
    }
}

private fun handleAuthResult(authResult: AuthorizationResult, viewModel: SettingsViewModel) {
    val email = authResult.toGoogleSignInAccount()?.email
    if (email != null) viewModel.setDriveAccount(email) else viewModel.setSignInError("Autorisierung erfolgreich, aber kein Email gefunden")
}

@Composable
private fun HealthToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun BackupPasswordDialog(mode: PasswordDialogMode, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val isUpload = mode == PasswordDialogMode.UPLOAD
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(if (isUpload) "Backup-Passwort festlegen" else "Backup-Passwort eingeben") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (isUpload) "Dieses Passwort wird benötigt, um das Backup auf einem anderen Gerät wiederherzustellen." else "Gib das Passwort ein, mit dem das Backup erstellt wurde.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(value = password, onValueChange = { password = it; error = null }, label = { Text("Passwort") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                if (isUpload) { OutlinedTextField(value = confirmPassword, onValueChange = { confirmPassword = it; error = null }, label = { Text("Passwort bestätigen") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth()) }
                error?.let { Text(it, color = DangerRed, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = { TextButton(onClick = { when { password.length < 6 -> error = "Mindestens 6 Zeichen"; isUpload && password != confirmPassword -> error = "Passwörter stimmen nicht überein"; else -> onConfirm(password) } }) { Text(if (isUpload) "Backup erstellen" else "Wiederherstellen") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}
