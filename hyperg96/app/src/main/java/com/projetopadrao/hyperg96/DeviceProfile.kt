package com.projetopadrao.hyperg96

import android.app.ActivityManager
import android.content.Context
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.GLES20
import android.os.Build
import java.io.File

/**
 * Leitura do hardware do aparelho sem root: /proc/cpuinfo, Build e um contexto
 * EGL minimo para descobrir a GPU real (GL_RENDERER).
 */
object DeviceProfile {

    data class CpuCore(val partHex: String, val name: String, val count: Int)

    /** Nome do SoC a partir do hardware conhecido; fallback para o valor cru. */
    fun socName(): String {
        val hw = (Build.HARDWARE ?: "").lowercase()
        val socModel = if (Build.VERSION.SDK_INT >= 31) (Build.SOC_MODEL ?: "") else ""
        val known = when {
            hw.contains("mt6785") || socModel.contains("6785") -> "MediaTek Helio G96"
            hw.contains("mt6769") -> "MediaTek Helio G80/G85/G88"
            hw.contains("mt6789") -> "MediaTek Helio G99"
            hw.contains("mt6833") -> "MediaTek Dimensity 700"
            hw.contains("mt6877") -> "MediaTek Dimensity 900"
            hw.contains("mt6893") -> "MediaTek Dimensity 1200"
            else -> null
        }
        return known ?: socModel.ifBlank { hw.ifBlank { Build.BOARD ?: "Desconhecido" } }
    }

    /** Contagem de núcleos por parte ARM (ex.: 0xd0b = Cortex-A76). */
    fun cpuCores(): List<CpuCore> {
        val counts = LinkedHashMap<String, Int>()
        try {
            File("/proc/cpuinfo").forEachLine { line ->
                if (line.startsWith("CPU part")) {
                    val hex = line.substringAfter(':').trim().lowercase()
                    if (hex.isNotEmpty()) counts[hex] = (counts[hex] ?: 0) + 1
                }
            }
        } catch (_: Exception) {
            // /proc/cpuinfo restrito: devolve lista vazia e o UI mostra o fallback.
        }
        return counts.map { (hex, n) -> CpuCore(hex, armPartName(hex), n) }
    }

    private fun armPartName(hex: String): String = when (hex) {
        "0xd03" -> "Cortex-A53"
        "0xd05" -> "Cortex-A55"
        "0xd07" -> "Cortex-A57"
        "0xd08" -> "Cortex-A72"
        "0xd09" -> "Cortex-A73"
        "0xd0a" -> "Cortex-A75"
        "0xd0b" -> "Cortex-A76"
        "0xd0d" -> "Cortex-A77"
        "0xd41" -> "Cortex-A78"
        "0xd44" -> "Cortex-X1"
        "0xd46" -> "Cortex-A510"
        "0xd47" -> "Cortex-A710"
        "0xd48" -> "Cortex-X2"
        "0xd49" -> "Cortex-A78C"
        "0xd4d" -> "Cortex-A715"
        "0xd4e" -> "Cortex-X3"
        "0xd80" -> "Cortex-A520"
        "0xd81" -> "Cortex-A720"
        "0xd82" -> "Cortex-X4"
        else -> "Núcleo ARM ($hex)"
    }

    fun ramTotalMb(context: Context): Long {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        return mi.totalMem / (1024 * 1024)
    }

    /** Descobre a GPU criando um contexto EGL 1x1 e lendo GL_RENDERER. */
    fun gpuName(): String {
        var display = EGL14.EGL_NO_DISPLAY
        var context = EGL14.EGL_NO_CONTEXT
        var surface = EGL14.EGL_NO_SURFACE
        return try {
            display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (display == EGL14.EGL_NO_DISPLAY) return "Desconhecida"
            val version = IntArray(2)
            if (!EGL14.eglInitialize(display, version, 0, version, 1)) return "Desconhecida"

            val configAttribs = intArrayOf(
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_NONE
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            if (!EGL14.eglChooseConfig(display, configAttribs, 0, configs, 0, 1, numConfigs, 0) ||
                numConfigs[0] == 0
            ) return "Desconhecida"

            context = EGL14.eglCreateContext(
                display, configs[0], EGL14.EGL_NO_CONTEXT,
                intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE), 0
            )
            if (context == EGL14.EGL_NO_CONTEXT) return "Desconhecida"

            surface = EGL14.eglCreatePbufferSurface(
                display, configs[0],
                intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE), 0
            )
            if (surface == EGL14.EGL_NO_SURFACE) return "Desconhecida"

            if (!EGL14.eglMakeCurrent(display, surface, surface, context)) return "Desconhecida"
            GLES20.glGetString(GLES20.GL_RENDERER) ?: "Desconhecida"
        } catch (_: Throwable) {
            "Desconhecida"
        } finally {
            try {
                if (display != EGL14.EGL_NO_DISPLAY) {
                    EGL14.eglMakeCurrent(
                        display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT
                    )
                    if (surface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(display, surface)
                    if (context != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(display, context)
                    EGL14.eglTerminate(display)
                }
            } catch (_: Throwable) {
            }
        }
    }

    /** "HyperOS 1 · Android 14" via SystemProperties (reflection com fallback). */
    fun systemLabel(): String {
        val os = sysProp("ro.mi.os.version.name") ?: sysProp("ro.miui.ui.version.name")
        val hyper = when {
            os == null -> null
            os.startsWith("OS1") -> "HyperOS 1"
            os.startsWith("OS2") -> "HyperOS 2"
            os.startsWith("OS") -> "HyperOS"
            else -> "MIUI $os"
        }
        val android = "Android ${Build.VERSION.RELEASE}"
        return (listOfNotNull(hyper) + android).joinToString(" · ")
    }

    fun sysProp(key: String): String? = try {
        val clazz = Class.forName("android.os.SystemProperties")
        val getter = clazz.getMethod("get", String::class.java)
        (getter.invoke(null, key) as? String)?.takeIf { it.isNotBlank() }
    } catch (_: Exception) {
        null
    }
}
