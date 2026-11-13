package de.schlafgut.app.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import de.schlafgut.app.BuildConfig
import de.schlafgut.app.R
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importJson(context, it) }
    }
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
    val driveAuthLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        try {
            val authResult = Identity.getAuthorizationClient(context)
                .getAuthorizationResultFromIntent(result.data)
            handleAuthResult(authResult, viewModel)
        } catch (e: Exception) {
            viewModel.setSignInError("Autorisierung fehlgeschlagen: ${e.message}")
        }
    }

    /**
     * Holt ein Drive-Access-Token. Ist der Scope bereits erteilt, passiert das still ohne UI;
     * sonst zeigt Google einmalig den Zustimmungsdialog.
     */
    fun requestDriveAuthorization() {
        viewModel.clearSignInError()
        val authRequest = AuthorizationRequest.Builder()
            .setRequestedScopes(listOf(Scope(DriveScopes.DRIVE_APPDATA)))
            .build()
        Identity.getAuthorizationClient(context).authorize(authRequest)
            .addOnSuccessListener { authResult ->
                if (authResult.hasResolution()) {
                    authResult.pendingIntent?.let { pi ->
                        driveAuthLauncher.launch(IntentSenderRequest.Builder(pi.intentSender).build())
                    }
                } else {
                    handleAuthResult(authResult, viewModel)
                }
            }
            .addOnFailureListener { e ->
                viewModel.setSignInError("Autorisierung fehlgeschlagen: ${e.message}")
            }
    }

    LaunchedEffect(state.successMessage) {
        if (state.successMessage != null) {
            delay(3000)
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
        Text("Einstellungen", style = MaterialTheme.typography.headlineLarge)
        state.successMessage?.let { msg ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Text(
                    msg,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        SettingsProfileSection(
            userName = state.userName,
            defaultSleepLatency = state.defaultSleepLatency,
            onUserNameChange = viewModel::setUserName,
            onDefaultLatencyChange = viewModel::setDefaultLatency
        )

        SettingsMedicationsSection(
            medications = state.regularMedications,
            pendingMedName = state.pendingMedName,
            pendingMedDosage = state.pendingMedDosage,
            onPendingMedNameChange = viewModel::setPendingMedName,
            onPendingMedDosageChange = viewModel::setPendingMedDosage,
            onAddMedication = { name, dosage -> viewModel.addMedication(name, dosage) },
            onRemoveMedication = { index -> viewModel.removeMedication(index) }
        )

        SettingsHealthSection(
            state = state,
            onInstallHealthConnect = {
                try {
                    context.startActivity(viewModel.getHealthConnectInstallIntent())
                } catch (e: Exception) {
                    Toast.makeText(context, "Play Store nicht verfügbar", Toast.LENGTH_SHORT).show()
                }
            },
            onHealthConnectEnabledChange = { enabled ->
                if (enabled) {
                    healthPermissionLauncher.launch(viewModel.healthConnectPermissions)
                } else {
                    viewModel.setHealthConnectEnabled(false)
                }
            },
            toggles = HealthToggleCallbacks(
                onWeightChange = { viewModel.setHealthReadWeight(it) },
                onBodyTempChange = { viewModel.setHealthReadBodyTemp(it) },
                onRestingHrChange = { viewModel.setHealthReadRestingHr(it) },
                onHeartRateChange = { viewModel.setHealthReadHeartRate(it) },
                onSpO2Change = { viewModel.setHealthReadSpO2(it) },
                onStepsChange = { viewModel.setHealthReadSteps(it) },
                onSleepChange = { viewModel.setHealthReadSleep(it) }
            )
        )

        SettingsSecuritySection(
            appLockEnabled = state.appLockEnabled,
            onAppLockEnabledChange = { viewModel.setAppLockEnabled(it) }
        )

        SettingsLocalBackupSection(
            onExport = { viewModel.exportJson(context) },
            onImport = { importLauncher.launch(arrayOf("application/json", "*/*")) }
        )

        SettingsDriveBackupSection(
            driveConnected = state.driveConnected,
            isDriveBackupRunning = state.isDriveBackupRunning,
            signInError = state.signInError,
            onConnect = { requestDriveAuthorization() },
            onUpload = { viewModel.showPasswordDialog(PasswordDialogMode.UPLOAD) },
            onRestore = { viewModel.showPasswordDialog(PasswordDialogMode.RESTORE) },
            onDisconnect = { viewModel.disconnectDrive() }
        )

        Button(onClick = { viewModel.saveSettings() }, modifier = Modifier.fillMaxWidth()) {
            Text("Einstellungen speichern")
        }

        SettingsDangerZone(onClearDataClick = { viewModel.showClearDataDialog(true) })

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.settings_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                stringResource(R.string.settings_by),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(80.dp))
    }

    if (state.showClearDataDialog) {
        ClearDataDialog(
            onConfirm = { viewModel.clearAllData() },
            onDismiss = { viewModel.showClearDataDialog(false) }
        )
    }
    if (state.showPasswordDialog) {
        BackupPasswordDialog(
            mode = state.passwordDialogMode,
            onConfirm = { pw ->
                // Token wird erst jetzt (frisch) geholt – Access-Tokens laufen nach ca. 1 h ab.
                viewModel.queueDriveAction(state.passwordDialogMode, pw)
                requestDriveAuthorization()
            },
            onDismiss = { viewModel.dismissPasswordDialog() }
        )
    }
}

private fun handleAuthResult(authResult: AuthorizationResult, viewModel: SettingsViewModel) {
    val token = authResult.accessToken
    if (token != null) {
        viewModel.onDriveAuthorized(token)
    } else {
        viewModel.setSignInError("Autorisierung erfolgreich, aber kein Zugriffstoken erhalten")
    }
}
