package com.zinqshere.zerodhaportfoliowidget.data

data class Holding(val symbol: String, val quantity: Double, val averagePrice: Double, val lastPrice: Double, val pnl: Double, val dayPnl: Double)
data class FundHolding(val name: String, val units: Double, val invested: Double, val value: Double, val pnl: Double)
data class PortfolioSnapshot(
    val equityValue: Double = 0.0,
    val equityInvested: Double = 0.0,
    val equityPnl: Double = 0.0,
    val equityDayPnl: Double = 0.0,
    val coinValue: Double = 0.0,
    val coinInvested: Double = 0.0,
    val coinPnl: Double = 0.0,
    val updatedAt: Long = 0L
) {
    val totalValue get() = equityValue + coinValue
    val totalInvested get() = equityInvested + coinInvested
    val totalPnl get() = equityPnl + coinPnl
}
