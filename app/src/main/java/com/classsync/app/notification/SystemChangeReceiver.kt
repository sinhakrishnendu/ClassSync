package com.classsync.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class SystemChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in SupportedActions) return
        WorkManager.getInstance(context).enqueueUniqueWork(
            RescheduleWorkName,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<RescheduleRemindersWorker>().build(),
        )
    }

    companion object {
        const val RescheduleWorkName = "reschedule_class_reminders"
        private val SupportedActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }
}

