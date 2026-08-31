package com.zinqshere.zerodhaportfoliowidget.widget

import android.content.Context

object WidgetAppearance {
    const val LIGHT = "light"
    const val DARK = "dark"
    const val PITCH_BLACK = "pitch_black"

    private const val PREFS = "widget_appearance"
    private const val THEME_PREFIX = "theme_"
    private const val OPACITY_PREFIX = "opacity_"

    fun theme(context: Context, appWidgetId: Int): String =
        prefs(context).getString(THEME_PREFIX + appWidgetId, DARK) ?: DARK

    fun opacity(context: Context, appWidgetId: Int): Int =
        prefs(context).getInt(OPACITY_PREFIX + appWidgetId, 100).coerceIn(20, 100)

    fun save(context: Context, appWidgetId: Int, theme: String, opacity: Int) {
        prefs(context).edit()
            .putString(THEME_PREFIX + appWidgetId, theme)
            .putInt(OPACITY_PREFIX + appWidgetId, opacity.coerceIn(20, 100))
            .apply()
    }

    fun remove(context: Context, appWidgetId: Int) {
        prefs(context).edit()
            .remove(THEME_PREFIX + appWidgetId)
            .remove(OPACITY_PREFIX + appWidgetId)
            .apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
