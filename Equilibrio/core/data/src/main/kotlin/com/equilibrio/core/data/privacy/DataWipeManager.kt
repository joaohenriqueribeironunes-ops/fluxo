package com.equilibrio.core.data.privacy

import android.content.Context
import com.equilibrio.core.data.db.EquilibrioDatabase
import com.equilibrio.core.data.security.KeystoreSecretStore
import com.equilibrio.core.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataWipeManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: EquilibrioDatabase,
    private val settings: SettingsRepository,
    private val secrets: KeystoreSecretStore
) {
    /**
     * Apaga dados de saúde e arquivos exportados. Mantém a passphrase do banco
     * para que o app continue funcional; o conteúdo fica vazio e cifrado.
     * Para remoção física completa, desinstale o app.
     */
    suspend fun wipeAll() = withContext(Dispatchers.IO) {
        database.clearAllTables()
        settings.reset()
        secrets.setGeminiApiKey(null)
        listOf(
            File(context.filesDir, "reports"),
            File(context.cacheDir, "reports"),
            File(context.filesDir, "exports")
        ).forEach { dir -> runCatching { dir.deleteRecursively() } }
    }
}
