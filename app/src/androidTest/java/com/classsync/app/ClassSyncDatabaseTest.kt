package com.classsync.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.classsync.app.data.local.ClassSyncDatabase
import com.classsync.app.data.repository.RoomScheduleRepository
import com.classsync.app.domain.model.RecurrenceType
import com.classsync.app.domain.model.ScheduleDraft
import com.classsync.app.domain.model.UserMode
import com.classsync.app.domain.time.TimeProvider
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClassSyncDatabaseTest {
    private lateinit var database: ClassSyncDatabase
    private lateinit var repository: RoomScheduleRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ClassSyncDatabase::class.java).build()
        repository = RoomScheduleRepository(
            database,
            database.academicGroupDao(),
            database.subjectDao(),
            database.classScheduleDao(),
            database.scheduleExceptionDao(),
            InstrumentedTimeProvider,
        )
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun repositoryCreatesRelationsUpdatesAndDeletesSchedule() = runBlocking {
        val id = repository.saveSchedule(
            ScheduleDraft(
                mode = UserMode.TEACHER,
                programme = "MSc Zoology",
                semester = "Semester I",
                subjectName = "Animal Physiology",
                dayOfWeek = DayOfWeek.MONDAY,
                startTime = LocalTime.of(9, 0),
                endTime = LocalTime.of(10, 0),
                recurrenceType = RecurrenceType.WEEKLY,
            ),
        )

        val saved = repository.getEntry(id)
        assertEquals("MSc Zoology", saved?.group?.programme)
        assertEquals("Animal Physiology", saved?.subject?.name)

        repository.setReminderEnabled(id, false)
        assertEquals(false, repository.getEntry(id)?.schedule?.reminderEnabled)

        repository.deleteSchedule(id)
        assertNull(repository.getEntry(id))
    }
}

private object InstrumentedTimeProvider : TimeProvider {
    private val value = ZonedDateTime.of(2026, 1, 5, 8, 0, 0, 0, ZoneId.of("Asia/Kolkata"))
    override fun now(): ZonedDateTime = value
    override fun zoneId(): ZoneId = value.zone
}

