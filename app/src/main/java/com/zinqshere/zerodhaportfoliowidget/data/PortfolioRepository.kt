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
                // Keep the saved session and cached snapshot. The Kite token may have
                // expired, but deleting it here makes the app look disconnected and
                // discards useful state before the user explicitly reconnects.
                throw KiteSessionExpiredException(error)
            }
            throw error
        }
    }
}

class KiteSessionExpiredException(cause: Throwable) :
    IllegalStateException(
        "Kite session expired or is invalid. Your last portfolio data is still available. Tap Reconnect Zerodha in Settings to sign in again.",
        cause
    )
