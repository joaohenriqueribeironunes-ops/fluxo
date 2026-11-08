package com.arborecence.databehavior.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.arborecence.databehavior.dao.BehaviorDao
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

@Database(
    entities = [
        Situacao::class, LeituraNd::class, LeituraNt::class, Ponte::class,
        RelacaoTipo::class, RespostaRelacionamento::class, Emocao::class,
        AlexitimiaPista::class, SensorialPerfil::class, FuncaoExecutivaEstrategia::class,
        PerfilUsuario::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class BehaviorDatabase : RoomDatabase() {
    abstract fun dao(): BehaviorDao

    companion object {
        const val NAME = "behavior.db"

        fun build(context: Context): BehaviorDatabase = Room.databaseBuilder(
            context.applicationContext,
            BehaviorDatabase::class.java,
            NAME,
        )
            .addCallback(FtsCallback())
            .addMigrations(*ALL_MIGRATIONS)
            .build()

        private class FtsCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                createFts(db)
            }
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // Ensure FTS objects exist (idempotent path for dev/migrations).
                runCatching { createFts(db) }
            }
        }

        private fun createFts(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE VIRTUAL TABLE IF NOT EXISTS resposta_relacionamento_fts USING fts5(" +
                    "texto, subtema, tags, content='resposta_relacionamento', content_rowid='id')"
            )
            db.execSQL(
                "CREATE TRIGGER IF NOT EXISTS rr_ai AFTER INSERT ON resposta_relacionamento BEGIN " +
                    "INSERT INTO resposta_relacionamento_fts(rowid, texto, subtema, tags) " +
                    "VALUES (new.id, new.texto, new.subtema, new.tags); END"
            )
            db.execSQL(
                "CREATE TRIGGER IF NOT EXISTS rr_ad AFTER DELETE ON resposta_relacionamento BEGIN " +
                    "INSERT INTO resposta_relacionamento_fts(resposta_relacionamento_fts, rowid, texto, subtema, tags) " +
                    "VALUES('delete', old.id, old.texto, old.subtema, old.tags); END"
            )
            db.execSQL(
                "CREATE TRIGGER IF NOT EXISTS rr_au AFTER UPDATE ON resposta_relacionamento BEGIN " +
                    "INSERT INTO resposta_relacionamento_fts(resposta_relacionamento_fts, rowid, texto, subtema, tags) " +
                    "VALUES('delete', old.id, old.texto, old.subtema, old.tags); " +
                    "INSERT INTO resposta_relacionamento_fts(rowid, texto, subtema, tags) " +
                    "VALUES (new.id, new.texto, new.subtema, new.tags); END"
            )
        }

        // FASE 1: v1 baseline. Adicione migrações aqui nas próximas fases.
        val ALL_MIGRATIONS: Array<Migration> = emptyArray()
    }
}
