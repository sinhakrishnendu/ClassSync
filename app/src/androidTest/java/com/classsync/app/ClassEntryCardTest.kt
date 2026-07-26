package com.classsync.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.classsync.app.domain.model.AcademicGroup
import com.classsync.app.domain.model.ClassEntry
import com.classsync.app.domain.model.ClassSchedule
import com.classsync.app.domain.model.RecurrenceType
import com.classsync.app.domain.model.Subject
import com.classsync.app.domain.model.UserMode
import com.classsync.app.domain.model.UserPreferences
import com.classsync.app.ui.components.ClassEntryCard
import com.classsync.app.ui.theme.ClassSyncTheme
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ClassEntryCardTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun cardShowsCoreScheduleInformationAndIsClickable() {
        var clicked = false
        composeRule.setContent {
            ClassSyncTheme(dynamicColor = false) {
                ClassEntryCard(testCardEntry(), UserPreferences(), onClick = { clicked = true })
            }
        }

        composeRule.onNodeWithText("Animal Physiology").assertIsDisplayed()
        composeRule.onNodeWithText("MSc Zoology - Semester I - Section A").assertIsDisplayed()
        composeRule.onNodeWithText("Room 204").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Reminder on").assertIsDisplayed()
        composeRule.onNodeWithText("Animal Physiology").performClick()
        assertTrue(clicked)
    }
}

private fun testCardEntry(): ClassEntry {
    val instant = Instant.parse("2026-01-01T00:00:00Z")
    val group = AcademicGroup(1, "MSc Zoology", "Semester I", "Section A", null, instant, instant)
    val subject = Subject(1, 1, "Animal Physiology", null, instant, instant)
    return ClassEntry(
        ClassSchedule(
            id = 1,
            mode = UserMode.TEACHER,
            academicGroupId = 1,
            subjectId = 1,
            dayOfWeek = DayOfWeek.MONDAY,
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(10, 0),
            classroom = "Room 204",
            topic = null,
            teacherName = null,
            notes = null,
            recurrenceType = RecurrenceType.WEEKLY,
            oneTimeDate = null,
            reminderEnabled = true,
            reminderMinutes = 30,
            createdAt = instant,
            updatedAt = instant,
        ),
        group,
        subject,
        emptyList(),
    )
}
