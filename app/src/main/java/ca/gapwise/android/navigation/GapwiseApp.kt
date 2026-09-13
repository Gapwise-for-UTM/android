package ca.gapwise.android.navigation

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import ca.gapwise.android.core.persistence.AppPreferences
import ca.gapwise.android.core.persistence.AppThemeMode
import ca.gapwise.android.core.persistence.MeetingJson
import ca.gapwise.android.core.persistence.SecureLocalStore
import ca.gapwise.android.data.account.AccountIdentity
import ca.gapwise.android.data.account.AccountMaintenance
import ca.gapwise.android.data.account.AuthProvider
import ca.gapwise.android.data.account.GapwiseAccountManager
import ca.gapwise.android.data.sync.EncryptedCloudMaintenance
import ca.gapwise.android.data.sync.EncryptedCloudSync
import ca.gapwise.android.data.timetable.IcsParser
import ca.gapwise.android.feature.gapplan.GapPlanScreen
import ca.gapwise.android.feature.map.MapScreen
import ca.gapwise.android.feature.settings.AccountSettingsDialog
import ca.gapwise.android.feature.settings.CompactMoreSheetContent
import ca.gapwise.android.feature.settings.RoutePreferencesSheet
import ca.gapwise.android.feature.timetable.TimetableScreen
import ca.gapwise.android.feature.today.TodayScreen
import kotlinx.coroutines.launch

