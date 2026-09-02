package com.zinqshere.zerodhaportfoliowidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
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
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = renderAll(context, manager, ids)
    override fun onReceive(context: Context, intent: Intent) { super.onReceive(context, intent); if (intent.action == ACTION_REFRESH) refresh(context, true) }
    override fun onDeleted(context: Context, ids: IntArray) { ids.forEach { WidgetAppearance.remove(context, it) }; super.onDeleted(context, ids) }
    override fun onEnabled(context: Context) { super.onEnabled(context); refresh(context) }

    companion object {
        private const val ACTION_REFRESH = "com.zinqshere.zerodhaportfoliowidget.widget.REFRESH"
        private const val FRAME_COUNT = 12

        fun refresh(context: Context, animate: Boolean = false) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PortfolioWidgetReceiver::class.java))
            if (ids.isEmpty()) return
            renderAll(context, manager, ids)
            Thread {
                runCatching { PortfolioRepository(PortfolioStore(context)).refresh() }
                renderAll(context, manager, ids)
            }.start()
            if (animate) Thread { for (i in 1 until FRAME_COUNT) { Thread.sleep(70); renderAll(context, manager, ids, i) } }.start()
        }

        private fun renderAll(context: Context, manager: AppWidgetManager, ids: IntArray, frame: Int = 0) {
            val store = PortfolioStore(context)
            ids.forEach { id ->
                val accountId = WidgetAppearance.accountId(context, id)
                val snapshot = if (accountId == WidgetAppearance.ALL_ACCOUNTS) store.combinedSnapshot() else store.cachedSnapshotForAccount(accountId)
                render(context, manager, id, snapshot, frame, accountId, store)
            }
        }

        private fun render(context: Context, manager: AppWidgetManager, id: Int, s: PortfolioSnapshot, frame: Int, accountId: String, store: PortfolioStore) {
            val views = RemoteViews(context.packageName, R.layout.widget_portfolio)
            val theme = WidgetAppearance.theme(context, id)
            val background = when (theme) { WidgetAppearance.LIGHT -> Color.rgb(247,247,249); WidgetAppearance.PITCH_BLACK -> Color.BLACK; else -> Color.rgb(37,35,41) }
            val foreground = if (theme == WidgetAppearance.LIGHT) Color.rgb(25,24,28) else Color.WHITE
            val secondary = if (theme == WidgetAppearance.LIGHT) Color.rgb(80,78,86) else Color.rgb(205,201,211)
            val positive = if (theme == WidgetAppearance.LIGHT) Color.rgb(24,120,65) else Color.rgb(145,235,164)
            val negative = if (theme == WidgetAppearance.LIGHT) Color.rgb(180,48,48) else Color.rgb(255,150,150)

            views.setInt(R.id.widget_content, "setBackgroundColor", background)
            listOf(R.id.widget_title,R.id.widget_value,R.id.widget_equity,R.id.widget_mf).forEach { views.setTextColor(it, foreground) }
            listOf(R.id.widget_updated,R.id.widget_refresh,R.id.widget_equity_value_label,R.id.widget_equity_returns_label,R.id.widget_mf_value_label,R.id.widget_mf_returns_label,R.id.widget_equity_label,R.id.widget_mf_label).forEach { views.setTextColor(it, secondary) }
            views.setTextColor(R.id.widget_pnl, if (s.totalPnl >= 0) positive else negative)
            views.setTextColor(R.id.widget_today, if (s.equityDayPnl >= 0) positive else negative)
            views.setTextColor(R.id.widget_equity_pnl, if (s.equityPnl >= 0) positive else negative)
            views.setTextColor(R.id.widget_mf_pnl, if (s.coinPnl >= 0) positive else negative)
            views.setTextColor(R.id.widget_equity_return_percent, if (s.equityPnl >= 0) positive else negative)
            views.setTextColor(R.id.widget_mf_return_percent, if (s.coinPnl >= 0) positive else negative)

            views.setTextViewText(R.id.widget_title, if (accountId == WidgetAppearance.ALL_ACCOUNTS) "Zerodha Portfolio • All accounts" else store.account(accountId)?.label ?: "Zerodha Portfolio")
            views.setTextViewText(R.id.widget_value, money(s.totalValue))
            views.setTextViewText(R.id.widget_pnl, "${signedMoney(s.totalPnl)}  ${signedPercent(percent(s.totalPnl, s.totalInvested))}")
            views.setTextViewText(R.id.widget_today, "Today\n${signedMoney(s.equityDayPnl)}  ${signedPercent(percent(s.equityDayPnl, s.equityInvested))}")
            views.setTextViewText(R.id.widget_equity, compactMoney(s.equityValue))
            views.setTextViewText(R.id.widget_mf, compactMoney(s.coinValue))
            views.setTextViewText(R.id.widget_equity_pnl, signedMoney(s.equityPnl))
            views.setTextViewText(R.id.widget_mf_pnl, signedMoney(s.coinPnl))
            views.setTextViewText(R.id.widget_equity_return_percent, signedPercent(percent(s.equityPnl, s.equityInvested)))
            views.setTextViewText(R.id.widget_mf_return_percent, signedPercent(percent(s.coinPnl, s.coinInvested)))
            views.setTextViewText(R.id.widget_updated, if (s.updatedAt == 0L) "Connect Zerodha in app" else "Updated ${relativeTime(s.updatedAt)}")
            views.setTextViewText(R.id.widget_refresh, if (frame == 0) "↻" else arrayOf("↻","↪","↻","↩")[(frame / 3) % 4])

            val layout = WidgetAppearance.layout(context, id)
            val compact = layout == WidgetAppearance.COMPACT
            val dashboard = layout == WidgetAppearance.DASHBOARD
            views.setViewVisibility(R.id.widget_today, if (WidgetAppearance.showToday(context,id) && !compact) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.widget_breakdown, if (WidgetAppearance.showBreakdown(context,id) && !compact) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.widget_chart, if (WidgetAppearance.showChart(context,id) && dashboard) View.VISIBLE else View.GONE)

            val openIntent = Intent(context, MainActivity::class.java)
            views.setOnClickPendingIntent(R.id.widget_content, PendingIntent.getActivity(context,id+30000,openIntent,PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            views.setOnClickPendingIntent(R.id.widget_value, PendingIntent.getActivity(context,id,openIntent,PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            views.setOnClickPendingIntent(R.id.widget_refresh, PendingIntent.getBroadcast(context,id+100000,Intent(context,PortfolioWidgetReceiver::class.java).setAction(ACTION_REFRESH),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            manager.updateAppWidget(id, views)
        }

        private fun percent(pnl: Double, invested: Double) = if (invested == 0.0) 0.0 else pnl / invested * 100.0
        private fun signedPercent(v: Double) = if (v >= 0) "+${"%.2f".format(Locale.US,v)}%" else "${"%.2f".format(Locale.US,v)}%"
        private fun money(v: Double) = NumberFormat.getCurrencyInstance(Locale("en","IN")).apply { maximumFractionDigits=0 }.format(v)
        private fun signedMoney(v: Double) = if (v >= 0) "+${money(v)}" else "-${money(-v)}"
        private fun compactMoney(v: Double): String { val a=kotlin.math.abs(v); return when { a>=10_000_000 -> "₹%.2fCr".format(Locale.US,v/10_000_000); a>=100_000 -> "₹%.2fL".format(Locale.US,v/100_000); a>=1_000 -> "₹%.1fK".format(Locale.US,v/1_000); else -> money(v) } }
        private fun relativeTime(t: Long): String { val m=((System.currentTimeMillis()-t)/60000L).coerceAtLeast(0); return when { m<1 -> "just now"; m<60 -> "${m}m ago"; m<1440 -> "${m/60}h ago"; else -> "${m/1440}d ago" } }
    }
}
