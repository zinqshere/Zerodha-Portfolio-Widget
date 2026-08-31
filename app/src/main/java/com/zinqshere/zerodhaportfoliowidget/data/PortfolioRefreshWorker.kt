package com.zinqshere.zerodhaportfoliowidget.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zinqshere.zerodhaportfoliowidget.widget.PortfolioWidgetReceiver

class PortfolioRefreshWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result = runCatching {
        val snapshot = PortfolioRepository(PortfolioStore(applicationContext)).refresh()
        PortfolioSnapshotStore(applicationContext).save(snapshot)
        PortfolioWidgetReceiver.refresh(applicationContext)
    }.fold({ Result.success() }, { Result.retry() })
}
