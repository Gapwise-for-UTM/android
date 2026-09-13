package ca.gapwise.android.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ca.gapwise.android.R
import ca.gapwise.android.core.model.Meeting
import ca.gapwise.android.data.timetable.IcsParser
import ca.gapwise.android.feature.gapplan.GapPlanScreen
import ca.gapwise.android.feature.map.MapScreen
import ca.gapwise.android.feature.settings.SettingsScreen
import ca.gapwise.android.feature.timetable.TimetableScreen
import ca.gapwise.android.feature.today.TodayScreen

@Composable
fun GapwiseApp() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Destination.Today.route
    val currentDestination = Destination.entries.firstOrNull { it.route == currentRoute } ?: Destination.Today

    var meetings by remember { mutableStateOf<List<Meeting>>(emptyList()) }
    var importStatus by remember { mutableStateOf<String?>(null) }

    val calendarPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        val result = runCatching {
            val knownLength = context.contentResolver
                .openAssetFileDescriptor(uri, "r")
                ?.use { it.length }
                ?: -1L
            require(knownLength <= 0L || knownLength <= IcsParser.MAX_ICS_CHARS * 2L) {
                "Calendar is too large to import safely."
            }
            val text = context.contentResolver
                .openInputStream(uri)
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: error("Could not read this calendar.")
            IcsParser.parse(text)
        }

        result.onSuccess { parsed ->
            meetings = parsed.meetings
            importStatus = buildString {
                append("Imported ${parsed.meetings.size} class meetings locally.")
                if (parsed.warnings.isNotEmpty()) append(" ${parsed.warnings.size} item(s) need review.")
            }
        }.onFailure { error ->
            importStatus = error.message ?: "Could not import this ACORN calendar."
        }
    }

    val startImport = {
        calendarPicker.launch(arrayOf("text/calendar", "text/*", "application/octet-stream"))
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            GapwiseTopBar(pageLabel = currentDestination.pageLabel)
        },
        bottomBar = {
            GapwiseBottomBar(
                currentRoute = currentRoute,
                onNavigate = { destination ->
                    navController.navigate(destination.route) {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(Destination.Today.route) { saveState = true }
                    }
                },
            )
        },
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Destination.Today.route,
            modifier = Modifier.padding(paddingValues),
        ) {
            composable(Destination.Today.route) {
                TodayScreen(meetings = meetings, importStatus = importStatus, onImport = startImport)
            }
            composable(Destination.Timetable.route) {
                TimetableScreen(meetings = meetings, importStatus = importStatus, onImport = startImport)
            }
            composable(Destination.GapPlan.route) {
                GapPlanScreen(meetings = meetings, onImport = startImport)
            }
            composable(Destination.Map.route) {
                MapScreen(meetings = meetings)
            }
            composable(Destination.Settings.route) {
                SettingsScreen(
                    meetings = meetings,
                    importStatus = importStatus,
                    onImport = startImport,
                    onClearTimetable = {
                        meetings = emptyList()
                        importStatus = "Timetable cleared from this app session."
                    },
                )
            }
        }
    }
}

@Composable
private fun GapwiseTopBar(pageLabel: String) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Image(
                        painter = painterResource(R.drawable.gapwise_logo_mark),
                        contentDescription = "Gapwise",
                        modifier = Modifier.size(28.dp),
                    )
                    Text(
                        text = "Gapwise",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    text = pageLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun GapwiseBottomBar(
    currentRoute: String,
    onNavigate: (Destination) -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background.copy(alpha = 0.98f)) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(modifier = Modifier.fillMaxWidth()) {
                Destination.entries.forEach { destination ->
                    val selected = currentRoute == destination.route
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(role = Role.Tab) { onNavigate(destination) }
                            .padding(top = 8.dp, bottom = 7.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (selected) {
                                Surface(
                                    modifier = Modifier.size(width = 34.dp, height = 24.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                    content = {},
                                )
                            }
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label,
                                modifier = Modifier.size(18.dp),
                                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = destination.label,
                            fontSize = 9.sp,
                            lineHeight = 10.sp,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
