package de.schlafgut.app.data.backup

import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import de.schlafgut.app.data.export.JsonImportExport
import de.schlafgut.app.data.repository.SleepRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Verwaltet passwort-verschlüsselte Backups in Google Drive appDataFolder.
 * - appDataFolder: Versteckter App-spezifischer Ordner
 * - Verschlüsselung: PBKDF2 + AES-256-GCM (passwortbasiert, geräteübergreifend)
 */
@Singleton
class DriveBackupManager @Inject constructor(
    private val repository: SleepRepository
) {
    companion object {
        private const val BACKUP_FILE_NAME = "schlafgut_backup.enc"
    }

    /**
     * Baut den Drive-Client mit einem OAuth-Access-Token aus der
     * Identity-AuthorizationClient-API (kein GoogleSignInAccount / GoogleAccountCredential nötig).
     * Das Token ist ca. 1 h gültig und wird vor jeder Operation still neu geholt.
     */
    private fun getDriveService(accessToken: String): Drive {
        val requestInitializer = HttpRequestInitializer { request ->
            request.headers.authorization = "Bearer $accessToken"
        }

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            requestInitializer
        )
            .setApplicationName("SchlafGut")
            .build()
    }

    /**
     * Erstellt ein passwort-verschlüsseltes Backup und lädt es nach Google Drive hoch.
     */
    suspend fun uploadBackup(accessToken: String, password: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val driveService = getDriveService(accessToken)

                // 1. Daten sammeln
                val entries = repository.getAllEntries().first()
                val settings = repository.getSettingsOnce()
                val jsonString = JsonImportExport.exportToJson(entries, settings)

                // 2. Passwort-verschlüsseln
                val encryptedBytes = BackupEncryption.encryptWithPassword(jsonString, password)

                // 3. Vorhandenes Backup löschen
                deleteExistingBackup(driveService)

                // 4. Hochladen
                val fileMetadata = com.google.api.services.drive.model.File().apply {
                    name = BACKUP_FILE_NAME
                    parents = listOf("appDataFolder")
                }
                val content = ByteArrayContent("application/octet-stream", encryptedBytes)
                driveService.files().create(fileMetadata, content)
                    .setFields("id, name, modifiedTime")
                    .execute()

                Result.success("Backup erstellt (${entries.size} Einträge)")
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Lädt das Backup herunter und entschlüsselt es mit dem Passwort.
     */
    suspend fun downloadAndRestoreBackup(
        accessToken: String,
        password: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService(accessToken)

            // 1. Backup-Datei finden
            val fileId = findBackupFileId(driveService)
                ?: return@withContext Result.failure(Exception("Kein Backup gefunden"))

            // 2. Herunterladen
            val outputStream = ByteArrayOutputStream()
            driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)
            val encryptedBytes = outputStream.toByteArray()

            // 3. Passwort-entschlüsseln
            val jsonString = try {
                BackupEncryption.decryptWithPassword(encryptedBytes, password)
            } catch (e: Exception) {
                return@withContext Result.failure(
                    Exception("Falsches Passwort oder beschädigtes Backup")
                )
            }

            // 4. Importieren
            val importResult = JsonImportExport.importFromJson(jsonString)
            var imported = 0
            var skipped = 0

            for (entry in importResult.entries) {
                val existing = repository.getEntryById(entry.id)
                if (existing == null) {
                    repository.saveEntry(entry)
                    imported++
                } else {
                    skipped++
                }
            }

            importResult.settings?.let { settings ->
                val current = repository.getSettingsOnce()
                if (current.userName.isBlank() && settings.userName.isNotBlank()) {
                    repository.saveSettings(
                        current.copy(
                            userName = settings.userName,
                            defaultSleepLatency = settings.defaultSleepLatency
                        )
                    )
                }
            }

            val msg = "$imported Einträge wiederhergestellt" +
                if (skipped > 0) ", $skipped übersprungen (Duplikate)" else ""
            Result.success(msg)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun hasBackup(accessToken: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService(accessToken)
            findBackupFileId(driveService) != null
        } catch (e: Exception) {
            false
        }
    }

    private fun findBackupFileId(driveService: Drive): String? {
        val result = driveService.files().list()
            .setSpaces("appDataFolder")
            .setQ("name = '$BACKUP_FILE_NAME'")
            .setFields("files(id, name, modifiedTime)")
            .setPageSize(1)
            .execute()
        return result.files?.firstOrNull()?.id
    }

    private fun deleteExistingBackup(driveService: Drive) {
        val existingId = findBackupFileId(driveService)
        if (existingId != null) {
            driveService.files().delete(existingId).execute()
        }
    }
}
