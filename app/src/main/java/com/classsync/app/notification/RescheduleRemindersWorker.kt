package com.classsync.app.notification

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class RescheduleRemindersWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val reminderScheduler: ReminderScheduler,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        reminderScheduler.rescheduleAll()
        Result.success()
    } catch (error: Exception) {
        Log.e("ClassSyncReminder", "Unable to restore class reminders", error)
        Result.retry()
    }
}
