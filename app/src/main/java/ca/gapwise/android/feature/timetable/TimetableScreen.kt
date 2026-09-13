package ca.gapwise.android.feature.timetable

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.gapwise.android.core.designsystem.WebControlShape
import ca.gapwise.android.core.designsystem.WebEyebrow
import ca.gapwise.android.core.designsystem.WebSegmentButton
import ca.gapwise.android.core.designsystem.WebSurface
import ca.gapwise.android.core.model.ActivityType
import ca.gapwise.android.core.model.Meeting
import ca.gapwise.android.core.model.Term
import ca.gapwise.android.core.model.formatTime
import java.time.DayOfWeek
import java.time.LocalDate

private val BaseDays = listOf(
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
)

private data class TimetableGap(
    val previous: Meeting,
    val next: Meeting,
) {
    val startTime: Int get() = previous.endTime
    val endTime: Int get() = next.startTime
    val durationMinutes: Int get() = endTime - startTime
}

@Composable
fun TimetableScreen(
    meetings: List<Meeting>,
    importStatus: String?,
    onImport: () -> Unit,
    onOpenGapPlan: () -> Unit = {},
) {
    if (meetings.isEmpty()) {
        EmptyTimetable(importStatus = importStatus, onImport = onImport)
        return
    }

    val terms = remember(meetings) { meetings.map { it.term }.distinct() }
    var selectedTerm by remember(meetings) {
        mutableStateOf(terms.firstOrNull { it == currentTerm() } ?: terms.first())
    }
    val termMeetings = meetings.filter { it.term == selectedTerm }
    val visibleDays = remember(termMeetings) {
        BaseDays + listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).filter { weekend ->
            termMeetings.any { it.weekday == weekend }
        }
    }
    var selectedDay by remember(selectedTerm, termMeetings) {
        mutableStateOf(
            LocalDate.now().dayOfWeek.takeIf { day -> termMeetings.any { it.weekday == day } }
                ?: visibleDays.firstOrNull { day -> termMeetings.any { it.weekday == day } }
                ?: DayOfWeek.MONDAY,
        )
    }
    val dayMeetings = termMeetings.filter { it.weekday == selectedDay }.sortedBy { it.startTime }
    val dayClasses = dayMeetings.filterNot { it.isAssessmentWindow }
    val reservedCount = dayMeetings.size - dayClasses.size
    val gapAfterMeeting = remember(dayClasses) {
        dayClasses
            .zipWithNext()
            .mapNotNull { (previous, next) ->
                if (next.startTime > previous.endTime) TimetableGap(previous, next) else null
            }
            .associateBy { it.previous.id }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            WebSurface {
                WebEyebrow("Day timetable")
                Text(
                    text = selectedDay.displayName(),
                    modifier = Modifier.padding(top = 6.dp),
                    fontSize = 24.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.7).sp,
                )
                Text(
                    text = when {
                        dayMeetings.isEmpty() -> "Nothing scheduled in ${selectedTerm.label}"
                        dayClasses.isEmpty() -> "0 classes · $reservedCount ${if (reservedCount == 1) "reserved window" else "reserved windows"}"
                        else -> buildString {
                            append("${dayClasses.size} ${if (dayClasses.size == 1) "class" else "classes"}")
                            if (reservedCount > 0) append(" · $reservedCount reserved")
                            append(" · ${formatTime(dayClasses.first().startTime)} – ${formatTime(dayClasses.last().endTime)}")
                        }
                    },
                    modifier = Modifier.padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )

                if (terms.size > 1) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        shape = WebControlShape,
                        color = MaterialTheme.colorScheme.background,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shadowElevation = 0.dp,
                    ) {
                        Row(modifier = Modifier.padding(3.dp)) {
                            terms.forEach { term ->
                                WebSegmentButton(
                                    label = term.label,
                                    selected = selectedTerm == term,
                                    onClick = { selectedTerm = term },
                                    modifier = Modifier.weight(1f),
                                    minHeight = 38.dp,
                                )
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    shape = WebControlShape,
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shadowElevation = 0.dp,
                ) {
                    Row(modifier = Modifier.padding(3.dp)) {
                        visibleDays.forEach { day ->
                            val dayEvents = termMeetings.filter { it.weekday == day }
                            val reserved = dayEvents.count { it.isAssessmentWindow }
                            val count = dayEvents.size - reserved
                            WebSegmentButton(
                                label = day.shortName(),
                                secondaryLabel = buildString {
                                    append(if (count == 0) "–" else count.toString())
                                    if (reserved > 0) append(" · R$reserved")
                                },
                                selected = selectedDay == day,
                                onClick = { selectedDay = day },
                                modifier = Modifier.weight(1f),
                                minHeight = 48.dp,
                            )
                        }
                    }
                }
            }
        }

        if (importStatus != null) {
            item {
                Text(
                    text = importStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (dayMeetings.isEmpty()) {
            item {
                WebSurface(
                    padding = PaddingValues(horizontal = 20.dp, vertical = 34.dp),
                ) {
                    Text(
                        text = "Your ${selectedDay.displayName()} is clear",
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Pick another day to review the classes in your imported ACORN schedule.",
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                    )
                }
            }
        } else {
            item {
                WebSurface(padding = PaddingValues(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        dayMeetings.forEach { meeting ->
                            MeetingCard(meeting)
                            gapAfterMeeting[meeting.id]?.let { gap ->
                                GapCard(gap = gap, onClick = onOpenGapPlan)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MeetingCard(meeting: Meeting) {
    val accent = activityAccent(meeting)
    val location = locationPresentation(meeting)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = meeting.courseCode,
                    color = accent,
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Surface(
                    modifier = Modifier.padding(start = 8.dp),
                    shape = RoundedCornerShape(5.dp),
                    color = accent.copy(alpha = 0.11f),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.32f)),
                ) {
                    Text(
                        text = meeting.activityLabel,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        color = accent,
                        fontSize = 9.5.sp,
                        lineHeight = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 6.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier.padding(top = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccessTime,
                    contentDescription = null,
                    tint = accent,
                )
                Text(
                    text = "${formatTime(meeting.startTime)} – ${formatTime(meeting.endTime)}",
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Row(
                modifier = Modifier.padding(top = 6.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = if (meeting.isAssessmentWindow) Icons.Outlined.AccessTime else Icons.Outlined.Place,
                    contentDescription = null,
                    tint = accent,
                )
                Column {
                    Text(
                        text = location.first,
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    location.second?.let { floor ->
                        Text(
                            text = floor,
                            modifier = Modifier.padding(top = 2.dp),
                            color = if (meeting.isAssessmentWindow) accent.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            if (meeting.courseName.isNotBlank()) {
                Text(
                    text = meeting.courseName,
                    modifier = Modifier.padding(top = 9.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
private fun GapCard(gap: TimetableGap, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.tertiary
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = accent.copy(alpha = 0.045f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.30f)),
        shadowElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = accent,
                )
                Text(
                    text = "${compactGapDuration(gap.durationMinutes)} gap",
                    modifier = Modifier.padding(start = 7.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "View gap plan",
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                    color = accent,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = "${formatTime(gap.startTime)} – ${formatTime(gap.endTime)}",
                modifier = Modifier.padding(top = 5.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.5.sp,
            )
        }
    }
}

@Composable
private fun activityAccent(meeting: Meeting): Color = when {
    meeting.isAssessmentWindow -> Color(0xFFF2C94C)
    meeting.activityType == ActivityType.PRA -> Color(0xFFA78BFA)
    meeting.activityType == ActivityType.TUT -> Color(0xFF2EC4B6)
    meeting.activityType == ActivityType.LEC -> MaterialTheme.colorScheme.tertiary
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun locationPresentation(meeting: Meeting): Pair<String, String?> {
    if (meeting.isAssessmentWindow) return "Reserved assessment window" to "Only active when announced"
    val code = meeting.buildingCode
    val room = meeting.room
    val building = when (code) {
        "MN" -> "Maanjiwe nendamowinan"
        "DH" -> "Deerfield Hall"
        "IB" -> "Instructional Centre"
        "DV" -> "William G. Davis Building"
        "CCT" -> "Communication, Culture and Technology Building"
        "HM" -> "Hazel McCallion Academic Learning Centre"
        "KN" -> "Kaneff Centre"
        "RAWC" -> "Recreation, Athletics and Wellness Centre"
        "XR" -> "Student Centre"
        "HB" -> "Terrence Donnelly Health Sciences Complex"
        else -> null
    }
    val compact = when {
        building != null && room != null -> "$building · $room"
        building != null -> building
        else -> meeting.locationLabel
    }
    val floor = room?.firstOrNull()?.digitToIntOrNull()?.takeIf { it > 0 }?.let { value ->
        val suffix = when (value) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
        "$value$suffix floor"
    }
    return compact to floor
}

@Composable
private fun EmptyTimetable(importStatus: String?, onImport: () -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            WebSurface(padding = PaddingValues(20.dp)) {
                WebEyebrow("Timetable import")
                Text(
                    text = "Add your timetable",
                    modifier = Modifier.padding(top = 7.dp),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "Import your ACORN .ics calendar. UTM, St. George, Scarborough, and mixed-campus schedules are handled in one timetable.",
                    modifier = Modifier.padding(top = 9.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                )
                Button(
                    onClick = onImport,
                    modifier = Modifier.padding(top = 14.dp),
                    shape = RoundedCornerShape(9.dp),
                ) {
                    Text("Import ACORN calendar")
                }
            }
        }
        if (importStatus != null) {
            item {
                Text(importStatus, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun compactGapDuration(minutes: Int): String = when {
    minutes < 60 -> "${minutes}m"
    minutes % 60 == 0 -> "${minutes / 60}h"
    else -> "${minutes / 60}h ${minutes % 60}m"
}

private fun DayOfWeek.displayName(): String = name.lowercase().replaceFirstChar(Char::titlecase)
private fun DayOfWeek.shortName(): String = name.take(3).lowercase().replaceFirstChar(Char::titlecase)

private fun currentTerm(): Term = when (LocalDate.now().monthValue) {
    in 1..4 -> Term.WINTER
    in 5..8 -> Term.SUMMER
    else -> Term.FALL
}
