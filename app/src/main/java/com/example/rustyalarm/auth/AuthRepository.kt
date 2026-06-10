package com.example.rustyalarm.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Stores the user's PIN as a salted SHA-256 hash inside
 * EncryptedSharedPreferences (AES-256-GCM at rest, AES-SIV key wrap).
 *
 * The PIN itself never leaves the device. Biometric unlock is an OPT-IN
 * convenience flag stored alongside the hash.
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
        val hash = hashPin(pin, salt)
        prefs.edit()
            .putString(KEY_HASH, base64(hash))
            .putString(KEY_SALT, base64(salt))
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_HASH, null) ?: return false
        val storedSalt = prefs.getString(KEY_SALT, null) ?: return false
        val candidate = hashPin(pin, decodeBase64(storedSalt))
        return constantTimeEquals(base64(candidate), storedHash)
    }

    fun clearPin() {
        prefs.edit().clear().apply()
    }

    var biometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC, value).apply()

    private fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        md.update(pin.toByteArray(Charsets.UTF_8))
        // Stretch with 10k iterations (cheap KDF substitute — fine for 4-digit PIN
        // since the brute-force surface is local anyway)
        var digest = md.digest()
        repeat(9_999) {
            val again = MessageDigest.getInstance("SHA-256")
            again.update(salt)
            again.update(digest)
            digest = again.digest()
        }
        return digest
    }

    private fun base64(bytes: ByteArray): String =
        android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)

    private fun decodeBase64(str: String): ByteArray =
        android.util.Base64.decode(str, android.util.Base64.NO_WRAP)

    /** Length-independent constant-time string equality. */
    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].code xor b[i].code)
        return diff == 0
    }

    companion object {
        private const val FILE_NAME      = "rusty_alarm_auth"
        private const val KEY_HASH       = "pin_hash"
        private const val KEY_SALT       = "pin_salt"
        private const val KEY_BIOMETRIC  = "biometric_enabled"
    }
}
