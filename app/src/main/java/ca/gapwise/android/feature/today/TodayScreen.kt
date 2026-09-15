package ca.gapwise.android.feature.today

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.gapwise.android.core.designsystem.WebEyebrow
import ca.gapwise.android.core.designsystem.WebSurface
import ca.gapwise.android.core.model.Meeting
import ca.gapwise.android.core.model.Term
import ca.gapwise.android.core.model.formatTime
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun TodayScreen(
    meetings: List<Meeting>,
    importStatus: String?,
    onImport: () -> Unit,
    onDayRoute: () -> Unit,
) {
    if (meetings.isEmpty()) {
        EmptyToday(importStatus = importStatus, onImport = onImport)
        return
    }

    val today = LocalDate.now()
    val currentTerm = termForMonth(today.monthValue)
    val termMeetings = meetings
        .filter { it.term == currentTerm && !it.isAssessmentWindow }
        .sortedWith(compareBy<Meeting>({ it.weekday.value }, { it.startTime }))
    val todayMeetings = termMeetings.filter { it.weekday == today.dayOfWeek }.sortedBy { it.startTime }
    val now = LocalTime.now()
    val nowMinutes = now.hour * 60 + now.minute
    val active = todayMeetings.firstOrNull { nowMinutes in it.startTime until it.endTime }
    val nextToday = todayMeetings.firstOrNull { it.startTime > nowMinutes }
    val nextOccurrence = findNextOccurrence(termMeetings, today.dayOfWeek)

    val gapsToday = calculateGaps(todayMeetings)
    val termGapCount = DayOfWeek.entries.sumOf { day -> calculateGaps(termMeetings.filter { it.weekday == day }).size }
    val scheduledMinutes = todayMeetings.sumOf { (it.endTime - it.startTime).coerceAtLeast(0) }
    val openMinutes = if (todayMeetings.size > 1) {
        (todayMeetings.last().endTime - todayMeetings.first().startTime - scheduledMinutes).coerceAtLeast(0)
    } else {
        0
    }

    val primaryTitle: String
    val primaryDetail: String
    when {
        active != null -> {
            primaryTitle = "Now: ${active.courseCode}"
            primaryDetail = active.courseName.ifBlank { "Until ${formatTime(active.endTime)}" }
        }
        nextToday != null -> {
            primaryTitle = "Next: ${nextToday.courseCode}"
            primaryDetail = "${formatTime(nextToday.startTime)} · starts in ${compactDuration(nextToday.startTime - nowMinutes)}"
        }
        todayMeetings.isNotEmpty() -> {
            primaryTitle = "Done for today"
            primaryDetail = nextOccurrence?.let { occurrenceLead(it) } ?: "No more classes are scheduled in this term."
        }
        else -> {
            primaryTitle = "No classes today"
            primaryDetail = nextOccurrence?.let { occurrenceLead(it) } ?: "No later classes are scheduled in this term."
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            WebSurface(padding = PaddingValues(17.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                    WebEyebrow(
                        text = "Today · ${today.dayOfWeek.displayName()}",
                        modifier = Modifier.padding(start = 7.dp),
                        accent = false,
                    )
                }
                Text(
                    text = primaryTitle,
                    modifier = Modifier.padding(top = 11.dp),
                    fontSize = 27.sp,
                    lineHeight = 29.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.9).sp,
                )
                Text(
                    text = primaryDetail,
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )

                val focusMeeting = active ?: nextToday
                if (focusMeeting != null) {
                    HorizontalDivider(
                        modifier = Modifier.padding(top = 14.dp, bottom = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                        )
                        Text(
                            text = focusMeeting.locationLabel,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                        )
                    }
                }

                Button(
                    onClick = onDayRoute,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(9.dp),
                ) {
                    Icon(imageVector = Icons.Outlined.Navigation, contentDescription = null)
                    Text("Day route", modifier = Modifier.padding(start = 7.dp), fontWeight = FontWeight.SemiBold)
                }
            }
        }

        item {
            TodayStats(
                classes = todayMeetings.size,
                planned = 0,
                gaps = gapsToday.size,
                openMinutes = openMinutes,
            )
        }

        item {
            Column {
                WebEyebrow("My day", accent = false)
                Text(
                    text = "Timeline",
                    modifier = Modifier.padding(top = 5.dp),
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        item {
            if (todayMeetings.isEmpty()) {
                WebSurface(padding = PaddingValues(horizontal = 18.dp, vertical = 30.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircleOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Nothing scheduled today",
                        modifier = Modifier.padding(top = 13.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Your day is open.",
                        modifier = Modifier.padding(top = 5.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column {
                        todayMeetings.forEachIndexed { index, meeting ->
                            TimelineRow(
                                meeting = meeting,
                                active = nowMinutes in meeting.startTime until meeting.endTime,
                                passed = nowMinutes >= meeting.endTime,
                            )
                            if (index != todayMeetings.lastIndex) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "${termMeetings.size} meetings in ${currentTerm.label} · $termGapCount ${if (termGapCount == 1) "gap" else "gaps"} in the term",
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.5.sp,
                lineHeight = 14.sp,
            )
        }

        if (importStatus != null) {
            item {
                Text(importStatus, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TodayStats(classes: Int, planned: Int, gaps: Int, openMinutes: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row {
            StatCell("Classes", classes.toString(), Modifier.weight(1f))
            StatCell("Planned", planned.toString(), Modifier.weight(1f), divider = true)
            StatCell("Gaps", gaps.toString(), Modifier.weight(1f), divider = true)
            StatCell("Open", compactDuration(openMinutes), Modifier.weight(1f), divider = true)
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier, divider: Boolean = false) {
    Row(modifier = modifier) {
        if (divider) {
            androidx.compose.material3.VerticalDivider(
                modifier = Modifier.padding(vertical = 0.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.5.sp, maxLines = 1)
            Text(value, modifier = Modifier.padding(top = 4.dp), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun TimelineRow(meeting: Meeting, active: Boolean, passed: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Column(modifier = Modifier.padding(top = 1.dp)) {
            Text(formatTime(meeting.startTime), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(
                formatTime(meeting.endTime),
                modifier = Modifier.padding(top = 3.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(meeting.courseCode, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Surface(
                    modifier = Modifier.padding(start = 7.dp),
                    shape = RoundedCornerShape(5.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Text(
                        meeting.activityLabel,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (active || passed) {
                    Text(
                        text = if (active) "Now" else "Done",
                        modifier = Modifier.padding(start = 7.dp),
                        color = if (active) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            if (meeting.courseName.isNotBlank()) {
                Text(
                    meeting.courseName,
                    modifier = Modifier.padding(top = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                )
            }
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(imageVector = Icons.Outlined.Place, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(meeting.locationLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, maxLines = 1)
            }
        }
    }
}

private data class NextOccurrence(val meeting: Meeting, val daysAway: Int)

private fun findNextOccurrence(meetings: List<Meeting>, today: DayOfWeek): NextOccurrence? = meetings
    .map { meeting ->
        val delta = (meeting.weekday.value - today.value + 7) % 7
        NextOccurrence(meeting, if (delta == 0) 7 else delta)
    }
    .minWithOrNull(compareBy<NextOccurrence>({ it.daysAway }, { it.meeting.startTime }))

private fun occurrenceLead(next: NextOccurrence): String {
    val whenLabel = when (next.daysAway) {
        1 -> "Tomorrow"
        else -> next.meeting.weekday.displayName()
    }
    return "$whenLabel starts at ${formatTime(next.meeting.startTime)} in ${next.meeting.locationLabel}"
}

private fun calculateGaps(meetings: List<Meeting>): List<Int> = meetings
    .sortedBy { it.startTime }
    .zipWithNext()
    .mapNotNull { (from, to) -> (to.startTime - from.endTime).takeIf { it > 0 } }

private fun compactDuration(minutes: Int): String = when {
    minutes <= 0 -> "0m"
    minutes < 60 -> "${minutes}m"
    minutes % 60 == 0 -> "${minutes / 60}h"
    else -> "${minutes / 60}h ${minutes % 60}m"
}

@Composable
private fun EmptyToday(importStatus: String?, onImport: () -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            WebSurface(padding = PaddingValues(20.dp)) {
                WebEyebrow("Today")
                Text("Your day starts with your timetable", modifier = Modifier.padding(top = 7.dp), fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Import your ACORN calendar to see what is next, your gaps, and your route.",
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                )
                Button(onClick = onImport, modifier = Modifier.padding(top = 14.dp), shape = RoundedCornerShape(9.dp)) {
                    Text("Import ACORN calendar")
                }
            }
        }
        if (importStatus != null) item { Text(importStatus, style = MaterialTheme.typography.bodySmall) }
    }
}

private fun DayOfWeek.displayName(): String = name.lowercase().replaceFirstChar(Char::titlecase)

private fun termForMonth(month: Int): Term = when (month) {
    in 1..4 -> Term.WINTER
    in 5..8 -> Term.SUMMER
    else -> Term.FALL
}
