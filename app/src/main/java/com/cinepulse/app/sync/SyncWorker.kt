// sync/SyncWorker.kt
package com.cinepulse.app.sync

import android.content.Context
import androidx.work.*
import com.cinepulse.app.CinePulseApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class SyncWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val repo = (applicationContext as CinePulseApp).container.repo
        return@withContext runCatching {
            repo.pushPending()
            Result.success()
        }.getOrElse { Result.retry() }
    }

    companion object {
        private const val UNIQUE_NAME = "cinepulse_sync"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15, TimeUnit.SECONDS
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.KEEP, request)
        }
    }
}