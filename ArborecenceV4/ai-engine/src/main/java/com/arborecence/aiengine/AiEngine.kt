package com.arborecence.aiengine

/** FASE 1+: recuperação FTS5 + embeddings e composição de respostas (ND/NT, Curto/Padrão/Detalhado). */
interface AiEngine {
    enum class Modo { CURTO, PADRAO, DETALHADO }
    enum class Visao { ND, NT }
}
