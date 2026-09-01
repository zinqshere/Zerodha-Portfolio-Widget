package com.zinqshere.zerodhaportfoliowidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import android.widget.RemoteViews
import com.zinqshere.zerodhaportfoliowidget.MainActivity
import com.zinqshere.zerodhaportfoliowidget.R
import com.zinqshere.zerodhaportfoliowidget.data.PortfolioRepository
import com.zinqshere.zerodhaportfoliowidget.data.PortfolioSnapshot
import com.zinqshere.zerodhaportfoliowidget.data.PortfolioStore
import java.text.NumberFormat
import java.util.Locale

class PortfolioWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val store = PortfolioStore(context)
        render(context, manager, ids, store.cachedSnapshot())
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) refresh(context, animate = true)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        appWidgetIds.forEach { WidgetAppearance.remove(context, it) }
        super.onDeleted(context, appWidgetIds)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        refresh(context, animate = false)
    }

    companion object {
        private const val ACTION_REFRESH = "com.zinqshere.zerodhaportfoliowidget.widget.REFRESH"

        fun refresh(context: Context, animate: Boolean = false) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PortfolioWidgetReceiver::class.java))
            if (ids.isEmpty()) return
            val store = PortfolioStore(context)
            val cached = store.cachedSnapshot()
            render(context, manager, ids, cached, refreshing = animate)

            Thread {
                runCatching { PortfolioRepository(store).refresh() }
                    .onSuccess {
                        store.saveSnapshot(it)
                        WidgetAppearance.recordSnapshot(context, it)
                        render(context, manager, ids, it, refreshing = false)
                    }
                    .onFailure {
                        render(context, manager, ids, store.cachedSnapshot(), refreshing = false)
                    }
            }.start()
        }

        private fun render(
            context: Context,
            manager: AppWidgetManager,
            ids: IntArray,
            s: PortfolioSnapshot,
            refreshing: Boolean = false
        ) {
            ids.forEach { id ->
                val views = RemoteViews(context.packageName, R.layout.widget_portfolio)
                val theme = WidgetAppearance.theme(context, id)
                val configuredLayout = WidgetAppearance.layout(context, id)
                val options = manager.getAppWidgetOptions(id)
                val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180)
                val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80)
                val layout = when (configuredLayout) {
                    WidgetAppearance.COMPACT -> WidgetAppearance.COMPACT
                    WidgetAppearance.STANDARD -> WidgetAppearance.STANDARD
                    WidgetAppearance.DASHBOARD -> WidgetAppearance.DASHBOARD
                    else -> when {
                        width < 230 || height < 100 -> WidgetAppearance.COMPACT
                        width >= 300 && height >= 170 -> WidgetAppearance.DASHBOARD
                        else -> WidgetAppearance.STANDARD
                    }
                }

                val isLight = theme == WidgetAppearance.LIGHT
                val background = when (theme) {
                    WidgetAppearance.LIGHT -> Color.rgb(247, 247, 249)
                    WidgetAppearance.PITCH_BLACK -> Color.BLACK
                    else -> Color.rgb(37, 35, 41)
                }
                val foreground = if (isLight) Color.rgb(25, 24, 28) else Color.WHITE
                val secondary = if (isLight) Color.rgb(80, 78, 86) else Color.rgb(205, 201, 211)
                val positive = if (isLight) Color.rgb(24, 120, 65) else Color.rgb(145, 235, 164)
                val negative = if (isLight) Color.rgb(180, 48, 48) else Color.rgb(255, 150, 150)

                views.setInt(R.id.widget_content, "setBackgroundColor", background)
                views.setTextColor(R.id.widget_title, foreground)
                views.setTextColor(R.id.widget_value, foreground)
                views.setTextColor(R.id.widget_pnl, if (s.totalPnl >= 0) positive else negative)
                views.setTextColor(R.id.widget_today, if (s.equityDayPnl >= 0) positive else negative)
                views.setTextColor(R.id.widget_updated, secondary)
                views.setTextColor(R.id.widget_refresh, secondary)
                views.setTextColor(R.id.widget_equity, foreground)
                views.setTextColor(R.id.widget_mf, foreground)
                views.setTextColor(R.id.widget_equity_pnl, if (s.equityPnl >= 0) positive else negative)
                views.setTextColor(R.id.widget_mf_pnl, if (s.coinPnl >= 0) positive else negative)
                views.setTextColor(R.id.widget_equity_label, secondary)
                views.setTextColor(R.id.widget_mf_label, secondary)

                views.setTextViewText(R.id.widget_value, money(s.totalValue))
                views.setTextViewText(R.id.widget_pnl, "${signedMoney(s.totalPnl)}  ${signedPercent(percent(s.totalPnl, s.totalInvested))}")
                views.setTextViewText(R.id.widget_today, "Today\n${signedMoney(s.equityDayPnl)}  ${signedPercent(percent(s.equityDayPnl, s.equityInvested))}")
                views.setTextViewText(R.id.widget_equity, compactMoney(s.equityValue))
                views.setTextViewText(R.id.widget_mf, compactMoney(s.coinValue))
                views.setTextViewText(R.id.widget_equity_pnl, signedPercent(percent(s.equityPnl, s.equityInvested)))
                views.setTextViewText(R.id.widget_mf_pnl, signedPercent(percent(s.coinPnl, s.coinInvested)))
                views.setTextViewText(
                    R.id.widget_updated,
                    if (s.updatedAt == 0L) "Connect Zerodha in app" else "Updated ${relativeTime(s.updatedAt)}"
                )

                val compact = layout == WidgetAppearance.COMPACT
                val dashboard = layout == WidgetAppearance.DASHBOARD
                val breakdown = WidgetAppearance.showBreakdown(context, id) && !compact
                val today = WidgetAppearance.showToday(context, id) && !compact
                val chart = WidgetAppearance.showChart(context, id) && dashboard

                views.setViewVisibility(R.id.widget_today, if (today) View.VISIBLE else View.GONE)
                views.setViewVisibility(R.id.widget_breakdown, if (breakdown) View.VISIBLE else View.GONE)
                views.setViewVisibility(R.id.widget_chart, if (chart) View.VISIBLE else View.GONE)
                if (chart) {
                    val bitmap = chartBitmap(WidgetAppearance.history(context), positive, negative)
                    if (bitmap != null) views.setImageViewBitmap(R.id.widget_chart, bitmap)
                    else views.setViewVisibility(R.id.widget_chart, View.GONE)
                }

                // Android launcher widgets do not provide arbitrary view animations,
                // so we use a frame-based visual animation driven by successive
                // RemoteViews updates while the refresh request is in flight.
                views.setTextViewText(R.id.widget_refresh, if (refreshing) "⟳" else "↻")

                val openIntent = Intent(context, MainActivity::class.java)
                views.setOnClickPendingIntent(
                    R.id.widget_value,
                    PendingIntent.getActivity(context, id, openIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                )
                views.setOnClickPendingIntent(
                    R.id.widget_equity,
                    PendingIntent.getActivity(context, id + 10000, openIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                )
                views.setOnClickPendingIntent(
                    R.id.widget_mf,
                    PendingIntent.getActivity(context, id + 20000, openIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                )

                val refreshIntent = Intent(context, PortfolioWidgetReceiver::class.java).setAction(ACTION_REFRESH)
                views.setOnClickPendingIntent(
                    R.id.widget_refresh,
                    PendingIntent.getBroadcast(context, id + 100000, refreshIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                )
                views.setOnClickPendingIntent(
                    R.id.widget_content,
                    PendingIntent.getActivity(context, id + 30000, openIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                )
                manager.updateAppWidget(id, views)
            }
        }

        private fun percent(pnl: Double, invested: Double): Double =
            if (invested == 0.0) 0.0 else pnl / invested * 100.0

        private fun signedPercent(value: Double): String =
            if (value >= 0) "+${format(value)}%" else "${format(value)}%"

        private fun format(value: Double): String = "%.2f".format(Locale.US, value)

        private fun money(value: Double): String =
            NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply { maximumFractionDigits = 0 }.format(value)

        private fun compactMoney(value: Double): String {
            val abs = kotlin.math.abs(value)
            return when {
                abs >= 10_000_000 -> "₹%.2fCr".format(Locale.US, value / 10_000_000)
                abs >= 100_000 -> "₹%.2fL".format(Locale.US, value / 100_000)
                abs >= 1_000 -> "₹%.1fK".format(Locale.US, value / 1_000)
                else -> money(value)
            }
        }

        private fun signedMoney(value: Double): String =
            if (value >= 0) "+${money(value)}" else "-${money(-value)}"

        private fun relativeTime(timestamp: Long): String {
            val minutes = ((System.currentTimeMillis() - timestamp) / 60000L).coerceAtLeast(0)
            return when {
                minutes < 1 -> "just now"
                minutes < 60 -> "${minutes}m ago"
                minutes < 1440 -> "${minutes / 60}h ago"
                else -> "${minutes / 1440}d ago"
            }
        }

        private fun chartBitmap(values: List<Float>, positive: Int, negative: Int): Bitmap? {
            if (values.size < 2) return null
            val width = 600
            val height = 120
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                strokeWidth = 6f
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                color = positive
            }
            val min = values.minOrNull() ?: return null
            val max = values.maxOrNull() ?: return null
            val range = (max - min).takeIf { it > 0.0001f } ?: 1f
            val path = Path()
            values.forEachIndexed { index, value ->
                val x = index * (width - 16f) / (values.size - 1) + 8f
                val y = height - 10f - ((value - min) / range) * (height - 20f)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            paint.color = if (values.last() >= values.first()) positive else negative
            canvas.drawPath(path, paint)
            return bitmap
        }
    }
}
