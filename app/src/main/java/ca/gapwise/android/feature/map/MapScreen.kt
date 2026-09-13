package ca.gapwise.android.feature.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Maximize
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import ca.gapwise.android.core.designsystem.WebControlShape
import ca.gapwise.android.core.designsystem.WebEyebrow
import ca.gapwise.android.core.designsystem.WebSegmentButton
import ca.gapwise.android.core.designsystem.WebSurface
import ca.gapwise.android.core.model.Campus
import ca.gapwise.android.core.model.LocationType
import ca.gapwise.android.core.model.Meeting
import ca.gapwise.android.core.model.Term
import ca.gapwise.android.feature.settings.RoutePreferencesSheet
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.FillExtrusionLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.PropertyFactory.fillColor
import org.maplibre.android.style.layers.PropertyFactory.fillOpacity
import org.maplibre.android.style.layers.PropertyFactory.fillOutlineColor
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max

private const val UTM_LAT = 43.55105
private const val UTM_LON = -79.66475
private const val CAMPUS_ZOOM = 16.0
private const val BUILDING_ZOOM = 17.25
private const val LIGHT_STYLE = "https://tiles.openfreemap.org/styles/positron"
private const val DARK_STYLE = "https://tiles.openfreemap.org/styles/dark"
private const val SELECTED_BUILDING_SOURCE = "gapwise-selected-building-source"
private const val SELECTED_BUILDING_FILL = "gapwise-selected-building-fill"

private val BaseDays = listOf(
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
)

private data class BuildingPoint(
    val code: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val kind: String = "Academic",
)

private data class EntrancePoint(
    val id: String,
    val buildingCode: String,
    val label: String,
    val latitude: Double,
    val longitude: Double,
    val kind: String = "entrance",
    val accessibility: String = "unknown",
)

/**
 * Source-backed entrance points mirrored from gapwise/src/data/utm/entrances.geojson.
 * Keep these coordinates exact: class labels and entrance badges intentionally share
 * the same anchor semantics as the web map.
 */
private val EntrancePoints = listOf(
    EntrancePoint("mn-13738201127", "MN", "Mapped entrance", 43.5513221, -79.6654141),
    EntrancePoint("dh-13568164836", "DH", "Main entrance A", 43.5503162, -79.6659651),
    EntrancePoint("dh-13568164837", "DH", "Main entrance B", 43.5505658, -79.6666995),
    EntrancePoint("dh-13751172451", "DH", "Mapped entrance", 43.5505348, -79.6660506),
    EntrancePoint("ib-2383650599", "IB", "Main entrance", 43.5516425, -79.6636318),
    EntrancePoint("ib-2383653565", "IB", "Mapped entrance", 43.5514803, -79.6642634),
    EntrancePoint("dv-1728238982", "DV", "Main entrance A", 43.5484051, -79.6617704),
    EntrancePoint("dv-13568164844", "DV", "Accessible main entrance", 43.5494213, -79.6625598, accessibility = "accessible"),
    EntrancePoint("dv-13736463909", "DV", "Accessible main entrance", 43.5477041, -79.6618689, accessibility = "accessible"),
    EntrancePoint("dv-370306723", "DV", "Main entrance B", 43.5493975, -79.6623618),
    EntrancePoint("cct-13568164845", "CCT", "Accessible entrance", 43.5495452, -79.6626873, accessibility = "accessible"),
    EntrancePoint("hm-13731205434", "HM", "Main entrance", 43.5510334, -79.6630169),
    EntrancePoint("kn-13568164841", "KN", "Accessible main entrance A", 43.5480958, -79.6635084, accessibility = "accessible"),
    EntrancePoint("kn-1312579120", "KN", "Accessible main entrance B", 43.5479701, -79.6628687, accessibility = "accessible"),
    EntrancePoint("kn-13568164842", "KN", "Accessible main entrance C", 43.5485709, -79.6632372, accessibility = "accessible"),
    EntrancePoint("rawc-13568164832", "RAWC", "Main entrance", 43.5479331, -79.6606714),
    EntrancePoint("xr-13568164843", "XR", "Main entrance", 43.5488462, -79.6637236),
    EntrancePoint("xr-2105676602", "XR", "Mapped entrance", 43.5485377, -79.6639869),
    EntrancePoint("hb-13738956113", "HB", "Main entrance A", 43.5497526, -79.6622362),
    EntrancePoint("hb-13568164846", "HB", "Main entrance B", 43.5494997, -79.6623068),
    EntrancePoint("ax-1728239148", "AX", "Mapped pedestrian approach", 43.5481895, -79.6643646, kind = "approach"),
    EntrancePoint("dw-13767739551", "DW", "Mapped entrance", 43.5499078, -79.6661378),
)

