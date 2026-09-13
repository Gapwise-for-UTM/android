package ca.gapwise.android.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// sRGB equivalents of the canonical Gapwise web OKLCH tokens in src/brand-blue.css.
private val LightColors = lightColorScheme(
    primary = Color(0xFF007AD4),
    onPrimary = Color(0xFFFCFDFD),
    background = Color(0xFFF9FAFB),
    onBackground = Color(0xFF101214),
    surface = Color(0xFFFEFEFF),
    onSurface = Color(0xFF101214),
    surfaceVariant = Color(0xFFEEF0F3),
    onSurfaceVariant = Color(0xFF5D6165),
    surfaceContainer = Color(0xFFEEF0F3),
    surfaceContainerLow = Color(0xFFF9FAFB),
    surfaceContainerHigh = Color(0xFFE5E8EB),
    outline = Color(0xFFD8DBDE),
    outlineVariant = Color(0xFFD8DBDE),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF33A3F1),
    onPrimary = Color(0xFFFCFDFD),
    background = Color(0xFF040405),
    onBackground = Color(0xFFE9EBEE),
    surface = Color(0xFF07080A),
    onSurface = Color(0xFFE9EBEE),
    surfaceVariant = Color(0xFF0F1113),
    onSurfaceVariant = Color(0xFF898C90),
    surfaceContainer = Color(0xFF090A0C),
    surfaceContainerLow = Color(0xFF07080A),
    surfaceContainerHigh = Color(0xFF0F1113),
    outline = Color(0xFF1C1E21),
    outlineVariant = Color(0xFF1C1E21),
)

@Composable
fun GapwiseTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
