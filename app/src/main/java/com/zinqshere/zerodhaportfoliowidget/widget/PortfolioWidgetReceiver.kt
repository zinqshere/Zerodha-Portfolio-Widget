package com.zinqshere.zerodhaportfoliowidget.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.zinqshere.zerodhaportfoliowidget.R
import com.zinqshere.zerodhaportfoliowidget.data.PortfolioRepository
import com.zinqshere.zerodhaportfoliowidget.data.PortfolioStore
import java.text.NumberFormat
import java.util.Locale

class PortfolioWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val repo = PortfolioRepository(PortfolioStore(context))
        val snapshot = runCatching { repo.refresh() }.getOrElse { null }
        ids.forEach { id ->
            val views = RemoteViews(context.packageName, R.layout.widget_portfolio)
            if (snapshot != null) {
                views.setTextViewText(R.id.widget_value, money(snapshot.totalValue))
                views.setTextViewText(R.id.widget_pnl, "P&L ${money(snapshot.totalPnl)}")
                views.setTextViewText(R.id.widget_updated, "Updated just now")
            } else {
                views.setTextViewText(R.id.widget_value, "₹—")
                views.setTextViewText(R.id.widget_pnl, "Connect Kite in the app")
                views.setTextViewText(R.id.widget_updated, "Waiting for data")
            }
            manager.updateAppWidget(id, views)
        }
    }

    private fun money(value: Double): String = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(value)
}
