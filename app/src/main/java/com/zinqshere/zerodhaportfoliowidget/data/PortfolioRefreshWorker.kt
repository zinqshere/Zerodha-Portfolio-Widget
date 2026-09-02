package com.zinqshere.zerodhaportfoliowidget.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zinqshere.zerodhaportfoliowidget.widget.PortfolioWidgetReceiver

class PortfolioRefreshWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        return try {
            val store = PortfolioStore(applicationContext)
            PortfolioRepository(store).refresh()
            PortfolioWidgetReceiver.refresh(applicationContext)
            Result.success()
        } catch (_: KiteSessionExpiredException) {
            // Kite access tokens expire daily. Keep the cached snapshot and stop
            // retrying until the user explicitly reconnects.
            PortfolioWidgetReceiver.refresh(applicationContext)
            Result.success()
        } catch (_: Throwable) {
            Result.retry()
        }
    }
}
