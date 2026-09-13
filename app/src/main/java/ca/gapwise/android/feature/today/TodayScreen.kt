package ca.gapwise.android.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ca.gapwise.android.core.model.Meeting
import ca.gapwise.android.core.model.formatTime
import java.time.LocalDate

@Composable
fun TodayScreen(
    meetings: List<Meeting>,
    importStatus: String?,
    onImport: () -> Unit,
) {
    val todayMeetings = meetings
        .filter { it.weekday == LocalDate.now().dayOfWeek }
        .sortedBy { it.startTime }

    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "GAPWISE FOR U OF T",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${LocalDate.now().dayOfWeek.name.lowercase().replaceFirstChar(Char::titlecase)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = when {
                            meetings.isEmpty() -> "Import your ACORN calendar to see your day."
                            todayMeetings.isEmpty() -> "Nothing scheduled today."
                            todayMeetings.size == 1 -> "1 class today"
                            else -> "${todayMeetings.size} classes today"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (meetings.isEmpty()) {
                        Button(onClick = onImport) {
                            Text("Import ACORN calendar")
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
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
            }
        }

        items(todayMeetings, key = { it.id }) { meeting ->
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = meeting.courseCode,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${formatTime(meeting.startTime)} – ${formatTime(meeting.endTime)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "${meeting.locationLabel} · ${meeting.campus.shortName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
