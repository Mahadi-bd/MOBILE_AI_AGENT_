package com.example.data.ai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores the user's own Gemini API key on the device only.
 * The key is encrypted with an AES-GCM key that lives in the Android Keystore
 * (it can not be copied off the phone). The key is only ever sent to Google's Gemini API.
 */
object ApiKeyStore {
    private const val ALIAS = "life_easy_api_key_v1"
    private const val PREFS = "life_easy_secure"
    private const val FIELD = "api_key"
    private const val PLAIN_PREFIX = "plain:"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun secretKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore")
        ks.load(null)
        val existing = ks.getKey(ALIAS, null)
        if (existing is SecretKey) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    fun save(context: Context, apiKey: String): Boolean {
        val clean = apiKey.trim()
        if (clean.isEmpty()) return false
        val packed = try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey())
            val encrypted = cipher.doFinal(clean.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
        } catch (_: Exception) {
            // Very rare: a phone with a broken Keystore. Keep the app usable (app-private storage).
            PLAIN_PREFIX + Base64.encodeToString(clean.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        }
        prefs(context).edit().putString(FIELD, packed).apply()
        return true
    }

    fun load(context: Context): String {
        val packed = prefs(context).getString(FIELD, null) ?: return ""
        return try {
            if (packed.startsWith(PLAIN_PREFIX)) {
                String(Base64.decode(packed.removePrefix(PLAIN_PREFIX), Base64.NO_WRAP), Charsets.UTF_8)
            } else {
                val all = Base64.decode(packed, Base64.NO_WRAP)
                val iv = all.copyOfRange(0, 12)
                val data = all.copyOfRange(12, all.size)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
                String(cipher.doFinal(data), Charsets.UTF_8)
            }
        } catch (_: Exception) {
            ""
        }
    }

    fun hasKey(context: Context): Boolean = load(context).isNotBlank()

    fun clear(context: Context) {
        prefs(context).edit().remove(FIELD).apply()
    }
}
