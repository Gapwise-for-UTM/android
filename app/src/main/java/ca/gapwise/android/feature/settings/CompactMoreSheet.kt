package ca.gapwise.android.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.gapwise.android.core.persistence.AppThemeMode

@Composable
fun CompactMoreSheetContent(
    themeMode: AppThemeMode,
    accountLabel: String,
    signedIn: Boolean,
    canRemoveTimetable: Boolean,
    onOpenAcademicPreferences: () -> Unit,
    onToggleTheme: () -> Unit,
    onOpenArrivalPreferences: () -> Unit,
    onOpenSettings: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    onUpdateTimetable: () -> Unit,
    onRemoveTimetable: () -> Unit,
) {
    var accountMenuOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Text(
            text = "PREFERENCES",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.5.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
        )
        LazyRow(
            modifier = Modifier.padding(top = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            item { CompactPreferenceChip("Academic work", onOpenAcademicPreferences) }
            item {
                Surface(
                    onClick = onToggleTheme,
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Icon(
                        imageVector = if (themeMode == AppThemeMode.DARK) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                        contentDescription = if (themeMode == AppThemeMode.DARK) "Use light theme" else "Use dark theme",
                        modifier = Modifier.padding(9.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item { CompactPreferenceChip("Arrival", onOpenArrivalPreferences) }
            item {
                Box {
                    Surface(
                        onClick = { if (signedIn) accountMenuOpen = true else onOpenSettings() },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.background,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(imageVector = Icons.Outlined.PersonOutline, contentDescription = null)
                            Text(
                                accountLabel,
                                modifier = Modifier.padding(start = 7.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                            )
                            if (signedIn) {
                                Icon(
                                    imageVector = Icons.Outlined.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.padding(start = 4.dp),
                                )
                            }
                        }
                    }

                    DropdownMenu(
                        expanded = accountMenuOpen,
                        onDismissRequest = { accountMenuOpen = false },
                        modifier = Modifier.widthIn(min = 235.dp),
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text("Account", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                "You'll stay signed in on this device until you sign out.",
                                modifier = Modifier.padding(top = 3.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.5.sp,
                                lineHeight = 15.sp,
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        DropdownMenuItem(
                            text = { Text("Settings", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                            onClick = {
                                accountMenuOpen = false
                                onOpenSettings()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Sign out", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Outlined.Logout, contentDescription = null) },
                            onClick = {
                                accountMenuOpen = false
                                onSignOut()
                            },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Delete account and cloud data",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.DeleteOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            },
                            onClick = {
                                accountMenuOpen = false
                                onDeleteAccount()
                            },
                        )
                    }
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(top = 17.dp, bottom = 15.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )

        Text(
            text = "TIMETABLE",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.5.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
        )
        Button(
            onClick = onUpdateTimetable,
            modifier = Modifier.fillMaxWidth().padding(top = 9.dp),
            shape = RoundedCornerShape(9.dp),
        ) {
            Icon(imageVector = Icons.Outlined.Upload, contentDescription = null)
            Text("Update timetable", modifier = Modifier.padding(start = 7.dp), fontWeight = FontWeight.SemiBold)
        }
        if (canRemoveTimetable) {
            OutlinedButton(
                onClick = onRemoveTimetable,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
                shape = RoundedCornerShape(9.dp),
            ) {
                Text("Remove timetable", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun CompactPreferenceChip(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
