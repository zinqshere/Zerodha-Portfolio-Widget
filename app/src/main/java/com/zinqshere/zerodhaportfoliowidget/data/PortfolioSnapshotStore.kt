package com.zinqshere.zerodhaportfoliowidget.data

import android.content.Context
import org.json.JSONObject

class PortfolioSnapshotStore(context: Context) {
    private val prefs = context.getSharedPreferences("portfolio_snapshot", Context.MODE_PRIVATE)
    fun save(s: PortfolioSnapshot) = prefs.edit().putString("snapshot", JSONObject().apply {
        put("equityValue", s.equityValue); put("equityInvested", s.equityInvested); put("equityPnl", s.equityPnl)
        put("equityDayPnl", s.equityDayPnl); put("coinValue", s.coinValue); put("coinInvested", s.coinInvested)
        put("coinPnl", s.coinPnl); put("updatedAt", s.updatedAt)
    }.toString()).apply()
    fun read(): PortfolioSnapshot {
        val o = runCatching { JSONObject(prefs.getString("snapshot", "{}")) }.getOrDefault(JSONObject())
        return PortfolioSnapshot(o.optDouble("equityValue"), o.optDouble("equityInvested"), o.optDouble("equityPnl"), o.optDouble("equityDayPnl"), o.optDouble("coinValue"), o.optDouble("coinInvested"), o.optDouble("coinPnl"), o.optLong("updatedAt"))
    }
}
