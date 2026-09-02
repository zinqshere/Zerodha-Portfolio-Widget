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
    const val ALL_ACCOUNTS = "__all__"

    private const val PREFS = "widget_appearance"
    private const val THEME_PREFIX = "theme_"
    private const val OPACITY_PREFIX = "opacity_"
    private const val LAYOUT_PREFIX = "layout_"
    private const val TODAY_PREFIX = "today_"
    private const val BREAKDOWN_PREFIX = "breakdown_"
    private const val CHART_PREFIX = "chart_"
    private const val ACCOUNT_PREFIX = "account_"
    private const val HISTORY = "portfolio_history"
    private const val HISTORY_LIMIT = 30

    fun theme(context: Context, id: Int) = prefs(context).getString(THEME_PREFIX + id, DARK) ?: DARK
    fun opacity(context: Context, id: Int) = prefs(context).getInt(OPACITY_PREFIX + id, 100).coerceIn(20, 100)
    fun layout(context: Context, id: Int): String = prefs(context).getString(LAYOUT_PREFIX + id, STANDARD) ?: STANDARD
    fun showToday(context: Context, id: Int) = prefs(context).getBoolean(TODAY_PREFIX + id, true)
    fun showBreakdown(context: Context, id: Int) = prefs(context).getBoolean(BREAKDOWN_PREFIX + id, true)
    fun showChart(context: Context, id: Int) = prefs(context).getBoolean(CHART_PREFIX + id, false)
    fun accountId(context: Context, id: Int): String = prefs(context).getString(ACCOUNT_PREFIX + id, ALL_ACCOUNTS) ?: ALL_ACCOUNTS

    fun save(context: Context, id: Int, theme: String, opacity: Int, layout: String = STANDARD, showToday: Boolean = true, showBreakdown: Boolean = true, showChart: Boolean = false, accountId: String = ALL_ACCOUNTS) {
        prefs(context).edit()
            .putString(THEME_PREFIX + id, theme)
            .putInt(OPACITY_PREFIX + id, opacity.coerceIn(20, 100))
            .putString(LAYOUT_PREFIX + id, layout)
            .putBoolean(TODAY_PREFIX + id, showToday)
            .putBoolean(BREAKDOWN_PREFIX + id, showBreakdown)
            .putBoolean(CHART_PREFIX + id, showChart)
            .putString(ACCOUNT_PREFIX + id, accountId)
            .apply()
    }

    fun remove(context: Context, id: Int) = prefs(context).edit()
        .remove(THEME_PREFIX + id).remove(OPACITY_PREFIX + id).remove(LAYOUT_PREFIX + id)
        .remove(TODAY_PREFIX + id).remove(BREAKDOWN_PREFIX + id).remove(CHART_PREFIX + id).remove(ACCOUNT_PREFIX + id).apply()

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

    fun history(context: Context): List<Float> = prefs(context).getString(HISTORY, "").orEmpty().split('|').mapNotNull { it.substringAfter(':', "").toFloatOrNull() }
    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
