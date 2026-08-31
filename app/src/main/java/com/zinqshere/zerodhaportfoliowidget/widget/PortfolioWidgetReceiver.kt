package com.zinqshere.zerodhaportfoliowidget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.zinqshere.zerodhaportfoliowidget.MainActivity
import com.zinqshere.zerodhaportfoliowidget.R
import com.zinqshere.zerodhaportfoliowidget.data.PortfolioRepository
import com.zinqshere.zerodhaportfoliowidget.data.PortfolioStore
import java.text.NumberFormat
import java.util.Locale

class PortfolioWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val store = PortfolioStore(context)
        render(context, manager, ids, store.cachedSnapshot())
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        refresh(context)
    }

    companion object {
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PortfolioWidgetReceiver::class.java))
            if (ids.isEmpty()) return
            val store = PortfolioStore(context)
            val cached = store.cachedSnapshot()
            render(context, manager, ids, cached)
            Thread {
                runCatching { PortfolioRepository(store).refresh() }
                    .onSuccess { store.saveSnapshot(it); render(context, manager, ids, it) }
            }.start()
        }

        private fun render(context: Context, manager: AppWidgetManager, ids: IntArray, s: com.zinqshere.zerodhaportfoliowidget.data.PortfolioSnapshot) {
            ids.forEach { id ->
                val views = RemoteViews(context.packageName, R.layout.widget_portfolio)
                views.setTextViewText(R.id.widget_value, money(s.totalValue))
                views.setTextViewText(R.id.widget_pnl, "P&L ${money(s.totalPnl)}")
                views.setTextViewText(R.id.widget_updated, if (s.updatedAt == 0L) "Connect Kite in app" else "Updated ${relativeTime(s.updatedAt)}")
                val intent = Intent(context, MainActivity::class.java)
                views.setOnClickPendingIntent(id, PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
                manager.updateAppWidget(id, views)
            }
        }

        private fun money(value: Double): String = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(value)
        private fun relativeTime(timestamp: Long): String {
            val minutes = ((System.currentTimeMillis() - timestamp) / 60000L).coerceAtLeast(0)
            return when { minutes < 1 -> "just now"; minutes < 60 -> "${minutes}m ago"; else -> "${minutes / 60}h ago" }
        }
    }
}