private const val TIMETABLE_ENTRY = "timetable.normalized"
private const val GAPWISE_TODAY_URL = "https://gapwise.ca/today"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GapwiseApp(
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit,
    authCallbackUri: Uri?,
    onAuthCallbackConsumed: () -> Unit,
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Destination.Today.route
    val currentDestination = Destination.entries.firstOrNull { it.route == currentRoute } ?: Destination.Today
    val scope = rememberCoroutineScope()

    val secureStore = remember { SecureLocalStore(context.applicationContext) }
    val preferences = remember { AppPreferences(context.applicationContext) }
    val accountManager = remember { GapwiseAccountManager(secureStore) }
    val accountMaintenance = remember { AccountMaintenance(accountManager) }
    val cloudSync = remember { EncryptedCloudSync(accountManager) }
    val cloudMaintenance = remember { EncryptedCloudMaintenance(accountManager) }

    var meetings by remember { mutableStateOf(MeetingJson.decodeLocal(secureStore.get(TIMETABLE_ENTRY))) }
    var importStatus by remember { mutableStateOf<String?>(null) }
    var account by remember { mutableStateOf<AccountIdentity?>(accountManager.storedIdentity()) }
    var syncEnabled by remember(account?.userId) {
        mutableStateOf(account?.let { preferences.encryptedSyncEnabled(it.userId) } ?: false)
    }
    var syncBusy by remember { mutableStateOf(false) }
    var syncStatus by remember { mutableStateOf<String?>(null) }
    var moreOpen by remember { mutableStateOf(false) }
    var accountSettingsOpen by remember { mutableStateOf(false) }
    var routePreferencesOpen by remember { mutableStateOf(false) }

    fun saveTimetable(value: List<Meeting>) {
        meetings = value
        secureStore.put(TIMETABLE_ENTRY, MeetingJson.encodeLocal(value))
    }

    fun pushIfEnabled(value: List<Meeting>) {
        if (!syncEnabled || account == null || syncBusy) return
        scope.launch {
            syncBusy = true
            runCatching { cloudSync.pushSchedule(value) }
                .onSuccess { syncStatus = it.message }
                .onFailure { syncStatus = it.message ?: "Encrypted sync failed." }
            syncBusy = false
        }
    }

    fun signOut() {
        scope.launch {
            syncBusy = true
            accountManager.signOut()
            account = null
            syncEnabled = false
            syncStatus = "Signed out. Your timetable remains saved on this device."
            syncBusy = false
        }
    }

    val calendarPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val result = runCatching {
            val knownLength = context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
            require(knownLength <= 0L || knownLength <= IcsParser.MAX_ICS_CHARS * 2L) {
                "Calendar is too large to import safely."
            }
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: error("Could not read this calendar.")
            IcsParser.parse(text)
        }
        result.onSuccess { parsed ->
            saveTimetable(parsed.meetings)
            importStatus = buildString {
                append("Imported ${parsed.meetings.size} class meetings and saved them on this device.")
                if (parsed.warnings.isNotEmpty()) append(" ${parsed.warnings.size} item(s) need review.")
            }
            pushIfEnabled(parsed.meetings)
        }.onFailure { error ->
            importStatus = error.message ?: "Could not import this ACORN calendar."
        }
    }

    val startImport = {
        calendarPicker.launch(arrayOf("text/calendar", "text/*", "application/octet-stream"))
    }

    LaunchedEffect(authCallbackUri) {
        val callback = authCallbackUri ?: return@LaunchedEffect
        syncBusy = true
        runCatching { accountManager.completeOAuth(callback) }
            .onSuccess { identity ->
                account = identity
                syncEnabled = preferences.encryptedSyncEnabled(identity.userId)
                syncStatus = "Signed in. Turn on encrypted account sync when you want this device linked."
            }
            .onFailure { error -> syncStatus = error.message ?: "Sign in failed." }
        syncBusy = false
        onAuthCallbackConsumed()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { GapwiseTopBar(pageLabel = currentDestination.pageLabel) },
        bottomBar = {
            GapwiseBottomBar(
                currentRoute = currentRoute,
                moreOpen = moreOpen,
                onNavigate = { destination ->
                    moreOpen = false
                    navController.navigate(destination.route) {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(Destination.Today.route) { saveState = true }
                    }
                },
                onOpenMore = { moreOpen = true },
            )
        },
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Destination.Today.route,
            modifier = Modifier.padding(paddingValues),
        ) {
            composable(Destination.Today.route) {
                TodayScreen(
                    meetings = meetings,
                    importStatus = importStatus,
                    onImport = startImport,
                    onDayRoute = {
                        navController.navigate(Destination.Map.route) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable(Destination.Timetable.route) {
                TimetableScreen(
                    meetings = meetings,
                    importStatus = importStatus,
                    onImport = startImport,
                    onOpenGapPlan = {
                        navController.navigate(Destination.Gaps.route) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable(Destination.Gaps.route) {
                GapPlanScreen(meetings = meetings, onImport = startImport)
            }
            composable(Destination.Map.route) {
                MapScreen(meetings = meetings, darkTheme = themeMode == AppThemeMode.DARK)
            }
        }
    }

    if (moreOpen) {
        ModalBottomSheet(
            onDismissRequest = { moreOpen = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        ) {
            Text(
                text = "More",
                fontSize = 19.sp,
                lineHeight = 23.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 14.dp),
            )
            CompactMoreSheetContent(
                themeMode = themeMode,
                accountLabel = account?.email?.substringBefore('@')?.takeIf { it.isNotBlank() }
                    ?: if (account == null) "Sign in" else "Account",
                signedIn = account != null,
                canRemoveTimetable = meetings.isNotEmpty(),
                onOpenAcademicPreferences = {
                    moreOpen = false
                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GAPWISE_TODAY_URL))) }
                },
                onToggleTheme = {
                    onThemeModeChange(if (themeMode == AppThemeMode.DARK) AppThemeMode.LIGHT else AppThemeMode.DARK)
                },
                onOpenArrivalPreferences = {
                    moreOpen = false
                    routePreferencesOpen = true
                },
                onOpenSettings = {
                    moreOpen = false
                    accountSettingsOpen = true
                },
                onSignOut = {
                    moreOpen = false
                    signOut()
                },
                onDeleteAccount = {
                    moreOpen = false
                    accountSettingsOpen = true
                },
                onUpdateTimetable = {
                    moreOpen = false
                    startImport()
                },
                onRemoveTimetable = {
                    saveTimetable(emptyList())
                    importStatus = "Timetable removed from this device."
                    pushIfEnabled(emptyList())
                    moreOpen = false
                },
            )
        }
    }

    RoutePreferencesSheet(
        open = routePreferencesOpen,
        onDismiss = { routePreferencesOpen = false },
    )

    AccountSettingsDialog(
        open = accountSettingsOpen,
        account = account,
        meetings = meetings,
        syncEnabled = syncEnabled,
        syncBusy = syncBusy,
        syncStatus = syncStatus,
        onDismiss = { accountSettingsOpen = false },
        onSignIn = { provider: AuthProvider ->
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, accountManager.startOAuth(provider))) }
                .onFailure { error -> syncStatus = error.message ?: "Could not open sign in." }
        },
        onSignOut = {
            accountSettingsOpen = false
            signOut()
        },
        onSetSyncEnabled = { enabled ->
            val identity = account
            if (identity != null) {
                preferences.setEncryptedSyncEnabled(identity.userId, enabled)
                syncEnabled = enabled
                if (enabled) {
                    scope.launch {
                        syncBusy = true
                        runCatching { cloudSync.pullOrInitialize(meetings) }
                            .onSuccess { synced ->
                                saveTimetable(synced.meetings)
                                syncStatus = synced.message
                            }
                            .onFailure { error ->
                                preferences.setEncryptedSyncEnabled(identity.userId, false)
                                syncEnabled = false
                                syncStatus = error.message ?: "Encrypted sync could not be enabled."
                            }
                        syncBusy = false
                    }
                } else {
                    syncStatus = "Encrypted account sync paused. Cloud data was not deleted."
                }
            }
        },
        onSyncNow = {
            if (account != null && syncEnabled) {
                scope.launch {
                    syncBusy = true
                    runCatching { cloudSync.pushSchedule(meetings) }
                        .onSuccess { synced -> syncStatus = synced.message }
                        .onFailure { error -> syncStatus = error.message ?: "Encrypted sync failed." }
                    syncBusy = false
                }
            }
        },
        onLoadSync = {
            if (account != null) {
                scope.launch {
                    syncBusy = true
                    runCatching { cloudSync.pullOrInitialize(emptyList()) }
                        .onSuccess { synced ->
                            if (!synced.message.contains("Nothing is stored in the cloud yet")) saveTimetable(synced.meetings)
                            syncStatus = synced.message
                        }
                        .onFailure { error -> syncStatus = error.message ?: "Encrypted sync failed." }
                    syncBusy = false
                }
            }
        },
        onDeleteSync = {
            val identity = account
            if (identity != null) {
                scope.launch {
                    syncBusy = true
                    runCatching { cloudMaintenance.deletePrivateCloud() }
                        .onSuccess {
                            preferences.setEncryptedSyncEnabled(identity.userId, false)
                            syncEnabled = false
                            syncStatus = "Encrypted synced data deleted. The timetable remains on this device."
                        }
                        .onFailure { error -> syncStatus = error.message ?: "Encrypted cloud deletion failed." }
                    syncBusy = false
                }
            }
        },
        onDeleteAccount = {
            val identity = account
            if (identity != null) {
                scope.launch {
                    syncBusy = true
                    runCatching { accountMaintenance.deleteAccount() }
                        .onSuccess {
                            preferences.setEncryptedSyncEnabled(identity.userId, false)
                            account = null
                            syncEnabled = false
                            accountSettingsOpen = false
                            syncStatus = "Your Gapwise account and cloud data were permanently deleted."
                        }
                        .onFailure { error -> syncStatus = error.message ?: "We couldn't delete your account. Please try again." }
                    syncBusy = false
                }
            }
        },
    )
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Image(
                        painter = painterResource(R.drawable.gapwise_logo_mark),
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                    )
                    Text(
                        text = "Gapwise",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.5).sp,
                    )
                }
                Text(
                    text = pageLabel,
                    fontSize = 12.sp,
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
    moreOpen: Boolean,
    onNavigate: (Destination) -> Unit,
    onOpenMore: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background.copy(alpha = 0.98f)) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(modifier = Modifier.fillMaxWidth()) {
                Destination.entries.forEach { destination ->
                    val selected = currentRoute == destination.route && !moreOpen
                    MobileNavItem(
                        label = destination.label,
                        iconRes = destination.iconRes,
                        selected = selected,
                        onClick = { onNavigate(destination) },
                        modifier = Modifier.weight(1f),
                    )
                }
                MobileNavItem(
                    label = "More",
                    iconRes = R.drawable.ic_lucide_menu,
                    selected = moreOpen,
                    onClick = onOpenMore,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MobileNavItem(
    label: String,
    iconRes: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val accent = MaterialTheme.colorScheme.tertiary
    val color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = modifier
            .heightIn(min = 60.dp)
            .background(if (selected) accent.copy(alpha = 0.07f) else Color.Transparent, RoundedCornerShape(7.dp))
            .clickable(role = Role.Tab, onClick = onClick),
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .width(26.dp)
                    .height(2.dp)
                    .background(accent, RoundedCornerShape(99.dp)),
            )
        }
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = label,
                modifier = Modifier.size(18.dp),
                tint = color,
            )
            Text(
                text = label,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                color = color,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 4.dp),
                maxLines = 1,
            )
        }
    }
}
