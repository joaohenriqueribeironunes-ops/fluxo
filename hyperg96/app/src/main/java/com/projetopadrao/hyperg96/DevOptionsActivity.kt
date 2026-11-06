package com.projetopadrao.hyperg96

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

/**
 * Guia das Opcoes do Desenvolvedor: o app le os valores atuais (leitura
 * publica), mostra o ideal para o G96 e abre a tela certa. A escrita exige
 * privilegio de sistema, entao quem toca no valor ideal e o usuario.
 */
class DevOptionsActivity : AppCompatActivity() {

    private enum class Kind { SCALE, BOOL, LIMIT }

    private class DevItem(
        val titleRes: Int,
        val idealRes: Int,
        val keys: List<String>,
        val kind: Kind
    )

    private val items = listOf(
        DevItem(
            R.string.dev_item_window_anim, R.string.dev_item_window_anim_ideal,
            listOf("window_animation_scale"), Kind.SCALE
        ),
        DevItem(
            R.string.dev_item_transition_anim, R.string.dev_item_transition_anim_ideal,
            listOf("transition_animation_scale"), Kind.SCALE
        ),
        DevItem(
            R.string.dev_item_animator_anim, R.string.dev_item_animator_anim_ideal,
            listOf("animator_duration_scale"), Kind.SCALE
        ),
        DevItem(
            R.string.dev_item_gpu_render, R.string.dev_item_gpu_render_ideal,
            listOf("force_gpu_rendering"), Kind.BOOL
        ),
        DevItem(
            R.string.dev_item_hw_overlay, R.string.dev_item_hw_overlay_ideal,
            listOf("disable_hw_overlays"), Kind.BOOL
        ),
        DevItem(
            R.string.dev_item_bg_limit, R.string.dev_item_bg_limit_ideal,
            listOf("background_process_limit"), Kind.LIMIT
        )
    )

    private val rendered = mutableListOf<Pair<DevItem, TextView>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dev_options)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        findViewById<MaterialButton>(R.id.btnOpenDev).setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))
            } catch (_: Exception) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }

        val container = findViewById<LinearLayout>(R.id.listContainer)
        val inflater = layoutInflater
        for (item in items) {
            val row = inflater.inflate(R.layout.item_dev_setting, container, false)
            row.findViewById<TextView>(R.id.txtTitle).setText(item.titleRes)
            row.findViewById<TextView>(R.id.txtIdeal).text =
                "${getString(R.string.dev_ideal)} ${getString(item.idealRes)}"
            val status = row.findViewById<TextView>(R.id.txtStatus)
            rendered.add(item to status)
            container.addView(row)
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun refresh() {
        for ((item, statusView) in rendered) {
            val value = item.keys.firstNotNullOfOrNull { readSetting(it) }
            val (text, colorRes) = interpret(item.kind, value)
            statusView.text = text
            statusView.setTextColor(ContextCompat.getColor(this, colorRes))
        }
    }

    private fun readSetting(key: String): String? = try {
        Settings.Global.getString(contentResolver, key)
            ?: Settings.Secure.getString(contentResolver, key)
    } catch (_: Exception) {
        null
    }

    private fun interpret(kind: Kind, value: String?): Pair<String, Int> = when {
        value == null -> getString(R.string.status_unknown) to R.color.warn

        kind == Kind.SCALE -> {
            val scale = value.toFloatOrNull()
            when {
                scale == null -> value to R.color.warn
                scale <= 0.5f -> getString(R.string.status_ok, "${scale}x") to R.color.accent
                else -> getString(R.string.status_change, "${scale}x") to R.color.warn
            }
        }

        kind == Kind.BOOL -> {
            val on = value == "1" || value.equals("true", ignoreCase = true)
            if (on) getString(R.string.status_ok, "Ativado") to R.color.accent
            else getString(R.string.status_change, "Desativado") to R.color.warn
        }

        else -> {
            val atDefault = value.isBlank() || value == "0" || value == "-1"
            val limit = value.toIntOrNull()
            when {
                atDefault -> getString(R.string.status_ok, "Padrão") to R.color.accent
                limit != null && limit <= 4 ->
                    getString(R.string.status_ok, "Máx. $limit") to R.color.accent
                else -> getString(R.string.status_change, "Máx. $value") to R.color.warn
            }
        }
    }
}
