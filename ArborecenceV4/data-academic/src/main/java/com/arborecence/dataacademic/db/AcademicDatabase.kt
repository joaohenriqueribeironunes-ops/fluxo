package com.arborecence.dataacademic.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.arborecence.dataacademic.dao.AcademicDao
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

@Database(
    entities = [
        Area::class, Subarea::class, Conceito::class, Explicacao::class,
        FraseExplicativa::class, Exemplo::class, AnalogiaVisual::class,
        ErroComum::class, Exercicio::class, Glossario::class, Etimologia::class,
        NormaTecnica::class, Referencia::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AcademicDatabase : RoomDatabase() {
    abstract fun dao(): AcademicDao

    companion object {
        const val NAME = "academic.db"

        fun build(context: Context): AcademicDatabase = Room.databaseBuilder(
            context.applicationContext,
            AcademicDatabase::class.java,
            NAME,
        )
            .addCallback(FtsCallback())
            .addMigrations(*ALL_MIGRATIONS)
            .build()

        private class FtsCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) { super.onCreate(db); createFts(db) }
            override fun onOpen(db: SupportSQLiteDatabase) { super.onOpen(db); runCatching { createFts(db) } }
        }

        private fun createFts(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE VIRTUAL TABLE IF NOT EXISTS frase_explicativa_fts USING fts5(" +
                    "texto, angulo, content='frase_explicativa', content_rowid='id')"
            )
            db.execSQL(
                "CREATE TRIGGER IF NOT EXISTS fe_ai AFTER INSERT ON frase_explicativa BEGIN " +
                    "INSERT INTO frase_explicativa_fts(rowid, texto, angulo) VALUES (new.id, new.texto, new.angulo); END"
            )
            db.execSQL(
                "CREATE TRIGGER IF NOT EXISTS fe_ad AFTER DELETE ON frase_explicativa BEGIN " +
                    "INSERT INTO frase_explicativa_fts(frase_explicativa_fts, rowid, texto, angulo) " +
                    "VALUES('delete', old.id, old.texto, old.angulo); END"
            )
            db.execSQL(
                "CREATE TRIGGER IF NOT EXISTS fe_au AFTER UPDATE ON frase_explicativa BEGIN " +
                    "INSERT INTO frase_explicativa_fts(frase_explicativa_fts, rowid, texto, angulo) " +
                    "VALUES('delete', old.id, old.texto, old.angulo); " +
                    "INSERT INTO frase_explicativa_fts(rowid, texto, angulo) VALUES (new.id, new.texto, new.angulo); END"
            )
            db.execSQL(
                "CREATE VIRTUAL TABLE IF NOT EXISTS conceito_fts USING fts5(" +
                    "nome, definicaoCurta, tags, content='conceito', content_rowid='id')"
            )
        }

        val ALL_MIGRATIONS: Array<Migration> = emptyArray()
    }
}
