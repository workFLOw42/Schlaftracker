package de.schlafgut.app.data.backup

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.crypto.Cipher

/**
 * Verschlüsselt und entschlüsselt Backup-Daten.
 *
 * Zwei Modi:
 * - **Gerätegebunden** (Tink/Android Keystore): Schnell, aber nur auf diesem Gerät entschlüsselbar.
 * - **Passwortbasiert** (PBKDF2 + AES-256-GCM): Geräteübergreifend, mit Benutzerpasswort.
 */
object BackupEncryption {

    private const val KEYSET_NAME = "schlafgut_backup_keyset"
    private const val PREF_FILE = "schlafgut_backup_keys"
    private const val MASTER_KEY_URI = "android-keystore://schlafgut_backup_master"

    // Passwortbasierte Verschlüsselung
    private const val PBKDF2_ITERATIONS = 210_000
    private const val SALT_SIZE = 32
    private const val IV_SIZE = 12
    private const val KEY_SIZE = 256
    private const val TAG_SIZE = 128

    init {
        AeadConfig.register()
    }

    // ============================================================
    // Passwortbasierte Verschlüsselung (geräteübergreifend)
    // ============================================================

    /**
     * Verschlüsselt mit Passwort (PBKDF2 + AES-256-GCM).
     * Format: [salt (32 bytes)][iv (12 bytes)][ciphertext + tag]
     */
    fun encryptWithPassword(plaintext: String, password: String): ByteArray {
        val salt = ByteArray(SALT_SIZE).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_SIZE).also { SecureRandom().nextBytes(it) }

        val keySpec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_SIZE)
        val keyBytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(keySpec).encoded
        val secretKey = SecretKeySpec(keyBytes, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(TAG_SIZE, iv))
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        return salt + iv + ciphertext
    }

    /**
     * Entschlüsselt mit Passwort.
     * @throws javax.crypto.AEADBadTagException wenn das Passwort falsch ist
     */
    fun decryptWithPassword(data: ByteArray, password: String): String {
        val salt = data.copyOfRange(0, SALT_SIZE)
        val iv = data.copyOfRange(SALT_SIZE, SALT_SIZE + IV_SIZE)
        val ciphertext = data.copyOfRange(SALT_SIZE + IV_SIZE, data.size)

        val keySpec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_SIZE)
        val keyBytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(keySpec).encoded
        val secretKey = SecretKeySpec(keyBytes, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_SIZE, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }
}
