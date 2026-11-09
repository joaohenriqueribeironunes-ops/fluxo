package com.arborecence.app.util

import android.content.Context
import com.arborecence.databehavior.dao.BehaviorDao
import com.arborecence.databehavior.entity.Compromisso
import com.arborecence.databehavior.entity.DestaqueCognitivo
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object BackupUtil {
    suspend fun exportAll(context: Context, dao: BehaviorDao): File {
        val dir = File(context.getExternalFilesDir(null), "export").apply { mkdirs() }
        val obj = JSONObject().apply {
            put("schema", "arborecence-export-v2")
            put("exportadoEm", System.currentTimeMillis())
            put("contagemRespostas", dao.countRespostas())
            put("perfil", dao.getPerfil()?.let { p ->
                JSONObject().apply {
                    put("preferenciasSensoriais", p.preferenciasSensoriais)
                    put("gatilhos", p.gatilhos); put("rotinas", p.rotinas)
                    put("energiaSocialAtual", p.energiaSocialAtual)
                    put("modoRespostaPadrao", p.modoRespostaPadrao)
                }
            } ?: JSONObject())
            put("compromissos", JSONArray().apply {
                dao.listCompromissos().forEach { c ->
                    put(JSONObject().apply {
                        put("titulo", c.titulo); put("descricao", c.descricao)
                        put("dataHoraMillis", c.dataHoraMillis ?: JSONObject.NULL)
                        put("local", c.local); put("tipo", c.tipo); put("concluido", c.concluido)
                    })
                }
            })
            put("destaques", JSONArray().apply {
                dao.listDestaques(500).forEach { d ->
                    put(JSONObject().apply {
                        put("texto", d.texto); put("cor", d.cor); put("categoria", d.categoria)
                        put("origem", d.origem); put("nota", d.nota)
                    })
                }
            })
        }
        val f = File(dir, "arborecence-backup.json")
        f.writeText(obj.toString(2), Charsets.UTF_8)
        return f
    }

    suspend fun importAll(context: Context, dao: BehaviorDao, file: File): String {
        val obj = JSONObject(file.readText(Charsets.UTF_8))
        var cCount = 0; var dCount = 0
        val carr = obj.optJSONArray("compromissos") ?: JSONArray()
        for (i in 0 until carr.length()) {
            val c = carr.getJSONObject(i)
            dao.upsertCompromisso(Compromisso(
                titulo = c.optString("titulo"), descricao = c.optString("descricao"),
                dataHoraMillis = if (c.isNull("dataHoraMillis")) null else c.optLong("dataHoraMillis"),
                local = c.optString("local"), tipo = c.optString("tipo", "geral"),
                concluido = c.optBoolean("concluido", false), dataGeracao = System.currentTimeMillis(),
            )); cCount++
        }
        val darr = obj.optJSONArray("destaques") ?: JSONArray()
        for (i in 0 until darr.length()) {
            val d = darr.getJSONObject(i)
            dao.insertDestaque(DestaqueCognitivo(
                texto = d.optString("texto"), cor = d.optString("cor", "lavanda"),
                categoria = d.optString("categoria", "conceito"), origem = d.optString("origem", "backup"),
                nota = d.optString("nota"), dataGeracao = System.currentTimeMillis(),
            )); dCount++
        }
        return "Importado: $cCount compromissos, $dCount destaques."
    }
}
