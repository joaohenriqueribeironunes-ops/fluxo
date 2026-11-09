package com.lambda.aiengine

import com.lambda.databehavior.dao.BehaviorDao
import com.lambda.dataacademic.dao.AcademicDao
import java.util.ArrayDeque
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class OfflineAiEngine @Inject constructor(
    private val behaviorDao: BehaviorDao,
    private val academicDao: AcademicDao,
) : AiEngine {
    private val recentTexts = ArrayDeque<String>(40)
    private val lock = Any()

    override suspend fun responder(req: AiRequest): AiResult {
        val q = FtsQuery.sanitize(req.query)
        // 1) Tentar comportamento (relações/situações) com FTS
        if (q.isNotBlank()) {
            val hits = runCatching {
                behaviorDao.searchFtsPerfil(q, req.visao.name, 15).ifEmpty { behaviorDao.searchFts(q, 15) }
            }.getOrDefault(emptyList())
            pickBehavior(hits, req)?.let { return it }
        }
        // 2) Fallback: por relação/perfil se especificada ou aleatório informativo
        val rel = req.relacaoTipo ?: "amizade"
        val pool = runCatching { behaviorDao.respostasPorRelacaoPerfil(rel, req.visao.name, 12) }.getOrDefault(emptyList())
        pickBehavior(pool, req)?.let { return it }
        // 3) Acadêmico
        if (q.isNotBlank()) {
            val ah = runCatching { academicDao.searchFrasesFts(q, 12) }.getOrDefault(emptyList())
            val chosen = ah.firstOrNull { !isRecent(it.texto) }
            if (chosen != null) {
                remember(chosen.texto)
                return AiResult(adaptText(chosen.texto, req.modo), chosen.fonte, chosen.nivelConfianca, req.visao, req.modo, "ângulo: ${chosen.angulo}")
            }
        }
        return AiResult(
            texto = adaptText(
                "Não encontrei uma resposta pronta para isso nos bancos locais. Você pode perguntar de outro jeito, com mais detalhes (relação, situação, o que foi dito), ou usar o modo offline após popular os bancos com o seed local.",
                req.modo,
            ),
            fonte = "app",
            nivelConfianca = 0.2,
            visao = req.visao,
            modo = req.modo,
        )
    }

    private fun pickBehavior(hits: List<com.lambda.databehavior.entity.RespostaRelacionamento>, req: AiRequest): AiResult? {
        val ordered = hits.sortedBy { if (it.tamanho.equals(req.modo.name, true)) 0 else 1 }
        val chosen = ordered.firstOrNull { !isRecent(it.texto) } ?: ordered.firstOrNull() ?: return null
        remember(chosen.texto)
        val text = adaptText(chosen.texto, req.modo)
        return AiResult(text, chosen.fonte, chosen.nivelConfianca, req.visao, req.modo, "${chosen.relacaoTipo} / ${chosen.subtema}")
    }

    private fun adaptText(text: String, modo: AiEngine.Modo): String = when (modo) {
        AiEngine.Modo.CURTO -> text.split(Regex("(?<=[.!?])\\s+")).take(2).joinToString(" ").ifBlank { text.take(180) }
        AiEngine.Modo.PADRAO -> text
        AiEngine.Modo.DETALHADO -> text
    }

    private fun isRecent(text: String): Boolean = synchronized(lock) { recentTexts.any { it == text } }
    private fun remember(text: String) = synchronized(lock) {
        if (recentTexts.size >= 40) recentTexts.removeFirst()
        recentTexts.addLast(text)
    }
}

// extend interface for FASE 3
suspend fun AiEngine.responder(req: AiRequest): AiResult = (this as? OfflineAiEngine)?.responder(req)
    ?: AiResult("Motor offline indisponível.", "app", 0.0, req.visao, req.modo)
