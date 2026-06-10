package com.example.rustyalarm.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Stores the user's PIN as a PBKDF2-SHA256 hash inside EncryptedSharedPreferences
 * (AES-256-GCM at rest, AES-SIV key wrap). The PIN itself never leaves the device.
 *
 * Brute-force protection: failure counter + exponential backoff persisted in the
 * same encrypted store (survives process restart).
 *
 * Biometric unlock is an opt-in convenience flag stored alongside the hash. The
 * biometric path is **not** cryptographically bound to a Keystore CryptoObject —
 * this is a known limitation. Treat the device-level Keystore + screen lock as
 * the root of trust.
 */
class AuthRepository(context: Context) {

    private val prefs: SharedPreferences = run {
        val master = MasterKey.Builder(context.applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context.applicationContext,
            FILE_NAME,
            master,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun hasPin(): Boolean = prefs.contains(KEY_HASH)

    fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = pbkdf2(pin, salt)
        prefs.edit()
            .putString(KEY_HASH, base64(hash))
            .putString(KEY_SALT, base64(salt))
            .remove(KEY_FAIL_COUNT)
            .remove(KEY_LAST_FAIL_MILLIS)
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_HASH, null) ?: return false
        val storedSalt = prefs.getString(KEY_SALT, null) ?: return false
        val candidate = pbkdf2(pin, decodeBase64(storedSalt))
        val ok = constantTimeEquals(base64(candidate), storedHash)
        if (ok) {
            // Clear throttle on success
            prefs.edit()
                .remove(KEY_FAIL_COUNT)
                .remove(KEY_LAST_FAIL_MILLIS)
                .apply()
        } else {
            val count = prefs.getInt(KEY_FAIL_COUNT, 0) + 1
            prefs.edit()
                .putInt(KEY_FAIL_COUNT, count)
                .putLong(KEY_LAST_FAIL_MILLIS, System.currentTimeMillis())
                .apply()
        }
        return ok
    }

    fun clearPin() {
        prefs.edit().clear().apply()
    }

    var biometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC, value).apply()

    /**
     * Seconds the caller must wait before the next PIN attempt is allowed.
     * 0 means "no throttle".
     */
    fun throttleSecondsRemaining(): Long {
        val count = prefs.getInt(KEY_FAIL_COUNT, 0)
        if (count < THROTTLE_THRESHOLD) return 0
        val lastFail = prefs.getLong(KEY_LAST_FAIL_MILLIS, 0)
        val backoffSeconds = backoffSeconds(count)
        val nextAllowed = lastFail + backoffSeconds * 1000L
        val remaining = (nextAllowed - System.currentTimeMillis()) / 1000L
        return maxOf(0L, remaining)
    }

    fun failureCount(): Int = prefs.getInt(KEY_FAIL_COUNT, 0)

    private fun backoffSeconds(failCount: Int): Long {
        // 5 fails → 30s; 6 → 60s; 7 → 120s; 8+ → 300s capped
        return when {
            failCount < THROTTLE_THRESHOLD -> 0L
            failCount == 5 -> 30L
            failCount == 6 -> 60L
            failCount == 7 -> 120L
            else -> 300L
        }
    }

    private fun pbkdf2(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private fun base64(bytes: ByteArray): String =
        android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)

    private fun decodeBase64(str: String): ByteArray =
        android.util.Base64.decode(str, android.util.Base64.NO_WRAP)

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].code xor b[i].code)
        return diff == 0
    }

    companion object {
        private const val FILE_NAME           = "rusty_alarm_auth"
        private const val KEY_HASH            = "pin_hash"
        private const val KEY_SALT            = "pin_salt"
        private const val KEY_BIOMETRIC       = "biometric_enabled"
        private const val KEY_FAIL_COUNT      = "fail_count"
        private const val KEY_LAST_FAIL_MILLIS = "last_fail_millis"

        private const val PBKDF2_ITERATIONS   = 100_000
        private const val PBKDF2_KEY_BITS     = 256
        private const val THROTTLE_THRESHOLD  = 5
    }
}
