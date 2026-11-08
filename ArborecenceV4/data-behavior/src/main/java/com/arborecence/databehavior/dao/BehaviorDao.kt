package com.arborecence.databehavior.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.arborecence.databehavior.entity.AlexitimiaPista
import com.arborecence.databehavior.entity.Emocao
import com.arborecence.databehavior.entity.FuncaoExecutivaEstrategia
import com.arborecence.databehavior.entity.LeituraNd
import com.arborecence.databehavior.entity.LeituraNt
import com.arborecence.databehavior.entity.PerfilUsuario
import com.arborecence.databehavior.entity.Ponte
import com.arborecence.databehavior.entity.RelacaoTipo
import com.arborecence.databehavior.entity.RespostaRelacionamento
import com.arborecence.databehavior.entity.SensorialPerfil
import com.arborecence.databehavior.entity.Situacao

@Dao
interface BehaviorDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSituacao(e: Situacao): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertLeituraNd(e: LeituraNd)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertLeituraNt(e: LeituraNt)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertPonte(e: Ponte)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertRelacaoTipo(e: RelacaoTipo): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertResposta(e: RespostaRelacionamento): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertRespostas(list: List<RespostaRelacionamento>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertEmocao(e: Emocao): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAlexitimia(e: AlexitimiaPista): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSensorial(e: SensorialPerfil): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertFuncaoExecutiva(e: FuncaoExecutivaEstrategia): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertPerfil(e: PerfilUsuario)

    @Query("SELECT COUNT(*) FROM resposta_relacionamento") suspend fun countRespostas(): Int
    @Query("SELECT COUNT(*) FROM resposta_relacionamento WHERE perfilNdOuNt = :perfil") suspend fun countRespostasPorPerfil(perfil: String): Int
    @Query("SELECT * FROM resposta_relacionamento WHERE relacaoTipo = :relacao AND perfilNdOuNt = :perfil ORDER BY RANDOM() LIMIT :limit")
    suspend fun respostasPorRelacaoPerfil(relacao: String, perfil: String, limit: Int): List<RespostaRelacionamento>
    @Query("SELECT * FROM perfil_usuario WHERE id = 1") suspend fun getPerfil(): PerfilUsuario?
}
