package ca.gapwise.android.core.model

import java.time.DayOfWeek

enum class ActivityType { LEC, TUT, PRA, OTHER }
enum class Term(val label: String) { FALL("Fall"), WINTER("Winter"), SUMMER("Summer") }
enum class LocationType { PHYSICAL, TBA, ONLINE, UNKNOWN }

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
) {
    val locationLabel: String
        get() = when (locationType) {
            LocationType.ONLINE -> "Online"
            LocationType.TBA, LocationType.UNKNOWN -> "Location TBA"
            LocationType.PHYSICAL -> when {
                buildingCode != null && room != null -> "$buildingCode $room"
                !sourceLocation.isNullOrBlank() -> sourceLocation
                else -> "Location TBA"
            }
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
