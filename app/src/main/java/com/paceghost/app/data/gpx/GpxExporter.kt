package com.paceghost.app.data.gpx

import android.content.Context
import com.paceghost.app.data.local.entity.TrackPointEntity
import com.paceghost.app.data.local.entity.WalkEntity
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale

object GpxExporter {

    /**
     * Generates a safe, sanitized filename for exporting a walk in GPX format.
     * Format: "${safeTitle}_${yyyyMMdd_HHmm}.gpx"
     */
    fun generateGpxFileName(title: String, startTime: Long): String {
        val sanitized = title.trim()
            .replace(Regex("[^\\p{L}\\p{Nd}_-]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
        val safeTitle = if (sanitized.isBlank()) "Walk" else sanitized
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date(startTime))
        return "${safeTitle}_${dateStr}.gpx"
    }

    /**
     * Exports a walk and its track points to a file in the given output directory.
     */
    fun exportToFile(outputDir: File, walk: WalkEntity, points: List<TrackPointEntity>): File {
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        val fileName = generateGpxFileName(walk.title, walk.startTime)
        val file = File(outputDir, fileName)
        val xml = exportToGpx(walk, points)
        file.writeText(xml, Charsets.UTF_8)
        return file
    }

    /**
     * Exports a walk to the app's cache directory under "gpx/", cleaning up files older than 24h.
     */
    fun exportToCache(context: Context, walk: WalkEntity, points: List<TrackPointEntity>): File {
        val gpxDir = File(context.cacheDir, "gpx")
        if (gpxDir.exists()) {
            val now = System.currentTimeMillis()
            gpxDir.listFiles()?.forEach { oldFile ->
                if (oldFile.isFile && oldFile.name.endsWith(".gpx")) {
                    if (now - oldFile.lastModified() > 24 * 60 * 60 * 1000L) {
                        oldFile.delete()
                    }
                }
            }
        }
        return exportToFile(gpxDir, walk, points)
    }

    /**
     * Exports a walk and its track points into a standard GPX 1.1 XML string.
     */
    fun exportToGpx(walk: WalkEntity, points: List<TrackPointEntity>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>""").append("\n")
        sb.append("""<gpx version="1.1" creator="PaceGhost Android"""")
        sb.append(""" xmlns="http://www.topografix.com/GPX/1/1"""")
        sb.append(""" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"""")
        sb.append(""" xsi:schemaLocation="http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd">""").append("\n")

        val escapedTitle = escapeXml(walk.title)
        val startTimeIso = formatIso8601(walk.startTime)

        sb.append("  <metadata>\n")
        sb.append("    <name>").append(escapedTitle).append("</name>\n")
        sb.append("    <time>").append(startTimeIso).append("</time>\n")
        sb.append("  </metadata>\n")

        sb.append("  <trk>\n")
        sb.append("    <name>").append(escapedTitle).append("</name>\n")
        sb.append("    <trkseg>\n")

        for (pt in points) {
            sb.append(
                String.format(
                    Locale.US,
                    "      <trkpt lat=\"%.6f\" lon=\"%.6f\">\n",
                    pt.latitude,
                    pt.longitude
                )
            )
            sb.append(String.format(Locale.US, "        <ele>%.1f</ele>\n", pt.altitude))
            sb.append("        <time>").append(formatIso8601(pt.timestamp)).append("</time>\n")
            sb.append("        <extensions>\n")
            sb.append(String.format(Locale.US, "          <speed>%.2f</speed>\n", pt.speed))
            sb.append(String.format(Locale.US, "          <stepCount>%d</stepCount>\n", pt.stepCountAtPoint))
            sb.append(String.format(Locale.US, "          <elapsedSeconds>%d</elapsedSeconds>\n", pt.elapsedSeconds))
            sb.append(String.format(Locale.US, "          <cumulativeDistance>%.2f</cumulativeDistance>\n", pt.cumulativeDistance))
            sb.append("        </extensions>\n")
            sb.append("      </trkpt>\n")
        }

        sb.append("    </trkseg>\n")
        sb.append("  </trk>\n")
        sb.append("</gpx>\n")

        return sb.toString()
    }

    private fun escapeXml(input: String): String {
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun formatIso8601(timestampMs: Long): String {
        return try {
            Instant.ofEpochMilli(timestampMs).toString()
        } catch (_: Exception) {
            "1970-01-01T00:00:00Z"
        }
    }
}
