package ca.gapwise.android.feature.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.max

private const val UTM_LAT = 43.55105
private const val UTM_LON = -79.66475
private const val CAMPUS_ZOOM = 16.0
private const val BUILDING_ZOOM = 17.6
private const val LIGHT_STYLE = "https://tiles.openfreemap.org/styles/positron"
private const val DARK_STYLE = "https://tiles.openfreemap.org/styles/dark"

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
    val mappedEntrances: Int = 1,
)

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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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
                        Icon(
                            imageVector = Icons.Outlined.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                        )
                        Text("Options", modifier = Modifier.padding(start = 6.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (availableTerms.size > 1) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 9.dp),
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
            CampusMapCard(
                meetings = dayMeetings,
                darkTheme = darkTheme,
            )
        }
    }

    if (optionsOpen) {
        ModalBottomSheet(
            onDismissRequest = { optionsOpen = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("Route options", fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Walking speed, transition buffer, indoor preference, and step-free routing are available from More.",
                    modifier = Modifier.padding(top = 7.dp, bottom = 24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                )
            }
        }
    }
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
            point.code.lowercase().contains(normalized) ||
                point.name.lowercase().contains(normalized) ||
                compact.startsWith(code)
        }.take(8)
    }
    val selected = selectedCode?.let(BuildingPoints::get)
    val selectedMeeting = selectedMeetingId?.let { id -> destinations.firstOrNull { it.id == id } }
    val mapKey = destinations.joinToString("|") { "${it.id}:${it.buildingCode}:${it.startTime}" }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(390.dp),
        shape = RoundedCornerShape(11.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 0.dp,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            key(darkTheme, mapKey) {
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
                    modifier = Modifier.fillMaxSize(),
                )
            }

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
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.tertiary),
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 9.dp),
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 5.dp),
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
                                        Text(
                                            building.name,
                                            modifier = Modifier.padding(start = 10.dp),
                                            fontSize = 12.dp.value.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 84.dp, end = 10.dp),
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
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(10.dp),
                )
            } else if (selected != null) {
                BuildingMapCard(
                    building = selected,
                    onClose = { selectedCode = null },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(10.dp),
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
                    Text(
                        point?.name ?: meeting.locationLabel,
                        modifier = Modifier.padding(top = 4.dp),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "${formatTimeLabel(meeting.startTime)} · ${meeting.courseCode} · ${meeting.locationLabel}",
                        modifier = Modifier.padding(top = 5.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close class details",
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .clickable(onClick = onClose)
                        .padding(7.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BuildingMapCard(building: BuildingPoint, onClose: () -> Unit, modifier: Modifier) {
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
                    Text(
                        building.name,
                        modifier = Modifier.padding(top = 5.dp),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Close building details",
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .clickable(onClick = onClose)
                        .padding(7.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${building.mappedEntrances} mapped ${if (building.mappedEntrances == 1) "entrance" else "entrances"}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Partial coverage",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.5.sp,
                )
            }
            Text(
                "${building.mappedEntrances} source-backed mapped ${if (building.mappedEntrances == 1) "entrance is" else "entrances are"} known. Other entrances may be missing. Indoor room paths are not currently mapped.",
                modifier = Modifier.padding(top = 10.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.5.sp,
                lineHeight = 16.sp,
            )
        }
    }
}

@Composable
private fun MapControl(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(9.dp))
            .clickable(onClick = onClick),
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
    modifier: Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val styleUrl = if (darkTheme) DARK_STYLE else LIGHT_STYLE
    val mapView = remember(styleUrl, destinations) {
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
                map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(UTM_LAT, UTM_LON))
                    .zoom(CAMPUS_ZOOM)
                    .build()
                map.setOnMarkerClickListener { marker ->
                    val snippet = marker.snippet.orEmpty()
                    if (snippet.startsWith("meeting:")) {
                        onMeetingSelected(snippet.removePrefix("meeting:"))
                        true
                    } else {
                        false
                    }
                }
                map.setStyle(styleUrl) {
                    val accent = if (darkTheme) 0xFF60A5FA.toInt() else 0xFF146BB8.toInt()
                    val routePoints = destinations.mapNotNull { meeting ->
                        meeting.buildingCode?.let(BuildingPoints::get)?.let { point ->
                            LatLng(point.latitude, point.longitude)
                        }
                    }
                    if (routePoints.size > 1 && routePoints.distinct().size > 1) {
                        map.addPolyline(
                            PolylineOptions()
                                .addAll(routePoints)
                                .color(accent)
                                .width(4f),
                        )
                    }

                    val grouped = destinations.groupBy { it.buildingCode }
                    grouped.forEach { (code, buildingMeetings) ->
                        val point = code?.let(BuildingPoints::get) ?: return@forEach
                        buildingMeetings.sortedBy { it.startTime }.forEachIndexed { index, meeting ->
                            val latitudeOffset = index * 0.000075
                            map.addMarker(
                                MarkerOptions()
                                    .position(LatLng(point.latitude + latitudeOffset, point.longitude))
                                    .icon(
                                        IconFactory.getInstance(context).fromBitmap(
                                            createTimeMarkerBitmap(
                                                label = formatTimeLabel(meeting.startTime),
                                                darkTheme = darkTheme,
                                                density = context.resources.displayMetrics.density,
                                            ),
                                        ),
                                    )
                                    .snippet("meeting:${meeting.id}"),
                            )
                        }
                    }
                    fitDayRoute(map, destinations, animated = false)
                }
            }
        }
    }

    LaunchedEffect(focusedBuilding?.code, focusedMeeting?.id, mapView) {
        mapView.getMapAsync { map ->
            val point = focusedMeeting?.buildingCode?.let(BuildingPoints::get) ?: focusedBuilding
            if (point != null) {
                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(LatLng(point.latitude, point.longitude), BUILDING_ZOOM),
                    520,
                )
            }
        }
    }

    LaunchedEffect(fitRequest, mapView) {
        if (fitRequest > 0) mapView.getMapAsync { map -> fitDayRoute(map, destinations, animated = true) }
    }

    LaunchedEffect(campusRequest, mapView) {
        if (campusRequest > 0) {
            mapView.getMapAsync { map ->
                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(LatLng(UTM_LAT, UTM_LON), CAMPUS_ZOOM),
                    520,
                )
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

private fun fitDayRoute(map: org.maplibre.android.maps.MapLibreMap, destinations: List<Meeting>, animated: Boolean) {
    val points = destinations.mapNotNull { meeting -> meeting.buildingCode?.let(BuildingPoints::get) }
    if (points.isEmpty()) {
        val update = CameraUpdateFactory.newLatLngZoom(LatLng(UTM_LAT, UTM_LON), CAMPUS_ZOOM)
        if (animated) map.animateCamera(update, 520) else map.moveCamera(update)
        return
    }

    val centerLat = points.map { it.latitude }.average()
    val centerLon = points.map { it.longitude }.average()
    val latSpan = points.maxOf { it.latitude } - points.minOf { it.latitude }
    val lonSpan = points.maxOf { it.longitude } - points.minOf { it.longitude }
    val spread = max(abs(latSpan), abs(lonSpan))
    val zoom = when {
        spread < 0.00025 -> 17.35
        spread < 0.001 -> 16.75
        spread < 0.0025 -> 16.15
        spread < 0.0045 -> 15.65
        else -> 15.2
    }
    val update = CameraUpdateFactory.newLatLngZoom(LatLng(centerLat, centerLon), zoom)
    if (animated) map.animateCamera(update, 520) else map.moveCamera(update)
}

private fun createTimeMarkerBitmap(label: String, darkTheme: Boolean, density: Float): Bitmap {
    val horizontalPadding = 10f * density
    val verticalPadding = 6f * density
    val tailHeight = 5f * density
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 11f * density
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    val textWidth = textPaint.measureText(label)
    val width = (textWidth + horizontalPadding * 2).toInt().coerceAtLeast((58f * density).toInt())
    val bodyHeight = (textPaint.fontMetrics.descent - textPaint.fontMetrics.ascent + verticalPadding * 2).toInt()
    val height = bodyHeight + tailHeight.toInt()
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (darkTheme) android.graphics.Color.rgb(45, 157, 226) else android.graphics.Color.rgb(20, 107, 184)
    }
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.25f * density
        color = if (darkTheme) android.graphics.Color.rgb(113, 198, 255) else android.graphics.Color.rgb(10, 79, 137)
    }
    val radius = 11f * density
    val rect = RectF(0f, 0f, width.toFloat(), bodyHeight.toFloat())
    canvas.drawRoundRect(rect, radius, radius, fill)
    canvas.drawRoundRect(rect, radius, radius, stroke)

    val center = width / 2f
    val tail = Path().apply {
        moveTo(center - 5f * density, bodyHeight.toFloat() - 1f)
        lineTo(center, height.toFloat())
        lineTo(center + 5f * density, bodyHeight.toFloat() - 1f)
        close()
    }
    canvas.drawPath(tail, fill)

    val baseline = bodyHeight / 2f - (textPaint.fontMetrics.ascent + textPaint.fontMetrics.descent) / 2f
    canvas.drawText(label, (width - textWidth) / 2f, baseline, textPaint)
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
