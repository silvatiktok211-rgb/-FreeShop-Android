package com.afilishop.app.notifications

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.afilishop.app.data.AfiliShopRepository
import java.util.concurrent.TimeUnit

class PushTokenSyncWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {

    override suspend fun doWork(): Result {
        val token = PushTokenStore.pending(applicationContext)
            ?: PushTokenStore.current(applicationContext)
            ?: return Result.success()
        val repository = AfiliShopRepository(applicationContext)
        return try {
            val session = repository.restoreSession()
            val userId = session?.user?.id
            if (userId.isNullOrBlank()) {
                Result.success()
            } else if (repository.registerPushToken(userId, token)) {
                PushTokenStore.markSynced(applicationContext, token)
                Result.success()
            } else {
                val refreshedUserId = repository.refreshSession().getOrNull()?.user?.id
                if (!refreshedUserId.isNullOrBlank() && repository.registerPushToken(refreshedUserId, token)) {
                    PushTokenStore.markSynced(applicationContext, token)
                    Result.success()
                } else {
                    Result.retry()
                }
            }
        } catch (_: Throwable) {
            Result.retry()
        } finally {
            repository.close()
        }
    }

    companion object {
        private const val UNIQUE_WORK = "afilishop-push-token-sync"

        fun enqueue(context: Context, token: String? = null) {
            token?.takeIf { it.isNotBlank() }?.let { PushTokenStore.savePending(context, it) }
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<PushTokenSyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                UNIQUE_WORK,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }
}
