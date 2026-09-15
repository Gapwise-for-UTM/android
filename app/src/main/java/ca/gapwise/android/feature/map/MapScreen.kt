package ca.gapwise.android.feature.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
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
import ca.gapwise.android.core.persistence.AppPreferences
import ca.gapwise.android.core.persistence.RouteMode
import ca.gapwise.android.feature.settings.RoutePreferencesSheet
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.PropertyFactory.fillColor
import org.maplibre.android.style.layers.PropertyFactory.fillOpacity
import org.maplibre.android.style.layers.PropertyFactory.fillOutlineColor
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.max

private const val UTM_LAT = 43.55105
private const val UTM_LON = -79.66475
private const val CAMPUS_ZOOM = 16.0
private const val BUILDING_ZOOM = 17.25
private const val LIGHT_STYLE = "https://tiles.openfreemap.org/styles/positron"
private const val DARK_STYLE = "https://tiles.openfreemap.org/styles/dark"
private const val BUILDING_HIT_SOURCE = "gapwise-canonical-building-source"
private const val BUILDING_HIT_FILL = "gapwise-canonical-building-hit"
private const val SELECTED_BUILDING_SOURCE = "gapwise-selected-building-source"
private const val SELECTED_BUILDING_FILL = "gapwise-selected-building-fill"

private val BaseDays = listOf(
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
)

private val FallbackEntrances = listOf(
    CampusEntrance("mn-13738201127", "MN", "Mapped entrance", -79.6654141, 43.5513221, "entrance", "unknown"),
    CampusEntrance("dh-13568164836", "DH", "Main entrance A", -79.6659651, 43.5503162, "entrance", "unknown"),
    CampusEntrance("dh-13568164837", "DH", "Main entrance B", -79.6666995, 43.5505658, "entrance", "unknown"),
    CampusEntrance("dh-13751172451", "DH", "Mapped entrance", -79.6660506, 43.5505348, "entrance", "unknown"),
    CampusEntrance("ib-2383650599", "IB", "Main entrance", -79.6636318, 43.5516425, "entrance", "unknown"),
    CampusEntrance("ib-2383653565", "IB", "Mapped entrance", -79.6642634, 43.5514803, "entrance", "unknown"),
    CampusEntrance("dv-1728238982", "DV", "Main entrance A", -79.6617704, 43.5484051, "entrance", "unknown"),
    CampusEntrance("dv-13568164844", "DV", "Accessible main entrance", -79.6625598, 43.5494213, "entrance", "accessible"),
    CampusEntrance("dv-13736463909", "DV", "Accessible main entrance", -79.6618689, 43.5477041, "entrance", "accessible"),
    CampusEntrance("dv-370306723", "DV", "Main entrance B", -79.6623618, 43.5493975, "entrance", "unknown"),
    CampusEntrance("cct-13568164845", "CCT", "Accessible entrance", -79.6626873, 43.5495452, "entrance", "accessible"),
    CampusEntrance("hm-13731205434", "HM", "Main entrance", -79.6630169, 43.5510334, "entrance", "unknown"),
    CampusEntrance("kn-13568164841", "KN", "Accessible main entrance A", -79.6635084, 43.5480958, "entrance", "accessible"),
    CampusEntrance("kn-1312579120", "KN", "Accessible main entrance B", -79.6628687, 43.5479701, "entrance", "accessible"),
    CampusEntrance("kn-13568164842", "KN", "Accessible main entrance C", -79.6632372, 43.5485709, "entrance", "accessible"),
    CampusEntrance("rawc-13568164832", "RAWC", "Main entrance", -79.6606714, 43.5479331, "entrance", "unknown"),
    CampusEntrance("xr-13568164843", "XR", "Main entrance", -79.6637236, 43.5488462, "entrance", "unknown"),
    CampusEntrance("xr-2105676602", "XR", "Mapped entrance", -79.6639869, 43.5485377, "entrance", "unknown"),
    CampusEntrance("hb-13738956113", "HB", "Main entrance A", -79.6622362, 43.5497526, "entrance", "unknown"),
    CampusEntrance("hb-13568164846", "HB", "Main entrance B", -79.6623068, 43.5494997, "entrance", "unknown"),
    CampusEntrance("ax-1728239148", "AX", "Mapped pedestrian approach", -79.6643646, 43.5481895, "approach", "unknown"),
    CampusEntrance("dw-13767739551", "DW", "Mapped entrance", -79.6661378, 43.5499078, "entrance", "unknown"),
)

