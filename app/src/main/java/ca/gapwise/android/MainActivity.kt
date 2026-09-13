package ca.gapwise.android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ca.gapwise.android.core.designsystem.GapwiseTheme
import ca.gapwise.android.core.persistence.AppPreferences
import ca.gapwise.android.core.persistence.AppThemeMode
import ca.gapwise.android.navigation.GapwiseApp

class MainActivity : ComponentActivity() {
    private var authCallbackUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        captureAuthCallback(intent)

        setContent {
            val preferences = remember { AppPreferences(applicationContext) }
            var themeMode by remember { mutableStateOf(preferences.themeMode()) }

            GapwiseTheme(darkTheme = themeMode == AppThemeMode.DARK) {
                GapwiseApp(
                    themeMode = themeMode,
                    onThemeModeChange = { mode ->
                        preferences.setThemeMode(mode)
                        themeMode = mode
                    },
                    authCallbackUri = authCallbackUri,
                    onAuthCallbackConsumed = { authCallbackUri = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        captureAuthCallback(intent)
    }

    private fun captureAuthCallback(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "gapwise" && uri.host == "auth-callback") {
            authCallbackUri = uri
        }
    }
}
