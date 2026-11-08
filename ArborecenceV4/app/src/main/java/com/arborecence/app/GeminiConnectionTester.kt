package com.arborecence.app

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GoogleGenerativeAIException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

sealed interface GeminiTestResult {
    data object Success : GeminiTestResult
    data class Failure(val message: String, val code: Int?) : GeminiTestResult
}

@Singleton
class GeminiConnectionTester @Inject constructor() {
    suspend fun test(apiKey: String): GeminiTestResult = withContext(Dispatchers.IO) {
        val key = apiKey.trim()
        if (key.isBlank()) return@withContext GeminiTestResult.Failure("Chave vazia.", null)
        try {
            val model = GenerativeModel(modelName = "gemini-1.5-flash", apiKey = key)
            val response = model.generateContent("Responda apenas: ok")
            val text = response.text?.trim().orEmpty()
            if (text.isNotEmpty()) GeminiTestResult.Success
            else GeminiTestResult.Failure("Resposta vazia do Gemini.", null)
        } catch (e: GoogleGenerativeAIException) {
            val msg = e.message.orEmpty()
            val code = when {
                msg.contains("401") || msg.contains("API_KEY_INVALID", true) || msg.contains("UNAUTHENTICATED", true) -> 401
                msg.contains("403") || msg.contains("PERMISSION_DENIED", true) -> 403
                msg.contains("429") || msg.contains("RESOURCE_EXHAUSTED", true) || msg.contains("quota", true) -> 429
                else -> null
            }
            val human = when (code) {
                401, 403 -> "Chave inválida ou sem permissão ($code). Verifique a chave no Google AI Studio e cole outra."
                429 -> "Cota/limite atingido (429). Aguarde um pouco ou use outra chave."
                else -> "Erro ao conectar: ${msg.ifBlank { "desconhecido" }}"
            }
            GeminiTestResult.Failure(human, code)
        } catch (e: Exception) {
            GeminiTestResult.Failure("Erro ao conectar: ${e.message ?: "desconhecido"}", null)
        }
    }
}
