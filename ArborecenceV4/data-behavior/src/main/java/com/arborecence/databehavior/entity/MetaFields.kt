package com.arborecence.databehavior.entity

/** Convenção de metadados para conteúdo gerado/importado (ambos os bancos). */
interface ContentMeta {
    val fonte: String
    val nivelConfianca: Double
    val dataGeracao: Long
    val revisado: Boolean
}
