package com.classsync.app.data.local

import androidx.room.TypeConverter
import com.classsync.app.domain.model.ExceptionStatus
import com.classsync.app.domain.model.RecurrenceType
import com.classsync.app.domain.model.UserMode
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class ClassSyncConverters {
    @TypeConverter fun instantToLong(value: Instant?): Long? = value?.toEpochMilli()
    @TypeConverter fun longToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter fun localDateToString(value: LocalDate?): String? = value?.toString()
    @TypeConverter fun stringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter fun localTimeToMinutes(value: LocalTime?): Int? = value?.let { it.hour * 60 + it.minute }
    @TypeConverter fun minutesToLocalTime(value: Int?): LocalTime? = value?.let { LocalTime.of(it / 60, it % 60) }

    @TypeConverter fun dayToInt(value: DayOfWeek?): Int? = value?.value
    @TypeConverter fun intToDay(value: Int?): DayOfWeek? = value?.let(DayOfWeek::of)

    @TypeConverter fun modeToString(value: UserMode?): String? = value?.name
    @TypeConverter fun stringToMode(value: String?): UserMode? = value?.let(UserMode::valueOf)

    @TypeConverter fun recurrenceToString(value: RecurrenceType?): String? = value?.name
    @TypeConverter fun stringToRecurrence(value: String?): RecurrenceType? = value?.let(RecurrenceType::valueOf)

    @TypeConverter fun exceptionToString(value: ExceptionStatus?): String? = value?.name
    @TypeConverter fun stringToException(value: String?): ExceptionStatus? = value?.let(ExceptionStatus::valueOf)
}