private val FallbackBuildings: Map<String, CampusBuildingGeometry> = listOf(
    Triple("MN", "Maanjiwe nendamowinan", -79.6654141 to 43.5513221),
    Triple("DH", "Deerfield Hall", -79.6659651 to 43.5503162),
    Triple("IB", "Instructional Centre", -79.6636318 to 43.5516425),
    Triple("DV", "William G. Davis Building", -79.6617704 to 43.5484051),
    Triple("CCT", "Communication, Culture and Technology Building", -79.6626873 to 43.5495452),
    Triple("HM", "Hazel McCallion Academic Learning Centre", -79.6630169 to 43.5510334),
    Triple("KN", "Kaneff Centre", -79.6635084 to 43.5480958),
    Triple("RAWC", "Recreation, Athletics and Wellness Centre", -79.6606714 to 43.5479331),
    Triple("XR", "Student Centre", -79.6637236 to 43.5488462),
    Triple("HB", "Terrence Donnelly Health Sciences Complex", -79.6622362 to 43.5497526),
    Triple("AX", "Academic Annex", -79.6643646 to 43.5481895),
    Triple("DW", "Erindale Studio Theatre", -79.6661378 to 43.5499078),
).associate { (code, name, point) ->
    code to CampusBuildingGeometry(
        code = code,
        name = name,
        category = "academic",
        longitude = point.first,
        latitude = point.second,
        footprint = null,
        entrances = FallbackEntrances.filter { it.buildingCode == code },
    )
}

@Composable
fun MapScreen(
    meetings: List<Meeting>,
    darkTheme: Boolean,
) {
    val context = LocalContext.current
    val preferencesStore = remember { AppPreferences(context.applicationContext) }
    var routePreferences by remember {
        mutableStateOf(
            CampusRoutePreferences(
                preferencesStore.routeMode(),
                preferencesStore.walkingSpeedMps(),
                preferencesStore.transitionBufferMinutes(),
            ),
        )
    }
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

    LaunchedEffect(optionsOpen) {
        if (!optionsOpen) {
            routePreferences = CampusRoutePreferences(
                preferencesStore.routeMode(),
                preferencesStore.walkingSpeedMps(),
                preferencesStore.transitionBufferMinutes(),
            )
        }
    }

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
            CampusMapCard(
                meetings = dayMeetings,
                darkTheme = darkTheme,
                routePreferences = routePreferences,
            )
        }
    }

    RoutePreferencesSheet(open = optionsOpen, onDismiss = { optionsOpen = false })
}

