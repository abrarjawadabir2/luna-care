package com.example.security

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object HashUtils {
    // In a real production app, this pepper would be injected via BuildConfig or a secure remote config,
    // and ideally never hardcoded. 
    private const val SERVER_PEPPER = "LUNA_CARE_SECURE_PEPPER_2026_XYZ"

    enum class HashPurpose(val label: String) {
        EMAIL_RATE_LIMIT("email_rate_limit"),
        PHONE_RATE_LIMIT("phone_rate_limit"),
        IP_RATE_LIMIT("ip_rate_limit"),
        USER_AGENT_RATE_LIMIT("user_agent_rate_limit"),
        DEVICE_RATE_LIMIT("device_rate_limit"),
        EMAIL_LOOKUP("email_lookup"),
        PHONE_LOOKUP("phone_lookup"),
        AUDIT_REFERENCE("audit_reference")
    }

    fun normalizeEmail(email: String): String {
        return email.trim().lowercase()
    }

    fun normalizePhone(phone: String): String {
        val trimmed = phone.trim()
        val normalized = trimmed.replace(Regex("[\\s().\\-]"), "")
        if (!normalized.matches(Regex("^\\+[1-9]\\d{7,14}\$"))) {
            return "" // Invalid format
        }
        return normalized
    }

    fun hashForRateLimit(value: String, purpose: HashPurpose): String {
        return hmacSha256(SERVER_PEPPER, "${purpose.label}:$value")
    }

    fun hashForLookup(value: String, purpose: HashPurpose): String {
        return hmacSha256(SERVER_PEPPER, "${purpose.label}:$value")
    }

    fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return email
        val user = parts[0]
        val domain = parts[1]
        
        val maskedUser = if (user.length <= 2) {
            user.map { '*' }.joinToString("")
        } else {
            user.first() + "*".repeat(user.length - 2) + user.last()
        }
        return "$maskedUser@$domain"
    }

    fun maskPhone(phone: String): String {
        val normalized = normalizePhone(phone)
        if (normalized.isEmpty() || normalized.length < 8) return ""
        val prefix = normalized.substring(0, 3)
        val suffix = normalized.substring(normalized.length - 4)
        return "$prefix •••• $suffix"
    }

    private fun hmacSha256(key: String, message: String): String {
        return try {
            val hmacSha256 = "HmacSHA256"
            val secretKeySpec = SecretKeySpec(key.toByteArray(Charsets.UTF_8), hmacSha256)
            val mac = Mac.getInstance(hmacSha256)
            mac.init(secretKeySpec)
            val bytes = mac.doFinal(message.toByteArray(Charsets.UTF_8))
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "HASH_ERROR"
        }
    }
}
