package com.example.data

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object EncryptionHelper {
    private var sessionKey: ByteArray? = null
    private val fallbackKeyString = "LunaCareDefaultSecureKeyFallbackHex2026!"
    
    // Salt used for session key derivation (stable local salt)
    private val stableLocalSalt = byteArrayOf(
        0x12.toByte(), 0x34.toByte(), 0x56.toByte(), 0x78.toByte(), 0x90.toByte(), 0xAB.toByte(), 0xCD.toByte(), 0xEF.toByte(),
        0xFE.toByte(), 0xDC.toByte(), 0xBA.toByte(), 0x09.toByte(), 0x87.toByte(), 0x65.toByte(), 0x43.toByte(), 0x21.toByte()
    )

    private var cachedFallbackKey: ByteArray? = null

    fun setSessionKeyFromPassword(password: String, saltHex: String) {
        val salt = try {
            hexToByteArray(saltHex)
        } catch (e: Exception) {
            stableLocalSalt
        }
        sessionKey = deriveKey(password, salt)
    }

    fun clearSession() {
        sessionKey = null
    }

    fun getActiveKey(): ByteArray {
        sessionKey?.let { return it }
        cachedFallbackKey?.let { return it }
        val key = deriveKey(fallbackKeyString, stableLocalSalt)
        cachedFallbackKey = key
        return key
    }

    fun deriveKey(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, 2000, 256)
        val f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return f.generateSecret(spec).encoded
    }

    fun encryptSensitiveText(plainText: String?): String? {
        if (plainText == null) return null
        if (plainText.isEmpty()) return ""
        return try {
            val keyBytes = getActiveKey()
            val secretKey = SecretKeySpec(keyBytes, "AES")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = ByteArray(12)
            SecureRandom().nextBytes(iv)
            val parameterSpec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)
            val cipherTextBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            
            // Combine IV and CipherText
            val combined = ByteArray(iv.size + cipherTextBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherTextBytes, 0, combined, iv.size, cipherTextBytes.size)
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            null // Return null to indicate encryption failure
        }
    }

    fun decryptSensitiveText(cipherText: String?): String? {
        if (cipherText == null) return null
        if (cipherText.isEmpty()) return ""
        return try {
            val combined = Base64.decode(cipherText, Base64.NO_WRAP)
            if (combined.size < 12) return cipherText
            val iv = ByteArray(12)
            System.arraycopy(combined, 0, iv, 0, 12)
            val cipherTextBytes = ByteArray(combined.size - 12)
            System.arraycopy(combined, 12, cipherTextBytes, 0, cipherTextBytes.size)
            
            val keyBytes = getActiveKey()
            val secretKey = SecretKeySpec(keyBytes, "AES")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val parameterSpec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec)
            val decryptedBytes = cipher.doFinal(cipherTextBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            null // Return null to indicate decryption failure (locked state or wrong key)
        }
    }

    fun hashLookupValue(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(value.trim().lowercase().toByteArray(Charsets.UTF_8))
        return byteArrayToHex(hashBytes)
    }

    fun maskEmail(email: String): String {
        val trimmed = email.trim()
        val parts = trimmed.split("@")
        if (parts.size != 2) return trimmed
        val local = parts[0]
        val domain = parts[1]
        if (local.length <= 1) return "$local***@$domain"
        return "${local.first()}***@$domain"
    }

    fun generateSalt(): String {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return byteArrayToHex(salt)
    }

    fun byteArrayToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { String.format("%02x", it) }
    }

    fun hexToByteArray(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
