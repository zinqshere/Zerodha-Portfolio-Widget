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
import com.zinqshere.zerodhaportfoliowidget.data.PortfolioSnapshot
import com.zinqshere.zerodhaportfoliowidget.data.PortfolioStore
import java.text.NumberFormat
import java.util.Locale

class PortfolioWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val store = PortfolioStore(context)
        render(context, manager, ids, store.cachedSnapshot(), "Cached")

        val pending = goAsync()
        Thread {
            try {
                val snapshot = PortfolioRepository(store).refresh()
                render(context, manager, ids, snapshot, "Updated just now")
            } catch (_: Exception) {
                // Keep the last successful cached snapshot visible.
            } finally {
                pending.finish()
            }
        }.start()
    }

    private fun render(context: Context, manager: AppWidgetManager, ids: IntArray, s: PortfolioSnapshot, updated: String) {
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_portfolio)
            views.setTextViewText(R.id.widget_value, money(s.totalValue))
            views.setTextViewText(R.id.widget_pnl, "P&L ${money(s.totalPnl)}")
            views.setTextViewText(R.id.widget_updated, if (s.updatedAt == 0L) "Open app to connect" else updated)
            val intent = Intent(context, MainActivity::class.java)
            views.setOnClickPendingIntent(id, PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            manager.updateAppWidget(id, views)
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(
            AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, javaClass)), R.id.widget_value
        )
    }

    private fun money(value: Double): String = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(value)
}
