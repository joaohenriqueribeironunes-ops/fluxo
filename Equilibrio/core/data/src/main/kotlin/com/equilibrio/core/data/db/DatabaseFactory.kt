package com.equilibrio.core.data.db

import android.content.Context
import androidx.room.Room
import com.equilibrio.core.data.security.KeystoreSecretStore
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/**
 * Cria o Room sobre SQLCipher. A passphrase é aleatória, gerada no aparelho,
 * cifrada por uma chave AES/GCM do Android Keystore e nunca sai do dispositivo.
 */
class DatabaseFactory(
    private val context: Context,
    private val secrets: KeystoreSecretStore
) {
    @Volatile
    private var instance: EquilibrioDatabase? = null

    fun create(): EquilibrioDatabase {
        instance?.let { return it }
        return synchronized(this) {
            instance ?: build().also { instance = it }
        }
    }

    private fun build(): EquilibrioDatabase {
        runCatching { System.loadLibrary("sqlcipher") }
        val passphrase = secrets.databasePassphrase().toByteArray(Charsets.UTF_8)
        val factory = SupportOpenHelperFactory(passphrase)
        return Room.databaseBuilder(
            context.applicationContext,
            EquilibrioDatabase::class.java,
            EquilibrioDatabase.NAME
        )
            .openHelperFactory(factory)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }
}
