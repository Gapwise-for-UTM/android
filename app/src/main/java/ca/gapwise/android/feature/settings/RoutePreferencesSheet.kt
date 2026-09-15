package ca.gapwise.android.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.gapwise.android.core.persistence.AppPreferences
import ca.gapwise.android.core.persistence.CommuteMode
import ca.gapwise.android.core.persistence.DayOrigin
import ca.gapwise.android.core.persistence.RouteMode
import kotlin.math.roundToInt

private data class ArrivalChoice(val id: String, val label: String)

private val Residences = listOf(
    ArrivalChoice("EH", "Erindale Hall"),
    ArrivalChoice("LL", "Leacock Lane"),
    ArrivalChoice("MV", "MaGrath Valley"),
    ArrivalChoice("MC", "McLuhan Court"),
    ArrivalChoice("OPH", "Oscar Peterson Hall"),
    ArrivalChoice("PP", "Putnam Place"),
    ArrivalChoice("RIH", "Roy Ivor Hall"),
    ArrivalChoice("SW", "Schreiberwood"),
    ArrivalChoice("NRB", "New Residence Building"),
)

private val TransitPoints = listOf(
    ArrivalChoice("miway-utm-bus-station", "UTM Bus Station (MiWay)"),
    ArrivalChoice("utm-shuttle-instructional-centre", "UTM Shuttle — Instructional Centre"),
)

private val ParkingPoints = listOf(
    ArrivalChoice("parking-p8", "Parking Lot P8"),
    ArrivalChoice("parking-p9", "Parking Lot P9"),
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun RoutePreferencesSheet(
    open: Boolean,
    onDismiss: () -> Unit,
) {
    if (!open) return

    val context = LocalContext.current
    val preferences = remember { AppPreferences(context.applicationContext) }
    var routeMode by remember { mutableStateOf(preferences.routeMode()) }
    var walkingSpeed by remember { mutableStateOf(preferences.walkingSpeedMps()) }
    var transitionBuffer by remember { mutableStateOf(preferences.transitionBufferMinutes()) }
    var dayOrigin by remember { mutableStateOf(preferences.dayOrigin()) }
    var commuteMode by remember { mutableStateOf(preferences.commuteMode()) }
    var residenceCode by remember { mutableStateOf(preferences.residenceBuildingCode() ?: "OPH") }
    var accessPointId by remember { mutableStateOf(preferences.campusAccessPointId()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Route options", fontSize = 19.sp, lineHeight = 23.sp, fontWeight = FontWeight.Medium)
            Text(
                "The same walking and campus-arrival assumptions used by Gapwise on the web.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            PreferenceLabel("Walking")
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                RouteMode.entries.forEach { mode ->
                    FilterChip(
                        selected = routeMode == mode,
                        onClick = {
                            routeMode = mode
                            preferences.setRouteMode(mode)
                        },
                        label = { Text(mode.label, fontSize = 11.sp) },
                    )
                }
            }
            CompactSlider(
                label = "Walking speed",
                valueLabel = "${"%.2f".format(walkingSpeed)} m/s",
                value = walkingSpeed,
                range = 0.5f..2.5f,
                steps = 39,
                onValueChange = { walkingSpeed = it },
                onFinished = { preferences.setWalkingSpeedMps(walkingSpeed) },
            )
            CompactSlider(
                label = "Transition buffer",
                valueLabel = "$transitionBuffer min",
                value = transitionBuffer.toFloat(),
                range = 0f..30f,
                steps = 29,
                onValueChange = { transitionBuffer = it.roundToInt() },
                onFinished = { preferences.setTransitionBufferMinutes(transitionBuffer) },
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            PreferenceLabel("Campus arrival")
            Text(
                "Gapwise stores where your campus walk begins — never your home address.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 17.sp,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = dayOrigin == DayOrigin.RESIDENCE,
                    onClick = {
                        dayOrigin = DayOrigin.RESIDENCE
                        commuteMode = null
                        accessPointId = null
                        preferences.setDayOrigin(dayOrigin)
                        preferences.setCommuteMode(null)
                        preferences.setCampusAccessPointId(null)
                        preferences.setResidenceBuildingCode(residenceCode)
                    },
                    label = { Text("Live on campus", fontSize = 11.sp) },
                )
                FilterChip(
                    selected = dayOrigin == DayOrigin.COMMUTE,
                    onClick = {
                        dayOrigin = DayOrigin.COMMUTE
                        preferences.setDayOrigin(dayOrigin)
                        preferences.setResidenceBuildingCode(null)
                    },
                    label = { Text("Commute", fontSize = 11.sp) },
                )
            }

            if (dayOrigin == DayOrigin.RESIDENCE) {
                Text("Residence building", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(Residences, key = { it.id }) { residence ->
                        FilterChip(
                            selected = residenceCode == residence.id,
                            onClick = {
                                residenceCode = residence.id
                                preferences.setResidenceBuildingCode(residence.id)
                            },
                            label = { Text("${residence.label} (${residence.id})", fontSize = 10.5.sp) },
                        )
                    }
                }
            } else {
                Text("How you arrive", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(CommuteMode.entries) { mode ->
                        FilterChip(
                            selected = commuteMode == mode,
                            onClick = {
                                commuteMode = mode
                                accessPointId = null
                                preferences.setCommuteMode(mode)
                                preferences.setCampusAccessPointId(null)
                            },
                            label = { Text(mode.label, fontSize = 10.5.sp) },
                        )
                    }
                }
                val points = when (commuteMode) {
                    CommuteMode.TRANSIT -> TransitPoints
                    CommuteMode.PARKING -> ParkingPoints
                    CommuteMode.PICKUP, null -> emptyList()
                }
                if (points.isNotEmpty()) {
                    Text("Campus arrival point", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        items(points, key = { it.id }) { point ->
                            FilterChip(
                                selected = accessPointId == point.id,
                                onClick = {
                                    accessPointId = point.id
                                    preferences.setCampusAccessPointId(point.id)
                                },
                                label = { Text(point.label, fontSize = 10.5.sp) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreferenceLabel(text: String) {
    Text(
        text.uppercase(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
    )
}

@Composable
private fun CompactSlider(
    label: String,
    valueLabel: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    onFinished: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(valueLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            onValueChangeFinished = onFinished,
            valueRange = range,
            steps = steps,
        )
    }
}
