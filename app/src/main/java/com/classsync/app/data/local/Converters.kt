package com.classsync.app.data.local

import androidx.room.TypeConverter
import com.classsync.app.domain.model.ExceptionStatus
import com.classsync.app.domain.model.RecurrenceType
import com.classsync.app.domain.model.UserMode
import com.classsync.app.domain.master.AvailabilityStatus
import com.classsync.app.domain.master.GenerationStatus
import com.classsync.app.domain.master.MasterEntryType
import com.classsync.app.domain.master.MasterPeriodType
import com.classsync.app.domain.master.MasterRoutineStatus
import com.classsync.app.domain.master.MasterRoutineStep
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
    @TypeConverter fun stringToMode(value: String?): UserMode? = value?.let {
        if (it == UserMode.ADMINISTRATION.name) UserMode.ADMINISTRATION else UserMode.TEACHER
    }

    @TypeConverter fun recurrenceToString(value: RecurrenceType?): String? = value?.name
    @TypeConverter fun stringToRecurrence(value: String?): RecurrenceType? = value?.let(RecurrenceType::valueOf)

    @TypeConverter fun exceptionToString(value: ExceptionStatus?): String? = value?.name
    @TypeConverter fun stringToException(value: String?): ExceptionStatus? = value?.let(ExceptionStatus::valueOf)

    @TypeConverter fun masterRoutineStatusToString(value: MasterRoutineStatus?): String? = value?.name
    @TypeConverter fun stringToMasterRoutineStatus(value: String?): MasterRoutineStatus? = value?.let(MasterRoutineStatus::valueOf)

    @TypeConverter fun masterRoutineStepToString(value: MasterRoutineStep?): String? = value?.name
    @TypeConverter fun stringToMasterRoutineStep(value: String?): MasterRoutineStep? = value?.let(MasterRoutineStep::valueOf)

    @TypeConverter fun masterPeriodTypeToString(value: MasterPeriodType?): String? = value?.name
    @TypeConverter fun stringToMasterPeriodType(value: String?): MasterPeriodType? = value?.let(MasterPeriodType::valueOf)

    @TypeConverter fun masterEntryTypeToString(value: MasterEntryType?): String? = value?.name
    @TypeConverter fun stringToMasterEntryType(value: String?): MasterEntryType? = value?.let(MasterEntryType::valueOf)

    @TypeConverter fun availabilityToString(value: AvailabilityStatus?): String? = value?.name
    @TypeConverter fun stringToAvailability(value: String?): AvailabilityStatus? = value?.let(AvailabilityStatus::valueOf)

    @TypeConverter fun generationStatusToString(value: GenerationStatus?): String? = value?.name
    @TypeConverter fun stringToGenerationStatus(value: String?): GenerationStatus? = value?.let(GenerationStatus::valueOf)
}
