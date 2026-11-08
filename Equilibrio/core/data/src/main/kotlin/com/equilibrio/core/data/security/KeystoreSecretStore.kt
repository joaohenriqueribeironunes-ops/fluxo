package com.equilibrio.core.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Segredos em repouso:
 * - passphrase aleatória do SQLCipher;
 * - chave opcional do Gemini (modo direto), para quem aceitar o risco.
 *
 * Ambos são cifrados com AES-256/GCM usando uma chave não exportável do
 * Android Keystore. O APK nunca contém a chave do Gemini.
 */
class KeystoreSecretStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val lock = Any()

    private fun keyStore(): KeyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }

    private fun getOrCreateKey(): SecretKey {
        val ks = keyStore()
        (ks.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): String {
        val combined = Base64.decode(encoded, Base64.NO_WRAP)
        require(combined.size > IV_BYTES) { "Conteúdo cifrado inválido" }
        val iv = combined.copyOfRange(0, IV_BYTES)
        val ciphertext = combined.copyOfRange(IV_BYTES, combined.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    fun databasePassphrase(): String = synchronized(lock) {
        val stored = prefs.getString(KEY_DB_PASSPHRASE, null)
        if (stored != null) {
            runCatching { decrypt(stored) }.getOrElse {
                throw IllegalStateException(
                    "A chave do Keystore foi invalidada. Os dados antigos não podem ser lidos. " +
                        "Use 'Apagar todos os dados' para recomeçar.",
                    it
                )
            }
        } else {
            val random = ByteArray(32).also { SecureRandom().nextBytes(it) }
            val passphrase = Base64.encodeToString(random, Base64.NO_WRAP)
            val committed = prefs.edit()
                .putString(KEY_DB_PASSPHRASE, encrypt(passphrase))
                .commit()
            check(committed) { "Falha ao persistir a passphrase do banco" }
            passphrase
        }
    }

    fun geminiApiKey(): String? = synchronized(lock) {
        prefs.getString(KEY_GEMINI, null)?.let {
            runCatching { decrypt(it) }.getOrNull()
        }
    }

    fun hasGeminiApiKey(): Boolean = geminiApiKey()?.isNotBlank() == true

    fun setGeminiApiKey(key: String?) = synchronized(lock) {
        if (key.isNullOrBlank()) {
            prefs.edit().remove(KEY_GEMINI).commit()
        } else {
            prefs.edit().putString(KEY_GEMINI, encrypt(key.trim())).commit()
        }
    }

    fun clearAll() = synchronized(lock) {
        prefs.edit().clear().commit()
        runCatching {
            val ks = keyStore()
            if (ks.containsAlias(ALIAS)) ks.deleteEntry(ALIAS)
        }
    }

    companion object {
        private const val KEYSTORE = "AndroidKeyStore"
        private const val ALIAS = "equilibrio_master_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_BYTES = 12
        private const val PREFS_NAME = "equilibrio_secrets"
        private const val KEY_DB_PASSPHRASE = "db_passphrase"
        private const val KEY_GEMINI = "gemini_api_key"
    }
}
