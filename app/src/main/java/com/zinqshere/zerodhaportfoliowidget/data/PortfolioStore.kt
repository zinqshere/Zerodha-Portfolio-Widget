package com.zinqshere.zerodhaportfoliowidget.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import org.json.JSONArray
import org.json.JSONObject

class PortfolioStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = EncryptedSharedPreferences.create(
        appContext, "portfolio_secrets",
        MasterKey.Builder(appContext).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    init { migrateLegacyAccount() }

    fun accounts(): List<ZerodhaAccount> {
        val raw = prefs.getString(KEY_ACCOUNTS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val o = array.optJSONObject(i) ?: return@mapNotNull null
                ZerodhaAccount(
                    id = o.optString("id"),
                    label = o.optString("label").ifBlank { "Zerodha account" },
                    apiKey = o.optString("apiKey"),
                    accessToken = o.optString("accessToken")
                )
            }.filter { it.id.isNotBlank() }
        }.getOrDefault(emptyList())
    }

    fun account(id: String): ZerodhaAccount? = accounts().firstOrNull { it.id == id }

    fun addAccount(label: String = "Account ${accounts().size + 1}"): ZerodhaAccount {
        val account = ZerodhaAccount("account_${System.currentTimeMillis()}", label.trim().ifBlank { "Zerodha account" })
        saveAccounts(accounts() + account)
        return account
    }

    fun renameAccount(id: String, label: String) {
        val safe = label.trim().ifBlank { "Zerodha account" }
        saveAccounts(accounts().map { if (it.id == id) it.copy(label = safe) else it })
    }

    fun saveKiteForAccount(id: String, apiKey: String, accessToken: String) {
        val current = accounts().toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index >= 0) current[index] = current[index].copy(apiKey = apiKey, accessToken = accessToken)
        else current += ZerodhaAccount(id, "Zerodha account", apiKey, accessToken)
        saveAccounts(current)
    }

    fun removeAccount(id: String) {
        saveAccounts(accounts().filterNot { it.id == id })
        prefs.edit().remove(snapshotKey(id)).remove(pendingKey(id)).apply()
    }

    fun setPendingAccountId(id: String?) = prefs.edit().putString(KEY_PENDING_ACCOUNT, id).apply()
    fun pendingAccountId(): String? = prefs.getString(KEY_PENDING_ACCOUNT, null)
    fun clearPendingAccountId() = prefs.edit().remove(KEY_PENDING_ACCOUNT).apply()

    fun saveKite(apiKey: String, accessToken: String) {
        val first = accounts().firstOrNull() ?: addAccount("Personal")
        saveKiteForAccount(first.id, apiKey, accessToken)
    }

    fun clearKite() {
        accounts().firstOrNull()?.let { saveKiteForAccount(it.id, "", "") }
    }

    fun apiKey() = accounts().firstOrNull()?.apiKey.orEmpty()
    fun accessToken() = accounts().firstOrNull()?.accessToken.orEmpty()

    fun saveBackendUrl(url: String) = prefs.edit().putString("backend_url", url.trim().trimEnd('/')).apply()
    fun backendUrl() = prefs.getString("backend_url", "") ?: ""

    fun saveRefreshInterval(minutes: Long) {
        prefs.edit().putLong("refresh_interval_minutes", minutes).apply()
        if (accounts().any { it.connected }) {
            val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            val request = OneTimeWorkRequestBuilder<PortfolioRefreshWorker>().setConstraints(constraints).build()
            WorkManager.getInstance(appContext).enqueueUniqueWork("portfolio-refresh-now", ExistingWorkPolicy.REPLACE, request)
        }
    }

    fun refreshIntervalMinutes(): Long = prefs.getLong("refresh_interval_minutes", 30L)

    fun saveCoin(invested: Double, value: Double) = prefs.edit()
        .putLong("coin_invested", invested.toBits()).putLong("coin_value", value.toBits()).apply()
    fun coinInvested() = Double.fromBits(prefs.getLong("coin_invested", 0L))
    fun coinValue() = Double.fromBits(prefs.getLong("coin_value", 0L))

    fun saveTheme(theme: String) = prefs.edit().putString("theme", theme).apply()
    fun theme() = prefs.getString("theme", "dark_monet") ?: "dark_monet"

    fun saveSnapshot(s: PortfolioSnapshot) = saveSnapshotForAccount(accounts().firstOrNull()?.id ?: LEGACY_ACCOUNT_ID, s)

    fun saveSnapshotForAccount(accountId: String, s: PortfolioSnapshot) = prefs.edit()
        .putLong(snapshotKey(accountId, "equity_value"), s.equityValue.toBits())
        .putLong(snapshotKey(accountId, "equity_invested"), s.equityInvested.toBits())
        .putLong(snapshotKey(accountId, "equity_pnl"), s.equityPnl.toBits())
        .putLong(snapshotKey(accountId, "equity_day_pnl"), s.equityDayPnl.toBits())
        .putLong(snapshotKey(accountId, "coin_value"), s.coinValue.toBits())
        .putLong(snapshotKey(accountId, "coin_invested"), s.coinInvested.toBits())
        .putLong(snapshotKey(accountId, "coin_pnl"), s.coinPnl.toBits())
        .putLong(snapshotKey(accountId, "updated_at"), s.updatedAt).apply()

    fun cachedSnapshot() = cachedSnapshotForAccount(accounts().firstOrNull()?.id ?: LEGACY_ACCOUNT_ID)

    fun cachedSnapshotForAccount(accountId: String): PortfolioSnapshot = PortfolioSnapshot(
        equityValue = Double.fromBits(prefs.getLong(snapshotKey(accountId, "equity_value"), 0L)),
        equityInvested = Double.fromBits(prefs.getLong(snapshotKey(accountId, "equity_invested"), 0L)),
        equityPnl = Double.fromBits(prefs.getLong(snapshotKey(accountId, "equity_pnl"), 0L)),
        equityDayPnl = Double.fromBits(prefs.getLong(snapshotKey(accountId, "equity_day_pnl"), 0L)),
        coinValue = Double.fromBits(prefs.getLong(snapshotKey(accountId, "coin_value"), 0L)),
        coinInvested = Double.fromBits(prefs.getLong(snapshotKey(accountId, "coin_invested"), 0L)),
        coinPnl = Double.fromBits(prefs.getLong(snapshotKey(accountId, "coin_pnl"), 0L)),
        updatedAt = prefs.getLong(snapshotKey(accountId, "updated_at"), 0L)
    )

    fun combinedSnapshot(accountIds: List<String> = accounts().map { it.id }): PortfolioSnapshot {
        val snapshots = accountIds.map { cachedSnapshotForAccount(it) }
        return snapshots.fold(PortfolioSnapshot()) { a, b ->
            PortfolioSnapshot(
                equityValue = a.equityValue + b.equityValue,
                equityInvested = a.equityInvested + b.equityInvested,
                equityPnl = a.equityPnl + b.equityPnl,
                equityDayPnl = a.equityDayPnl + b.equityDayPnl,
                coinValue = a.coinValue + b.coinValue,
                coinInvested = a.coinInvested + b.coinInvested,
                coinPnl = a.coinPnl + b.coinPnl,
                updatedAt = maxOf(a.updatedAt, b.updatedAt)
            )
        }
    }

    private fun saveAccounts(value: List<ZerodhaAccount>) {
        val array = JSONArray()
        value.forEach { a ->
            array.put(JSONObject().apply {
                put("id", a.id); put("label", a.label); put("apiKey", a.apiKey); put("accessToken", a.accessToken)
            })
        }
        prefs.edit().putString(KEY_ACCOUNTS, array.toString()).apply()
    }

    private fun migrateLegacyAccount() {
        if (prefs.contains(KEY_ACCOUNTS)) return
        val apiKey = prefs.getString("api_key", "").orEmpty()
        val token = prefs.getString("access_token", "").orEmpty()
        if (apiKey.isBlank() && token.isBlank()) return
        val account = ZerodhaAccount(LEGACY_ACCOUNT_ID, "Personal", apiKey, token)
        saveAccounts(listOf(account))
        val old = PortfolioSnapshot(
            equityValue = Double.fromBits(prefs.getLong("equity_value", 0L)),
            equityInvested = Double.fromBits(prefs.getLong("equity_invested", 0L)),
            equityPnl = Double.fromBits(prefs.getLong("equity_pnl", 0L)),
            equityDayPnl = Double.fromBits(prefs.getLong("equity_day_pnl", 0L)),
            coinValue = Double.fromBits(prefs.getLong("coin_value", 0L)),
            coinInvested = Double.fromBits(prefs.getLong("coin_invested", 0L)),
            coinPnl = Double.fromBits(prefs.getLong("coin_pnl", 0L)),
            updatedAt = prefs.getLong("updated_at", 0L)
        )
        saveSnapshotForAccount(account.id, old)
    }

    private fun snapshotKey(accountId: String, field: String) = "snapshot_${accountId}_$field"
    private fun snapshotKey(accountId: String) = "snapshot_${accountId}_"
    private fun pendingKey(accountId: String) = "pending_${accountId}"

    companion object {
        private const val KEY_ACCOUNTS = "zerodha_accounts"
        private const val KEY_PENDING_ACCOUNT = "pending_account_id"
        private const val LEGACY_ACCOUNT_ID = "account_1"
    }
}
