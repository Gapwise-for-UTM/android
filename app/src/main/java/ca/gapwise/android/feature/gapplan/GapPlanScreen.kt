package ca.gapwise.android.feature.gapplan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

private data class GapPlanItem(
    val from: Meeting,
    val to: Meeting,
    val minutes: Int,
) {
    val usableMinutes: Int get() = (minutes - 17).coerceAtLeast(0)
}

@Composable
fun GapPlanScreen(
    meetings: List<Meeting>,
    onImport: () -> Unit,
) {
    if (meetings.isEmpty()) {
        EmptyGaps(onImport)
        return
    }

    val selectedTerm = currentTerm()
    val termMeetings = meetings.filter { it.term == selectedTerm }
    val gaps = remember(termMeetings) { calculateGaps(termMeetings) }
    val groups = remember(gaps) { gaps.groupBy { it.from.weekday }.toSortedMap(compareBy(DayOfWeek::getValue)) }
    val totalUsable = gaps.sumOf { it.usableMinutes }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                WebEyebrow("Between classes", accent = false)
                Text(
                    text = "Gap plan",
                    modifier = Modifier.padding(top = 5.dp),
                    fontSize = 24.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.6).sp,
                )
                Text(
                    text = "See what actually fits after walking time, setup, and the buffer before your next class.",
                    modifier = Modifier.padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                )
                Row(
                    modifier = Modifier.padding(top = 13.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = { },
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Icon(imageVector = Icons.Outlined.Tune, contentDescription = null)
                        Text("Tune", modifier = Modifier.padding(start = 6.dp), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick = { },
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Icon(imageVector = Icons.Outlined.PeopleOutline, contentDescription = null)
                        Text("Friend gaps", modifier = Modifier.padding(start = 6.dp), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                GapStats(gaps = gaps.size, usableMinutes = totalUsable, days = groups.size)
                HorizontalDivider(
                    modifier = Modifier.padding(top = 15.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }

        if (groups.isEmpty()) {
            item {
                WebSurface(padding = PaddingValues(horizontal = 18.dp, vertical = 30.dp)) {
                    Text("No gaps in this term", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Your scheduled classes are back to back, or there is only one class on each day.",
                        modifier = Modifier.padding(top = 6.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                    )
                }
            }
        }

        groups.forEach { (weekday, dayGaps) ->
            item(key = weekday) {
                DayGapSection(weekday = weekday, gaps = dayGaps)
            }
        }
    }
}

@Composable
private fun GapStats(gaps: Int, usableMinutes: Int, days: Int) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        shape = RoundedCornerShape(9.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row {
            GapStat("Gaps", gaps.toString(), Modifier.weight(1f))
            GapStat("Usable", compactDuration(usableMinutes), Modifier.weight(1f), divider = true)
            GapStat("Days", days.toString(), Modifier.weight(1f), divider = true)
        }
    }
}

@Composable
private fun GapStat(label: String, value: String, modifier: Modifier, divider: Boolean = false) {
    Row(modifier = modifier) {
        if (divider) androidx.compose.material3.VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(modifier = Modifier.padding(horizontal = 11.dp, vertical = 12.dp)) {
            WebEyebrow(label, accent = false)
            Text(value, modifier = Modifier.padding(top = 4.dp), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun DayGapSection(weekday: DayOfWeek, gaps: List<GapPlanItem>) {
    val selected = gaps.first()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(weekday.displayName(), fontSize = 17.sp, fontWeight = FontWeight.Medium)
                Text(
                    "${gaps.size} ${if (gaps.size == 1) "gap" else "gaps"}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                )
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                shape = RoundedCornerShape(9.dp),
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.55f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column {
                        Text(
                            "${formatTime(selected.from.endTime)}–${formatTime(selected.to.startTime)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            recommendationTitle(selected),
                            modifier = Modifier.padding(top = 4.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "${compactDuration(selected.usableMinutes)} usable",
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.5.sp,
                        )
                    }
                    Text(compactDuration(selected.minutes), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.5.sp)
                }
            }

            RecommendationInspector(selected)
        }
    }
}

@Composable
private fun RecommendationInspector(gap: GapPlanItem) {
    val accent = MaterialTheme.colorScheme.tertiary
    val lunch = recommendationTitle(gap).startsWith("Lunch")
    val protected = if (lunch) 30 else minOf(60, gap.usableMinutes)
    val remaining = (gap.usableMinutes - protected).coerceAtLeast(0)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        shape = RoundedCornerShape(9.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(11.dp), verticalAlignment = Alignment.Top) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accent.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.22f)),
                ) {
                    Icon(
                        imageVector = if (lunch) Icons.Outlined.Restaurant else Icons.Outlined.Route,
                        contentDescription = null,
                        modifier = Modifier.padding(9.dp),
                        tint = accent,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    WebEyebrow("Recommended", accent = false)
                    Text(
                        recommendationTitle(gap),
                        modifier = Modifier.padding(top = 5.dp),
                        fontSize = 19.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = if (lunch) {
                            "30 min protected for eating, with ${compactDuration(remaining)} left for studying or resting."
                        } else {
                            "${compactDuration(protected)} focused work fits, with ${compactDuration(remaining)} left to reset before class."
                        },
                        modifier = Modifier.padding(top = 5.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 19.sp,
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .height(5.dp),
                shape = RoundedCornerShape(99.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Row {
                    androidx.compose.foundation.layout.Spacer(
                        modifier = Modifier
                            .weight(gap.usableMinutes.coerceAtLeast(1).toFloat())
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(99.dp))
                            .background(accent),
                    )
                    androidx.compose.foundation.layout.Spacer(
                        modifier = Modifier.weight((gap.minutes - gap.usableMinutes).coerceAtLeast(1).toFloat()),
                    )
                }
            }

            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                Text("${compactDuration(gap.usableMinutes)} usable", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.5.sp)
                Text("7 min buffer", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.5.sp)
                Text("3 min travel", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.5.sp)
            }

            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                ChoiceChip(if (lunch) "Eat" else "Study", selected = true)
                ChoiceChip("Flexible", selected = false)
                ChoiceChip(if (lunch) "Study" else "Rest", selected = false)
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 13.dp),
                shape = RoundedCornerShape(9.dp),
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Outlined.Route, contentDescription = null, tint = accent)
                        Text("Transition", modifier = Modifier.padding(start = 7.dp), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        "${gap.from.locationLabel} → ${gap.to.courseCode} · ${gap.to.locationLabel}",
                        modifier = Modifier.padding(top = 8.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        lineHeight = 17.sp,
                    )
                    Text(
                        "Leave by ${formatTime(gap.to.startTime - 10)}",
                        modifier = Modifier.padding(top = 5.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (selected) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f) else Color.Transparent,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun EmptyGaps(onImport: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        item {
            WebSurface(padding = PaddingValues(20.dp)) {
                WebEyebrow("Between classes", accent = false)
                Text("Gap plan", modifier = Modifier.padding(top = 6.dp), fontSize = 24.sp, fontWeight = FontWeight.Medium)
                Text(
                    "Import your ACORN calendar first. Gapwise will find usable gaps between your classes.",
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
    }
}

private fun calculateGaps(meetings: List<Meeting>): List<GapPlanItem> = meetings
    .filterNot { it.isAssessmentWindow }
    .groupBy { it.weekday }
    .toSortedMap(compareBy(DayOfWeek::getValue))
    .flatMap { (_, dayMeetings) ->
        dayMeetings.sortedBy { it.startTime }.zipWithNext().mapNotNull { (from, to) ->
            val minutes = to.startTime - from.endTime
            if (minutes > 0) GapPlanItem(from, to, minutes) else null
        }
    }

private fun recommendationTitle(gap: GapPlanItem): String =
    if (gap.from.endTime <= 13 * 60 && gap.to.startTime >= 11 * 60 + 30 && gap.usableMinutes >= 30) {
        "Lunch fits comfortably"
    } else {
        "Study block fits comfortably"
    }

private fun compactDuration(minutes: Int): String = when {
    minutes <= 0 -> "0m"
    minutes < 60 -> "${minutes}m"
    minutes % 60 == 0 -> "${minutes / 60} hr"
    else -> "${minutes / 60} hr ${minutes % 60}m"
}

private fun DayOfWeek.displayName(): String = name.lowercase().replaceFirstChar(Char::titlecase)

private fun currentTerm(): Term = when (LocalDate.now().monthValue) {
    in 1..4 -> Term.WINTER
    in 5..8 -> Term.SUMMER
    else -> Term.FALL
}
