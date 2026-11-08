package com.equilibrio.core.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.equilibrio.core.domain.repository.SettingsRepository
import com.equilibrio.core.model.UserSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "equilibrio_settings"
)

class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context
) : SettingsRepository {

    private val dataStore = context.applicationContext.settingsDataStore

    override val settings: Flow<UserSettings> = dataStore.data.map { prefs -> read(prefs) }

    override suspend fun update(transform: (UserSettings) -> UserSettings) {
        dataStore.edit { prefs ->
            val current = read(prefs)
            val updated = transform(current)
            write(prefs, updated)
        }
    }

    override suspend fun current(): UserSettings = settings.first()

    override suspend fun reset() {
        dataStore.edit { it.clear() }
    }

    private fun read(prefs: Preferences): UserSettings = UserSettings(
        onboardingDone = prefs[Keys.ONBOARDING] ?: false,
        collectionEnabled = prefs[Keys.COLLECTION] ?: false,
        micEnabled = prefs[Keys.MIC] ?: true,
        cycleSeconds = prefs[Keys.CYCLE] ?: 30,
        windowSeconds = prefs[Keys.WINDOW] ?: 15,
        batteryThreshold = prefs[Keys.BATTERY] ?: 15,
        baselineStartMillis = prefs[Keys.BASELINE_START] ?: 0L,
        baselineDays = prefs[Keys.BASELINE_DAYS] ?: 14,
        alertThreshold = prefs[Keys.ALERT_THRESHOLD] ?: 70,
        maxAlertsPerDay = prefs[Keys.MAX_ALERTS] ?: 3,
        quietPredictions = prefs[Keys.QUIET] ?: false,
        alertLeadMinutes = prefs[Keys.ALERT_LEAD] ?: 60,
        crisisButtonConfirm = prefs[Keys.CRISIS_CONFIRM] ?: false,
        geminiEnabled = prefs[Keys.GEMINI_ENABLED] ?: true,
        geminiConsent = prefs[Keys.GEMINI_CONSENT] ?: false,
        geminiUseProxy = prefs[Keys.GEMINI_PROXY_MODE] ?: true,
        geminiProxyUrl = prefs[Keys.GEMINI_PROXY_URL] ?: "",
        geminiBatchMinutes = prefs[Keys.GEMINI_BATCH] ?: 90,
        calendarConsent = prefs[Keys.CALENDAR_CONSENT] ?: false,
        reduceMotion = prefs[Keys.REDUCE_MOTION] ?: false,
        reduceTransparency = prefs[Keys.REDUCE_TRANSPARENCY] ?: false,
        highContrast = prefs[Keys.HIGH_CONTRAST] ?: false,
        fontScale = prefs[Keys.FONT_SCALE] ?: 1f,
        darkTheme = prefs[Keys.DARK_THEME] ?: true,
        useDynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: false,
        checkInReminders = prefs[Keys.REMINDERS] ?: true,
        reminderStartHour = prefs[Keys.REMINDER_START] ?: 9,
        reminderEndHour = prefs[Keys.REMINDER_END] ?: 21,
        reminderIntervalMinutes = prefs[Keys.REMINDER_INTERVAL] ?: 240,
        secureScreen = prefs[Keys.SECURE_SCREEN] ?: false,
        lastGeminiBatchAt = prefs[Keys.LAST_BATCH] ?: 0L,
        lastAlertAt = prefs[Keys.LAST_ALERT] ?: 0L,
        alertsToday = prefs[Keys.ALERTS_TODAY] ?: 0,
        alertsDayEpoch = prefs[Keys.ALERTS_DAY] ?: 0L
    )

    private fun write(prefs: androidx.datastore.preferences.core.MutablePreferences, s: UserSettings) {
        prefs[Keys.ONBOARDING] = s.onboardingDone
        prefs[Keys.COLLECTION] = s.collectionEnabled
        prefs[Keys.MIC] = s.micEnabled
        prefs[Keys.CYCLE] = s.cycleSeconds
        prefs[Keys.WINDOW] = s.windowSeconds
        prefs[Keys.BATTERY] = s.batteryThreshold
        prefs[Keys.BASELINE_START] = s.baselineStartMillis
        prefs[Keys.BASELINE_DAYS] = s.baselineDays
        prefs[Keys.ALERT_THRESHOLD] = s.alertThreshold
        prefs[Keys.MAX_ALERTS] = s.maxAlertsPerDay
        prefs[Keys.QUIET] = s.quietPredictions
        prefs[Keys.ALERT_LEAD] = s.alertLeadMinutes
        prefs[Keys.CRISIS_CONFIRM] = s.crisisButtonConfirm
        prefs[Keys.GEMINI_ENABLED] = s.geminiEnabled
        prefs[Keys.GEMINI_CONSENT] = s.geminiConsent
        prefs[Keys.GEMINI_PROXY_MODE] = s.geminiUseProxy
        prefs[Keys.GEMINI_PROXY_URL] = s.geminiProxyUrl
        prefs[Keys.GEMINI_BATCH] = s.geminiBatchMinutes
        prefs[Keys.CALENDAR_CONSENT] = s.calendarConsent
        prefs[Keys.REDUCE_MOTION] = s.reduceMotion
        prefs[Keys.REDUCE_TRANSPARENCY] = s.reduceTransparency
        prefs[Keys.HIGH_CONTRAST] = s.highContrast
        prefs[Keys.FONT_SCALE] = s.fontScale
        prefs[Keys.DARK_THEME] = s.darkTheme
        prefs[Keys.DYNAMIC_COLOR] = s.useDynamicColor
        prefs[Keys.REMINDERS] = s.checkInReminders
        prefs[Keys.REMINDER_START] = s.reminderStartHour
        prefs[Keys.REMINDER_END] = s.reminderEndHour
        prefs[Keys.REMINDER_INTERVAL] = s.reminderIntervalMinutes
        prefs[Keys.SECURE_SCREEN] = s.secureScreen
        prefs[Keys.LAST_BATCH] = s.lastGeminiBatchAt
        prefs[Keys.LAST_ALERT] = s.lastAlertAt
        prefs[Keys.ALERTS_TODAY] = s.alertsToday
        prefs[Keys.ALERTS_DAY] = s.alertsDayEpoch
    }

    private object Keys {
        val ONBOARDING = booleanPreferencesKey("onboarding_done")
        val COLLECTION = booleanPreferencesKey("collection_enabled")
        val MIC = booleanPreferencesKey("mic_enabled")
        val CYCLE = intPreferencesKey("cycle_seconds")
        val WINDOW = intPreferencesKey("window_seconds")
        val BATTERY = intPreferencesKey("battery_threshold")
        val BASELINE_START = longPreferencesKey("baseline_start")
        val BASELINE_DAYS = intPreferencesKey("baseline_days")
        val ALERT_THRESHOLD = intPreferencesKey("alert_threshold")
        val MAX_ALERTS = intPreferencesKey("max_alerts_per_day")
        val QUIET = booleanPreferencesKey("quiet_predictions")
        val ALERT_LEAD = intPreferencesKey("alert_lead_minutes")
        val CRISIS_CONFIRM = booleanPreferencesKey("crisis_confirm")
        val GEMINI_ENABLED = booleanPreferencesKey("gemini_enabled")
        val GEMINI_CONSENT = booleanPreferencesKey("gemini_consent")
        val GEMINI_PROXY_MODE = booleanPreferencesKey("gemini_proxy_mode")
        val GEMINI_PROXY_URL = stringPreferencesKey("gemini_proxy_url")
        val GEMINI_BATCH = intPreferencesKey("gemini_batch_minutes")
        val CALENDAR_CONSENT = booleanPreferencesKey("calendar_consent")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val REDUCE_TRANSPARENCY = booleanPreferencesKey("reduce_transparency")
        val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val REMINDERS = booleanPreferencesKey("checkin_reminders")
        val REMINDER_START = intPreferencesKey("reminder_start_hour")
        val REMINDER_END = intPreferencesKey("reminder_end_hour")
        val REMINDER_INTERVAL = intPreferencesKey("reminder_interval_minutes")
        val SECURE_SCREEN = booleanPreferencesKey("secure_screen")
        val LAST_BATCH = longPreferencesKey("last_gemini_batch_at")
        val LAST_ALERT = longPreferencesKey("last_alert_at")
        val ALERTS_TODAY = intPreferencesKey("alerts_today")
        val ALERTS_DAY = longPreferencesKey("alerts_day_epoch")
    }
}
