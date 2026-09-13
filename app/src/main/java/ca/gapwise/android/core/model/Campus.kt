package ca.gapwise.android.core.model

enum class Campus(
    val shortName: String,
    val displayName: String,
) {
    UTM("UTM", "University of Toronto Mississauga"),
    UTSG("UTSG", "University of Toronto St. George"),
    UTSC("UTSC", "University of Toronto Scarborough"),
    UNKNOWN("U of T", "University of Toronto"),
    ;

    companion object {
        private val courseCodePattern = Regex("^[A-Z]{3}[A-Z0-9]\\d{2}[A-Z](\\d)$")

        fun fromCourseCode(courseCode: String): Campus {
            val campusDigit = courseCodePattern
                .matchEntire(courseCode.trim().uppercase())
                ?.groupValues
                ?.getOrNull(1)

            return when (campusDigit) {
                "1" -> UTSG
                "3" -> UTSC
                "5" -> UTM
                else -> UNKNOWN
            }
        }
    }
}
