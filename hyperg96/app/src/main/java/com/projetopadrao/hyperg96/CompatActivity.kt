package com.projetopadrao.hyperg96

import android.app.AppOpsManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch

class CompatActivity : AppCompatActivity() {

    private lateinit var switchAdpf: MaterialSwitch
    private lateinit var txtAdpfStatus: TextView
    private lateinit var txtUsageStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_compat)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        switchAdpf = findViewById(R.id.switchAdpf)
        txtAdpfStatus = findViewById(R.id.txtAdpfStatus)
        txtUsageStatus = findViewById(R.id.txtUsageStatus)
        setAdpfSwitch(AdpfLayer.active)
        txtAdpfStatus.setText(if (AdpfLayer.active) R.string.adpf_on else R.string.adpf_off)

        findViewById<MaterialButton>(R.id.btnFreeRam).setOnClickListener { freeRam() }
        findViewById<View>(R.id.btnUsage).setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            } catch (_: Exception) {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.parse("package:$packageName")
                })
            }
        }

        loadProfile()
    }

    override fun onResume() {
        super.onResume()
        val granted = hasUsageAccess()
        txtUsageStatus.setText(if (granted) R.string.usage_granted else R.string.usage_missing)
        txtUsageStatus.setTextColor(
            ContextCompat.getColor(this, if (granted) R.color.accent else R.color.warn)
        )
        setAdpfSwitch(AdpfLayer.active)
        txtAdpfStatus.setText(if (AdpfLayer.active) R.string.adpf_on else R.string.adpf_off)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun setAdpfSwitch(checked: Boolean) {
        switchAdpf.setOnCheckedChangeListener(null)
        switchAdpf.isChecked = checked
        switchAdpf.setOnCheckedChangeListener { _, on -> toggleAdpf(on) }
    }

    private fun toggleAdpf(on: Boolean) {
        if (on) {
            // Alvo de frame = tempo de um refresh do painel (90 Hz no Note 11S).
            @Suppress("DEPRECATION")
            val refresh = windowManager.defaultDisplay.refreshRate
            val hz = if (refresh > 0f) refresh else 60f
            val targetNanos = (1_000_000_000.0 / hz).toLong()
            val ok = AdpfLayer.start(this, targetNanos)
            if (ok) {
                txtAdpfStatus.setText(R.string.adpf_on)
            } else {
                txtAdpfStatus.text = AdpfLayer.lastError ?: getString(R.string.adpf_off)
                setAdpfSwitch(false)
            }
        } else {
            AdpfLayer.stop()
            txtAdpfStatus.setText(R.string.adpf_off)
        }
    }

    private fun freeRam() {
        val btn = findViewById<MaterialButton>(R.id.btnFreeRam)
        val out = findViewById<TextView>(R.id.txtFreeRamResult)
        btn.isEnabled = false
        Thread {
            val result = TurboEngine.run(applicationContext)
            runOnUiThread {
                btn.isEnabled = true
                out.text = getString(
                    R.string.turbo_ram,
                    result.availBeforeMb,
                    result.availAfterMb,
                    result.freedLabel
                )
            }
        }.start()
    }

    private fun hasUsageAccess(): Boolean = try {
        val appOps = getSystemService(AppOpsManager::class.java)
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName
        )
        mode == AppOpsManager.MODE_ALLOWED
    } catch (_: Exception) {
        false
    }

    private fun loadProfile() {
        val txt = findViewById<TextView>(R.id.txtProfile)
        Thread {
            val cores = DeviceProfile.cpuCores()
            val coreText = if (cores.isNotEmpty()) {
                cores.joinToString(" + ") { "${it.count}x ${it.name}" }
            } else {
                "núcleos não expostos"
            }
            val bigLittle = if (cores.any { it.name.contains("A76") } &&
                cores.any { it.name.contains("A55") }
            ) {
                "\nbig.LITTLE: o scheduler distribui entre desempenho (A76) e eficiência (A55)."
            } else ""

            val lines = buildString {
                append("SoC: ${DeviceProfile.socName()}\n")
                append("Núcleos: $coreText\n")
                append("GPU: ${DeviceProfile.gpuName()}\n")
                append("RAM: ${DeviceProfile.ramTotalMb(applicationContext)} MB\n")
                append("Sistema: ${DeviceProfile.systemLabel()}")
                append(bigLittle)
            }
            runOnUiThread { txt.text = lines }
        }.start()
    }
}
