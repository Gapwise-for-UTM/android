package ca.gapwise.android.core.model

data class CampusLocation private constructor(
    val campus: Campus,
    val buildingCode: String,
    val room: String?,
) {
    val stableKey: String
        get() = buildString {
            append(campus.name)
            append(':')
            append(buildingCode)
            append(':')
            append(room.orEmpty())
        }

    val displayName: String
        get() = if (room == null) buildingCode else "$buildingCode $room"

    companion object {
        fun of(
            campus: Campus,
            buildingCode: String,
            room: String? = null,
        ): CampusLocation {
            val normalizedBuildingCode = buildingCode.trim().uppercase()
            require(normalizedBuildingCode.isNotEmpty()) {
                "buildingCode must not be blank"
            }

            val normalizedRoom = room
                ?.trim()
                ?.takeIf(String::isNotEmpty)

            return CampusLocation(
                campus = campus,
                buildingCode = normalizedBuildingCode,
                room = normalizedRoom,
            )
        }
    }
}
