package com.example.finora

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class AppLockStore(context: Context) {

    private val preferences = context.applicationContext
        .getSharedPreferences("finora_app_lock", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_LOCK_ENABLED = "lock_enabled"

        private const val SALT_BYTES = 16
        private const val HASH_BITS = 256
        private const val ITERATIONS = 120_000
    }

    fun isPinSet(): Boolean =
        preferences.contains(KEY_PIN_SALT) &&
                preferences.contains(KEY_PIN_HASH)

    fun isEnabled(): Boolean =
        preferences.getBoolean(KEY_LOCK_ENABLED, false)

    fun setEnabled(enabled: Boolean): Boolean {
        if (enabled && !isPinSet()) return false

        return preferences.edit()
            .putBoolean(KEY_LOCK_ENABLED, enabled)
            .commit()
    }

    fun setPin(pin: String): Boolean {
        if (!pin.matches(Regex("^\\d{4,6}$"))) return false

        return try {
            val salt = ByteArray(SALT_BYTES).also {
                SecureRandom().nextBytes(it)
            }

            val hash = deriveHash(pin, salt)

            preferences.edit()
                .putString(KEY_PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
                .putString(KEY_PIN_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
                .commit()
        } catch (_: Exception) {
            false
        }
    }

    fun verifyPin(pin: String): Boolean {
        if (!pin.matches(Regex("^\\d{4,6}$"))) return false

        val saltEncoded = preferences.getString(KEY_PIN_SALT, null)
            ?: return false
        val hashEncoded = preferences.getString(KEY_PIN_HASH, null)
            ?: return false

        return try {
            val salt = Base64.decode(saltEncoded, Base64.NO_WRAP)
            val expectedHash = Base64.decode(hashEncoded, Base64.NO_WRAP)
            val actualHash = deriveHash(pin, salt)

            MessageDigest.isEqual(expectedHash, actualHash)
        } catch (_: Exception) {
            false
        }
    }

    fun clearPin() {
        preferences.edit()
            .remove(KEY_PIN_SALT)
            .remove(KEY_PIN_HASH)
            .putBoolean(KEY_LOCK_ENABLED, false)
            .commit()
    }

    private fun deriveHash(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(
            pin.toCharArray(),
            salt,
            ITERATIONS,
            HASH_BITS
        )

        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .encoded
        } finally {
            spec.clearPassword()
        }
    }
}