@Composable
private fun CampusMapCard(
    meetings: List<Meeting>,
    darkTheme: Boolean,
    routePreferences: CampusRoutePreferences,
) {
    var snapshot by remember { mutableStateOf<CampusMapSnapshot?>(null) }
    val buildings = snapshot?.buildings ?: FallbackBuildings
    val destinations = remember(meetings, buildings) {
        meetings
            .filter { it.campus == Campus.UTM && it.locationType == LocationType.PHYSICAL }
            .filter { it.buildingCode != null && buildings.containsKey(it.buildingCode) }
    }
    var paths by remember { mutableStateOf<Map<String, CampusPathGeometry>>(emptyMap()) }
    var query by remember { mutableStateOf("") }
    var selectedCode by remember { mutableStateOf<String?>(null) }
    var selectedMeetingId by remember { mutableStateOf<String?>(null) }
    var fitRequest by remember { mutableIntStateOf(0) }
    var campusRequest by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        runCatching { CampusMapApi.fetchMap() }.onSuccess { snapshot = it }
    }

    val routeKey = remember(destinations, routePreferences) {
        destinations.joinToString("|") { it.id } + "@" + routePreferences.cacheKey()
    }
    LaunchedEffect(routeKey) {
        val fetched = coroutineScope {
            destinations.zipWithNext().mapNotNull { (from, to) ->
                val fromCode = from.buildingCode ?: return@mapNotNull null
                val toCode = to.buildingCode ?: return@mapNotNull null
                val key = segmentKey(from, to)
                async {
                    key to runCatching {
                        CampusMapApi.fetchPath(fromCode, toCode, routePreferences)
                    }.getOrNull()
                }
            }.awaitAll()
        }
        paths = fetched.mapNotNull { (key, value) -> value?.let { key to it } }.toMap()
    }

    val results = remember(query, buildings) {
        val normalized = query.trim().lowercase()
        val compact = normalized.replace(" ", "")
        if (normalized.isBlank()) emptyList()
        else buildings.values.filter { building ->
            val code = building.code.lowercase()
            code.contains(normalized) || building.name.lowercase().contains(normalized) || compact.startsWith(code)
        }.take(8)
    }
    val selected = selectedCode?.let(buildings::get)
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
                buildings = buildings,
                paths = paths,
                darkTheme = darkTheme,
                focusedBuildingCode = selected?.code,
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
                    building = selectedMeeting.buildingCode?.let(buildings::get),
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
private fun MeetingMapCard(
    meeting: Meeting,
    building: CampusBuildingGeometry?,
    onClose: () -> Unit,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(9.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                WebEyebrow("${meeting.buildingCode ?: "UTM"} · UTM")
                Text(building?.name ?: meeting.locationLabel, modifier = Modifier.padding(top = 4.dp), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
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

@Composable
private fun BuildingMapCard(building: CampusBuildingGeometry, onClose: () -> Unit, modifier: Modifier) {
    val mappedEntrances = building.entrances.count { it.kind == "entrance" }
    val approaches = building.entrances.count { it.kind == "approach" }
    val uriHandler = LocalUriHandler.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(9.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    WebEyebrow("${building.code} · ${building.category}")
                    Text(building.name, modifier = Modifier.padding(top = 5.dp), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                CloseMapCardButton("Close building details", onClose)
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    when {
                        mappedEntrances > 0 -> "$mappedEntrances mapped ${if (mappedEntrances == 1) "entrance" else "entrances"}"
                        approaches > 0 -> "$approaches mapped ${if (approaches == 1) "approach" else "approaches"}"
                        else -> "No mapped entrances"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text("Partial coverage", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.5.sp)
            }
            Text(
                if (mappedEntrances > 0) "$mappedEntrances source-backed mapped ${if (mappedEntrances == 1) "entrance is" else "entrances are"} known. Other entrances may be missing. Indoor room paths are not currently mapped."
                else "Physical entrance coverage is not established for this building. Indoor room paths are not currently mapped.",
                modifier = Modifier.padding(top = 10.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.5.sp,
                lineHeight = 16.sp,
            )
            OutlinedButton(
                onClick = { uriHandler.openUri("https://data.gapwise.ca/contribute?building=${building.code}") },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp).heightIn(min = 40.dp),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text("Contribute entrance data", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
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
    buildings: Map<String, CampusBuildingGeometry>,
    paths: Map<String, CampusPathGeometry>,
    darkTheme: Boolean,
    focusedBuildingCode: String?,
    focusedMeeting: Meeting?,
    fitRequest: Int,
    campusRequest: Int,
    onMeetingSelected: (String) -> Unit,
    onBuildingSelected: (String) -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val styleUrl = if (darkTheme) DARK_STYLE else LIGHT_STYLE
    val meetingSelection by rememberUpdatedState(onMeetingSelected)
    val buildingSelection by rememberUpdatedState(onBuildingSelected)
    var styleEpoch by remember { mutableIntStateOf(0) }
    var lastAutoFitKey by remember { mutableStateOf<String?>(null) }

    val mapView = remember(context, styleUrl) {
        MapLibre.getInstance(context)
        MapView(context).apply {
            onCreate(null)
            onStart()
            onResume()
            addOnDidFinishLoadingStyleListener { styleEpoch += 1 }
            setOnTouchListener { view, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN ->
                        view.parent?.requestDisallowInterceptTouchEvent(true)
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                        view.parent?.requestDisallowInterceptTouchEvent(false)
                }
                false
            }
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

    LaunchedEffect(mapView) { lastAutoFitKey = null }

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
                if (style.getLayer(BUILDING_HIT_FILL) == null) return@OnMapClickListener false
                val point = map.projection.toScreenLocation(latLng)
                val codes = map.queryRenderedFeatures(point, BUILDING_HIT_FILL)
                    .mapNotNull { feature ->
                        runCatching {
                            if (feature.hasProperty("buildingCode")) feature.getStringProperty("buildingCode") else null
                        }.getOrNull()
                    }
                    .distinct()
                val code = codes.singleOrNull() ?: return@OnMapClickListener false
                buildingSelection(code)
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

    val contentKey = remember(destinations) { destinations.joinToString("|") { it.id } }
    LaunchedEffect(destinations, buildings, paths, focusedMeeting?.id, focusedBuildingCode, styleEpoch, darkTheme, mapView) {
        if (styleEpoch <= 0) return@LaunchedEffect
        mapView.getMapAsync { map ->
            val style = map.style ?: return@getMapAsync
            ensureCanonicalBuildingLayer(style, buildings.values.mapNotNull { it.footprint })
            syncSelectedBuildingHighlight(style, focusedBuildingCode, buildings, darkTheme)
            renderDayOverlays(
                map = map,
                destinations = destinations,
                buildings = buildings,
                paths = paths,
                darkTheme = darkTheme,
                selectedMeetingId = focusedMeeting?.id,
                context = context,
            )
            if (contentKey != lastAutoFitKey) {
                lastAutoFitKey = contentKey
                fitDayRoute(map, destinations, buildings, animated = true)
            }
        }
    }

    LaunchedEffect(focusedBuildingCode, focusedMeeting?.id, buildings, styleEpoch, mapView) {
        if (styleEpoch <= 0) return@LaunchedEffect
        val code = focusedMeeting?.buildingCode ?: focusedBuildingCode ?: return@LaunchedEffect
        val building = buildings[code] ?: return@LaunchedEffect
        mapView.getMapAsync { map ->
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(building.latitude, building.longitude), BUILDING_ZOOM),
                340,
            )
        }
    }

    LaunchedEffect(fitRequest, buildings, mapView) {
        if (fitRequest > 0) mapView.getMapAsync { map ->
            fitDayRoute(map, destinations, buildings, animated = true)
        }
    }

    LaunchedEffect(campusRequest, mapView) {
        if (campusRequest > 0) mapView.getMapAsync { map ->
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(UTM_LAT, UTM_LON), CAMPUS_ZOOM), 340)
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

private fun renderDayOverlays(
    map: org.maplibre.android.maps.MapLibreMap,
    destinations: List<Meeting>,
    buildings: Map<String, CampusBuildingGeometry>,
    paths: Map<String, CampusPathGeometry>,
    darkTheme: Boolean,
    selectedMeetingId: String?,
    context: android.content.Context,
) {
    map.removeAnnotations()
    val accent = if (darkTheme) 0xFF60A5FA.toInt() else 0xFF146BB8.toInt()

    destinations.zipWithNext().forEach { (from, to) ->
        val path = paths[segmentKey(from, to)] ?: return@forEach
        if (path.status != "routed" || path.coordinates.size < 2) return@forEach
        map.addPolyline(
            PolylineOptions()
                .addAll(path.coordinates.map { (longitude, latitude) -> LatLng(latitude, longitude) })
                .color(accent)
                .width(4f),
        )
    }

    val dayBuildingCodes = destinations.mapNotNull { it.buildingCode }.distinct()
    dayBuildingCodes.flatMap { code -> buildings[code]?.entrances.orEmpty() }.forEach { entrance ->
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

    val grouped = destinations.withIndex().groupBy { it.value.buildingCode }
    grouped.forEach { (code, indexedMeetings) ->
        val building = code?.let(buildings::get) ?: return@forEach
        val ordered = indexedMeetings.sortedBy { it.value.startTime }
        ordered.forEachIndexed { stackIndex, indexedMeeting ->
            val meeting = indexedMeeting.value
            val coordinate = meetingAnchor(indexedMeeting.index, destinations, paths, building)
            map.addMarker(
                MarkerOptions()
                    .position(coordinate)
                    .icon(
                        IconFactory.getInstance(context).fromBitmap(
                            createTimeMarkerBitmap(
                                label = formatTimeLabel(meeting.startTime),
                                darkTheme = darkTheme,
                                density = context.resources.displayMetrics.density,
                                stackFromBottom = ordered.lastIndex - stackIndex,
                                selected = meeting.id == selectedMeetingId,
                            ),
                        ),
                    )
                    .snippet("meeting:${meeting.id}"),
            )
        }
    }
}

private fun meetingAnchor(
    index: Int,
    destinations: List<Meeting>,
    paths: Map<String, CampusPathGeometry>,
    building: CampusBuildingGeometry,
): LatLng {
    if (index > 0) {
        val incoming = paths[segmentKey(destinations[index - 1], destinations[index])]
        incoming?.coordinates?.lastOrNull()?.let { (longitude, latitude) ->
            return LatLng(latitude, longitude)
        }
    }
    if (index < destinations.lastIndex) {
        val outgoing = paths[segmentKey(destinations[index], destinations[index + 1])]
        outgoing?.coordinates?.firstOrNull()?.let { (longitude, latitude) ->
            return LatLng(latitude, longitude)
        }
    }
    val entrance = building.entrances.firstOrNull()
    return if (entrance != null) LatLng(entrance.latitude, entrance.longitude)
    else LatLng(building.latitude, building.longitude)
}

private fun segmentKey(from: Meeting, to: Meeting): String = "${from.id}--${to.id}"

private fun ensureCanonicalBuildingLayer(style: Style, features: List<Feature>) {
    if (features.isEmpty()) return
    val collection = FeatureCollection.fromFeatures(features)
    val source = style.getSource(BUILDING_HIT_SOURCE) as? GeoJsonSource
    if (source == null) {
        style.addSource(GeoJsonSource(BUILDING_HIT_SOURCE, collection))
    } else {
        source.setGeoJson(collection)
    }
    if (style.getLayer(BUILDING_HIT_FILL) == null) {
        style.addLayer(
            FillLayer(BUILDING_HIT_FILL, BUILDING_HIT_SOURCE).withProperties(
                fillColor("#000000"),
                fillOpacity(0.001f),
            ),
        )
    }
}

private fun syncSelectedBuildingHighlight(
    style: Style,
    code: String?,
    buildings: Map<String, CampusBuildingGeometry>,
    darkTheme: Boolean,
) {
    clearBuildingHighlight(style)
    val feature = code?.let(buildings::get)?.footprint ?: return
    style.addSource(GeoJsonSource(SELECTED_BUILDING_SOURCE, feature))
    val accent = if (darkTheme) "#60A5FA" else "#146BB8"
    style.addLayer(
        FillLayer(SELECTED_BUILDING_FILL, SELECTED_BUILDING_SOURCE).withProperties(
            fillColor(accent),
            fillOpacity(0.30f),
            fillOutlineColor(accent),
        ),
    )
}

private fun clearBuildingHighlight(style: Style) {
    if (style.getLayer(SELECTED_BUILDING_FILL) != null) style.removeLayer(SELECTED_BUILDING_FILL)
    if (style.getSource(SELECTED_BUILDING_SOURCE) != null) style.removeSource(SELECTED_BUILDING_SOURCE)
}

private fun fitDayRoute(
    map: org.maplibre.android.maps.MapLibreMap,
    destinations: List<Meeting>,
    buildings: Map<String, CampusBuildingGeometry>,
    animated: Boolean,
) {
    val points = destinations.mapNotNull { meeting -> meeting.buildingCode?.let(buildings::get) }
    if (points.isEmpty()) {
        val update = CameraUpdateFactory.newLatLngZoom(LatLng(UTM_LAT, UTM_LON), CAMPUS_ZOOM)
        if (animated) map.animateCamera(update, 340) else map.moveCamera(update)
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
    if (animated) map.animateCamera(update, 340) else map.moveCamera(update)
}

private fun createTimeMarkerBitmap(
    label: String,
    darkTheme: Boolean,
    density: Float,
    stackFromBottom: Int,
    selected: Boolean,
): Bitmap {
    val horizontalPadding = 9f * density
    val verticalPadding = 4.5f * density
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 10.5f * density
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
    }
    val textWidth = textPaint.measureText(label)
    val bodyWidth = (textWidth + horizontalPadding * 2).coerceAtLeast(56f * density)
    val bodyHeight = textPaint.fontMetrics.descent - textPaint.fontMetrics.ascent + verticalPadding * 2
    val shadowPad = 3f * density
    val bottomPadding = (38f + stackFromBottom * 33f) * density
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
    entrance: CampusEntrance,
    darkTheme: Boolean,
    density: Float,
): Bitmap {
    val canvasSize = (34f * density).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(canvasSize, canvasSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val center = canvasSize / 2f
    val radius = 13f * density
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (darkTheme) android.graphics.Color.rgb(11, 17, 26) else android.graphics.Color.WHITE
        setShadowLayer(2.5f * density, 0f, 1.5f * density, 0x66000000)
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
        textSize = (if (markerText == "♿") 11.5f else 12f) * density
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
