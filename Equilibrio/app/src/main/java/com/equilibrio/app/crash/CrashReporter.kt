package com.equilibrio.app.crash

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

/**
 * Registra exceções não tratadas em arquivo local exportável e mostra
 * mensagem amigável. Nunca impede que o log seja gravado; falhas internas
 * são ignoradas.
 */
object CrashReporter {

    private const val TAG = "CrashReporter"
    private const val DIR = "crash"
    private const val FILE = "equilibrio-crash.log"
    private const val MAX_BYTES = 512 * 1024L
    private const val PREFS = "equilibrio_crash"
    private const val KEY_LAST = "last_crash_at"
    @Volatile private var installed = false

    fun install(context: Context) {
        if (installed) return
        installed = true
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { writeCrash(appContext, thread, throwable) }
            runCatching {
                Handler(Looper.getMainLooper()).post {
                    runCatching {
                        Toast.makeText(
                            appContext,
                            "O Equilíbrio encontrou um erro. Um registro foi salvo em Configurações > Exportar log de erros.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            // Dá tempo do toast/log em processos que ainda respondem; o default handler
            // geralmente mata o processo. Delegamos para preservar comportamento do sistema.
            runCatching { previous?.uncaughtException(thread, throwable) }.onFailure {
                Log.e(TAG, "Falha no handler anterior", it)
                Process.killProcess(Process.myPid())
                exitProcess(10)
            }
        }
    }

    fun logDir(context: Context): File = File(context.filesDir, DIR).apply { mkdirs() }

    fun logFile(context: Context): File = File(logDir(context), FILE)

    fun hasLog(context: Context): Boolean = logFile(context).let { it.exists() && it.length() > 0L }

    fun clear(context: Context) {
        runCatching { logFile(context).delete() }
        runCatching {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_LAST).apply()
        }
    }

    fun lastCrashAt(context: Context): Long =
        runCatching {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_LAST, 0L)
        }.getOrDefault(0L)

    fun consumeLastCrashAt(context: Context): Long {
        val at = lastCrashAt(context)
        if (at > 0L) {
            runCatching {
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_LAST).apply()
            }
        }
        return at
    }

    fun exportIntent(context: Context): Intent? {
        val file = logFile(context)
        if (!file.exists() || file.length() <= 0L) return null
        val uri = runCatching {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }.getOrNull() ?: return null
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Log de erros - Equilíbrio")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun writeCrash(context: Context, thread: Thread, throwable: Throwable) {
        val file = logFile(context)
        runCatching {
            if (file.exists() && file.length() > MAX_BYTES) {
                val backup = File(logDir(context), "equilibrio-crash.prev.log")
                runCatching { backup.delete() }
                runCatching { file.renameTo(backup) }
            }
        }
        val now = System.currentTimeMillis()
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.US).format(Date(now))
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val entry = buildString {
            appendLine("===== CRASH $stamp =====")
            appendLine("thread=${thread.name} id=${thread.id}")
            appendLine("app=${context.packageName} sdk=${Build.VERSION.SDK_INT} device=${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine(sw.toString().trimEnd())
            appendLine()
        }
        runCatching { file.appendText(entry) }
        runCatching {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong(KEY_LAST, now).apply()
        }
        Log.e(TAG, "Exceção não tratada registrada em ${file.absolutePath}", throwable)
    }
}
