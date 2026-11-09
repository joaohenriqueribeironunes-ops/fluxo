package com.arborecence.dataacademic.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.arborecence.dataacademic.entity.AnalogiaVisual
import com.arborecence.dataacademic.entity.Area
import com.arborecence.dataacademic.entity.Conceito
import com.arborecence.dataacademic.entity.ErroComum
import com.arborecence.dataacademic.entity.Etimologia
import com.arborecence.dataacademic.entity.Exemplo
import com.arborecence.dataacademic.entity.Exercicio
import com.arborecence.dataacademic.entity.Explicacao
import com.arborecence.dataacademic.entity.FraseExplicativa
import com.arborecence.dataacademic.entity.Glossario
import com.arborecence.dataacademic.entity.NormaTecnica
import com.arborecence.dataacademic.entity.Referencia
import com.arborecence.dataacademic.entity.Subarea

@Dao
interface AcademicDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertArea(e: Area): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSubarea(e: Subarea): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertConceito(e: Conceito): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertExplicacao(e: Explicacao): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertFrase(e: FraseExplicativa): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertFrases(list: List<FraseExplicativa>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertExemplo(e: Exemplo): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAnalogia(e: AnalogiaVisual): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertErro(e: ErroComum): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertExercicio(e: Exercicio): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertGlossario(e: Glossario): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertEtimologia(e: Etimologia): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertNorma(e: NormaTecnica): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertReferencia(e: Referencia): Long

    @Query("SELECT COUNT(*) FROM frase_explicativa") suspend fun countFrases(): Int
    @Query("SELECT COUNT(*) FROM etimologia") suspend fun countEtimologias(): Int
    @Query("SELECT COUNT(DISTINCT subareaId) FROM frase_explicativa") suspend fun countSubareasComFrases(): Int
    @Query("SELECT * FROM area ORDER BY nome") suspend fun listAreas(): List<Area>
    @Query("SELECT * FROM subarea WHERE areaId = :areaId ORDER BY nome") suspend fun listSubareas(areaId: Long): List<Subarea>

    @RawQuery(observedEntities = [FraseExplicativa::class])
    suspend fun searchFrasesFtsRaw(query: SupportSQLiteQuery): List<FraseExplicativa>

    suspend fun searchFrasesFts(query: String, limit: Int = 20): List<FraseExplicativa> =
        searchFrasesFtsRaw(
            SimpleSQLiteQuery(
                "SELECT fe.* FROM frase_explicativa fe JOIN frase_explicativa_fts f ON fe.id = f.rowid WHERE frase_explicativa_fts MATCH ? ORDER BY rank LIMIT ?",
                arrayOf(query, limit),
            )
        )

    @Query("SELECT * FROM etimologia WHERE termo LIKE '%' || :q || '%' ORDER BY termo LIMIT :limit")
    suspend fun searchEtimologia(q: String, limit: Int = 30): List<Etimologia>
}
