package com.projetopadrao.hyperg96

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Process
import android.os.SystemClock

/**
 * Liberacao de RAM com a permissao normal KILL_BACKGROUND_PROCESSES.
 * Mata apenas processos que o proprio Android ja marcou como em cache;
 * nao derruba apps em primeiro plano nem servicos ativos.
 */
object TurboEngine {

    data class Result(
        val processed: Int,
        val availBeforeMb: Long,
        val availAfterMb: Long,
        val lowMemory: Boolean
    ) {
        val freedMb: Long get() = availAfterMb - availBeforeMb

        val freedLabel: String
            get() = if (freedMb > 0) "+$freedMb MB" else "liberação em andamento"

        fun report(context: Context): String = buildString {
            append(context.getString(R.string.turbo_candidates, processed))
            append('\n')
            append(context.getString(R.string.turbo_ram, availBeforeMb, availAfterMb, freedLabel))
            if (lowMemory) {
                append('\n')
                append(context.getString(R.string.turbo_low_memory))
            }
        }
    }

    fun run(context: Context): Result {
        val app = context.applicationContext
        val am = app.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        val before = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val beforeMb = before.availMem / (1024 * 1024)

        val pkgs = launcherPackages(app)
        for (pkg in pkgs) {
            if (pkg == app.packageName) continue
            try {
                am.killBackgroundProcesses(pkg)
            } catch (_: Exception) {
            }
        }
        Runtime.getRuntime().gc()
        SystemClock.sleep(300)

        val after = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        return Result(
            processed = pkgs.size,
            availBeforeMb = beforeMb,
            availAfterMb = after.availMem / (1024 * 1024),
            lowMemory = after.lowMemory
        )
    }

    /** Pacotes com tela de lancador (candidatos a limpeza), via <queries> no manifest. */
    fun launcherPackages(context: Context): List<String> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return context.packageManager.queryIntentActivities(intent, 0)
            .mapNotNull { it.activityInfo?.packageName }
            .distinct()
    }
}
