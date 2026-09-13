package ca.gapwise.android.core.model

enum class Campus(
    val shortName: String,
    val displayName: String,
) {
    UTM(
        shortName = "UTM",
        displayName = "University of Toronto Mississauga",
    ),
    UTSG(
        shortName = "UTSG",
        displayName = "University of Toronto St. George",
    ),
}
