package ca.gapwise.android.feature.map

import ca.gapwise.android.core.persistence.RouteMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.maplibre.geojson.Feature
import java.net.HttpURLConnection
import java.net.URL

internal data class CampusEntrance(
    val id: String,
    val buildingCode: String,
    val label: String,
    val longitude: Double,
    val latitude: Double,
    val kind: String,
    val accessibility: String,
)

internal data class CampusBuildingGeometry(
    val code: String,
    val name: String,
    val category: String,
    val longitude: Double,
    val latitude: Double,
    val footprint: Feature?,
    val entrances: List<CampusEntrance>,
)

internal data class CampusMapSnapshot(
    val dataVersion: String,
    val buildings: Map<String, CampusBuildingGeometry>,
) {
    val footprintFeatures: List<Feature>
        get() = buildings.values.mapNotNull { it.footprint }
}

internal data class CampusPathGeometry(
    val status: String,
    val coordinates: List<Pair<Double, Double>>,
)

internal data class CampusRoutePreferences(
    val mode: RouteMode,
    val walkingSpeedMps: Float,
    val transitionBufferMinutes: Int,
) {
    fun cacheKey(): String = "${mode.name}|$walkingSpeedMps|$transitionBufferMinutes"
}

internal object CampusMapApi {
    private const val MAP_URL = "https://gapwise.ca/api/utm-map"
    private const val PATH_URL = "https://gapwise.ca/api/utm-path"
    private const val CONNECT_TIMEOUT_MS = 5_000
    private const val READ_TIMEOUT_MS = 8_000

    suspend fun fetchMap(): CampusMapSnapshot = withContext(Dispatchers.IO) {
        parseMap(request(MAP_URL, "GET", null))
    }

    suspend fun fetchPath(
        from: String,
        to: String,
        preferences: CampusRoutePreferences,
    ): CampusPathGeometry = withContext(Dispatchers.IO) {
        if (from == to) return@withContext CampusPathGeometry("same-building", emptyList())
        val body = JSONObject()
            .put("from", from)
            .put("to", to)
            .put(
                "preferences",
                JSONObject()
                    .put("mode", preferences.mode.apiValue())
                    .put("walkingSpeedMps", preferences.walkingSpeedMps.toDouble())
                    .put("transitionBufferMinutes", preferences.transitionBufferMinutes),
            )
            .toString()
        parsePath(request(PATH_URL, "POST", body))
    }

    private fun request(url: String, method: String, body: String?): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "Gapwise-Android/0.1")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            check(status in 200..299) { "Gapwise campus API returned HTTP $status." }
            return text
        } finally {
            connection.disconnect()
        }
    }

    private fun parseMap(raw: String): CampusMapSnapshot {
        val root = JSONObject(raw)
        val buildingsJson = root.getJSONArray("buildings")
        val buildings = LinkedHashMap<String, CampusBuildingGeometry>(buildingsJson.length())
        for (index in 0 until buildingsJson.length()) {
            val item = buildingsJson.getJSONObject(index)
            val code = item.getString("code").uppercase()
            val navigation = item.optJSONArray("navigationPoint")
            if (navigation == null || navigation.length() < 2) continue
            val entrancesJson = item.optJSONArray("entrances") ?: JSONArray()
            val entrances = buildList {
                for (entranceIndex in 0 until entrancesJson.length()) {
                    val entrance = entrancesJson.getJSONObject(entranceIndex)
                    val point = entrance.getJSONArray("coordinates")
                    add(
                        CampusEntrance(
                            id = entrance.getString("id"),
                            buildingCode = code,
                            label = entrance.optString("label", "Mapped entrance"),
                            longitude = point.getDouble(0),
                            latitude = point.getDouble(1),
                            kind = entrance.optString("kind", "entrance"),
                            accessibility = entrance.optString("accessibility", "unknown"),
                        ),
                    )
                }
            }
            val footprint = item.optJSONObject("footprint")?.let { objectJson ->
                runCatching { Feature.fromJson(objectJson.toString()) }.getOrNull()
            }
            buildings[code] = CampusBuildingGeometry(
                code = code,
                name = item.getString("name"),
                category = item.optString("category", "academic"),
                longitude = navigation.getDouble(0),
                latitude = navigation.getDouble(1),
                footprint = footprint,
                entrances = entrances,
            )
        }
        check(buildings.isNotEmpty()) { "Gapwise campus API returned no mapped buildings." }
        return CampusMapSnapshot(
            dataVersion = root.optString("dataVersion", "unknown"),
            buildings = buildings,
        )
    }

    private fun parsePath(raw: String): CampusPathGeometry {
        val root = JSONObject(raw)
        val coordinatesJson = root.optJSONArray("displayCoordinates") ?: JSONArray()
        val coordinates = buildList {
            for (index in 0 until coordinatesJson.length()) {
                val point = coordinatesJson.getJSONArray(index)
                if (point.length() >= 2) add(point.getDouble(0) to point.getDouble(1))
            }
        }
        return CampusPathGeometry(
            status = root.optString("status", if (coordinates.size >= 2) "routed" else "unavailable"),
            coordinates = coordinates,
        )
    }
}

private fun RouteMode.apiValue(): String = when (this) {
    RouteMode.FASTEST -> "fastest"
    RouteMode.PREFER_INDOOR -> "prefer-indoor"
    RouteMode.STEP_FREE -> "step-free"
}
