package com.projetopadrao.hyperg96

import android.content.Context
import android.os.Build
import android.os.PerformanceHintManager
import android.os.Process

/**
 * Camada de compatibilidade via ADPF (Android Dynamic Performance Framework).
 * Cria uma sessao de hints e reporta a duracao real de cada frame ao
 * scheduler, que pode assim manter frequencia nos núcleos certos durante a
 * renderizacao. Limites reais: so melhora o processo deste app, e o HAL de
 * hints depende do suporte do fabricante (o HyperOS pode recusar).
 */
object AdpfLayer {

    private var session: PerformanceHintManager.Session? = null

    var lastError: String? = null
        private set

    val active: Boolean get() = session != null

    fun start(context: Context, targetDurationNanos: Long): Boolean {
        stop()
        if (Build.VERSION.SDK_INT < 30) {
            lastError = "Requer Android 11+ (este aparelho roda Android ${Build.VERSION.RELEASE})."
            return false
        }
        return try {
            val phm = context.applicationContext.getSystemService(PerformanceHintManager::class.java)
            session = phm.createHintSession(intArrayOf(Process.myPid()), targetDurationNanos)
            lastError = null
            true
        } catch (e: UnsupportedOperationException) {
            lastError = "O HyperOS não expõe o HAL de hints neste aparelho."
            false
        } catch (e: Throwable) {
            lastError = e.message ?: "Falha ao criar a sessão de hints."
            false
        }
    }

    fun reportFrame(durationNanos: Long) {
        val s = session ?: return
        try {
            s.reportActualWorkDuration(durationNanos)
        } catch (_: Throwable) {
        }
    }

    fun stop() {
        try {
            session?.close()
        } catch (_: Throwable) {
        }
        session = null
    }
}
