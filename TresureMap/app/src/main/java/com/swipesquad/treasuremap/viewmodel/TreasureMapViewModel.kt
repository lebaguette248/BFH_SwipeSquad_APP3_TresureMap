package com.swipesquad.treasuremap.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.swipesquad.treasuremap.data.TreasureMarker
import com.swipesquad.treasuremap.data.TreasureRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class TreasureMapViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TreasureRepository(application)

    val treasureMarkers: StateFlow<List<TreasureMarker>> = repository.markers.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        emptyList()
    )

    fun addMarker(latitude: Double, longitude: Double) {
        repository.addMarker(latitude, longitude)
    }

    fun removeMarker(latitude: Double, longitude: Double): Boolean {
        return repository.removeMarker(latitude, longitude)
    }

    fun clearAllMarkers() {
        repository.clearAllMarkers()
    }

    fun getFormattedCoordinatesForLogbook(): String {
        return repository.getFormattedCoordinatesForLogbook()
    }
}