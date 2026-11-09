package com.lambda.dataacademic.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "area", indices = [Index(value = ["nome"], unique = true)])
data class Area(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nome: String,
    val descricao: String = "",
    val fonte: String = "seed",
    val nivelConfianca: Double = 0.5,
    val dataGeracao: Long = 0L,
    val revisado: Boolean = false,
)

@Entity(
    tableName = "subarea",
    foreignKeys = [ForeignKey(entity = Area::class, parentColumns = ["id"], childColumns = ["areaId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("areaId"), Index(value = ["areaId", "nome"], unique = true)],
)
data class Subarea(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val areaId: Long,
    val nome: String,
    val descricao: String = "",
    val fonte: String = "seed",
    val nivelConfianca: Double = 0.5,
    val dataGeracao: Long = 0L,
    val revisado: Boolean = false,
)

@Entity(
    tableName = "conceito",
    foreignKeys = [ForeignKey(entity = Subarea::class, parentColumns = ["id"], childColumns = ["subareaId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("subareaId")],
)
data class Conceito(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subareaId: Long,
    val nome: String,
    val definicaoCurta: String = "",
    val tags: String = "",
    val fonte: String = "seed",
    val nivelConfianca: Double = 0.5,
    val dataGeracao: Long = 0L,
    val revisado: Boolean = false,
)

@Entity(
    tableName = "explicacao",
    foreignKeys = [ForeignKey(entity = Conceito::class, parentColumns = ["id"], childColumns = ["conceitoId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("conceitoId"), Index("nivel")],
)
data class Explicacao(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conceitoId: Long,
    val nivel: String, // basico|intermediario|avancado
    val texto: String,
    val fonte: String = "seed",
    val nivelConfianca: Double = 0.5,
    val dataGeracao: Long = 0L,
    val revisado: Boolean = false,
)

@Entity(
    tableName = "frase_explicativa",
    foreignKeys = [ForeignKey(entity = Conceito::class, parentColumns = ["id"], childColumns = ["conceitoId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("conceitoId"), Index("subareaId"), Index("angulo")],
)
data class FraseExplicativa(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conceitoId: Long,
    val subareaId: Long,
    val angulo: String, // definição, analogia, exemplo cotidiano, causa/efeito, erro comum, curiosidade, conexão com outra área, versão visual, versão para hiperfoco, versão curta/longa
    val texto: String,
    val fonte: String = "seed",
    val nivelConfianca: Double = 0.5,
    val dataGeracao: Long = 0L,
    val revisado: Boolean = false,
)

@Entity(
    tableName = "exemplo",
    foreignKeys = [ForeignKey(entity = Conceito::class, parentColumns = ["id"], childColumns = ["conceitoId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("conceitoId")],
)
data class Exemplo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conceitoId: Long,
    val titulo: String,
    val texto: String,
    val contexto: String = "",
    val fonte: String = "seed",
    val nivelConfianca: Double = 0.5,
    val dataGeracao: Long = 0L,
    val revisado: Boolean = false,
)

@Entity(
    tableName = "analogia_visual",
    foreignKeys = [ForeignKey(entity = Conceito::class, parentColumns = ["id"], childColumns = ["conceitoId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("conceitoId")],
)
data class AnalogiaVisual(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conceitoId: Long,
    val descricao: String,
    val elementos: String = "",
    val fonte: String = "seed",
    val nivelConfianca: Double = 0.5,
    val dataGeracao: Long = 0L,
    val revisado: Boolean = false,
)

@Entity(
    tableName = "erro_comum",
    foreignKeys = [ForeignKey(entity = Conceito::class, parentColumns = ["id"], childColumns = ["conceitoId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("conceitoId")],
)
data class ErroComum(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conceitoId: Long,
    val erro: String,
    val correcao: String,
    val porQueAcontece: String = "",
    val fonte: String = "seed",
    val nivelConfianca: Double = 0.5,
    val dataGeracao: Long = 0L,
    val revisado: Boolean = false,
)

@Entity(
    tableName = "exercicio",
    foreignKeys = [ForeignKey(entity = Conceito::class, parentColumns = ["id"], childColumns = ["conceitoId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("conceitoId")],
)
data class Exercicio(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conceitoId: Long,
    val enunciado: String,
    val resposta: String,
    val dica: String = "",
    val nivel: String = "basico",
    val fonte: String = "seed",
    val nivelConfianca: Double = 0.5,
    val dataGeracao: Long = 0L,
    val revisado: Boolean = false,
)

@Entity(tableName = "glossario", indices = [Index(value = ["termo"], unique = true)])
data class Glossario(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val termo: String,
    val definicao: String,
    val areaRelacionada: String = "",
    val fonte: String = "seed",
    val nivelConfianca: Double = 0.5,
    val dataGeracao: Long = 0L,
    val revisado: Boolean = false,
)

@Entity(tableName = "etimologia", indices = [Index("termo")])
data class Etimologia(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val termo: String,
    val raizOriginal: String, // latim, grego, sânscrito, proto-indo-europeu etc.
    val evolucaoFonetica: String,
    val mudancaSemantica: String,
    val fonte: String = "seed",
    val nivelConfianca: Double = 0.5,
    val dataGeracao: Long = 0L,
    val revisado: Boolean = false,
)

@Entity(tableName = "normas_tecnicas", indices = [Index("codigo")])
data class NormaTecnica(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sistema: String, // ABNT, APA, Vancouver, ISO, etc.
    val codigo: String,
    val titulo: String,
    val resumo: String,
    val fonte: String = "seed",
    val nivelConfianca: Double = 0.5,
    val dataGeracao: Long = 0L,
    val revisado: Boolean = false,
)

@Entity(tableName = "referencia")
data class Referencia(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entidadeTipo: String, // conceito, frase, norma, etimologia...
    val entidadeId: Long = 0,
    val citacao: String,
    val urlOuDoi: String = "",
    val fonte: String = "seed",
    val nivelConfianca: Double = 0.5,
    val dataGeracao: Long = 0L,
    val revisado: Boolean = false,
)
