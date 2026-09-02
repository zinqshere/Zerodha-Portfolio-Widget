package com.zinqshere.zerodhaportfoliowidget.data

class PortfolioRepository(private val store: PortfolioStore) {
    fun refresh(accountId: String? = null): PortfolioSnapshot {
        val accounts = if (accountId == null) store.accounts() else listOfNotNull(store.account(accountId))
        if (accounts.isEmpty()) return PortfolioSnapshot()

        var firstError: Throwable? = null
        accounts.filter { it.connected }.forEach { account ->
            try {
                val snapshot = fetch(account)
                store.saveSnapshotForAccount(account.id, snapshot)
            } catch (error: Throwable) {
                if (firstError == null) firstError = if (isAuthFailure(error)) KiteSessionExpiredException(error) else error
            }
        }

        if (accountId != null) {
            firstError?.let { throw it }
            return store.cachedSnapshotForAccount(accountId)
        }
        if (accounts.none { it.connected }) return PortfolioSnapshot()
        return store.combinedSnapshot(accounts.map { it.id })
    }

    private fun fetch(account: ZerodhaAccount): PortfolioSnapshot {
        val client = KiteClient(account.apiKey, account.accessToken)
        return try {
            val holdings = client.holdings()
            val mutualFunds = client.mutualFundHoldings()
            PortfolioSnapshot(
                equityValue = holdings.sumOf { it.quantity * it.lastPrice },
                equityInvested = holdings.sumOf { it.quantity * it.averagePrice },
                equityPnl = holdings.sumOf { it.pnl },
                equityDayPnl = holdings.sumOf { it.dayPnl },
                coinValue = mutualFunds.sumOf { it.value },
                coinInvested = mutualFunds.sumOf { it.invested },
                coinPnl = mutualFunds.sumOf { it.pnl },
                updatedAt = System.currentTimeMillis()
            )
        } catch (error: Throwable) {
            throw if (isAuthFailure(error)) KiteSessionExpiredException(error) else error
        }
    }

    private fun isAuthFailure(error: Throwable): Boolean {
        val message = error.message.orEmpty()
        return message.contains("api_key", ignoreCase = true) ||
            message.contains("access_token", ignoreCase = true) ||
            message.contains("authentication", ignoreCase = true) ||
            message.contains("unauthorized", ignoreCase = true) ||
            message.contains("invalid token", ignoreCase = true)
    }
}

class KiteSessionExpiredException(cause: Throwable) :
    IllegalStateException(
        "Kite session expired or is invalid. The last portfolio data is still available. Reconnect that Zerodha account in Account management.",
        cause
    )
