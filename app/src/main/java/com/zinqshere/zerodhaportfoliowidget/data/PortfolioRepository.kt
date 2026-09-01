package com.zinqshere.zerodhaportfoliowidget.data

class PortfolioRepository(private val store: PortfolioStore) {
    fun refresh(): PortfolioSnapshot {
        val hasKite = store.apiKey().isNotBlank() && store.accessToken().isNotBlank()
        val client = if (hasKite) KiteClient(store.apiKey(), store.accessToken()) else null

        try {
            val holdings = client?.holdings().orEmpty()
            val mutualFunds = client?.mutualFundHoldings().orEmpty()

            val equityValue = holdings.sumOf { it.quantity * it.lastPrice }
            val equityInvested = holdings.sumOf { it.quantity * it.averagePrice }
            val coinValue = mutualFunds.sumOf { it.value }
            val coinInvested = mutualFunds.sumOf { it.invested }

            val snapshot = PortfolioSnapshot(
                equityValue = equityValue,
                equityInvested = equityInvested,
                equityPnl = holdings.sumOf { it.pnl },
                equityDayPnl = holdings.sumOf { it.dayPnl },
                coinValue = coinValue,
                coinInvested = coinInvested,
                coinPnl = mutualFunds.sumOf { it.pnl },
                updatedAt = System.currentTimeMillis()
            )
            store.saveSnapshot(snapshot)
            return snapshot
        } catch (error: Throwable) {
            val message = error.message.orEmpty()
            val authFailure = message.contains("api_key", ignoreCase = true) ||
                message.contains("access_token", ignoreCase = true) ||
                message.contains("authentication", ignoreCase = true) ||
                message.contains("unauthorized", ignoreCase = true) ||
                message.contains("invalid token", ignoreCase = true)

            if (authFailure) {
                // Kite access tokens expire daily. Do not keep retrying a known-invalid
                // session or present it as connected after Kite has rejected it.
                store.clearKite()
                throw IllegalStateException(
                    "Kite session expired or is invalid. Tap Reconnect Zerodha in Settings to sign in again.",
                    error
                )
            }
            throw error
        }
    }
}
