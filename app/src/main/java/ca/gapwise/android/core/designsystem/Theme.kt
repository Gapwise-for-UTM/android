package ca.gapwise.android.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GapwiseBlue = Color(0xFF4EA7FE)
private val GapwiseBlueDark = Color(0xFF1769AA)

private val DarkColors = darkColorScheme(
    primary = GapwiseBlue,
    onPrimary = Color(0xFF001D33),
    background = Color(0xFF05080C),
    onBackground = Color(0xFFEAF1F8),
    surface = Color(0xFF0B1016),
    onSurface = Color(0xFFEAF1F8),
    surfaceVariant = Color(0xFF131B24),
    onSurfaceVariant = Color(0xFFAAB7C5),
    surfaceContainer = Color(0xFF0F151D),
    surfaceContainerLow = Color(0xFF0A0F15),
    surfaceContainerHigh = Color(0xFF151D27),
    outline = Color(0xFF334252),
    outlineVariant = Color(0xFF202B36),
)

private val LightColors = lightColorScheme(
    primary = GapwiseBlueDark,
    onPrimary = Color.White,
    background = Color(0xFFF7F9FC),
    onBackground = Color(0xFF111827),
    surface = Color.White,
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFF0F4F8),
    onSurfaceVariant = Color(0xFF5A6573),
    surfaceContainer = Color(0xFFF2F6FA),
    surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainerHigh = Color(0xFFEAF0F6),
    outline = Color(0xFFD7E0E9),
    outlineVariant = Color(0xFFE5EBF1),
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
