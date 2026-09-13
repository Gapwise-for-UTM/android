package ca.gapwise.android.data.timetable

import ca.gapwise.android.core.model.ActivityType
import ca.gapwise.android.core.model.Campus
import ca.gapwise.android.core.model.LocationType
import ca.gapwise.android.core.model.Meeting
import ca.gapwise.android.core.model.Term
import java.time.LocalDate

data class ParsedTimetable(
    val meetings: List<Meeting>,
    val warnings: List<String>,
)

object IcsParser {
    const val MAX_ICS_CHARS = 2_000_000

    private val courseCodePattern = Regex("\\b[A-Z]{3}[A-Z0-9]\\d{2}[A-Z][135]\\b")
    private val sectionPattern = Regex("\\b(LEC|TUT|PRA)\\s*(\\d{4})\\b")
    private val dateTimePattern = Regex("(\\d{8})T(\\d{4,6})")
    private val utmLocationPattern = Regex("^([A-Z]{1,4}\\d?)\\s+(.+)$")

    fun parse(text: String): ParsedTimetable {
        require(text.length <= MAX_ICS_CHARS) { "Calendar is too large to import safely." }

        val warnings = mutableListOf<String>()
        val meetings = unfold(text)
            .splitEvents()
            .mapNotNull { event -> parseEvent(event, warnings) }
            .sortedWith(compareBy<Meeting>({ it.term.ordinal }, { it.weekday.value }, { it.startTime }))

        require(meetings.isNotEmpty()) { "No U of T class meetings were found in this calendar." }
        return ParsedTimetable(meetings = meetings, warnings = warnings.distinct())
    }

    private fun parseEvent(lines: List<String>, warnings: MutableList<String>): Meeting? {
        val summary = property(lines, "SUMMARY").orEmpty().decodeIcsText().trim()
        val description = property(lines, "DESCRIPTION").orEmpty().decodeIcsText().trim()
        val courseCode = courseCodePattern.find(summary.uppercase())?.value
            ?: courseCodePattern.find(description.uppercase())?.value
            ?: return null

        val startRaw = property(lines, "DTSTART") ?: return null
        val endRaw = property(lines, "DTEND") ?: return null
        val start = parseDateTime(startRaw) ?: return null
        val end = parseDateTime(endRaw) ?: return null
        if (end.minutes <= start.minutes) {
            warnings += "$courseCode has an invalid or overnight time and was skipped."
            return null
        }

        val normalizedSummary = summary.uppercase()
        val sectionMatch = sectionPattern.find(normalizedSummary)
        val activityType = when (sectionMatch?.groupValues?.getOrNull(1)) {
            "LEC" -> ActivityType.LEC
            "TUT" -> ActivityType.TUT
            "PRA" -> ActivityType.PRA
            else -> ActivityType.OTHER
        }
        val sectionCode = sectionMatch?.let { "${it.groupValues[1]}${it.groupValues[2]}" }.orEmpty()
        val campus = Campus.fromCourseCode(courseCode)
        val rawLocation = property(lines, "LOCATION")?.decodeIcsText()?.trim().orEmpty()
        val normalizedLocation = rawLocation.uppercase()
        val locationType = when {
            rawLocation.isBlank() -> LocationType.TBA
            normalizedLocation in setOf("TBA", "TBD", "TO BE ANNOUNCED") -> LocationType.TBA
            listOf("ONLINE", "VIRTUAL", "ZOOM", "WEB").any(normalizedLocation::contains) -> LocationType.ONLINE
            else -> LocationType.PHYSICAL
        }
        val utmRoom = if (campus == Campus.UTM && locationType == LocationType.PHYSICAL) {
            utmLocationPattern.matchEntire(rawLocation.uppercase())
        } else {
            null
        }

        val courseName = summary
            .replace(courseCode, "", ignoreCase = true)
            .replace(sectionMatch?.value.orEmpty(), "", ignoreCase = true)
            .trim(' ', '-', '–', '—')
            .ifBlank { description.lineSequence().firstOrNull()?.trim().orEmpty() }

        return Meeting(
            id = property(lines, "UID")?.trim().orEmpty().ifBlank {
                "$courseCode-${start.date}-${start.minutes}-${sectionCode.ifBlank { "class" }}"
            },
            courseCode = courseCode,
            activityType = activityType,
            sectionCode = sectionCode,
            courseName = courseName,
            startTime = start.minutes,
            endTime = end.minutes,
            weekday = start.date.dayOfWeek,
            term = termForMonth(start.date.monthValue),
            campus = campus,
            sourceLocation = rawLocation.takeIf(String::isNotBlank),
            buildingCode = utmRoom?.groupValues?.getOrNull(1),
            room = utmRoom?.groupValues?.getOrNull(2),
            locationType = locationType,
        )
    }

    private data class ParsedDateTime(val date: LocalDate, val minutes: Int)

    private fun parseDateTime(value: String): ParsedDateTime? {
        val match = dateTimePattern.find(value) ?: return null
        val date = match.groupValues[1]
        val time = match.groupValues[2]
        return runCatching {
            val localDate = LocalDate.of(
                date.substring(0, 4).toInt(),
                date.substring(4, 6).toInt(),
                date.substring(6, 8).toInt(),
            )
            val hour = time.substring(0, 2).toInt()
            val minute = time.substring(2, 4).toInt()
            ParsedDateTime(localDate, hour * 60 + minute)
        }.getOrNull()
    }

    private fun termForMonth(month: Int): Term = when (month) {
        in 1..4 -> Term.WINTER
        in 5..8 -> Term.SUMMER
        else -> Term.FALL
    }

    private fun property(lines: List<String>, name: String): String? = lines
        .firstOrNull { line -> line.substringBefore(':').substringBefore(';').equals(name, ignoreCase = true) }
        ?.substringAfter(':', missingDelimiterValue = "")

    private fun unfold(text: String): List<String> {
        val normalized = text.replace("\r\n", "\n").replace('\r', '\n')
        val result = mutableListOf<String>()
        normalized.lineSequence().forEach { line ->
            if ((line.startsWith(' ') || line.startsWith('\t')) && result.isNotEmpty()) {
                result[result.lastIndex] += line.drop(1)
            } else {
                result += line
            }
        }
        return result
    }

    private fun List<String>.splitEvents(): List<List<String>> {
        val events = mutableListOf<List<String>>()
        var current: MutableList<String>? = null
        for (line in this) {
            when (line.trim().uppercase()) {
                "BEGIN:VEVENT" -> current = mutableListOf()
                "END:VEVENT" -> current?.let(events::add).also { current = null }
                else -> current?.add(line)
            }
        }
        return events
    }

    private fun String.decodeIcsText(): String = this
        .replace("\\n", "\n", ignoreCase = true)
        .replace("\\,", ",")
        .replace("\\;", ";")
        .replace("\\\\", "\\")
}
