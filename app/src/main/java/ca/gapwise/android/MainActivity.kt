package ca.gapwise.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ca.gapwise.android.core.designsystem.GapwiseTheme
import ca.gapwise.android.navigation.GapwiseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GapwiseTheme {
                GapwiseApp()
            }
        }
    }
}
