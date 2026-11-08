package com.arborecence.databehavior.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "situacao")
data class Situacao(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoria: String,
    val titulo: String,
    val descricao: String,
    val contextoAmbiente: String,
    val fonte: String = "seed",
    val nivelConfianca: Double = 0.5,
    val dataGeracao: Long = 0L,
    val revisado: Boolean = false,
)

@Entity(
    tableName = "leitura_nd",
    foreignKeys = [ForeignKey(entity = Situacao::class, parentColumns = ["id"], childColumns = ["situacaoId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("situacaoId")],
)
data class LeituraNd(
    @PrimaryKey val situacaoId: Long,
    val percepcaoSensorial: String,
    val interpretacaoLiteral: String,
    val emocaoProvavel: String,
    val cargaCognitiva: String,
    val sinalDeSobrecarga: String,
    val estrategiaDeRegulacao: String,
    val fonte: String = "seed",
    val nivelConfianca: Double = 0.5,
    val dataGeracao: Long = 0L,
    val revisado: Boolean = false,
)

@Entity(
    tableName = "leitura_nt",
    foreignKeys = [ForeignKey(entity = Situacao::class, parentColumns = ["id"], childColumns = ["situacaoId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("situacaoId")],
)
data class LeituraNt(
    @PrimaryKey val situacaoId: Long,
    val intencaoProvavel: String,
    val expectativaSocialImplicita: String,
    val comoInterpretaOComportamentoNd: String,
    val malEntendidoComum: String,
    val fonte: String = "seed",
    val nivelConfianca: Double = 0.5,
    val dataGeracao: Long = 0L,
    val revisado: Boolean = false,
)

@Entity(
    tableName = "ponte",
    foreignKeys = [ForeignKey(entity = Situacao::class, parentColumns = ["id"], childColumns = ["situacaoId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("situacaoId")],
)
data class Ponte(
    @PrimaryKey val situacaoId: Long,
    val oQueDizerScripts: String,
    val oQueFazer: String,
    val oQueEvitar: String,
    val comoExplicarMeuFuncionamento: String,
    val fonte: String = "seed",
    val nivelConfianca: Double = 0.5,
    val dataGeracao: Long = 0L,
    val revisado: Boolean = false,
)

@Entity(tableName = "relacao_tipo", indices = [Index(value = ["nome"], unique = true)])
data class RelacaoTipo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String, // amizade, amor, família, trabalho, colegas, vizinhos, desconhecidos, atendimento
    val descricao: String = "",
    val fonte: String = "seed",
    val nivelConfianca: Double = 1.0,
    val dataGeracao: Long = 0L,
    val revisado: Boolean = false,
)

@Entity(
    tableName = "resposta_relacionamento",
    indices = [Index("relacaoTipo"), Index("subtema"), Index("perfilNdOuNt")],
)
data class RespostaRelacionamento(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val relacaoTipo: String,
    val subtema: String,
    val perfilNdOuNt: String, // ND ou NT
    val texto: String,
    val tom: String,
    val tamanho: String, // curto|padrao|detalhado
    val gatilhoDeUso: String,
    val tags: String,
    val fonte: String = "seed",
    val nivelConfianca: Double = 0.5,
    val dataGeracao: Long = 0L,
    val revisado: Boolean = false,
)

@Entity(tableName = "emocao")
data class Emocao(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val descricao: String,
    val sinaisCorporais: String,
    val necessidadesComuns: String,
    val fonte: String = "seed",
    val nivelConfianca: Double = 0.5,
    val dataGeracao: Long = 0L,
    val revisado: Boolean = false,
)

@Entity(tableName = "alexitimia_pistas")
data class AlexitimiaPista(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pista: String,
    val possivelEmocao: String,
    val perguntaDeApoio: String,
    val fonte: String = "seed",
    val nivelConfianca: Double = 0.5,
    val dataGeracao: Long = 0L,
    val revisado: Boolean = false,
)

@Entity(tableName = "sensorial_perfil")
data class SensorialPerfil(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val modalidade: String, // som, luz, tato, cheiro, paladar, propriocepção, vestibular
    val padrao: String, // hipersensível/hipossensível/misto
    val descricao: String,
    val estrategia: String,
    val fonte: String = "seed",
    val nivelConfianca: Double = 0.5,
    val dataGeracao: Long = 0L,
    val revisado: Boolean = false,
)

@Entity(tableName = "funcao_executiva_estrategia")
data class FuncaoExecutivaEstrategia(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dominio: String, // iniciar tarefa, transição, tempo, memória de trabalho, procrastinação, hiperfoco, paralisia de decisão
    val titulo: String,
    val quandoUsar: String,
    val passos: String,
    val adaptacaoNd: String,
    val fonte: String = "seed",
    val nivelConfianca: Double = 0.5,
    val dataGeracao: Long = 0L,
    val revisado: Boolean = false,
)

/** Perfil do usuário (local). NÃO armazenar medicamentos nem dados clínicos. */
@Entity(tableName = "perfil_usuario")
data class PerfilUsuario(
    @PrimaryKey val id: Int = 1,
    val preferenciasSensoriais: String = "",
    val gatilhos: String = "",
    val rotinas: String = "",
    val energiaSocialAtual: String = "",
    val preferenciasTom: String = "padrao",
    val modoRespostaPadrao: String = "padrao", // curto|padrao|detalhado
    val dataAtualizacao: Long = 0L,
)
