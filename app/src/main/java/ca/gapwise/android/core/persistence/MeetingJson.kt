package ca.gapwise.android.core.persistence

import ca.gapwise.android.core.model.ActivityType
import ca.gapwise.android.core.model.Campus
import ca.gapwise.android.core.model.LocationType
import ca.gapwise.android.core.model.Meeting
import ca.gapwise.android.core.model.Term
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek

object MeetingJson {
    fun encodeLocal(meetings: List<Meeting>): String = JSONArray().apply {
        meetings.forEach { put(localObject(it)) }
    }.toString()

    fun decodeLocal(value: String?): List<Meeting> {
        if (value.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(value)
            buildList {
                for (index in 0 until array.length()) add(localMeeting(array.getJSONObject(index)))
            }
        }.getOrDefault(emptyList())
    }

    /** Web-compatible normalized schedule used by Gapwise encrypted account sync. */
    fun toWebArray(meetings: List<Meeting>): JSONArray = JSONArray().apply {
        meetings.forEach { meeting ->
            put(
                JSONObject().apply {
                    put("id", meeting.id)
                    put("courseCode", meeting.courseCode)
                    put("activityType", meeting.activityType.name)
                    put("sectionCode", meeting.sectionCode)
                    put("courseName", meeting.courseName)
                    put("startTime", meeting.startTime)
                    put("endTime", meeting.endTime)
                    put("weekday", webWeekday(meeting.weekday))
                    put("buildingCode", meeting.buildingCode ?: JSONObject.NULL)
                    put("room", meeting.room ?: JSONObject.NULL)
                    put("term", meeting.term.label)
                    put(
                        "locationUnknown",
                        meeting.locationType == LocationType.TBA || meeting.locationType == LocationType.UNKNOWN,
                    )
                    put("campus", meeting.campus.name)
                    meeting.sourceLocation?.takeIf(String::isNotBlank)?.let { put("sourceLocation", it) }
                    put("locationType", meeting.locationType.name.lowercase())
                },
            )
        }
    }

    fun fromWebArray(array: JSONArray): List<Meeting> = buildList {
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            runCatching { webMeeting(item) }.getOrNull()?.let(::add)
        }
    }

    private fun localObject(meeting: Meeting) = JSONObject().apply {
        put("id", meeting.id)
        put("courseCode", meeting.courseCode)
        put("activityType", meeting.activityType.name)
        put("sectionCode", meeting.sectionCode)
        put("courseName", meeting.courseName)
        put("startTime", meeting.startTime)
        put("endTime", meeting.endTime)
        put("weekday", meeting.weekday.name)
        put("term", meeting.term.name)
        put("campus", meeting.campus.name)
        put("sourceLocation", meeting.sourceLocation ?: JSONObject.NULL)
        put("buildingCode", meeting.buildingCode ?: JSONObject.NULL)
        put("room", meeting.room ?: JSONObject.NULL)
        put("locationType", meeting.locationType.name)
    }

    private fun localMeeting(item: JSONObject) = Meeting(
        id = item.getString("id"),
        courseCode = item.getString("courseCode"),
        activityType = ActivityType.valueOf(item.getString("activityType")),
        sectionCode = item.optString("sectionCode"),
        courseName = item.optString("courseName"),
        startTime = item.getInt("startTime"),
        endTime = item.getInt("endTime"),
        weekday = DayOfWeek.valueOf(item.getString("weekday")),
        term = Term.valueOf(item.getString("term")),
        campus = Campus.valueOf(item.getString("campus")),
        sourceLocation = nullableString(item, "sourceLocation"),
        buildingCode = nullableString(item, "buildingCode"),
        room = nullableString(item, "room"),
        locationType = LocationType.valueOf(item.getString("locationType")),
    )

    private fun webMeeting(item: JSONObject): Meeting {
        val courseCode = item.getString("courseCode").uppercase()
        val sourceLocation = nullableString(item, "sourceLocation")
        val locationType = runCatching {
            LocationType.valueOf(item.optString("locationType", "unknown").uppercase())
        }.getOrElse {
            if (item.optBoolean("locationUnknown", false)) LocationType.TBA else LocationType.PHYSICAL
        }
        return Meeting(
            id = item.getString("id"),
            courseCode = courseCode,
            activityType = runCatching {
                ActivityType.valueOf(item.optString("activityType", "OTHER"))
            }.getOrDefault(ActivityType.OTHER),
            sectionCode = item.optString("sectionCode"),
            courseName = item.optString("courseName"),
            startTime = item.getInt("startTime"),
            endTime = item.getInt("endTime"),
            weekday = parseWebWeekday(item.getString("weekday")),
            term = Term.entries.firstOrNull { it.label == item.getString("term") } ?: Term.FALL,
            campus = runCatching { Campus.valueOf(item.optString("campus")) }
                .getOrElse { Campus.fromCourseCode(courseCode) },
            sourceLocation = sourceLocation,
            buildingCode = nullableString(item, "buildingCode"),
            room = nullableString(item, "room"),
            locationType = locationType,
        )
    }

    private fun nullableString(item: JSONObject, name: String): String? =
        if (!item.has(name) || item.isNull(name)) null else item.optString(name).takeIf(String::isNotBlank)

    private fun webWeekday(day: DayOfWeek): String =
        day.name.lowercase().replaceFirstChar(Char::titlecase)

    private fun parseWebWeekday(value: String): DayOfWeek =
        DayOfWeek.valueOf(value.trim().uppercase())
}
