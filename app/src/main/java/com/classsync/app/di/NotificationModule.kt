package com.classsync.app.di

import com.classsync.app.notification.AndroidReminderScheduler
import com.classsync.app.notification.ReminderScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {
    @Binds abstract fun bindReminderScheduler(implementation: AndroidReminderScheduler): ReminderScheduler
}

