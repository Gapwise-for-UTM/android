package ca.gapwise.android.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GapwiseBlue = Color(0xFF4EA7FE)
private val GapwiseDark = Color(0xFF05080C)
private val GapwiseDarkSurface = Color(0xFF0C1118)
private val GapwiseLight = Color(0xFFF7F9FC)

private val DarkColors = darkColorScheme(
    primary = GapwiseBlue,
    secondary = GapwiseBlue,
    background = GapwiseDark,
    surface = GapwiseDarkSurface,
    surfaceVariant = Color(0xFF151D27),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1769AA),
    secondary = Color(0xFF1769AA),
    background = GapwiseLight,
    surface = Color.White,
    surfaceVariant = Color(0xFFEAF1F8),
)

@Composable
fun GapwiseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
