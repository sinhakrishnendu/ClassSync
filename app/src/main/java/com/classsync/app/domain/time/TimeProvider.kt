package com.classsync.app.domain.time

import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

interface TimeProvider {
    fun now(): ZonedDateTime
    fun zoneId(): ZoneId
}

class SystemTimeProvider @Inject constructor() : TimeProvider {
    override fun now(): ZonedDateTime = ZonedDateTime.now()
    override fun zoneId(): ZoneId = ZoneId.systemDefault()
}

