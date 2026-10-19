package com.paceghost.app.data.gpx

import com.paceghost.app.data.local.entity.TrackPointEntity
import com.paceghost.app.data.local.entity.WalkEntity
import com.paceghost.app.domain.engine.GeoMath
import com.paceghost.app.domain.engine.PaceCalculator
import com.paceghost.app.domain.model.TrackPoint
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.time.Instant
import java.time.format.DateTimeParseException
import javax.xml.parsers.DocumentBuilderFactory

object GpxImporter {

    /**
     * Parses a GPX 1.1 XML string into a [WalkEntity] and corresponding [List<TrackPointEntity>].
     */
    fun importFromGpx(
        gpxContent: String,
        walkId: Long = 0,
        titleOverride: String? = null
    ): Pair<WalkEntity, List<TrackPointEntity>> {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        try {
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        } catch (_: Exception) {
            // Some parsers might not support specific security features
        }

        val builder = factory.newDocumentBuilder()
        val document = builder.parse(ByteArrayInputStream(gpxContent.toByteArray(Charsets.UTF_8)))
        document.documentElement.normalize()

        // 1. Extract Walk title
        var parsedTitle: String? = null
        val trkList = document.getElementsByTagName("trk")
        if (trkList.length > 0) {
            val trkElem = trkList.item(0) as? Element
            parsedTitle = getChildElementText(trkElem, "name")
        }
        if (parsedTitle.isNullOrBlank()) {
            val metadataList = document.getElementsByTagName("metadata")
            if (metadataList.length > 0) {
                val metaElem = metadataList.item(0) as? Element
                parsedTitle = getChildElementText(metaElem, "name")
            }
        }
        val finalTitle = titleOverride ?: parsedTitle?.takeIf { it.isNotBlank() } ?: "Imported Walk"

        // 2. Extract track points
        val trkptList = document.getElementsByTagName("trkpt")
        val parsedPoints = ArrayList<RawPointData>(trkptList.length)

        for (i in 0 until trkptList.length) {
            val ptElem = trkptList.item(i) as? Element ?: continue
            val latStr = ptElem.getAttribute("lat")
            val lonStr = ptElem.getAttribute("lon")
            if (latStr.isBlank() || lonStr.isBlank()) continue

            val lat = latStr.toDoubleOrNull() ?: continue
            val lon = lonStr.toDoubleOrNull() ?: continue

            val eleText = getChildElementText(ptElem, "ele")
            val altitude = eleText?.toDoubleOrNull() ?: 0.0

            val timeText = getChildElementText(ptElem, "time")
            val timestamp = parseIso8601(timeText)

            // Optional extensions
            val extElem = getChildElement(ptElem, "extensions")
            val speedText = getChildElementText(extElem, "speed")
            val speed = speedText?.toFloatOrNull()

            val stepText = getChildElementText(extElem, "stepCount")
            val stepCount = stepText?.toIntOrNull()

            val elapsedText = getChildElementText(extElem, "elapsedSeconds")
            val elapsedSec = elapsedText?.toLongOrNull()

            val cumDistText = getChildElementText(extElem, "cumulativeDistance")
            val cumDist = cumDistText?.toDoubleOrNull()

            parsedPoints.add(
                RawPointData(
                    lat = lat,
                    lon = lon,
                    altitude = altitude,
                    timestamp = timestamp,
                    speed = speed,
                    stepCount = stepCount,
                    elapsedSeconds = elapsedSec,
                    cumulativeDistance = cumDist
                )
            )
        }

        if (parsedPoints.isEmpty()) {
            val emptyWalk = WalkEntity(
                id = walkId,
                title = finalTitle,
                startTime = System.currentTimeMillis(),
                endTime = System.currentTimeMillis(),
                durationSeconds = 0L,
                distanceMeters = 0.0,
                stepCount = 0,
                averagePaceSecondsPerKm = 0.0
            )
            return Pair(emptyWalk, emptyList())
        }

        // 3. Process timestamps, distance, speed, and steps
        val startTime = parsedPoints.first().timestamp ?: System.currentTimeMillis()
        var currentSteps = 0
        var fractionalSteps = 0.0

        // Ensure cumulative distance is calculated accurately
        val domainPoints = parsedPoints.map {
            TrackPoint(
                lat = it.lat,
                lon = it.lon,
                altitude = it.altitude,
                timestamp = it.timestamp ?: startTime
            )
        }
        val withDistances = GeoMath.computeCumulativeDistances(domainPoints)

        val pointEntities = ArrayList<TrackPointEntity>(parsedPoints.size)

        for (i in parsedPoints.indices) {
            val raw = parsedPoints[i]
            val withDist = withDistances[i]

            val pointTimestamp = raw.timestamp ?: (startTime + i * 1000L)
            val elapsedSec = raw.elapsedSeconds ?: ((pointTimestamp - startTime) / 1000L)
            val cumulativeDistance = raw.cumulativeDistance ?: withDist.cumulativeDistance

            val prevTimestamp = if (i > 0) pointEntities[i - 1].timestamp else startTime
            val deltaSec = maxOf(0.1, (pointTimestamp - prevTimestamp) / 1000.0)
            val prevDist = if (i > 0) pointEntities[i - 1].cumulativeDistance else 0.0
            val deltaDist = maxOf(0.0, cumulativeDistance - prevDist)

            val speed = raw.speed ?: (deltaDist / deltaSec).toFloat()
            val speedKmh = speed * 3.6

            val stepsAtPoint: Int
            if (raw.stepCount != null) {
                stepsAtPoint = raw.stepCount
                currentSteps = stepsAtPoint
            } else {
                val cadenceStepsPerSec = if (speedKmh > 0.0) (speedKmh / 5.0) * (115.0 / 60.0) else 0.0
                fractionalSteps += cadenceStepsPerSec * deltaSec
                val added = fractionalSteps.toInt()
                currentSteps += added
                fractionalSteps -= added
                stepsAtPoint = currentSteps
            }

            pointEntities.add(
                TrackPointEntity(
                    id = 0,
                    walkId = walkId,
                    latitude = raw.lat,
                    longitude = raw.lon,
                    altitude = raw.altitude,
                    timestamp = pointTimestamp,
                    elapsedSeconds = elapsedSec,
                    cumulativeDistance = cumulativeDistance,
                    speed = speed,
                    stepCountAtPoint = stepsAtPoint
                )
            )
        }

        val totalDistance = pointEntities.last().cumulativeDistance
        val endTime = pointEntities.last().timestamp
        val durationSeconds = maxOf(0L, (endTime - startTime) / 1000L)
        val totalSteps = pointEntities.last().stepCountAtPoint
        val avgPace = PaceCalculator.calculatePaceSecondsPerKm(totalDistance, durationSeconds)

        val walkEntity = WalkEntity(
            id = walkId,
            title = finalTitle,
            startTime = startTime,
            endTime = endTime,
            durationSeconds = durationSeconds,
            distanceMeters = totalDistance,
            stepCount = totalSteps,
            averagePaceSecondsPerKm = avgPace
        )

        return Pair(walkEntity, pointEntities)
    }

    private fun parseIso8601(isoString: String?): Long? {
        if (isoString.isNullOrBlank()) return null
        return try {
            Instant.parse(isoString.trim()).toEpochMilli()
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private fun getChildElement(parent: Element?, tagName: String): Element? {
        if (parent == null) return null
        val nl = parent.getElementsByTagName(tagName)
        return if (nl.length > 0) nl.item(0) as? Element else null
    }

    private fun getChildElementText(parent: Element?, tagName: String): String? {
        if (parent == null) return null
        val nl = parent.getElementsByTagName(tagName)
        if (nl.length == 0) return null
        val item = nl.item(0) ?: return null
        return item.textContent?.trim()
    }

    private data class RawPointData(
        val lat: Double,
        val lon: Double,
        val altitude: Double,
        val timestamp: Long?,
        val speed: Float?,
        val stepCount: Int?,
        val elapsedSeconds: Long?,
        val cumulativeDistance: Double?
    )
}
