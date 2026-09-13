package ca.gapwise.android.navigation

import ca.gapwise.android.R

/** Mirrors the current Gapwise web mobile shell exactly: four destinations plus More. */
internal enum class Destination(
    val route: String,
    val label: String,
    val pageLabel: String,
    val iconRes: Int,
) {
    Today(
        route = "today",
        label = "Today",
        pageLabel = "My day",
        iconRes = R.drawable.ic_lucide_calendar_clock,
    ),
    Timetable(
        route = "timetable",
        label = "Timetable",
        pageLabel = "Timetable",
        iconRes = R.drawable.ic_lucide_layout_grid,
    ),
    Gaps(
        route = "gaps",
        label = "Gaps",
        pageLabel = "Gap plan",
        iconRes = R.drawable.ic_lucide_calendar_range,
    ),
    Map(
        route = "map",
        label = "Map",
        pageLabel = "Campus map",
        iconRes = R.drawable.ic_lucide_map_pinned,
    ),
}
