package com.zinqshere.zerodhaportfoliowidget.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class PortfolioStore(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context, "portfolio_secrets",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveKite(apiKey: String, accessToken: String) = prefs.edit()
        .putString("api_key", apiKey).putString("access_token", accessToken).apply()
    fun apiKey() = prefs.getString("api_key", "") ?: ""
    fun accessToken() = prefs.getString("access_token", "") ?: ""

    fun saveCoin(invested: Double, value: Double) = prefs.edit()
        .putLong("coin_invested", invested.toBits()).putLong("coin_value", value.toBits()).apply()
    fun coinInvested() = Double.fromBits(prefs.getLong("coin_invested", 0L))
    fun coinValue() = Double.fromBits(prefs.getLong("coin_value", 0L))

    fun saveSnapshot(s: PortfolioSnapshot) = prefs.edit()
        .putLong("equity_value", s.equityValue.toBits())
        .putLong("equity_invested", s.equityInvested.toBits())
        .putLong("equity_pnl", s.equityPnl.toBits())
        .putLong("equity_day_pnl", s.equityDayPnl.toBits())
        .putLong("coin_value", s.coinValue.toBits())
        .putLong("coin_invested", s.coinInvested.toBits())
        .putLong("coin_pnl", s.coinPnl.toBits())
        .putLong("updated_at", s.updatedAt).apply()

    fun cachedSnapshot() = PortfolioSnapshot(
        equityValue = Double.fromBits(prefs.getLong("equity_value", 0L)),
        equityInvested = Double.fromBits(prefs.getLong("equity_invested", 0L)),
        equityPnl = Double.fromBits(prefs.getLong("equity_pnl", 0L)),
        equityDayPnl = Double.fromBits(prefs.getLong("equity_day_pnl", 0L)),
        coinValue = Double.fromBits(prefs.getLong("coin_value", 0L)),
        coinInvested = Double.fromBits(prefs.getLong("coin_invested", 0L)),
        coinPnl = Double.fromBits(prefs.getLong("coin_pnl", 0L)),
        updatedAt = prefs.getLong("updated_at", 0L)
    )
}
