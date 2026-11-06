package com.projetopadrao.hyperg96

import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.FrameMetrics
import android.view.View
import android.view.Window
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

class MainActivity : AppCompatActivity() {

    private lateinit var monitor: SysMonitor
    private lateinit var txtCpu: TextView
    private lateinit var txtRam: TextView
    private lateinit var txtThermal: TextView
    private lateinit var txtBattery: TextView
    private lateinit var txtFrame: TextView
    private lateinit var progressCpu: LinearProgressIndicator
    private lateinit var progressRam: LinearProgressIndicator

    private val lastFrameNanos = AtomicLong(-1L)
    private val mainHandler = Handler(Looper.getMainLooper())

    // Mede o tempo real de frame do app e alimenta a sessao ADPF.
    private val frameListener = Window.OnFrameMetricsAvailableListener { _, metrics, _ ->
        val total = metrics.getMetric(FrameMetrics.TOTAL_DURATION)
        if (total > 0) {
            lastFrameNanos.set(total)
            AdpfLayer.reportFrame(total)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        txtCpu = findViewById(R.id.txtCpu)
        txtRam = findViewById(R.id.txtRam)
        txtThermal = findViewById(R.id.txtThermal)
        txtBattery = findViewById(R.id.txtBattery)
        txtFrame = findViewById(R.id.txtFrame)
        progressCpu = findViewById(R.id.progressCpu)
        progressRam = findViewById(R.id.progressRam)

        monitor = SysMonitor(this) { snapshot -> render(snapshot) }

        findViewById<MaterialButton>(R.id.btnTurbo).setOnClickListener { runTurbo() }
        findViewById<View>(R.id.btnDev).setOnClickListener {
            startActivity(Intent(this, DevOptionsActivity::class.java))
        }
        findViewById<View>(R.id.btnCompat).setOnClickListener {
            startActivity(Intent(this, CompatActivity::class.java))
        }

        loadDeviceProfile()
    }

    override fun onResume() {
        super.onResume()
        monitor.start()
        if (Build.VERSION.SDK_INT >= 24) {
            try {
                window.addOnFrameMetricsAvailableListener(frameListener, mainHandler)
            } catch (_: Exception) {
            }
        }
    }

    override fun onPause() {
        monitor.stop()
        if (Build.VERSION.SDK_INT >= 24) {
            try {
                window.removeOnFrameMetricsAvailableListener(frameListener)
            } catch (_: Exception) {
            }
        }
        super.onPause()
    }

    private fun loadDeviceProfile() {
        val txt = findViewById<TextView>(R.id.txtDevice)
        Thread {
            val cores = DeviceProfile.cpuCores()
            val coreText = if (cores.isNotEmpty()) {
                cores.joinToString(" + ") { "${it.count}x ${it.name}" }
            } else {
                "núcleos não expostos"
            }
            val soc = DeviceProfile.socName()
            val gpu = DeviceProfile.gpuName()
            val ramMb = DeviceProfile.ramTotalMb(applicationContext)
            val device = "${Build.MANUFACTURER} ${Build.MODEL}"
            val system = DeviceProfile.systemLabel()
            val text = "$device\n$soc · $coreText\nGPU: $gpu\nRAM: $ramMb MB · $system"
            runOnUiThread { txt.text = text }
        }.start()
    }

    private fun render(s: SysMonitor.Snapshot) {
        txtCpu.text = when {
            s.cpuPercent != null -> "${s.cpuPercent}%"
            s.load1 != null -> "load ${s.load1}"
            else -> "—"
        }
        s.cpuPercent?.let { progressCpu.setProgressCompat(it, true) }

        val ramPct = if (s.ramTotalMb > 0) {
            ((s.ramUsedMb * 100) / s.ramTotalMb).toInt().coerceIn(0, 100)
        } else 0
        progressRam.setProgressCompat(ramPct, true)
        txtRam.text = "${s.ramUsedMb}/${s.ramTotalMb} MB"
        txtThermal.text = s.thermal
        txtBattery.text = buildString {
            if (s.batteryPercent >= 0) append("${s.batteryPercent}%")
            else append("—")
            if (s.charging) append(" · carregando")
        }

        val frameNs = lastFrameNanos.get()
        txtFrame.text = if (frameNs > 0) {
            val ms = String.format(Locale.getDefault(), "%.1f", frameNs / 1_000_000.0)
            "${getString(R.string.label_frame)}: $ms ms"
        } else {
            "${getString(R.string.label_frame)}: abra um app para medir"
        }
    }

    private fun runTurbo() {
        val btn = findViewById<MaterialButton>(R.id.btnTurbo)
        btn.isEnabled = false
        btn.setText(R.string.turbo_running)
        Thread {
            val result = TurboEngine.run(applicationContext)
            runOnUiThread {
                btn.isEnabled = true
                btn.setText(R.string.btn_turbo)
                AlertDialog.Builder(this)
                    .setTitle(R.string.turbo_title)
                    .setMessage(result.report(this))
                    .setPositiveButton(R.string.ok, null)
                    .show()
            }
        }.start()
    }
}
