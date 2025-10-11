package com.swipesquad.treasuremap.data

/**
 * Represents a marker on the treasure map
 */
data class TreasureMarker(
    val id: String = "",
    val latitude: Double,
    val longitude: Double,
    val title: String = "Flag Post",
    val description: String = ""
)