package ca.gapwise.android.core.model

import java.time.DayOfWeek

enum class ActivityType { LEC, TUT, PRA, RES, OTHER }
enum class Term(val label: String) { FALL("Fall"), WINTER("Winter"), SUMMER("Summer") }
enum class LocationType { PHYSICAL, TBA, ONLINE, UNKNOWN }

const val ASSESSMENT_WINDOW_NOTE = "Reserved assessment window"

data class Meeting(
    val id: String,
    val courseCode: String,
    val activityType: ActivityType,
    val sectionCode: String,
    val courseName: String,
    val startTime: Int,
    val endTime: Int,
    val weekday: DayOfWeek,
    val term: Term,
    val campus: Campus,
    val sourceLocation: String?,
    val buildingCode: String?,
    val room: String?,
    val locationType: LocationType,
    val notes: String? = null,
) {
    val isAssessmentWindow: Boolean
        get() = notes == ASSESSMENT_WINDOW_NOTE

    val activityLabel: String
        get() = if (isAssessmentWindow) "RES" else activityType.name

    val locationLabel: String
        get() = when {
            isAssessmentWindow -> "Reserved assessment window · location TBA"
            locationType == LocationType.ONLINE -> "Online"
            locationType == LocationType.TBA || locationType == LocationType.UNKNOWN -> "Location TBA"
            buildingCode != null && room != null -> "$buildingCode $room"
            !sourceLocation.isNullOrBlank() -> sourceLocation
            else -> "Location TBA"
        }
}

fun formatTime(minutes: Int): String {
    val h24 = minutes / 60
    val minute = minutes % 60
    val suffix = if (h24 >= 12) "PM" else "AM"
    val hour = when (val value = h24 % 12) {
        0 -> 12
        else -> value
    }
    return "%d:%02d %s".format(hour, minute, suffix)
}
