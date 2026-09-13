package ca.gapwise.android.core.persistence

import android.content.Context

enum class AppThemeMode { LIGHT, DARK }
enum class RouteMode(val label: String) {
    FASTEST("Fastest"),
    PREFER_INDOOR("Prefer indoor"),
    STEP_FREE("Step-free"),
}
enum class DayOrigin(val label: String) {
    COMMUTE("Commute"),
    RESIDENCE("Live on campus"),
}
enum class CommuteMode(val label: String) {
    TRANSIT("Public transit"),
    PARKING("Drive / park"),
    PICKUP("Drop-off / pick-up"),
}
enum class RiskTolerance(val label: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
}

class AppPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "gapwise_preferences_v1",
        Context.MODE_PRIVATE,
    )

    fun themeMode(): AppThemeMode = enumValue(KEY_THEME, AppThemeMode.DARK)
    fun setThemeMode(mode: AppThemeMode) = putString(KEY_THEME, mode.name)

    fun encryptedSyncEnabled(userId: String): Boolean =
        preferences.getBoolean("sync.enabled.$userId", false)

    fun setEncryptedSyncEnabled(userId: String, enabled: Boolean) {
        preferences.edit().putBoolean("sync.enabled.$userId", enabled).apply()
    }

    fun routeMode(): RouteMode = enumValue(KEY_ROUTE_MODE, RouteMode.FASTEST)
    fun setRouteMode(value: RouteMode) = putString(KEY_ROUTE_MODE, value.name)

    fun walkingSpeedMps(): Float = preferences.getFloat(KEY_WALKING_SPEED, 1.35f)
        .coerceIn(0.5f, 3f)

    fun setWalkingSpeedMps(value: Float) {
        preferences.edit().putFloat(KEY_WALKING_SPEED, value.coerceIn(0.5f, 3f)).apply()
    }

    fun transitionBufferMinutes(): Int = preferences.getInt(KEY_TRANSITION_BUFFER, 5)
        .coerceIn(0, 60)

    fun setTransitionBufferMinutes(value: Int) {
        preferences.edit().putInt(KEY_TRANSITION_BUFFER, value.coerceIn(0, 60)).apply()
    }

    fun dayOrigin(): DayOrigin = enumValue(KEY_DAY_ORIGIN, DayOrigin.COMMUTE)
    fun setDayOrigin(value: DayOrigin) = putString(KEY_DAY_ORIGIN, value.name)

    fun commuteMode(): CommuteMode? = nullableEnum(KEY_COMMUTE_MODE)
    fun setCommuteMode(value: CommuteMode?) = putNullableString(KEY_COMMUTE_MODE, value?.name)

    fun residenceBuildingCode(): String? = nullableString(KEY_RESIDENCE)
    fun setResidenceBuildingCode(value: String?) = putNullableString(KEY_RESIDENCE, value)

    fun campusAccessPointId(): String? = nullableString(KEY_ACCESS_POINT)
    fun setCampusAccessPointId(value: String?) = putNullableString(KEY_ACCESS_POINT, value)

    fun setupMinutes(): Int = preferences.getInt(KEY_SETUP_MINUTES, 4).coerceIn(0, 20)
    fun setSetupMinutes(value: Int) = putInt(KEY_SETUP_MINUTES, value.coerceIn(0, 20))

    fun packUpMinutes(): Int = preferences.getInt(KEY_PACK_UP_MINUTES, 3).coerceIn(0, 20)
    fun setPackUpMinutes(value: Int) = putInt(KEY_PACK_UP_MINUTES, value.coerceIn(0, 20))

    fun lunchWindowStart(): Int = preferences.getInt(KEY_LUNCH_START, 690).coerceIn(0, 1439)
    fun setLunchWindowStart(value: Int) = putInt(KEY_LUNCH_START, value.coerceIn(0, 1439))

    fun lunchWindowEnd(): Int = preferences.getInt(KEY_LUNCH_END, 870).coerceIn(15, 1440)
    fun setLunchWindowEnd(value: Int) = putInt(KEY_LUNCH_END, value.coerceIn(15, 1440))

    fun mealDurationMinutes(): Int = preferences.getInt(KEY_MEAL_DURATION, 30).coerceIn(15, 90)
    fun setMealDurationMinutes(value: Int) = putInt(KEY_MEAL_DURATION, value.coerceIn(15, 90))

    fun willingToLeaveCampus(): Boolean = preferences.getBoolean(KEY_LEAVE_CAMPUS, false)
    fun setWillingToLeaveCampus(value: Boolean) = putBoolean(KEY_LEAVE_CAMPUS, value)

    fun oneWayHomeCommuteMinutes(): Int? = if (preferences.contains(KEY_HOME_COMMUTE)) {
        preferences.getInt(KEY_HOME_COMMUTE, 30).coerceIn(5, 180)
    } else {
        null
    }

    fun setOneWayHomeCommuteMinutes(value: Int?) {
        if (value == null) preferences.edit().remove(KEY_HOME_COMMUTE).apply()
        else putInt(KEY_HOME_COMMUTE, value.coerceIn(5, 180))
    }

    fun minimumHomeStayMinutes(): Int = preferences.getInt(KEY_MIN_HOME_STAY, 90).coerceIn(30, 360)
    fun setMinimumHomeStayMinutes(value: Int) = putInt(KEY_MIN_HOME_STAY, value.coerceIn(30, 360))

    fun homeTurnaroundMinutes(): Int = preferences.getInt(KEY_HOME_TURNAROUND, 10).coerceIn(0, 30)
    fun setHomeTurnaroundMinutes(value: Int) = putInt(KEY_HOME_TURNAROUND, value.coerceIn(0, 30))

    fun riskTolerance(): RiskTolerance = enumValue(KEY_RISK, RiskTolerance.LOW)
    fun setRiskTolerance(value: RiskTolerance) = putString(KEY_RISK, value.name)

    private inline fun <reified T : Enum<T>> enumValue(key: String, fallback: T): T = runCatching {
        enumValueOf<T>(preferences.getString(key, fallback.name)!!)
    }.getOrDefault(fallback)

    private inline fun <reified T : Enum<T>> nullableEnum(key: String): T? =
        nullableString(key)?.let { raw -> runCatching { enumValueOf<T>(raw) }.getOrNull() }

    private fun nullableString(key: String): String? =
        preferences.getString(key, null)?.takeIf(String::isNotBlank)

    private fun putString(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }

    private fun putNullableString(key: String, value: String?) {
        if (value == null) preferences.edit().remove(key).apply()
        else putString(key, value)
    }

    private fun putInt(key: String, value: Int) {
        preferences.edit().putInt(key, value).apply()
    }

    private fun putBoolean(key: String, value: Boolean) {
        preferences.edit().putBoolean(key, value).apply()
    }

    private companion object {
        const val KEY_THEME = "appearance.theme"
        const val KEY_ROUTE_MODE = "routing.mode"
        const val KEY_WALKING_SPEED = "routing.walking_speed_mps"
        const val KEY_TRANSITION_BUFFER = "routing.transition_buffer_minutes"
        const val KEY_DAY_ORIGIN = "arrival.day_origin"
        const val KEY_COMMUTE_MODE = "arrival.commute_mode"
        const val KEY_RESIDENCE = "arrival.residence_building_code"
        const val KEY_ACCESS_POINT = "arrival.campus_access_point_id"
        const val KEY_SETUP_MINUTES = "gaps.setup_minutes"
        const val KEY_PACK_UP_MINUTES = "gaps.pack_up_minutes"
        const val KEY_LUNCH_START = "gaps.lunch_window_start"
        const val KEY_LUNCH_END = "gaps.lunch_window_end"
        const val KEY_MEAL_DURATION = "gaps.meal_duration_minutes"
        const val KEY_LEAVE_CAMPUS = "gaps.willing_to_leave_campus"
        const val KEY_HOME_COMMUTE = "gaps.one_way_home_commute_minutes"
        const val KEY_MIN_HOME_STAY = "gaps.minimum_home_stay_minutes"
        const val KEY_HOME_TURNAROUND = "gaps.home_turnaround_minutes"
        const val KEY_RISK = "gaps.risk_tolerance"
    }
}
