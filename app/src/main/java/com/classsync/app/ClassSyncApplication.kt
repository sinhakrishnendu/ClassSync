package com.classsync.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.classsync.app.notification.NotificationChannels
import com.classsync.app.notification.RescheduleRemindersWorker
import com.classsync.app.notification.SystemChangeReceiver
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ClassSyncApplication : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.create(this)
        WorkManager.getInstance(this).enqueueUniqueWork(
            SystemChangeReceiver.RescheduleWorkName,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<RescheduleRemindersWorker>().build(),
        )
    }
}

