package ca.gapwise.android.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val WebSurfaceShape = RoundedCornerShape(12.dp)
val WebControlShape = RoundedCornerShape(9.dp)
val WebInnerShape = RoundedCornerShape(7.dp)

/** The plain bordered card used by the mobile web app's .surface rule. */
@Composable
fun WebSurface(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = WebSurfaceShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(padding), content = content)
    }
}

/** Web-style uppercase kicker/eyebrow. */
@Composable
fun WebEyebrow(
    text: String,
    modifier: Modifier = Modifier,
    accent: Boolean = true,
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        color = if (accent) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 10.5.sp,
        lineHeight = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.15.sp,
    )
}

/** A single cell from the web segmented controls. */
@Composable
fun WebSegmentButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = 40.dp,
    secondaryLabel: String? = null,
) {
    val selectedBackground = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f)
    val selectedColor = MaterialTheme.colorScheme.tertiary
    val idleColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .heightIn(min = minHeight)
            .background(
                color = if (selected) selectedBackground else Color.Transparent,
                shape = WebInnerShape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                color = if (selected) selectedColor else idleColor,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            if (secondaryLabel != null) {
                Text(
                    text = secondaryLabel,
                    color = if (selected) selectedColor.copy(alpha = 0.78f) else idleColor.copy(alpha = 0.78f),
                    fontSize = 9.5.sp,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 2.dp),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun WebSegmentContainer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background, WebControlShape)
            .padding(3.dp),
        content = content,
    )
}
