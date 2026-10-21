package com.paceghost.app.domain.engine

import com.paceghost.app.domain.model.TrackPoint
import kotlin.math.abs

/**
 * SmartRouteNamingEngine samples GPS track points and formats a descriptive
 * route title based on the traversed streets, landmarks, and route geometry.
 */
object SmartRouteNamingEngine {

    private val REPLACEMENTS = listOf(
        // English replacements
        Regex("(?i)\\bStreet\\b") to "St",
        Regex("(?i)\\bAvenue\\b") to "Ave",
        Regex("(?i)\\bBoulevard\\b") to "Blvd",
        Regex("(?i)\\bRoad\\b") to "Rd",
        Regex("(?i)\\bDrive\\b") to "Dr",
        Regex("(?i)\\bLane\\b") to "Ln",
        Regex("(?i)\\bSquare\\b") to "Sq",
        Regex("(?i)\\bHighway\\b") to "Hwy",
        Regex("(?i)\\bCourt\\b") to "Ct",
        Regex("(?i)\\bPlace\\b") to "Pl",

        // Cyrillic replacements using Unicode escapes
        // ulitsa -> ul.
        Regex("(?U)(?i)\\b\\u0443\\u043B\\u0438\\u0446\\u0430\\b") to "\u0443\u043B.",
        // prospekt -> pr-kt
        Regex("(?U)(?i)\\b\\u043F\\u0440\\u043E\\u0441\\u043F\\u0435\\u043A\\u0442\\b") to "\u043F\u0440-\u043A\u0442",
        // pereulok -> per.
        Regex("(?U)(?i)\\b\\u043F\\u0435\\u0440\\u0435\\u0443\\u043B\\u043E\\u043A\\b") to "\u043F\u0435\u0440.",
        // naberezhnaya -> nab.
        Regex("(?U)(?i)\\b\\u043D\\u0430\\u0431\\u0435\\u0440\\u0435\\u0436\\u043D\\u0430\\u044F\\b") to "\u043D\u0430\u0431.",
        // shosse -> sh.
        Regex("(?U)(?i)\\b\\u0448\\u043E\\u0441\\u0441\\u0435\\b") to "\u0448.",
        // bulvar -> b-r
        Regex("(?U)(?i)\\b\\u0431\\u0443\\u043B\\u044C\\u0432\\u0430\\u0440\\b") to "\u0431-\u0440",
        // ploshchad -> pl.
        Regex("(?U)(?i)\\b\\u043F\\u043B\\u043E\\u0449\\u0430\\u0434\\u044C\\b") to "\u043F\u043B.",
        // proezd -> pr-d
        Regex("(?U)(?i)\\b\\u043F\\u0440\\u043E\\u0435\\u0437\\u0434\\b") to "\u043F\u0440-\u0434"
    )

    /**
     * Samples 2 to [maxPoints] key track points along the cumulative walk distance.
     * Always includes the first and last point. Intermediate points are spaced evenly.
     */
    fun sampleKeyPoints(
        points: List<TrackPoint>,
        minDistanceMeters: Double = 400.0,
        maxPoints: Int = 5
    ): List<TrackPoint> {
        if (points.isEmpty()) return emptyList()
        if (points.size <= 2) return points

        val firstPt = points.first()
        val lastPt = points.last()
        val startDist = firstPt.cumulativeDistance
        val totalDistance = lastPt.cumulativeDistance - startDist

        if (totalDistance < 100.0) {
            return listOf(firstPt, lastPt).distinct()
        }

        val pointCount = ((totalDistance / minDistanceMeters).toInt() + 1).coerceIn(2, maxPoints)
        if (pointCount <= 2) {
            return listOf(firstPt, lastPt)
        }

        val sampled = mutableListOf<TrackPoint>()
        sampled.add(firstPt)

        val stepDist = totalDistance / (pointCount - 1)
        for (i in 1 until (pointCount - 1)) {
            val targetDist = startDist + (i * stepDist)
            val closest = points.minByOrNull { abs(it.cumulativeDistance - targetDist) }
            if (closest != null && !sampled.contains(closest)) {
                sampled.add(closest)
            }
        }

        if (!sampled.contains(lastPt)) {
            sampled.add(lastPt)
        }

        return sampled
    }

    /**
     * Compacts street type designations (e.g. Street -> St, Avenue -> Ave).
     */
    fun compactStreetName(rawName: String): String {
        var compacted = rawName.trim()
        for ((regex, replacement) in REPLACEMENTS) {
            compacted = regex.replace(compacted, replacement)
        }
        return compacted.replace(Regex("\\s+"), " ").trim()
    }

    /**
     * Formats a list of resolved landmark names into a clean, intuitive route title.
     * Deduplicates consecutive identical streets and formats loop or linear routes.
     */
    fun formatRouteTitle(rawLandmarks: List<String>): String? {
        val cleaned = rawLandmarks
            .map { compactStreetName(it) }
            .filter { it.isNotBlank() }

        if (cleaned.isEmpty()) return null

        // Deduplicate consecutive identical names
        val deduplicated = mutableListOf<String>()
        for (name in cleaned) {
            if (deduplicated.isEmpty() || !deduplicated.last().equals(name, ignoreCase = true)) {
                deduplicated.add(name)
            }
        }

        return when {
            deduplicated.isEmpty() -> null
            deduplicated.size == 1 -> deduplicated.first()
            deduplicated.size == 2 -> "${deduplicated[0]} \u2192 ${deduplicated[1]}"
            else -> {
                val start = deduplicated.first()
                val end = deduplicated.last()
                val isLoop = start.equals(end, ignoreCase = true)

                if (isLoop) {
                    val mid = deduplicated.subList(1, deduplicated.size - 1)
                        .firstOrNull { !it.equals(start, ignoreCase = true) }
                    if (mid != null) {
                        "$start Loop (via $mid)"
                    } else {
                        "$start Loop"
                    }
                } else {
                    if (deduplicated.size == 3) {
                        "${deduplicated[0]} \u2192 ${deduplicated[1]} \u2192 ${deduplicated[2]}"
                    } else {
                        // For 4+ unique streets, pick start, a representative middle, and finish
                        val midIndex = deduplicated.size / 2
                        "${deduplicated.first()} \u2192 ${deduplicated[midIndex]} \u2192 ${deduplicated.last()}"
                    }
                }
            }
        }
    }
}
