package com.swipesquad.treasuremap.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Repository for managing treasure markers
 */
class TreasureRepository(private val context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("treasure_prefs", Context.MODE_PRIVATE)

    private val _markers = MutableStateFlow<List<TreasureMarker>>(emptyList())
    val markers: StateFlow<List<TreasureMarker>> = _markers.asStateFlow()

    init {
        loadMarkers()
    }

    private fun loadMarkers() {
        val markersJson = sharedPreferences.getString("markers", "[]")
        val jsonArray = JSONArray(markersJson)
        val markers = mutableListOf<TreasureMarker>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            markers.add(
                TreasureMarker(
                    id = obj.getString("id"),
                    latitude = obj.getDouble("latitude"),
                    longitude = obj.getDouble("longitude"),
                    title = obj.getString("title"),
                    description = obj.getString("description")
                )
            )
        }

        _markers.value = markers
    }

    private fun saveMarkers() {
        val jsonArray = JSONArray()
        _markers.value.forEach { marker ->
            val obj = JSONObject()
            obj.put("id", marker.id)
            obj.put("latitude", marker.latitude)
            obj.put("longitude", marker.longitude)
            obj.put("title", marker.title)
            obj.put("description", marker.description)
            jsonArray.put(obj)
        }

        sharedPreferences.edit().putString("markers", jsonArray.toString()).apply()
    }

    fun addMarker(latitude: Double, longitude: Double): TreasureMarker {
        val newMarker = TreasureMarker(
            id = UUID.randomUUID().toString(),
            latitude = latitude,
            longitude = longitude
        )

        _markers.value = _markers.value + newMarker
        saveMarkers()
        return newMarker
    }

    fun removeMarker(latitude: Double, longitude: Double): Boolean {
        val initialSize = _markers.value.size
        _markers.value = _markers.value.filterNot {
            it.latitude == latitude && it.longitude == longitude
        }

        val wasRemoved = initialSize != _markers.value.size
        if (wasRemoved) {
            saveMarkers()
        }
        return wasRemoved
    }

    fun clearAllMarkers() {
        _markers.value = emptyList()
        saveMarkers()
    }

    /**
     * Format coordinates according to the Logbook format requirements
     * Returns a string formatted according to the Logbook specifications
     */
    fun getFormattedCoordinatesForLogbook(): String {
        // NOTE: You need to adapt this format according to the specific Logbook format guide
        val coordinatesBuilder = StringBuilder()

        _markers.value.forEachIndexed { index, marker ->
            coordinatesBuilder.append(
                String.format(
                    "Flag %d: %.6f, %.6f",
                    index + 1,
                    marker.latitude,
                    marker.longitude
                )
            )
            if (index < _markers.value.size - 1) {
                coordinatesBuilder.append("\n")
            }
        }

        return coordinatesBuilder.toString()
    }
}