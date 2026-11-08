package com.arborecence.app

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiKeyStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun getKey(): String? = prefs.getString(KEY_GEMINI, null)?.trim()?.takeIf { it.isNotEmpty() }

    fun saveKey(key: String) {
        prefs.edit().putString(KEY_GEMINI, key.trim()).apply()
    }

    fun revokeKey() {
        prefs.edit().remove(KEY_GEMINI).apply()
    }

    fun hasKey(): Boolean = !getKey().isNullOrBlank()

    companion object {
        private const val PREFS_NAME = "secure_gemini_prefs"
        private const val KEY_GEMINI = "gemini_api_key"
    }
}
