package ca.gapwise.android.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ca.gapwise.android.core.model.Meeting
import ca.gapwise.android.core.persistence.AppThemeMode
import ca.gapwise.android.data.account.AccountIdentity
import ca.gapwise.android.data.account.AuthProvider

@Composable
fun SettingsScreen(
    meetings: List<Meeting>,
    importStatus: String?,
    themeMode: AppThemeMode,
    account: AccountIdentity?,
    syncEnabled: Boolean,
    syncBusy: Boolean,
    syncStatus: String?,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onImport: () -> Unit,
    onClearTimetable: () -> Unit,
    onSignIn: (AuthProvider) -> Unit,
    onSignOut: () -> Unit,
    onSetSyncEnabled: (Boolean) -> Unit,
    onSyncNow: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        PreferenceSection("Preferences") {
            Text("Appearance", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (themeMode == AppThemeMode.LIGHT) {
                    Button(onClick = { onThemeModeChange(AppThemeMode.LIGHT) }) { Text("Light") }
                    OutlinedButton(onClick = { onThemeModeChange(AppThemeMode.DARK) }) { Text("Dark") }
                } else {
                    OutlinedButton(onClick = { onThemeModeChange(AppThemeMode.LIGHT) }) { Text("Light") }
                    Button(onClick = { onThemeModeChange(AppThemeMode.DARK) }) { Text("Dark") }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        PreferenceSection("Account") {
            if (account == null) {
                Text(
                    "Sign in to sync your encrypted Gapwise private state between the web app and Android.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AuthProvider.entries.forEach { provider ->
                        OutlinedButton(
                            onClick = { onSignIn(provider) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(provider.label, maxLines = 1)
                        }
                    }
                }
            } else {
                Text(
                    account.email ?: "Signed in to Gapwise",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Encrypted account sync", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Sync normalized private state with the same encrypted cloud used by gapwise.ca.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = syncEnabled,
                        onCheckedChange = onSetSyncEnabled,
                        enabled = !syncBusy,
                    )
                }
                if (syncStatus != null) {
                    Text(syncStatus, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onSyncNow, enabled = syncEnabled && !syncBusy) {
                        if (syncBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(end = 8.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                        Text(if (syncBusy) "Syncing…" else "Sync now")
                    }
                    OutlinedButton(onClick = onSignOut, enabled = !syncBusy) { Text("Sign out") }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        PreferenceSection("Timetable") {
            Text(
                if (meetings.isEmpty()) "No timetable imported" else "${meetings.size} class meetings saved on this device",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (importStatus != null) {
                Text(importStatus, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
                Text(if (meetings.isEmpty()) "Import ACORN calendar" else "Update timetable")
            }
            if (meetings.isNotEmpty()) {
                OutlinedButton(onClick = onClearTimetable, modifier = Modifier.fillMaxWidth()) {
                    Text("Remove timetable")
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        PreferenceSection("Privacy") {
            Text(
                "Calendar parsing happens on-device. Gapwise stores only the normalized timetable in encrypted app-private storage; the original .ics file is never uploaded. Account sync is optional and encrypts private state before cloud storage.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "Timetables support UTM, St. George, Scarborough, and mixed-campus schedules. Campus mapping remains UTM-focused.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 18.dp),
            )
        }
    }
}

@Composable
private fun PreferenceSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
        )
        content()
    }
}
