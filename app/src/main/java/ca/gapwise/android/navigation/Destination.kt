package ca.gapwise.android.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Today
import androidx.compose.ui.graphics.vector.ImageVector

internal enum class Destination(
    val route: String,
    val label: String,
    val pageLabel: String,
    val icon: ImageVector,
) {
    Today(
        route = "today",
        label = "Today",
        pageLabel = "My day",
        icon = Icons.Outlined.Today,
    ),
    Timetable(
        route = "timetable",
        label = "Timetable",
        pageLabel = "Timetable",
        icon = Icons.Outlined.CalendarMonth,
    ),
    Map(
        route = "map",
        label = "Map",
        pageLabel = "UTM campus map",
        icon = Icons.Outlined.Map,
    ),
}
