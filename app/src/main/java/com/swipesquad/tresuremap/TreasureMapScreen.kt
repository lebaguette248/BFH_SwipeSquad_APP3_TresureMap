package com.swipesquad.tresuremap

import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun TreasureMap(
    treasureMarkers: List<TreasureMarker>,
    onAddMarker: (lat: Double, lng: Double) -> Unit,
    onRemoveMarker: (lat: Double, lng: Double) -> Boolean,
    modifier: Modifier = Modifier
) {
    val fineLocationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    if (fineLocationPermission.status == PermissionStatus.Granted) {
        GoogleMap(
            modifier = modifier.fillMaxSize(),
            uiSettings = MapUiSettings(
                compassEnabled = true,
                myLocationButtonEnabled = true,
                mapToolbarEnabled = true,
                zoomControlsEnabled = false
            ),
            properties = MapProperties(
                isMyLocationEnabled = true,
                isBuildingEnabled = false,
                isIndoorEnabled = false,
                isTrafficEnabled = false,
                mapStyleOptions = MapStyleOptions(
                    """
                    [
                      {
                        "featureType": "administrative",
                        "elementType": "geometry",
                        "stylers": [{"visibility": "off"}]
                      },
                      {
                        "featureType": "poi",
                        "stylers": [{"visibility": "off"}]
                      },
                      {
                        "featureType": "road",
                        "elementType": "labels.icon",
                        "stylers": [{"visibility": "off"}]
                      },
                      {
                        "featureType": "transit",
                        "stylers": [{"visibility": "off"}]
                      }
                    ]
                    """.trimIndent()
                )
            ),
            onMapLongClick = { latLng ->
                onAddMarker(latLng.latitude, latLng.longitude)
            }
        ) {
            treasureMarkers
                .map { LatLng(it.latitude, it.longitude) }
                .forEach { latLng ->
                    Marker(
                        state = MarkerState(position = latLng),
                        onClick = {
                            onRemoveMarker(it.position.latitude, it.position.longitude)
                        }
                    )
                }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = { fineLocationPermission.launchPermissionRequest() }) {
                Text(stringResource(R.string.request_permission_location_permission))
            }
        }
    }
}
