package com.equilibrio.core.data.gemini

import com.equilibrio.core.domain.gemini.GeminiPayload
import com.equilibrio.core.domain.repository.GeminiClient
import com.equilibrio.core.domain.repository.SettingsRepository
import com.equilibrio.core.model.GeminiAnalysis
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Escolhe proxy (recomendado) ou SDK direto. Exige consentimento explícito
 * registrado nas configurações antes de qualquer envio.
 */
@Singleton
class GeminiClientRouter @Inject constructor(
    private val settings: SettingsRepository,
    private val proxy: ProxyGeminiClient,
    private val direct: DirectGeminiClient
) : GeminiClient {

    override suspend fun analyze(payload: GeminiPayload): Result<GeminiAnalysis> {
        val current = settings.current()
        if (!current.geminiEnabled) {
            return Result.failure(IllegalStateException("Análise com Gemini desativada."))
        }
        if (!current.geminiConsent) {
            return Result.failure(
                IllegalStateException("É necessário autorizar o envio de resumos agregados ao Gemini.")
            )
        }
        return if (current.geminiUseProxy) proxy.analyze(payload) else direct.analyze(payload)
    }
}
