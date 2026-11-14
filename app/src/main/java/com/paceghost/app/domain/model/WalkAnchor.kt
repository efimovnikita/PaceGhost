package com.paceghost.app.domain.model

/**
 * Walk Anchor: the center of a walk's route bounding box, plus the bounds themselves.
 * Used to place Walk Presence Markers below the heatmap visibility cutoff (zoom < 10).
 */
data class WalkAnchor(
    val walkId: Long,
    val centerLat: Double,
    val centerLon: Double,
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double
)
