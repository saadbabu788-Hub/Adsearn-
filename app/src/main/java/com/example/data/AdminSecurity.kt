package com.example.data

import java.security.MessageDigest
import java.util.UUID

object AdminSecurity {
    // Salt used for password verification
    private const val PASSWORD_SALT = "adsearn_salt_2026:"

    // SHA-256 hash of "adsearn_salt_2026:Saad@1234"
    // The plain-text password is never hardcoded or stored in APK bytecode/strings!
    private const val DEFAULT_PASSWORD_HASH =
        "6fbf80cde7e2e8c6c3b6d9fbf09ca13123e751ee09b5612a53a58e4563ab6800"

    fun verifyPassword(inputPassword: String, storedHash: String? = null): Boolean {
        if (inputPassword.isEmpty()) return false
        val expectedHash = storedHash?.ifBlank { null } ?: DEFAULT_PASSWORD_HASH
        val computedHash = hashWithSalt(inputPassword)
        return MessageDigest.isEqual(
            computedHash.toByteArray(Charsets.UTF_8),
            expectedHash.toByteArray(Charsets.UTF_8)
        )
    }

    fun hashWithSalt(plainText: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val combined = PASSWORD_SALT + plainText
        val digest = md.digest(combined.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}

data class AdminAuditLog(
    val id: String = UUID.randomUUID().toString(),
    val action: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)
