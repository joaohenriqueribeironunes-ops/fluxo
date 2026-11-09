package com.arborecence.app.util

import android.content.Context
import com.arborecence.databehavior.dao.BehaviorDao
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object BackupUtil {
    suspend fun exportBehavior(context: Context, dao: BehaviorDao): File {
        val dir = File(context.getExternalFilesDir(null), "export").apply { mkdirs() }
        val obj = JSONObject().apply {
            put("schema", "behavior-export-v1")
            put("perfil", dao.getPerfil()?.let { p ->
                JSONObject().apply {
                    put("preferenciasSensoriais", p.preferenciasSensoriais)
                    put("gatilhos", p.gatilhos); put("rotinas", p.rotinas)
                    put("energiaSocialAtual", p.energiaSocialAtual)
                    put("modoRespostaPadrao", p.modoRespostaPadrao)
                }
            } ?: JSONObject())
            put("contagemRespostas", dao.countRespostas())
        }
        val f = File(dir, "behavior-export.json")
        f.writeText(obj.toString(2), Charsets.UTF_8)
        return f
    }
}
