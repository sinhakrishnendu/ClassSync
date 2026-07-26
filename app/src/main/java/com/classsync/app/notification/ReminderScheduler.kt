package com.classsync.app.notification

interface ReminderScheduler {
    suspend fun schedule(scheduleId: Long)
    suspend fun scheduleNext(scheduleId: Long)
    suspend fun cancel(scheduleId: Long)
    suspend fun rescheduleAll()
    suspend fun cancelAll()
}
