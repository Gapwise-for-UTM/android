package ca.gapwise.android.feature.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import ca.gapwise.android.R
import ca.gapwise.android.core.model.Campus
import ca.gapwise.android.core.model.LocationType
import ca.gapwise.android.core.model.Meeting
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView

private const val UTM_LAT = 43.55105
private const val UTM_LON = -79.66475
private const val LIGHT_STYLE = "https://tiles.openfreemap.org/styles/positron"
private const val DARK_STYLE = "https://tiles.openfreemap.org/styles/dark"

private data class BuildingPoint(
    val code: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
)

// Source-backed entrance points shared with Gapwise web's UTM routing dataset.
private val BuildingPoints = listOf(
    BuildingPoint("MN", "Maanjiwe nendamowinan", 43.5513221, -79.6654141),
    BuildingPoint("DH", "Deerfield Hall", 43.5503162, -79.6659651),
    BuildingPoint("IB", "Instructional Centre", 43.5516425, -79.6636318),
    BuildingPoint("DV", "William G. Davis Building", 43.5484051, -79.6617704),
    BuildingPoint("CCT", "Communication, Culture and Technology Building", 43.5495452, -79.6626873),
    BuildingPoint("HM", "Hazel McCallion Academic Learning Centre", 43.5510334, -79.6630169),
    BuildingPoint("KN", "Kaneff Centre", 43.5480958, -79.6635084),
    BuildingPoint("RAWC", "Recreation, Athletics and Wellness Centre", 43.5479331, -79.6606714),
    BuildingPoint("XR", "Student Centre", 43.5488462, -79.6637236),
    BuildingPoint("HB", "Terrence Donnelly Health Sciences Complex", 43.5497526, -79.6622362),
    BuildingPoint("AX", "Academic Annex", 43.5481895, -79.6643646),
    BuildingPoint("DW", "Erindale Studio Theatre", 43.5499078, -79.6661378),
).associateBy { it.code }

@Composable
fun MapScreen(
    meetings: List<Meeting>,
    darkTheme: Boolean,
) {
    val destinations = remember(meetings) {
        meetings
            .filter { it.campus == Campus.UTM && it.locationType == LocationType.PHYSICAL }
            .filter { it.buildingCode != null && BuildingPoints.containsKey(it.buildingCode) }
            .distinctBy { it.buildingCode }
    }
    var query by remember { mutableStateOf("") }
    var selectedCode by remember { mutableStateOf<String?>(null) }
    val results = remember(query) {
        val normalized = query.trim().lowercase()
        if (normalized.isBlank()) emptyList()
        else BuildingPoints.values
            .filter { it.code.lowercase().contains(normalized) || it.name.lowercase().contains(normalized) }
            .take(6)
    }
    val selected = selectedCode?.let(BuildingPoints::get)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .clip(RoundedCornerShape(16.dp)),
    ) {
        key(darkTheme, destinations.joinToString("|") { it.buildingCode.orEmpty() }, selectedCode) {
            UtmMap(
                destinations = destinations,
                darkTheme = darkTheme,
                focusedBuilding = selected,
                onBuildingSelected = { selectedCode = it },
                modifier = Modifier.fillMaxSize(),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .widthIn(max = 360.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text("Search MN, Deerfield, Kaneff…") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_lucide_search),
                        contentDescription = null,
                    )
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            if (query.isNotBlank()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                    tonalElevation = 4.dp,
                ) {
                    Column(modifier = Modifier.padding(vertical = 5.dp)) {
                        if (results.isEmpty()) {
                            Text(
                                "No mapped UTM building matches that search.",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            results.forEach { building ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedCode = building.code
                                            query = ""
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(7.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    ) {
                                        Text(
                                            building.code,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                    Text(building.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (selected != null) {
            val classAtBuilding = destinations.firstOrNull { it.buildingCode == selected.code }
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                tonalElevation = 5.dp,
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "${selected.code} · UTM",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(selected.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (classAtBuilding != null) {
                        Text(
                            "${classAtBuilding.courseCode} · ${classAtBuilding.locationLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UtmMap(
    destinations: List<Meeting>,
    darkTheme: Boolean,
    focusedBuilding: BuildingPoint?,
    onBuildingSelected: (String) -> Unit,
    modifier: Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val styleUrl = if (darkTheme) DARK_STYLE else LIGHT_STYLE
    val mapView = remember(styleUrl, destinations, focusedBuilding) {
        MapLibre.getInstance(context)
        MapView(context).apply {
            onCreate(null)
            onStart()
            onResume()
            getMapAsync { map ->
                val focus = focusedBuilding
                map.cameraPosition = CameraPosition.Builder()
                    .target(if (focus == null) LatLng(UTM_LAT, UTM_LON) else LatLng(focus.latitude, focus.longitude))
                    .zoom(if (focus == null) 15.8 else 17.2)
                    .build()
                map.setStyle(styleUrl) {
                    destinations.forEach { meeting ->
                        val point = meeting.buildingCode?.let(BuildingPoints::get) ?: return@forEach
                        map.addMarker(
                            MarkerOptions()
                                .position(LatLng(point.latitude, point.longitude))
                                .title("${meeting.courseCode} · ${meeting.locationLabel}")
                                .snippet(point.code),
                        )
                    }
                    if (focus != null && destinations.none { it.buildingCode == focus.code }) {
                        map.addMarker(
                            MarkerOptions()
                                .position(LatLng(focus.latitude, focus.longitude))
                                .title("${focus.code} · ${focus.name}")
                                .snippet(focus.code),
                        )
                    }
                    map.setOnMarkerClickListener { marker ->
                        marker.snippet?.takeIf(BuildingPoints::containsKey)?.let(onBuildingSelected)
                        false
                    }
                }
            }
        }
    }

    DisposableEffect(mapView) {
        onDispose {
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}
