package com.arborecence.aiengine

data class AiResult(
    val texto: String,
    val fonte: String,
    val nivelConfianca: Double,
    val visao: AiEngine.Visao,
    val modo: AiEngine.Modo,
    val meta: String = "", // ex.: relação/subtema/ângulo
)

data class AiRequest(
    val query: String,
    val visao: AiEngine.Visao = AiEngine.Visao.ND,
    val modo: AiEngine.Modo = AiEngine.Modo.PADRAO,
    val relacaoTipo: String? = null,
    val energiaSocial: String? = null,
    val sobrecarga: Boolean = false,
)
