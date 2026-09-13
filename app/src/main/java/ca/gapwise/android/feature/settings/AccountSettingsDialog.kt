package ca.gapwise.android.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ca.gapwise.android.core.model.Meeting
import ca.gapwise.android.data.account.AccountIdentity
import ca.gapwise.android.data.account.AuthProvider

private const val GAPWISE_TIMETABLE = "https://gapwise.ca/timetable"
private const val GAPWISE_ROUTE = "https://gapwise.ca/route"
private const val GAPWISE_AI = "https://ai.gapwise.ca"
private const val GAPWISE_MCP = "https://ai.gapwise.ca/api/mcp"

enum class AccountSettingsTab(val label: String) { ACCOUNT("Account"), EXPORTS("Exports"), AI("AI integrations") }

@Composable
fun AccountSettingsDialog(
    open: Boolean,
    account: AccountIdentity?,
    meetings: List<Meeting>,
    syncEnabled: Boolean,
    syncBusy: Boolean,
    syncStatus: String?,
    onDismiss: () -> Unit,
    onSignIn: (AuthProvider) -> Unit,
    onSignOut: () -> Unit,
    onSetSyncEnabled: (Boolean) -> Unit,
    onSyncNow: () -> Unit,
    onLoadSync: () -> Unit,
    onDeleteSync: () -> Unit,
    onDeleteAccount: () -> Unit,
) {
    if (!open) return
    val context = LocalContext.current
    var tab by remember { mutableStateOf(AccountSettingsTab.ACCOUNT) }
    var deleteSync by remember { mutableStateOf(false) }
    var deleteAccount by remember { mutableStateOf(false) }
    fun openUrl(url: String) { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth(.92f).heightIn(max = 700.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 18.dp, top = 16.dp, end = 10.dp, bottom = 14.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Settings", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Manage sync, device storage, exports, and Gapwise AI.",
                            modifier = Modifier.padding(top = 5.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                        )
                    }
                    Surface(onClick = onDismiss, shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface) {
                        Icon(Icons.Outlined.Close, "Close settings", Modifier.padding(8.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(9.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f),
                    ) {
                        Row(Modifier.padding(3.dp)) {
                            AccountSettingsTab.entries.forEach { item ->
                                SettingsTab(item, tab == item, { tab = item }, Modifier.weight(1f))
                            }
                        }
                    }
                    when (tab) {
                        AccountSettingsTab.ACCOUNT -> {
                            SettingsCard {
                                Text(if (account != null) "Signed in as" else "Guest mode", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                if (account != null) {
                                    Text(account.email ?: "Gapwise account", Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                } else {
                                    Text(
                                        "An account is optional. Planning, device storage, exports, campus tools, and public AI work without signing in.",
                                        Modifier.padding(top = 5.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, lineHeight = 16.sp,
                                    )
                                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        AuthProvider.entries.forEach { provider ->
                                            OutlinedButton(onClick = { onSignIn(provider) }) { Text(provider.label, fontSize = 10.5.sp) }
                                        }
                                    }
                                }
                                Text(
                                    "Your original ACORN .ics file is not stored in your account.",
                                    Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.5.sp,
                                )
                            }
                            SettingsCard {
                                Text("Keep timetable on this device", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (meetings.isEmpty()) "No timetable is stored yet." else "Your normalized timetable is encrypted in app-private storage on this device.",
                                    Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.5.sp, lineHeight = 16.sp,
                                )
                            }
                            if (account != null) {
                                SettingsCard {
                                    Text("Sync across devices", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    if (syncStatus != null) Text(syncStatus, Modifier.padding(top = 5.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.5.sp)
                                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                        Button(
                                            onClick = { if (syncEnabled) onSyncNow() else onSetSyncEnabled(true) },
                                            enabled = !syncBusy,
                                        ) { Text(if (syncEnabled) "Sync now" else "Enable sync", fontSize = 11.sp) }
                                        OutlinedButton(onClick = onLoadSync, enabled = !syncBusy) { Text("Load sync", fontSize = 11.sp) }
                                    }
                                    Row(Modifier.padding(top = 7.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                        OutlinedButton(onClick = { deleteSync = true }, enabled = !syncBusy) { Text("Delete sync", fontSize = 11.sp) }
                                        OutlinedButton(onClick = onSignOut, enabled = !syncBusy) { Text("Sign out", fontSize = 11.sp) }
                                    }
                                }
                                OutlinedButton(onClick = { deleteAccount = true }, modifier = Modifier.fillMaxWidth(), enabled = !syncBusy) {
                                    Text("Delete account and cloud data", fontSize = 11.sp)
                                }
                            }
                        }
                        AccountSettingsTab.EXPORTS -> {
                            SettingsCard {
                                Text("Timetable", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Export any term or combine every term in one private image or print-ready vector.", Modifier.padding(top = 5.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.5.sp, lineHeight = 16.sp)
                                OutlinedButton(onClick = { openUrl(GAPWISE_TIMETABLE) }, modifier = Modifier.padding(top = 10.dp)) {
                                    Icon(Icons.Outlined.Download, null); Text("Export", Modifier.padding(start = 6.dp), fontSize = 11.sp)
                                }
                            }
                            SettingsCard {
                                Text("UTM map heatmap", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Export the same campus-focused building and route heatmap as the web app.", Modifier.padding(top = 5.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.5.sp)
                                OutlinedButton(onClick = { openUrl(GAPWISE_ROUTE) }, modifier = Modifier.padding(top = 10.dp)) {
                                    Icon(Icons.Outlined.Download, null); Text("Export heatmap", Modifier.padding(start = 6.dp), fontSize = 11.sp)
                                }
                            }
                        }
                        AccountSettingsTab.AI -> {
                            SettingsCard {
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(Icons.Outlined.Link, null, tint = MaterialTheme.colorScheme.tertiary)
                                    Column(Modifier.padding(start = 10.dp).weight(1f)) {
                                        Text("Gapwise AI", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        Text("Public Gapwise AI and the MCP endpoint are available to everyone.", Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.5.sp)
                                        OutlinedButton(onClick = { openUrl(GAPWISE_AI) }, modifier = Modifier.padding(top = 10.dp)) {
                                            Text("Open Gapwise AI", fontSize = 11.sp); Icon(Icons.Outlined.OpenInNew, null, Modifier.padding(start = 6.dp))
                                        }
                                        Surface(
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                            shape = RoundedCornerShape(7.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .35f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        ) { Text(GAPWISE_MCP, Modifier.padding(10.dp), fontSize = 10.sp) }
                                    }
                                }
                            }
                            SettingsCard {
                                Text(if (account != null) "Private AI access" else "Private AI access is optional", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (account != null) "Private delegation remains minimized; academic classes are read-only." else "Sign in only if you want to authorize private timetable or student context.",
                                    Modifier.padding(top = 5.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.5.sp, lineHeight = 16.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (deleteSync) AlertDialog(
        onDismissRequest = { deleteSync = false }, title = { Text("Delete synced Gapwise data?") },
        text = { Text("This removes encrypted cloud data. Your local timetable stays on this device.") },
        confirmButton = { TextButton(onClick = { deleteSync = false; onDeleteSync() }) { Text("Delete sync") } },
        dismissButton = { TextButton(onClick = { deleteSync = false }) { Text("Cancel") } },
    )
    if (deleteAccount) AlertDialog(
        onDismissRequest = { deleteAccount = false }, title = { Text("Delete account and cloud data?") },
        text = { Text("This permanently removes your Gapwise account and account-owned cloud data. Your local timetable remains on this device.") },
        confirmButton = { TextButton(onClick = { deleteAccount = false; onDeleteAccount() }) { Text("Permanently delete") } },
        dismissButton = { TextButton(onClick = { deleteAccount = false }) { Text("Cancel") } },
    )
}

@Composable
private fun SettingsTab(tab: AccountSettingsTab, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        onClick = onClick, modifier = modifier, shape = RoundedCornerShape(7.dp),
        color = if (selected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f),
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
    ) {
        Row(Modifier.padding(horizontal = 6.dp, vertical = 9.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(
                when (tab) { AccountSettingsTab.ACCOUNT -> Icons.Outlined.PersonOutline; AccountSettingsTab.EXPORTS -> Icons.Outlined.Download; AccountSettingsTab.AI -> Icons.Outlined.Link },
                null,
            )
            Text(tab.label, Modifier.padding(start = 5.dp), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(11.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) { Column(Modifier.padding(14.dp), content = content) }
}
