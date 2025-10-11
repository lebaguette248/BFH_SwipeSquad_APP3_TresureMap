package com.swipesquad.treasuremap.ui

import android.Manifest
import android.content.pm.PackageManager
import android.preference.PreferenceManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.swipesquad.treasuremap.R
import com.swipesquad.treasuremap.data.TreasureMarker
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@Composable
fun OSMTreasureMap(
    treasureMarkers: List<TreasureMarker>,
    onAddMarker: (lat: Double, lng: Double) -> Unit,
    onRemoveMarker: (lat: Double, lng: Double) -> Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Permission handling
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
    }

    // Configure the OSMdroid file caching system
    DisposableEffect(context) {
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context))
        onDispose {
            // Cleanup
        }
    }

    if (hasLocationPermission) {
        Box(modifier = modifier.fillMaxSize()) {
            val mapView = remember {
                MapView(context).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(18.0)
                }
            }

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> mapView.onResume()
                        Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                        else -> {}
                    }
                }

                lifecycleOwner.lifecycle.addObserver(observer)

                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize(),
                update = { map ->
                    // Add my location overlay
                    val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), map)
                    locationOverlay.enableMyLocation()
                    locationOverlay.enableFollowLocation()
                    map.overlays.add(locationOverlay)

                    // Clear previous markers and overlays
                    map.overlays.removeIf { it is Marker || it is MapEventsOverlay }

                    // Add map events overlay for handling taps and long presses
                    val mapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                            return false // We're not handling single taps
                        }

                        override fun longPressHelper(p: GeoPoint): Boolean {
                            // Handle long press - add a marker at this location
                            onAddMarker(p.latitude, p.longitude)
                            return true
                        }
                    })
                    map.overlays.add(0, mapEventsOverlay) // Add at index 0 to ensure it receives events first

                    // Add treasure markers
                    treasureMarkers.forEach { treasureMarker ->
                        val marker = Marker(map)
                        marker.position = GeoPoint(treasureMarker.latitude, treasureMarker.longitude)
                        marker.title = treasureMarker.title
                        marker.snippet = treasureMarker.description
                        marker.setOnMarkerClickListener { marker, _ ->
                            onRemoveMarker(marker.position.latitude, marker.position.longitude)
                            true
                        }
                        map.overlays.add(marker)
                    }

                    map.invalidate()
                }
            )
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }) {
                Text("Request Location Permission")
            }
        }
    }
}