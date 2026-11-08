package com.equilibrio.core.data.gemini

import com.equilibrio.core.data.security.KeystoreSecretStore
import com.equilibrio.core.domain.gemini.GeminiPayload
import com.equilibrio.core.domain.gemini.GeminiPrompts
import com.equilibrio.core.domain.repository.GeminiClient
import com.equilibrio.core.model.GeminiAnalysis
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Caminho "SDK oficial" (Google AI SDK) com chave digitada pelo usuário e
 * guardada no Keystore. Só deve ser usado quando o usuário aceita o risco
 * documentado; o caminho recomendado é o proxy.
 */
@Singleton
class DirectGeminiClient @Inject constructor(
    private val secrets: KeystoreSecretStore,
    @Named("geminiModel") private val modelName: String
) : GeminiClient {

    override suspend fun analyze(payload: GeminiPayload): Result<GeminiAnalysis> =
        withContext(Dispatchers.IO) {
            runCatching {
                val apiKey = secrets.geminiApiKey()?.takeIf { it.isNotBlank() }
                    ?: error("Chave do Gemini não configurada. Use o proxy ou salve uma chave nas configurações.")
                val model = GenerativeModel(
                    modelName = modelName,
                    apiKey = apiKey,
                    generationConfig = generationConfig {
                        temperature = 0.4f
                        responseMimeType = "application/json"
                        maxOutputTokens = 4096
                    },
                    systemInstruction = content { text(GeminiPrompts.SYSTEM) }
                )
                val response = model.generateContent(
                    GeminiPrompts.userMessage(GeminiJson.payloadToJson(payload))
                )
                val text = response.text ?: error("O Gemini retornou uma resposta vazia.")
                GeminiJson.parse(text, "direct:$modelName", payload)
            }
        }
}
