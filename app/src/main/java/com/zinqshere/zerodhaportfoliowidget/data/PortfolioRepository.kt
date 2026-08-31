package com.zinqshere.zerodhaportfoliowidget.data

class PortfolioRepository(private val store: PortfolioStore) {
    fun refresh(): PortfolioSnapshot {
        val holdings = if (store.apiKey().isNotBlank() && store.accessToken().isNotBlank()) {
            KiteClient(store.apiKey(), store.accessToken()).holdings()
        } else emptyList()
        val equityValue = holdings.sumOf { it.quantity * it.lastPrice }
        val equityInvested = holdings.sumOf { it.quantity * it.averagePrice }
        val snapshot = PortfolioSnapshot(
            equityValue = equityValue,
            equityInvested = equityInvested,
            equityPnl = holdings.sumOf { it.pnl },
            equityDayPnl = holdings.sumOf { it.dayPnl },
            coinValue = store.coinValue(),
            coinInvested = store.coinInvested(),
            coinPnl = store.coinValue() - store.coinInvested(),
            updatedAt = System.currentTimeMillis()
        )
        store.saveSnapshot(snapshot)
        return snapshot
    }
}
