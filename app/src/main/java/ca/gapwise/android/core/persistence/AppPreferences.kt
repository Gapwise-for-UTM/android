package ca.gapwise.android.core.persistence

import android.content.Context

enum class AppThemeMode { LIGHT, DARK }

class AppPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "gapwise_preferences_v1",
        Context.MODE_PRIVATE,
    )

    fun themeMode(): AppThemeMode = runCatching {
        AppThemeMode.valueOf(preferences.getString(KEY_THEME, AppThemeMode.DARK.name)!!)
    }.getOrDefault(AppThemeMode.DARK)

    fun setThemeMode(mode: AppThemeMode) {
        preferences.edit().putString(KEY_THEME, mode.name).apply()
    }

    fun encryptedSyncEnabled(userId: String): Boolean =
        preferences.getBoolean("sync.enabled.$userId", false)

    fun setEncryptedSyncEnabled(userId: String, enabled: Boolean) {
        preferences.edit().putBoolean("sync.enabled.$userId", enabled).apply()
    }

    private companion object {
        const val KEY_THEME = "appearance.theme"
    }
}
