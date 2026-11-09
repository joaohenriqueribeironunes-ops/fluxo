package com.lambda.aiengine

/** Recuperação FTS5 + composição offline (ND/NT, Curto/Padrão/Detalhado), sem repetição. */
interface AiEngine {
    enum class Modo { CURTO, PADRAO, DETALHADO }
    enum class Visao { ND, NT }
    suspend fun responder(req: AiRequest): AiResult
}
