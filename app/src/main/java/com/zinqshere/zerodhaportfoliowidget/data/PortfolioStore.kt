package com.zinqshere.zerodhaportfoliowidget.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class PortfolioStore(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "portfolio_secrets",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    fun saveKite(apiKey: String, accessToken: String) = prefs.edit().putString("api_key", apiKey).putString("access_token", accessToken).apply()
    fun apiKey() = prefs.getString("api_key", "") ?: ""
    fun accessToken() = prefs.getString("access_token", "") ?: ""
    fun saveCoin(invested: Double, value: Double) = prefs.edit().putLong("coin_invested", invested.toBits()).putLong("coin_value", value.toBits()).apply()
    fun coinInvested() = Double.fromBits(prefs.getLong("coin_invested", 0L))
    fun coinValue() = Double.fromBits(prefs.getLong("coin_value", 0L))
}
