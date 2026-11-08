package com.equilibrio.core.model

/**
 * Configurações do app. Persistidas em DataStore; nenhum segredo aqui.
 * Segredos (chave do Gemini, passphrase do banco) ficam no Android Keystore.
 */
data class UserSettings(
    val onboardingDone: Boolean = false,
    val collectionEnabled: Boolean = false,
    val micEnabled: Boolean = true,
    val cycleSeconds: Int = 30,
    val windowSeconds: Int = 15,
    val batteryThreshold: Int = 15,
    val baselineStartMillis: Long = 0L,
    val baselineDays: Int = 14,
    val alertThreshold: Int = 70,
    val maxAlertsPerDay: Int = 3,
    val quietPredictions: Boolean = false,
    val alertLeadMinutes: Int = 60,
    val crisisButtonConfirm: Boolean = false,
    val geminiEnabled: Boolean = true,
    val geminiConsent: Boolean = false,
    val geminiUseProxy: Boolean = true,
    val geminiProxyUrl: String = "",
    val geminiBatchMinutes: Int = 90,
    val calendarConsent: Boolean = false,
    val reduceMotion: Boolean = false,
    val reduceTransparency: Boolean = false,
    val highContrast: Boolean = false,
    val fontScale: Float = 1f,
    val darkTheme: Boolean = true,
    val useDynamicColor: Boolean = false,
    val checkInReminders: Boolean = true,
    val reminderStartHour: Int = 9,
    val reminderEndHour: Int = 21,
    val reminderIntervalMinutes: Int = 240,
    val secureScreen: Boolean = false,
    val lastGeminiBatchAt: Long = 0L,
    val lastAlertAt: Long = 0L,
    val alertsToday: Int = 0,
    val alertsDayEpoch: Long = 0L
) {
    val baselineProgress: Float
        get() = if (baselineStartMillis <= 0L) 0f
        else ((System.currentTimeMillis() - baselineStartMillis) / (baselineDays * 86_400_000f))
            .coerceIn(0f, 1f)
}
