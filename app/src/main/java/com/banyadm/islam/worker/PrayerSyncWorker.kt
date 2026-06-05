package com.banyadm.islam.worker

import android.content.Context
import androidx.work.*
import com.banyadm.islam.alarm.AlarmScheduler
import com.banyadm.islam.data.PrayerRepository
import com.banyadm.islam.data.SalahPreferences
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class PrayerSyncWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = SalahPreferences(applicationContext)
        val repo = PrayerRepository()

        val location = prefs.location.first() ?: return Result.retry()
        val method = prefs.calcMethod.first()
        val toggles = prefs.prayerToggles.first()

        val result = repo.fetchTimes(location.first, location.second, method)

        return if (result.isSuccess) {
            val times = result.getOrThrow()
            prefs.cacheTimes(times)
            AlarmScheduler.scheduleAll(applicationContext, times, toggles)
            scheduleTomorrow(applicationContext)
            Result.success()
        } else {
            Result.retry()
        }
    }

    companion object {
        fun scheduleTomorrow(context: Context) {
            val now = java.util.Calendar.getInstance()
            val tomorrow = java.util.Calendar.getInstance().apply {
                add(java.util.Calendar.DAY_OF_YEAR, 1)
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 1)
                set(java.util.Calendar.SECOND, 0)
            }
            val delay = tomorrow.timeInMillis - now.timeInMillis

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<PrayerSyncWorker>()
                .setConstraints(constraints)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork("daily_sync", ExistingWorkPolicy.REPLACE, request)
        }
    }
}
