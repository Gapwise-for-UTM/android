package ca.gapwise.android.feature.map

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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

// Source-backed entrance points shared with the Gapwise web routing dataset.
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
fun MapScreen(meetings: List<Meeting>) {
    val utmDestinations = meetings
        .filter { it.campus == Campus.UTM && it.locationType == LocationType.PHYSICAL }
        .distinctBy { "${it.buildingCode}:${it.room}" }
        .sortedBy { it.startTime }
    val mappedDestinations = utmDestinations.filter { it.buildingCode != null && BuildingPoints.containsKey(it.buildingCode) }
    val unmappedCount = utmDestinations.size - mappedDestinations.size

    LazyColumn(
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    text = "UTM CAMPUS MAP",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Text("Explore Mississauga", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "Pan, pinch, and tap your imported UTM class destinations. Timetable support remains U of T-wide.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            key(mappedDestinations.joinToString("|") { it.id }) {
                UtmMap(
                    destinations = mappedDestinations,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(430.dp)
                        .padding(horizontal = 12.dp)
                        .clip(RoundedCornerShape(18.dp)),
                )
            }
        }

        if (unmappedCount > 0) {
            item {
                Text(
                    text = "$unmappedCount imported UTM location(s) do not have a source-backed native pin yet; they remain visible in Timetable.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        if (utmDestinations.isNotEmpty()) {
            item {
                Text(
                    text = "UTM destinations in your timetable",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                )
            }
            items(utmDestinations, key = { it.id }) { meeting ->
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(15.dp),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(meeting.locationLabel, fontWeight = FontWeight.Bold)
                        Text(
                            text = meeting.courseCode,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 3.dp),
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
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    val styleUrl = if (darkTheme) DARK_STYLE else LIGHT_STYLE

    val mapView = remember(styleUrl, destinations) {
        MapLibre.getInstance(context)
        MapView(context).apply {
            onCreate(null)
            onStart()
            onResume()
            getMapAsync { map ->
                map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(UTM_LAT, UTM_LON))
                    .zoom(15.7)
                    .build()
                map.setStyle(styleUrl) {
                    destinations.forEach { meeting ->
                        val point = meeting.buildingCode?.let(BuildingPoints::get) ?: return@forEach
                        map.addMarker(
                            MarkerOptions()
                                .position(LatLng(point.latitude, point.longitude))
                                .title("${meeting.courseCode} · ${meeting.locationLabel}")
                                .snippet(point.name),
                        )
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

    AndroidView(
        factory = { mapView },
        modifier = modifier,
    )
}