private val EntrancesByBuilding = EntrancePoints.groupBy { it.buildingCode }

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
    val availableTerms = remember(meetings) { meetings.map { it.term }.distinct() }
    var selectedTerm by remember(meetings) {
        mutableStateOf(availableTerms.firstOrNull { it == currentTerm() } ?: availableTerms.firstOrNull() ?: Term.FALL)
    }
    val termMeetings = meetings.filter { it.term == selectedTerm && !it.isAssessmentWindow }
    val routeDays = remember(termMeetings) {
        BaseDays + listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).filter { weekend ->
            termMeetings.any { it.weekday == weekend }
        }
    }
    var weekday by remember(selectedTerm, termMeetings) {
        mutableStateOf(
            LocalDate.now().dayOfWeek.takeIf { current -> termMeetings.any { it.weekday == current } }
                ?: routeDays.firstOrNull { day -> termMeetings.any { it.weekday == day } }
                ?: DayOfWeek.MONDAY,
        )
    }
    val dayMeetings = termMeetings.filter { it.weekday == weekday }.sortedBy { it.startTime }
    var optionsOpen by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            WebSurface(padding = PaddingValues(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        WebEyebrow("Campus route")
                        Text(
                            text = weekday.displayName(),
                            modifier = Modifier.padding(top = 4.dp),
                            fontSize = 20.sp,
                            lineHeight = 23.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.6f).sp,
                        )
                        Text(
                            text = "${dayMeetings.size} ${if (dayMeetings.size == 1) "class" else "classes"} · time-labelled map",
                            modifier = Modifier.padding(top = 2.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                        )
                    }
                    OutlinedButton(
                        onClick = { optionsOpen = true },
                        modifier = Modifier.heightIn(min = 44.dp),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Icon(Icons.Outlined.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                        Text("Options", modifier = Modifier.padding(start = 6.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (availableTerms.size > 1) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        shape = WebControlShape,
                        color = MaterialTheme.colorScheme.background,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Row(modifier = Modifier.padding(3.dp)) {
                            availableTerms.forEach { term ->
                                WebSegmentButton(
                                    label = term.label,
                                    selected = selectedTerm == term,
                                    onClick = { selectedTerm = term },
                                    modifier = Modifier.weight(1f),
                                    minHeight = 38.dp,
                                )
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 9.dp),
                    shape = WebControlShape,
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(modifier = Modifier.padding(3.dp)) {
                        routeDays.forEach { day ->
                            WebSegmentButton(
                                label = day.shortName(),
                                selected = weekday == day,
                                onClick = { weekday = day },
                                modifier = Modifier.weight(1f),
                                minHeight = 42.dp,
                            )
                        }
                    }
                }
            }
        }

        item {
            CampusMapCard(meetings = dayMeetings, darkTheme = darkTheme)
        }
    }

    RoutePreferencesSheet(open = optionsOpen, onDismiss = { optionsOpen = false })
}

@Composable
private fun CampusMapCard(
    meetings: List<Meeting>,
    darkTheme: Boolean,
) {
    val destinations = remember(meetings) {
        meetings
            .filter { it.campus == Campus.UTM && it.locationType == LocationType.PHYSICAL }
            .filter { it.buildingCode != null && BuildingPoints.containsKey(it.buildingCode) }
    }
    var query by remember { mutableStateOf("") }
    var selectedCode by remember { mutableStateOf<String?>(null) }
    var selectedMeetingId by remember { mutableStateOf<String?>(null) }
    var fitRequest by remember { mutableIntStateOf(0) }
    var campusRequest by remember { mutableIntStateOf(0) }
    val results = remember(query) {
        val normalized = query.trim().lowercase()
        val compact = normalized.replace(" ", "")
        if (normalized.isBlank()) emptyList()
        else BuildingPoints.values.filter { point ->
            val code = point.code.lowercase()
            code.contains(normalized) || point.name.lowercase().contains(normalized) || compact.startsWith(code)
        }.take(8)
    }
    val selected = selectedCode?.let(BuildingPoints::get)
    val selectedMeeting = selectedMeetingId?.let { id -> destinations.firstOrNull { it.id == id } }

    Surface(
        modifier = Modifier.fillMaxWidth().height(390.dp),
        shape = RoundedCornerShape(11.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 0.dp,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            UtmMap(
                destinations = destinations,
                darkTheme = darkTheme,
                focusedBuilding = selected,
                focusedMeeting = selectedMeeting,
                fitRequest = fitRequest,
                campusRequest = campusRequest,
                onMeetingSelected = { id ->
                    selectedMeetingId = id
                    selectedCode = null
                    query = ""
                },
                onBuildingSelected = { code ->
                    selectedMeetingId = null
                    selectedCode = code
                    query = ""
                },
                modifier = Modifier.fillMaxSize(),
            )

            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(start = 10.dp, top = 10.dp, end = 68.dp),
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(9.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.tertiary),
                            modifier = Modifier.weight(1f).padding(start = 9.dp),
                            decorationBox = { inner ->
                                if (query.isBlank()) {
                                    Text("Search MN, Deerfield, Kaneff…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                                }
                                inner()
                            },
                        )
                    }
                }

                if (query.isNotBlank()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
                        shape = RoundedCornerShape(9.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.99f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Column(modifier = Modifier.padding(5.dp)) {
                            if (results.isEmpty()) {
                                Text(
                                    "No mapped UTM building matches that search.",
                                    modifier = Modifier.padding(9.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                )
                            } else {
                                results.forEach { building ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                selectedMeetingId = null
                                                selectedCode = building.code
                                                query = ""
                                            }
                                            .padding(horizontal = 10.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                                        ) {
                                            Text(
                                                building.code,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                                color = MaterialTheme.colorScheme.tertiary,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                        Text(building.name, modifier = Modifier.padding(start = 10.dp), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 84.dp, end = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                MapControl(Icons.Outlined.Maximize) {
                    selectedCode = null
                    selectedMeetingId = null
                    fitRequest += 1
                }
                MapControl(Icons.Outlined.MyLocation) {
                    selectedCode = null
                    selectedMeetingId = null
                    campusRequest += 1
                }
            }

            if (selectedMeeting != null) {
                MeetingMapCard(
                    meeting = selectedMeeting,
                    onClose = { selectedMeetingId = null },
                    modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(10.dp),
                )
            } else if (selected != null) {
                BuildingMapCard(
                    building = selected,
                    onClose = { selectedCode = null },
                    modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(10.dp),
                )
            }
        }
    }
}

@Composable
private fun MeetingMapCard(meeting: Meeting, onClose: () -> Unit, modifier: Modifier) {
    val point = meeting.buildingCode?.let(BuildingPoints::get)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(9.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    WebEyebrow("${meeting.buildingCode ?: "UTM"} · UTM")
                    Text(point?.name ?: meeting.locationLabel, modifier = Modifier.padding(top = 4.dp), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${formatTimeLabel(meeting.startTime)} · ${meeting.courseCode} · ${meeting.locationLabel}",
                        modifier = Modifier.padding(top = 5.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                    )
                }
                CloseMapCardButton("Close class details", onClose)
            }
        }
    }
}

@Composable
private fun BuildingMapCard(building: BuildingPoint, onClose: () -> Unit, modifier: Modifier) {
    val mappedEntrances = EntrancesByBuilding[building.code].orEmpty().size
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(9.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    WebEyebrow("${building.code} · ${building.kind}")
                    Text(building.name, modifier = Modifier.padding(top = 5.dp), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                CloseMapCardButton("Close building details", onClose)
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("$mappedEntrances mapped ${if (mappedEntrances == 1) "entrance" else "entrances"}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text("Partial coverage", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.5.sp)
            }
            Text(
                "$mappedEntrances source-backed mapped ${if (mappedEntrances == 1) "entrance is" else "entrances are"} known. Other entrances may be missing. Indoor room paths are not currently mapped.",
                modifier = Modifier.padding(top = 10.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.5.sp,
                lineHeight = 16.sp,
            )
        }
    }
}

@Composable
private fun CloseMapCardButton(description: String, onClose: () -> Unit) {
    Icon(
        imageVector = Icons.Outlined.Close,
        contentDescription = description,
        modifier = Modifier.size(30.dp).clip(RoundedCornerShape(7.dp)).clickable(onClick = onClose).padding(7.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun MapControl(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(38.dp).clip(RoundedCornerShape(9.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(9.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(17.dp))
        }
    }
}

@Composable
private fun UtmMap(
    destinations: List<Meeting>,
    darkTheme: Boolean,
    focusedBuilding: BuildingPoint?,
    focusedMeeting: Meeting?,
    fitRequest: Int,
    campusRequest: Int,
    onMeetingSelected: (String) -> Unit,
    onBuildingSelected: (String) -> Unit,
    modifier: Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val styleUrl = if (darkTheme) DARK_STYLE else LIGHT_STYLE
    val meetingSelection by rememberUpdatedState(onMeetingSelected)
    val buildingSelection by rememberUpdatedState(onBuildingSelected)

    val mapView = remember(context, styleUrl) {
        MapLibre.getInstance(context)
        MapView(context).apply {
            onCreate(null)
            onStart()
            onResume()
            getMapAsync { map ->
                map.uiSettings.setLogoEnabled(false)
                map.uiSettings.setAttributionEnabled(true)
                map.uiSettings.setCompassEnabled(false)
                map.uiSettings.setAllGesturesEnabled(true)
                map.cameraPosition = CameraPosition.Builder().target(LatLng(UTM_LAT, UTM_LON)).zoom(CAMPUS_ZOOM).build()
                map.setStyle(styleUrl)
            }
        }
    }

    DisposableEffect(mapView) {
        var mapClickListener: org.maplibre.android.maps.MapLibreMap.OnMapClickListener? = null
        mapView.getMapAsync { map ->
            map.setOnMarkerClickListener { marker ->
                val snippet = marker.snippet.orEmpty()
                when {
                    snippet.startsWith("meeting:") -> {
                        meetingSelection(snippet.removePrefix("meeting:"))
                        true
                    }
                    snippet.startsWith("entrance:") -> {
                        buildingSelection(snippet.substringAfter("entrance:").substringBefore(':'))
                        true
                    }
                    else -> false
                }
            }
            val listener = org.maplibre.android.maps.MapLibreMap.OnMapClickListener { latLng ->
                val style = map.style ?: return@OnMapClickListener false
                val feature = renderedBuildingFeature(map, style, latLng, preferredCode = null)
                    ?: return@OnMapClickListener false
                val building = buildingForFeature(feature, latLng) ?: return@OnMapClickListener false
                highlightFeature(style, feature, darkTheme)
                buildingSelection(building.code)
                true
            }
            map.addOnMapClickListener(listener)
            mapClickListener = listener
        }
        onDispose {
            mapView.getMapAsync { map -> mapClickListener?.let(map::removeOnMapClickListener) }
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    LaunchedEffect(destinations, focusedBuilding?.code, focusedMeeting?.id, mapView, darkTheme) {
        mapView.getMapAsync { map ->
            map.getStyle { style ->
                map.removeAnnotations()
                val accent = if (darkTheme) 0xFF60A5FA.toInt() else 0xFF146BB8.toInt()
                val routePoints = destinations.mapNotNull { meeting ->
                    meeting.buildingCode?.let(::primaryEntranceForBuilding)?.let { entrance ->
                        LatLng(entrance.latitude, entrance.longitude)
                    } ?: meeting.buildingCode?.let(BuildingPoints::get)?.let { point ->
                        LatLng(point.latitude, point.longitude)
                    }
                }
                if (routePoints.size > 1 && routePoints.distinct().size > 1) {
                    map.addPolyline(PolylineOptions().addAll(routePoints).color(accent).width(4f))
                }

                val grouped = destinations.groupBy { it.buildingCode }
                grouped.forEach { (code, buildingMeetings) ->
                    val buildingCode = code ?: return@forEach
                    val building = BuildingPoints[buildingCode] ?: return@forEach
                    val anchor = primaryEntranceForBuilding(buildingCode)
                    val anchorLat = anchor?.latitude ?: building.latitude
                    val anchorLon = anchor?.longitude ?: building.longitude
                    val ordered = buildingMeetings.sortedBy { it.startTime }
                    ordered.forEachIndexed { index, meeting ->
                        val stackFromBottom = ordered.lastIndex - index
                        map.addMarker(
                            MarkerOptions()
                                .position(LatLng(anchorLat, anchorLon))
                                .icon(
                                    IconFactory.getInstance(context).fromBitmap(
                                        createTimeMarkerBitmap(
                                            label = formatTimeLabel(meeting.startTime),
                                            darkTheme = darkTheme,
                                            density = context.resources.displayMetrics.density,
                                            stackFromBottom = stackFromBottom,
                                            selected = meeting.id == focusedMeeting?.id,
                                        ),
                                    ),
                                )
                                .snippet("meeting:${meeting.id}"),
                        )
                    }
                }

                val entranceBuildingCode = focusedMeeting?.buildingCode ?: focusedBuilding?.code
                EntrancesByBuilding[entranceBuildingCode].orEmpty().forEach { entrance ->
                    map.addMarker(
                        MarkerOptions()
                            .position(LatLng(entrance.latitude, entrance.longitude))
                            .icon(
                                IconFactory.getInstance(context).fromBitmap(
                                    createEntranceMarkerBitmap(
                                        entrance = entrance,
                                        darkTheme = darkTheme,
                                        density = context.resources.displayMetrics.density,
                                    ),
                                ),
                            )
                            .snippet("entrance:${entrance.buildingCode}:${entrance.id}"),
                    )
                }

                if (focusedBuilding == null && focusedMeeting == null) {
                    clearBuildingHighlight(style)
                    fitDayRoute(map, destinations, animated = true)
                }
            }
        }
    }

    LaunchedEffect(focusedBuilding?.code, focusedMeeting?.id, mapView, darkTheme) {
        mapView.getMapAsync { map ->
            val point = focusedMeeting?.buildingCode?.let(BuildingPoints::get) ?: focusedBuilding
            map.getStyle { style ->
                if (point == null) {
                    clearBuildingHighlight(style)
                    return@getStyle
                }
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(point.latitude, point.longitude), BUILDING_ZOOM), 360)
                mapView.postDelayed({
                    map.getStyle { currentStyle ->
                        val feature = renderedBuildingFeature(
                            map,
                            currentStyle,
                            LatLng(point.latitude, point.longitude),
                            preferredCode = point.code,
                        )
                        if (feature != null) highlightFeature(currentStyle, feature, darkTheme)
                    }
                }, 380)
            }
        }
    }

    LaunchedEffect(fitRequest, mapView) {
        if (fitRequest > 0) mapView.getMapAsync { map ->
            map.getStyle { style -> clearBuildingHighlight(style) }
            fitDayRoute(map, destinations, animated = true)
        }
    }

    LaunchedEffect(campusRequest, mapView) {
        if (campusRequest > 0) mapView.getMapAsync { map ->
            map.getStyle { style -> clearBuildingHighlight(style) }
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(UTM_LAT, UTM_LON), CAMPUS_ZOOM), 360)
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

private fun primaryEntranceForBuilding(code: String): EntrancePoint? = EntrancesByBuilding[code]?.firstOrNull()

private fun renderedBuildingFeature(
    map: org.maplibre.android.maps.MapLibreMap,
    style: Style,
    coordinate: LatLng,
    preferredCode: String?,
): Feature? {
    val layerIds = buildingLayerIds(style)
    if (layerIds.isEmpty()) return null
    val center = map.projection.toScreenLocation(coordinate)
    val offsets = listOf(
        0f to 0f,
        8f to 0f,
        -8f to 0f,
        0f to 8f,
        0f to -8f,
        14f to 14f,
        -14f to 14f,
        14f to -14f,
        -14f to -14f,
    )
    val features = offsets.asSequence()
        .flatMap { (dx, dy) -> map.queryRenderedFeatures(PointF(center.x + dx, center.y + dy), *layerIds).asSequence() }
        .distinctBy { it.id() ?: it.toJson() }
        .toList()
    if (features.isEmpty()) return null
    if (preferredCode != null) {
        val building = BuildingPoints[preferredCode]
        val match = features.firstOrNull { featureMatchesBuilding(it, building) }
        if (match != null) return match
    }
    return features.firstOrNull()
}

private fun buildingLayerIds(style: Style): Array<String> = style.layers.mapNotNull { layer ->
    when (layer) {
        is FillLayer -> if (layer.sourceLayer == "building") layer.id else null
        is FillExtrusionLayer -> if (layer.sourceLayer == "building") layer.id else null
        else -> null
    }
}.toTypedArray()

private fun featureMatchesBuilding(feature: Feature, building: BuildingPoint?): Boolean {
    if (building == null) return false
    val candidates = listOf("ref", "name", "name:en", "short_name")
        .mapNotNull { key ->
            runCatching { if (feature.hasProperty(key)) feature.getStringProperty(key) else null }.getOrNull()
        }
        .map { it.trim().lowercase() }
    val code = building.code.lowercase()
    val name = building.name.lowercase()
    return candidates.any { it == code || it == name || it.contains(name) }
}

private fun buildingForFeature(feature: Feature, tap: LatLng): BuildingPoint? {
    BuildingPoints.values.firstOrNull { featureMatchesBuilding(feature, it) }?.let { return it }
    val nearest = BuildingPoints.values.minByOrNull { point ->
        hypot(point.latitude - tap.latitude, (point.longitude - tap.longitude) * 0.73)
    } ?: return null
    val distance = hypot(nearest.latitude - tap.latitude, (nearest.longitude - tap.longitude) * 0.73)
    return nearest.takeIf { distance < 0.00135 }
}

private fun highlightFeature(style: Style, feature: Feature, darkTheme: Boolean) {
    clearBuildingHighlight(style)
    style.addSource(GeoJsonSource(SELECTED_BUILDING_SOURCE, feature))
    val accent = if (darkTheme) "#60A5FA" else "#146BB8"
    style.addLayer(
        FillLayer(SELECTED_BUILDING_FILL, SELECTED_BUILDING_SOURCE).withProperties(
            fillColor(accent),
            fillOpacity(0.28f),
            fillOutlineColor(accent),
        ),
    )
}

private fun clearBuildingHighlight(style: Style) {
    if (style.getLayer(SELECTED_BUILDING_FILL) != null) style.removeLayer(SELECTED_BUILDING_FILL)
    if (style.getSource(SELECTED_BUILDING_SOURCE) != null) style.removeSource(SELECTED_BUILDING_SOURCE)
}

private fun fitDayRoute(map: org.maplibre.android.maps.MapLibreMap, destinations: List<Meeting>, animated: Boolean) {
    val points = destinations.mapNotNull { meeting -> meeting.buildingCode?.let(BuildingPoints::get) }
    if (points.isEmpty()) {
        val update = CameraUpdateFactory.newLatLngZoom(LatLng(UTM_LAT, UTM_LON), CAMPUS_ZOOM)
        if (animated) map.animateCamera(update, 360) else map.moveCamera(update)
        return
    }
    val centerLat = points.map { it.latitude }.average()
    val centerLon = points.map { it.longitude }.average()
    val latSpan = points.maxOf { it.latitude } - points.minOf { it.latitude }
    val lonSpan = points.maxOf { it.longitude } - points.minOf { it.longitude }
    val spread = max(abs(latSpan), abs(lonSpan))
    val zoom = when {
        spread < 0.00025 -> 17.2
        spread < 0.001 -> 16.7
        spread < 0.0025 -> 16.1
        spread < 0.0045 -> 15.6
        else -> 15.2
    }
    val update = CameraUpdateFactory.newLatLngZoom(LatLng(centerLat, centerLon), zoom)
    if (animated) map.animateCamera(update, 360) else map.moveCamera(update)
}

private fun createTimeMarkerBitmap(
    label: String,
    darkTheme: Boolean,
    density: Float,
    stackFromBottom: Int,
    selected: Boolean,
): Bitmap {
    val horizontalPadding = 10f * density
    val verticalPadding = 5f * density
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 10.5f * density
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    val textWidth = textPaint.measureText(label)
    val bodyWidth = (textWidth + horizontalPadding * 2).coerceAtLeast(58f * density)
    val bodyHeight = textPaint.fontMetrics.descent - textPaint.fontMetrics.ascent + verticalPadding * 2
    val shadowPad = 3f * density
    // MapLibre centers custom marker bitmaps on their geographic coordinate. Extra transparent
    // space below the pill therefore produces a stable screen-space upward offset without
    // inventing fake latitude offsets. One full slot remains below the lowest time for E/A/♿.
    val bottomPadding = (64f + stackFromBottom * 58f) * density
    val width = (bodyWidth + shadowPad * 2).toInt().coerceAtLeast(1)
    val height = (bodyHeight + shadowPad * 2 + bottomPadding).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val left = shadowPad
    val top = shadowPad
    val rect = RectF(left, top, left + bodyWidth, top + bodyHeight)
    val radius = 11f * density
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (darkTheme) android.graphics.Color.rgb(38, 145, 220) else android.graphics.Color.rgb(20, 107, 184)
        setShadowLayer(2.5f * density, 0f, 1.5f * density, 0x66000000)
    }
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = (if (selected) 2.2f else 1.4f) * density
        color = if (selected) android.graphics.Color.WHITE
        else if (darkTheme) android.graphics.Color.rgb(113, 198, 255)
        else android.graphics.Color.rgb(10, 79, 137)
    }
    canvas.drawRoundRect(rect, radius, radius, fill)
    canvas.drawRoundRect(rect, radius, radius, stroke)
    val baseline = top + bodyHeight / 2f - (textPaint.fontMetrics.ascent + textPaint.fontMetrics.descent) / 2f
    canvas.drawText(label, left + (bodyWidth - textWidth) / 2f, baseline, textPaint)
    return bitmap
}

private fun createEntranceMarkerBitmap(
    entrance: EntrancePoint,
    darkTheme: Boolean,
    density: Float,
): Bitmap {
    val canvasSize = (38f * density).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(canvasSize, canvasSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = canvasSize / 2f
    val radius = 14f * density
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (darkTheme) android.graphics.Color.rgb(11, 17, 26) else android.graphics.Color.WHITE
        setShadowLayer(3f * density, 0f, 1.5f * density, 0x66000000)
    }
    val accent = if (darkTheme) android.graphics.Color.rgb(96, 165, 250) else android.graphics.Color.rgb(20, 107, 184)
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accent
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
        if (entrance.kind == "approach") pathEffect = DashPathEffect(floatArrayOf(4f * density, 3f * density), 0f)
    }
    canvas.drawCircle(center, center, radius, fill)
    canvas.drawCircle(center, center, radius, stroke)

    val markerText = when {
        entrance.accessibility == "accessible" -> "♿"
        entrance.kind == "approach" -> "A"
        else -> "E"
    }
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accent
        textSize = (if (markerText == "♿") 12f else 12.5f) * density
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    val baseline = center - (textPaint.fontMetrics.ascent + textPaint.fontMetrics.descent) / 2f
    canvas.drawText(markerText, center, baseline, textPaint)
    return bitmap
}

private fun formatTimeLabel(minutes: Int): String {
    val h24 = minutes / 60
    val minute = minutes % 60
    val suffix = if (h24 >= 12) "PM" else "AM"
    val hour = when (val value = h24 % 12) { 0 -> 12 else -> value }
    return "%d:%02d %s".format(hour, minute, suffix)
}

private fun DayOfWeek.displayName(): String = name.lowercase().replaceFirstChar(Char::titlecase)
private fun DayOfWeek.shortName(): String = name.take(3).lowercase().replaceFirstChar(Char::titlecase)

private fun currentTerm(): Term = when (LocalDate.now().monthValue) {
    in 1..4 -> Term.WINTER
    in 5..8 -> Term.SUMMER
    else -> Term.FALL
}
