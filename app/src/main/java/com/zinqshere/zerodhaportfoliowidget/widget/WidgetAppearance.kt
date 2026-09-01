package com.zinqshere.zerodhaportfoliowidget.widget

import android.content.Context
import com.zinqshere.zerodhaportfoliowidget.data.PortfolioSnapshot

object WidgetAppearance {
    const val LIGHT = "light"
    const val DARK = "dark"
    const val PITCH_BLACK = "pitch_black"

    const val COMPACT = "compact"
    const val STANDARD = "standard"
    const val DASHBOARD = "dashboard"

    private const val PREFS = "widget_appearance"
    private const val THEME_PREFIX = "theme_"
    private const val OPACITY_PREFIX = "opacity_"
    private const val LAYOUT_PREFIX = "layout_"
    private const val TODAY_PREFIX = "today_"
    private const val BREAKDOWN_PREFIX = "breakdown_"
    private const val CHART_PREFIX = "chart_"
    private const val HISTORY = "portfolio_history"
    private const val HISTORY_LIMIT = 30

    fun theme(context: Context, appWidgetId: Int): String =
        prefs(context).getString(THEME_PREFIX + appWidgetId, DARK) ?: DARK

    fun opacity(context: Context, appWidgetId: Int): Int =
        prefs(context).getInt(OPACITY_PREFIX + appWidgetId, 100).coerceIn(20, 100)

    fun layout(context: Context, appWidgetId: Int): String {
        val stored = prefs(context).getString(LAYOUT_PREFIX + appWidgetId, STANDARD) ?: STANDARD
        return when (stored) {
            COMPACT, STANDARD, DASHBOARD -> stored
            else -> STANDARD
        }
    }

    fun showToday(context: Context, appWidgetId: Int): Boolean =
        prefs(context).getBoolean(TODAY_PREFIX + appWidgetId, true)

    fun showBreakdown(context: Context, appWidgetId: Int): Boolean =
        prefs(context).getBoolean(BREAKDOWN_PREFIX + appWidgetId, true)

    fun showChart(context: Context, appWidgetId: Int): Boolean =
        prefs(context).getBoolean(CHART_PREFIX + appWidgetId, false)

    fun save(
        context: Context,
        appWidgetId: Int,
        theme: String,
        opacity: Int,
        layout: String = STANDARD,
        showToday: Boolean = true,
        showBreakdown: Boolean = true,
        showChart: Boolean = false
    ) {
        val safeLayout = when (layout) {
            COMPACT, STANDARD, DASHBOARD -> layout
            else -> STANDARD
        }
        prefs(context).edit()
            .putString(THEME_PREFIX + appWidgetId, theme)
            .putInt(OPACITY_PREFIX + appWidgetId, opacity.coerceIn(20, 100))
            .putString(LAYOUT_PREFIX + appWidgetId, safeLayout)
            .putBoolean(TODAY_PREFIX + appWidgetId, showToday)
            .putBoolean(BREAKDOWN_PREFIX + appWidgetId, showBreakdown)
            .putBoolean(CHART_PREFIX + appWidgetId, showChart)
            .apply()
    }

    fun remove(context: Context, appWidgetId: Int) {
        prefs(context).edit()
            .remove(THEME_PREFIX + appWidgetId)
            .remove(OPACITY_PREFIX + appWidgetId)
            .remove(LAYOUT_PREFIX + appWidgetId)
            .remove(TODAY_PREFIX + appWidgetId)
            .remove(BREAKDOWN_PREFIX + appWidgetId)
            .remove(CHART_PREFIX + appWidgetId)
            .apply()
    }

    fun recordSnapshot(context: Context, snapshot: PortfolioSnapshot) {
        val existing = prefs(context).getString(HISTORY, "").orEmpty()
        val now = snapshot.updatedAt.takeIf { it > 0 } ?: System.currentTimeMillis()
        val entry = "$now:${snapshot.totalValue}"
        val values = existing.split('|').filter { it.isNotBlank() }.toMutableList()
        if (values.lastOrNull()?.substringBefore(':') == now.toString()) return
        values += entry
        while (values.size > HISTORY_LIMIT) values.removeAt(0)
        prefs(context).edit().putString(HISTORY, values.joinToString("|")).apply()
    }

    fun history(context: Context): List<Float> =
        prefs(context).getString(HISTORY, "").orEmpty()
            .split('|')
            .mapNotNull { it.substringAfter(':', "").toFloatOrNull() }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
