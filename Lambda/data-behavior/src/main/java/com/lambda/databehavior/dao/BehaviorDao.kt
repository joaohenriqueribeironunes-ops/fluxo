package com.lambda.databehavior.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.lambda.databehavior.entity.AlexitimiaPista
import com.lambda.databehavior.entity.Compromisso
import com.lambda.databehavior.entity.DestaqueCognitivo
import com.lambda.databehavior.entity.Emocao
import com.lambda.databehavior.entity.FuncaoExecutivaEstrategia
import com.lambda.databehavior.entity.LeituraNd
import com.lambda.databehavior.entity.LeituraNt
import com.lambda.databehavior.entity.PerfilUsuario
import com.lambda.databehavior.entity.Ponte
import com.lambda.databehavior.entity.RelacaoTipo
import com.lambda.databehavior.entity.RespostaRelacionamento
import com.lambda.databehavior.entity.SensorialPerfil
import com.lambda.databehavior.entity.Situacao
import kotlinx.coroutines.flow.Flow

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
    @Query("SELECT COUNT(*) FROM resposta_relacionamento") fun countRespostasFlow(): Flow<Int>
    @Query("SELECT COUNT(*) FROM resposta_relacionamento WHERE perfilNdOuNt = :perfil") suspend fun countRespostasPorPerfil(perfil: String): Int
    @Query("SELECT * FROM resposta_relacionamento WHERE relacaoTipo = :relacao AND perfilNdOuNt = :perfil ORDER BY RANDOM() LIMIT :limit")
    suspend fun respostasPorRelacaoPerfil(relacao: String, perfil: String, limit: Int): List<RespostaRelacionamento>

    @RawQuery(observedEntities = [RespostaRelacionamento::class])
    suspend fun searchFtsRaw(query: SupportSQLiteQuery): List<RespostaRelacionamento>

    suspend fun searchFts(query: String, limit: Int = 20): List<RespostaRelacionamento> =
        searchFtsRaw(SimpleSQLiteQuery(
            "SELECT r.* FROM resposta_relacionamento r JOIN resposta_relacionamento_fts f ON r.id = f.rowid WHERE resposta_relacionamento_fts MATCH ? ORDER BY rank LIMIT ?",
            arrayOf(query, limit),
        ))

    suspend fun searchFtsPerfil(query: String, perfil: String, limit: Int = 20): List<RespostaRelacionamento> =
        searchFtsRaw(SimpleSQLiteQuery(
            "SELECT r.* FROM resposta_relacionamento r JOIN resposta_relacionamento_fts f ON r.id = f.rowid WHERE resposta_relacionamento_fts MATCH ? AND r.perfilNdOuNt = ? ORDER BY rank LIMIT ?",
            arrayOf(query, perfil, limit),
        ))

    @Query("SELECT * FROM situacao WHERE categoria = :categoria ORDER BY id ASC")
    suspend fun listSituacoesPorCategoria(categoria: String): List<Situacao>
    @Query("SELECT * FROM situacao ORDER BY id ASC LIMIT :limit")
    suspend fun listSituacoes(limit: Int = 100): List<Situacao>
    @Query("SELECT COUNT(*) FROM situacao") fun countSituacoesFlow(): Flow<Int>
    @Query("SELECT * FROM situacao WHERE id = :id") suspend fun getSituacao(id: Long): Situacao?
    @Query("SELECT * FROM leitura_nd WHERE situacaoId = :id") suspend fun getLeituraNd(id: Long): LeituraNd?
    @Query("SELECT * FROM leitura_nt WHERE situacaoId = :id") suspend fun getLeituraNt(id: Long): LeituraNt?
    @Query("SELECT * FROM ponte WHERE situacaoId = :id") suspend fun getPonte(id: Long): Ponte?

    @Query("SELECT DISTINCT relacaoTipo FROM resposta_relacionamento ORDER BY relacaoTipo")
    suspend fun listRelacoes(): List<String>

    @Query("SELECT DISTINCT subtema FROM resposta_relacionamento WHERE relacaoTipo = :relacao ORDER BY subtema")
    suspend fun listSubtemas(relacao: String): List<String>

    @Query("SELECT * FROM perfil_usuario WHERE id = 1") suspend fun getPerfil(): PerfilUsuario?

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertCompromisso(e: Compromisso): Long
    @Query("SELECT * FROM compromisso ORDER BY concluido ASC, COALESCE(dataHoraMillis, 9999999999999) ASC, id DESC")
    suspend fun listCompromissos(): List<Compromisso>
    @Query("SELECT * FROM compromisso ORDER BY concluido ASC, COALESCE(dataHoraMillis, 9999999999999) ASC, id DESC")
    fun compromissosFlow(): Flow<List<Compromisso>>
    @Query("SELECT COUNT(*) FROM compromisso") fun countCompromissosFlow(): Flow<Int>
    @Query("UPDATE compromisso SET concluido = :v WHERE id = :id") suspend fun setCompromissoConcluido(id: Long, v: Boolean)
    @Query("DELETE FROM compromisso WHERE id = :id") suspend fun deleteCompromisso(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertDestaque(e: DestaqueCognitivo): Long
    @Query("SELECT * FROM destaque_cognitivo ORDER BY id DESC LIMIT :limit") suspend fun listDestaques(limit: Int = 200): List<DestaqueCognitivo>
    @Query("SELECT COUNT(*) FROM destaque_cognitivo") fun countDestaquesFlow(): Flow<Int>
    @Query("DELETE FROM destaque_cognitivo WHERE id = :id") suspend fun deleteDestaque(id: Long)
}
