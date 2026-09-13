package ca.gapwise.android.feature.gapplan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ca.gapwise.android.core.model.Meeting
import ca.gapwise.android.core.model.Term
import ca.gapwise.android.core.model.formatTime
import java.time.DayOfWeek
import java.time.LocalDate

private data class GapPlanItem(
    val from: Meeting,
    val to: Meeting,
    val minutes: Int,
)

@Composable
fun GapPlanScreen(
    meetings: List<Meeting>,
    onImport: () -> Unit,
) {
    if (meetings.isEmpty()) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            "GAP PLAN",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        Text("Make the time between classes count", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Import your ACORN calendar first. Gapwise will find usable gaps between your classes across your timetable.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(onClick = onImport) { Text("Import ACORN calendar") }
                    }
                }
            }
        }
        return
    }

    val terms = remember(meetings) { meetings.map { it.term }.distinct() }
    var selectedTerm by remember(meetings) {
        mutableStateOf(terms.firstOrNull { it == currentTerm() } ?: terms.first())
    }
    val termMeetings = meetings.filter { it.term == selectedTerm }
    val gaps = remember(termMeetings) { calculateGaps(termMeetings) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        "GAP PLAN",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text("Your time between classes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (gaps.isEmpty()) "No positive class-to-class gaps in ${selectedTerm.label}." else "${gaps.size} usable ${if (gaps.size == 1) "gap" else "gaps"} found in ${selectedTerm.label}.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (terms.size > 1) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(terms) { term ->
                                FilterChip(
                                    selected = term == selectedTerm,
                                    onClick = { selectedTerm = term },
                                    label = { Text(term.label) },
                                )
                            }
                        }
                    }
                }
            }
        }

        items(gaps, key = { "${it.from.id}-${it.to.id}" }) { gap ->
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        gap.from.weekday.name.lowercase().replaceFirstChar(Char::titlecase),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        durationLabel(gap.minutes),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "${gap.from.courseCode} → ${gap.to.courseCode}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "${formatTime(gap.from.endTime)} – ${formatTime(gap.to.startTime)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "${gap.from.locationLabel} → ${gap.to.locationLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
        dayMeetings
            .sortedBy { it.startTime }
            .zipWithNext()
            .mapNotNull { (from, to) ->
                val minutes = to.startTime - from.endTime
                if (minutes > 0) GapPlanItem(from = from, to = to, minutes = minutes) else null
            }
    }

private fun durationLabel(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} hr"
    else -> "${minutes / 60} hr ${minutes % 60} min"
}

private fun currentTerm(): Term = when (LocalDate.now().monthValue) {
    in 1..4 -> Term.WINTER
    in 5..8 -> Term.SUMMER
    else -> Term.FALL
}
