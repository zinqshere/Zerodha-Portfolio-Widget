package com.zinqshere.zerodhaportfoliowidget.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zinqshere.zerodhaportfoliowidget.widget.PortfolioWidgetReceiver

class PortfolioRefreshWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = runCatching {
        val store = PortfolioStore(applicationContext)
        store.saveSnapshot(PortfolioRepository(store).refresh())
        PortfolioWidgetReceiver.refresh(applicationContext)
    }.fold({ Result.success() }, { Result.retry() })
}
