package ca.gapwise.android.feature.timetable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ca.gapwise.android.core.model.Campus
import ca.gapwise.android.core.model.CampusLocation

private val ExampleLocations = listOf(
    CampusLocation.of(Campus.UTM, "MN", "1270"),
    CampusLocation.of(Campus.UTSG, "BA", "1170"),
)

@Composable
fun TimetableScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Timetable",
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            text = "Campus identity is part of every meeting location from the start.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        ExampleLocations.forEach { location ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = location.displayName,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = location.campus.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
