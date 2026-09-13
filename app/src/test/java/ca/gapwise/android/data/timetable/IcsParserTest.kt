package ca.gapwise.android.data.timetable

import ca.gapwise.android.core.model.ASSESSMENT_WINDOW_NOTE
import ca.gapwise.android.core.model.ActivityType
import ca.gapwise.android.core.model.Campus
import ca.gapwise.android.core.model.LocationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IcsParserTest {
    @Test
    fun `course codes identify all three U of T campuses`() {
        assertEquals(Campus.UTSG, Campus.fromCourseCode("CSC108H1"))
        assertEquals(Campus.UTSC, Campus.fromCourseCode("CSCA08H3"))
        assertEquals(Campus.UTM, Campus.fromCourseCode("CSC110Y5"))
    }

    @Test
    fun `mixed campus calendar preserves non UTM locations and only maps UTM rooms`() {
        val parsed = IcsParser.parse(
            calendar(
                event("sg", "CSC108H1 LEC0101", "BA 1170", "20260907T090000", "20260907T110000"),
                event("sc", "CSCA08H3 LEC0101", "SW 319", "20260908T120000", "20260908T140000"),
                event("utm", "CSC110Y5 LEC0101", "MN 1270", "20260909T150000", "20260909T170000"),
            ),
        )

        val utsg = parsed.meetings.first { it.courseCode == "CSC108H1" }
        val utsc = parsed.meetings.first { it.courseCode == "CSCA08H3" }
        val utm = parsed.meetings.first { it.courseCode == "CSC110Y5" }

        assertEquals(Campus.UTSG, utsg.campus)
        assertEquals("BA 1170", utsg.locationLabel)
        assertNull(utsg.buildingCode)

        assertEquals(Campus.UTSC, utsc.campus)
        assertEquals("SW 319", utsc.locationLabel)
        assertNull(utsc.buildingCode)

        assertEquals(Campus.UTM, utm.campus)
        assertEquals("MN", utm.buildingCode)
        assertEquals("1270", utm.room)
        assertEquals(LocationType.PHYSICAL, utm.locationType)
    }

    @Test
    fun `ZZ TBA assessment placeholder is RES and remains TBA`() {
        val parsed = IcsParser.parse(
            calendar(
                event(
                    uid = "assessment",
                    summary = "MAT157Y5 LEC0101",
                    location = "ZZ TBA",
                    start = "20260912T130000",
                    end = "20260912T150000",
                    description = "Analysis I\\n**********************",
                ),
            ),
        )

        val meeting = parsed.meetings.single()
        assertEquals(ActivityType.RES, meeting.activityType)
        assertEquals(LocationType.TBA, meeting.locationType)
        assertEquals(ASSESSMENT_WINDOW_NOTE, meeting.notes)
        assertTrue(meeting.isAssessmentWindow)
        assertEquals("Reserved assessment window · location TBA", meeting.locationLabel)
        assertEquals("0101", meeting.sectionCode)
    }

    private fun event(
        uid: String,
        summary: String,
        location: String,
        start: String,
        end: String,
        description: String = "",
    ) = buildList {
        add("BEGIN:VEVENT")
        add("UID:$uid")
        add("DTSTART:$start")
        add("DTEND:$end")
        add("SUMMARY:$summary")
        if (description.isNotBlank()) add("DESCRIPTION:$description")
        add("LOCATION:$location")
        add("END:VEVENT")
    }.joinToString("\r\n")

    private fun calendar(vararg events: String) = listOf(
        "BEGIN:VCALENDAR",
        "VERSION:2.0",
        *events,
        "END:VCALENDAR",
    ).joinToString("\r\n")
}
