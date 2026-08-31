package com.zinqshere.zerodhaportfoliowidget.data

class PortfolioRepository(private val store: PortfolioStore) {
    fun refresh(): PortfolioSnapshot {
        val hasKite = store.apiKey().isNotBlank() && store.accessToken().isNotBlank()
        val client = if (hasKite) KiteClient(store.apiKey(), store.accessToken()) else null
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
    }
}
