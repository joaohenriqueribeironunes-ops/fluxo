package com.equilibrio.core.data.gemini

import com.equilibrio.core.domain.gemini.GeminiPayload
import com.equilibrio.core.domain.gemini.GeminiPrompts
import com.equilibrio.core.domain.repository.GeminiClient
import com.equilibrio.core.domain.repository.SettingsRepository
import com.equilibrio.core.model.GeminiAnalysis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Proxy recomendado: o APK não contém chave. O Worker valida limite de uso,
 * chama a API Gemini com responseSchema e devolve apenas o texto.
 *
 * Contrato: POST {base}/analyze
 *   { "model": "...", "system": "...", "prompt": "...", "responseSchema": {...},
 *     "temperature": 0.4 }
 * Resposta: { "text": "<json string>" }
 */
@Singleton
class ProxyGeminiClient @Inject constructor(
    private val settings: SettingsRepository,
    private val http: OkHttpClient,
    @Named("geminiProxyUrl") private val defaultProxyUrl: String,
    @Named("geminiModel") private val modelName: String
) : GeminiClient {

    override suspend fun analyze(payload: GeminiPayload): Result<GeminiAnalysis> =
        withContext(Dispatchers.IO) {
            runCatching {
                val current = settings.current()
                val base = current.geminiProxyUrl.ifBlank { defaultProxyUrl }
                    .trim().trimEnd('/')
                require(base.isNotBlank()) {
                    "Nenhum proxy configurado. Informe a URL do Cloudflare Worker nas configurações."
                }
                val isLocalDev = base.startsWith("http://10.0.2.2")
                require(base.startsWith("https://") || isLocalDev) {
                    "Por segurança, o proxy precisa usar HTTPS."
                }

                val body = buildJsonObject {
                    put("model", modelName)
                    put("system", GeminiPrompts.SYSTEM)
                    put("prompt", GeminiPrompts.userMessage(GeminiJson.payloadToJson(payload)))
                    put("temperature", 0.4)
                    put("responseSchema", GeminiJson.responseSchema())
                }.toString()

                val request = Request.Builder()
                    .url("$base/analyze")
                    .header("Content-Type", "application/json")
                    .header("X-Equilibrio-Client", "android")
                    .post(body.toRequestBody(JSON))
                    .build()

                http.newCall(request).execute().use { response ->
                    val text = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        error("Proxy respondeu ${response.code}: ${text.take(240)}")
                    }
                    val root = runCatching {
                        Json.parseToJsonElement(text).jsonObject
                    }.getOrNull()
                    val raw = root?.get("text")?.jsonPrimitive?.contentOrNull
                        ?: root?.get("analysis")?.toString()
                        ?: text
                    GeminiJson.parse(raw, "proxy:$modelName", payload)
                }
            }
        }

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
