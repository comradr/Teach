package com.example

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object GeminiApiKeyProvider {
    private const val PLACEHOLDER = "MY_GEMINI_API_KEY"
    private const val PREFS_NAME = "secure_local_settings"
    private const val ENCRYPTED_KEY = "gemini_api_key_ciphertext"
    private const val IV_KEY = "gemini_api_key_iv"
    private const val KEY_ALIAS = "mksh_gemini_api_key_v1"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    fun get(context: Context): String = resolve(readLocal(context), BuildConfig.GEMINI_API_KEY)

    fun isConfigured(context: Context): Boolean = isUsable(get(context))

    fun hasLocalKey(context: Context): Boolean = isUsable(readLocal(context).orEmpty())

    fun saveLocal(context: Context, value: String): Boolean {
        val key = value.trim()
        if (!isUsable(key)) return false

        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
            }
            val encrypted = cipher.doFinal(key.toByteArray(Charsets.UTF_8))
            context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(ENCRYPTED_KEY, Base64.encodeToString(encrypted, Base64.NO_WRAP))
                .putString(IV_KEY, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
                .commit()
        }.getOrDefault(false)
    }

    fun clearLocal(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(ENCRYPTED_KEY)
            .remove(IV_KEY)
            .apply()
    }

    internal fun resolve(localValue: String?, buildValue: String): String =
        localValue?.trim()?.takeIf(::isUsable) ?: buildValue.trim()

    fun isUsable(value: String): Boolean {
        val key = value.trim()
        return key.isNotEmpty() && key != PLACEHOLDER && !key.contains("YOUR_API_KEY", ignoreCase = true)
    }

    private fun readLocal(context: Context): String? = runCatching {
        val preferences = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encrypted = preferences.getString(ENCRYPTED_KEY, null) ?: return null
        val iv = preferences.getString(IV_KEY, null) ?: return null
        val secretKey = getExistingSecretKey() ?: return null
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(
                Cipher.DECRYPT_MODE,
                secretKey,
                GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP))
            )
        }
        String(cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP)), Charsets.UTF_8)
    }.getOrNull()

    private fun getExistingSecretKey(): SecretKey? {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return keyStore.getKey(KEY_ALIAS, null) as? SecretKey
    }

    private fun getOrCreateSecretKey(): SecretKey = getExistingSecretKey() ?: run {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        keyGenerator.generateKey()
    }
}
