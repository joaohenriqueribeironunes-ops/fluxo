package com.equilibrio.app.ui

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val CHECKIN = "checkin"
    const val TIMELINE = "timeline"
    const val FORECAST = "forecast"
    const val INSIGHTS = "insights"
    const val REPORT = "report"
    const val CRISES = "crises"
    const val SETTINGS = "settings"
    const val ACCURACY = "accuracy"
    const val CALM = "calm"

    const val DEBRIEF_PATTERN = "debrief/{crisisId}"
    const val ARG_CRISIS_ID = "crisisId"

    fun debrief(crisisId: Long): String = "debrief/$crisisId"

    const val EXTRA_ROUTE = "equilibrio.route"

    val mainDestinations = setOf(HOME, TIMELINE, CHECKIN, FORECAST, INSIGHTS)
}
