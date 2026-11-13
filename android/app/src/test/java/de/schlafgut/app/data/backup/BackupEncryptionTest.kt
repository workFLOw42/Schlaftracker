package de.schlafgut.app.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.GeneralSecurityException

class BackupEncryptionTest {

    private val password = "correct horse battery staple"
    private val plaintext = """{"version":1,"entries":[{"id":"abc","notes":"Schlaf gut – äöüß 😴"}]}"""

    @Test
    fun roundtrip_returnsOriginalPlaintext() {
        val encrypted = BackupEncryption.encryptWithPassword(plaintext, password)
        assertEquals(plaintext, BackupEncryption.decryptWithPassword(encrypted, password))
    }

    @Test
    fun roundtrip_emptyString() {
        val encrypted = BackupEncryption.encryptWithPassword("", password)
        // salt(32) + iv(12) + tag(16)
        assertEquals(32 + 12 + 16, encrypted.size)
        assertEquals("", BackupEncryption.decryptWithPassword(encrypted, password))
    }

    @Test
    fun output_hasExpectedLength() {
        val encrypted = BackupEncryption.encryptWithPassword(plaintext, password)
        val plainBytes = plaintext.toByteArray(Charsets.UTF_8).size
        assertEquals(32 + 12 + plainBytes + 16, encrypted.size)
    }

    @Test
    fun wrongPassword_throws() {
        val encrypted = BackupEncryption.encryptWithPassword(plaintext, password)
        assertThrows(GeneralSecurityException::class.java) {
            BackupEncryption.decryptWithPassword(encrypted, "wrong password")
        }
    }

    @Test
    fun tamperedCiphertext_throws() {
        val encrypted = BackupEncryption.encryptWithPassword(plaintext, password)
        encrypted[encrypted.size - 1] = (encrypted[encrypted.size - 1].toInt() xor 0x01).toByte()
        assertThrows(GeneralSecurityException::class.java) {
            BackupEncryption.decryptWithPassword(encrypted, password)
        }
    }

    @Test
    fun encryptTwice_producesDifferentBytes_saltAndIvDiffer() {
        val a = BackupEncryption.encryptWithPassword(plaintext, password)
        val b = BackupEncryption.encryptWithPassword(plaintext, password)
        assertFalse(a.contentEquals(b))
        assertFalse("Salt should differ", a.copyOfRange(0, 32).contentEquals(b.copyOfRange(0, 32)))
        assertFalse("IV should differ", a.copyOfRange(32, 44).contentEquals(b.copyOfRange(32, 44)))
        // Beide bleiben entschlüsselbar
        assertEquals(plaintext, BackupEncryption.decryptWithPassword(a, password))
        assertEquals(plaintext, BackupEncryption.decryptWithPassword(b, password))
    }

    @Test
    fun ciphertextDoesNotContainPlaintext() {
        val encrypted = BackupEncryption.encryptWithPassword(plaintext, password)
        val asString = String(encrypted, Charsets.ISO_8859_1)
        assertTrue(!asString.contains("version"))
    }
}
