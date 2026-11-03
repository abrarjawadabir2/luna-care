package com.example.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec

/**
 * SecurityUtils — AES-256-GCM encryption using Android Keystore.
 *
 * Sensitive fields (medical notes, journal entries, reminder notes, etc.)
 * are encrypted before being stored in the Room database.
 * The key never leaves the Android Keystore hardware module.
 *
 * SECURITY RULES:
 * - Never log or print decrypted content.
 * - Encrypted blobs stored in DB contain IV prepended (Base64).
 * - Admin roles cannot decrypt — decryption only happens on-device.
 */
object SecurityUtils {

    private const val KEY_ALIAS = "LunaCareDataKey"
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
    private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM
    private const val PADDING = KeyProperties.ENCRYPTION_PADDING_NONE
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12 // bytes
    private const val GCM_TAG_LENGTH = 128 // bits
    private const val PIN_HASH_PREFIX = "pbkdf2"
    private const val PIN_PBKDF2_ITERATIONS = 120_000
    private const val PIN_SALT_BYTES = 16
    private const val PIN_KEY_BITS = 256

    /**
     * Hashes a string (e.g. IP address or user-agent) using SHA-256.
     * Hashed value can be stored in audit_logs without exposing raw data.
     */
    fun hashForAudit(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return java.util.Base64.getEncoder().encodeToString(hashBytes)
    }

    /**
     * Hashes a user passcode/PIN using PBKDF2-HMAC-SHA256 with a random 128-bit salt.
     * Format: pbkdf2$<pinLength>$<saltBase64>$<hashBase64>
     */
    fun hashPin(pin: String): String {
        require(pin.isNotEmpty()) { "PIN cannot be empty" }
        val salt = ByteArray(PIN_SALT_BYTES)
        SecureRandom().nextBytes(salt)
        val derived = derivePinBytes(pin, salt, PIN_PBKDF2_ITERATIONS)
        val encoder = java.util.Base64.getEncoder().withoutPadding()
        val saltB64 = encoder.encodeToString(salt)
        val hashB64 = encoder.encodeToString(derived)
        return "$PIN_HASH_PREFIX$${pin.length}$$saltB64$$hashB64"
    }

    /**
     * Verifies a candidate PIN against a stored PBKDF2 hash (or legacy plaintext PIN)
     * using constant-time byte comparison.
     */
    fun verifyPin(input: String, stored: String): Boolean {
        if (input.isEmpty() || stored.isEmpty()) return false
        if (stored.startsWith("$PIN_HASH_PREFIX$")) {
            val parts = stored.split("$")
            if (parts.size != 4) return false
            return try {
                val decoder = java.util.Base64.getDecoder()
                val salt = decoder.decode(parts[2])
                val expectedHash = decoder.decode(parts[3])
                val computedHash = derivePinBytes(input, salt, PIN_PBKDF2_ITERATIONS)
                MessageDigest.isEqual(computedHash, expectedHash)
            } catch (e: Exception) {
                false
            }
        }
        return MessageDigest.isEqual(
            input.toByteArray(Charsets.UTF_8),
            stored.toByteArray(Charsets.UTF_8)
        )
    }

    /**
     * Extracts the expected PIN digit length from an encoded PIN hash or legacy PIN string.
     */
    fun extractPinLength(stored: String): Int {
        if (stored.startsWith("$PIN_HASH_PREFIX$")) {
            val parts = stored.split("$")
            if (parts.size == 4) {
                return parts[1].toIntOrNull()?.coerceIn(4, 12) ?: 4
            }
        }
        return stored.length.coerceIn(4, 12)
    }

    private fun derivePinBytes(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, PIN_KEY_BITS)
        return try {
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            factory.generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
        keyStore.load(null)

        if (keyStore.containsAlias(KEY_ALIAS)) {
            val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (entry != null) return entry.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(ALGORITHM, KEYSTORE_PROVIDER)
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(BLOCK_MODE)
                .setEncryptionPaddings(PADDING)
                .setKeySize(256)
                .setUserAuthenticationRequired(false) // PIN is handled at app level
                .build()
        )
        return keyGenerator.generateKey()
    }

    /**
     * Encrypts plaintext string. Returns Base64(IV + ciphertext).
     * Returns null if input is null.
     * NEVER call this on already-encrypted data.
     */
    fun encrypt(plaintext: String?): String? {
        if (plaintext == null) return null
        return try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv // 12 bytes GCM IV
            val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            val combined = iv + ciphertext
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            // Encryption failure — return null rather than storing plaintext
            null
        }
    }

    /**
     * Decrypts a Base64(IV + ciphertext) blob. Returns null if input is null or decryption fails.
     * NEVER log the result.
     */
    fun decrypt(encrypted: String?): String? {
        if (encrypted == null) return null
        return try {
            val combined = Base64.decode(encrypted, Base64.NO_WRAP)
            val iv = combined.sliceArray(0 until GCM_IV_LENGTH)
            val ciphertext = combined.sliceArray(GCM_IV_LENGTH until combined.size)

            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Crisis keyword detection — checks any user-entered text for self-harm indicators.
 * Returns true if crisis keywords are detected.
 * Does NOT send data to server; triggers local crisis UI only.
 */
object CrisisDetector {
    private val CRISIS_KEYWORDS = listOf(
        "suicide", "suicidal", "kill myself", "kill myself",
        "hurt myself", "self harm", "self-harm", "selfharm",
        "don't want to live", "dont want to live",
        "end my life", "end it all",
        "not want to be here", "want to die",
        "no reason to live"
    )

    fun detect(vararg texts: String?): Boolean {
        val combined = texts.filterNotNull().joinToString(" ").lowercase()
        return CRISIS_KEYWORDS.any { keyword -> combined.contains(keyword) }
    }
}
