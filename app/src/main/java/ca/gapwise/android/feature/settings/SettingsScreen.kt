package ca.gapwise.android.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ca.gapwise.android.core.model.Campus
import ca.gapwise.android.core.model.Meeting

@Composable
fun SettingsScreen(
    meetings: List<Meeting>,
    importStatus: String?,
    onImport: () -> Unit,
    onClearTimetable: () -> Unit,
) {
    val campusSummary = Campus.entries.mapNotNull { campus ->
        meetings.count { it.campus == campus }.takeIf { it > 0 }?.let { count -> "${campus.shortName} $count" }
    }

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
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        "TIMETABLE DATA",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (meetings.isEmpty()) "No timetable imported" else "${meetings.size} class meetings imported",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (campusSummary.isNotEmpty()) {
                        Text(campusSummary.joinToString(" · "), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (importStatus != null) {
                        Text(importStatus, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onImport) {
                            Icon(Icons.Outlined.FileOpen, contentDescription = null)
                            Text("Import", modifier = Modifier.padding(start = 8.dp))
                        }
                        if (meetings.isNotEmpty()) {
                            OutlinedButton(onClick = onClearTimetable) {
                                Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
                                Text("Clear", modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            }
        }

        item {
            SettingsSection(title = "Privacy") {
                SettingsRow(
                    icon = Icons.Outlined.Lock,
                    title = "Private timetable import",
                    detail = "Your ACORN file is parsed on-device. Gapwise does not upload the calendar just to build your timetable.",
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsRow(
                    icon = Icons.Outlined.CloudOff,
                    title = "No account required",
                    detail = "The native timetable and gap plan work without signing in. The campus map only downloads map tiles over HTTPS.",
                )
            }
        }

        item {
            SettingsSection(title = "Campus support") {
                SettingsRow(
                    icon = Icons.Outlined.Map,
                    title = "Gapwise for U of T",
                    detail = "Timetables support UTM, St. George, Scarborough, and mixed-campus schedules. The native interactive map is intentionally UTM-only for now.",
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
            )
            content()
        }
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    detail: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
