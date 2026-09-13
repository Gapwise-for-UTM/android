package ca.gapwise.android.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ca.gapwise.android.core.model.Campus

@Composable
fun TodayScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            text = "Today",
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            text = "Your day, the gaps between classes, and where you can realistically go next.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "Native shell online",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "The first Android milestone is wired up. Timetable import and live gap calculations come next.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CampusPill(Campus.UTM)
                    CampusPill(Campus.UTSG)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Next up",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "ACORN timetable import → campus-aware locations → native timetable → timetable-to-map navigation",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CampusPill(campus: Campus) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        contentColor = MaterialTheme.colorScheme.primary,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                modifier = Modifier.size(7.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                content = {},
            )
            Text(
                text = campus.shortName,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
