package de.schlafgut.app.util

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricHelper {

    fun canAuthenticate(activity: FragmentActivity): Boolean {
        val biometricManager = BiometricManager.from(activity)
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                // If user cancels or hasn't set up screen lock, we report it so the UI can react
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || 
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    onError("Authentifizierung abgebrochen")
                } else {
                    onError(errString.toString())
                }
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("SchlafGut")
            .setSubtitle("Bitte entsperre die App mit deiner Displaysperre")
            // This allows using Biometrics OR Device PIN/Pattern/Password
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        try {
            BiometricPrompt(activity, executor, callback).authenticate(promptInfo)
        } catch (e: Exception) {
            onError(e.message ?: "Fehler bei der Authentifizierung")
        }
    }
}
