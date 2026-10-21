package com.paceghost.app.domain.engine

import com.paceghost.app.domain.model.TrackPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartRouteNamingEngineTest {

    @Test
    fun `sampleKeyPoints returns empty list when points are empty`() {
        val sampled = SmartRouteNamingEngine.sampleKeyPoints(emptyList())
        assertTrue(sampled.isEmpty())
    }

    @Test
    fun `sampleKeyPoints returns all points when points size is 2`() {
        val pt1 = TrackPoint(lat = 55.0, lon = 37.0, cumulativeDistance = 0.0)
        val pt2 = TrackPoint(lat = 55.01, lon = 37.01, cumulativeDistance = 50.0)
        val sampled = SmartRouteNamingEngine.sampleKeyPoints(listOf(pt1, pt2))
        assertEquals(2, sampled.size)
        assertEquals(pt1, sampled.first())
        assertEquals(pt2, sampled.last())
    }

    @Test
    fun `sampleKeyPoints for short walk under 100m returns start and finish`() {
        val points = (0..10).map { i ->
            TrackPoint(lat = 55.0 + i * 0.0001, lon = 37.0, cumulativeDistance = i * 8.0)
        }
        val sampled = SmartRouteNamingEngine.sampleKeyPoints(points)
        assertEquals(2, sampled.size)
        assertEquals(points.first(), sampled.first())
        assertEquals(points.last(), sampled.last())
    }

    @Test
    fun `sampleKeyPoints for 2km walk samples evenly spaced points up to maxPoints`() {
        // 200 points spanning 2000 meters (10m per point)
        val points = (0..200).map { i ->
            TrackPoint(lat = 55.0 + i * 0.0001, lon = 37.0, cumulativeDistance = i * 10.0)
        }
        val sampled = SmartRouteNamingEngine.sampleKeyPoints(points, minDistanceMeters = 500.0, maxPoints = 5)

        assertEquals(5, sampled.size)
        assertEquals(0.0, sampled.first().cumulativeDistance, 0.001)
        assertEquals(2000.0, sampled.last().cumulativeDistance, 0.001)

        // Check monotonicity
        for (i in 0 until sampled.size - 1) {
            assertTrue(sampled[i].cumulativeDistance < sampled[i + 1].cumulativeDistance)
        }
    }

    @Test
    fun `compactStreetName compacts English suffixes`() {
        assertEquals("Main St", SmartRouteNamingEngine.compactStreetName("Main Street"))
        assertEquals("5th Ave", SmartRouteNamingEngine.compactStreetName("5th Avenue"))
        assertEquals("Ocean Blvd", SmartRouteNamingEngine.compactStreetName("Ocean Boulevard"))
        assertEquals("Country Rd", SmartRouteNamingEngine.compactStreetName("Country Road"))
        assertEquals("Sunset Dr", SmartRouteNamingEngine.compactStreetName("Sunset Drive"))
        assertEquals("Park Ln", SmartRouteNamingEngine.compactStreetName("Park Lane"))
        assertEquals("Times Sq", SmartRouteNamingEngine.compactStreetName("Times Square"))
        assertEquals("Pacific Hwy", SmartRouteNamingEngine.compactStreetName("Pacific Highway"))
    }

    @Test
    fun `compactStreetName compacts Cyrillic designations`() {
        // "улица Арбат" -> "ул. Арбат"
        val ulitsaArbat = "\u0443\u043B\u0438\u0446\u0430 \u0410\u0440\u0431\u0430\u0442"
        val expectedUlitsa = "\u0443\u043B. \u0410\u0440\u0431\u0430\u0442"
        assertEquals(expectedUlitsa, SmartRouteNamingEngine.compactStreetName(ulitsaArbat))

        // "Невский проспект" -> "Невский пр-кт"
        val nevsky = "\u041D\u0435\u0432\u0441\u043A\u0438\u0439 \u043F\u0440\u043E\u0441\u043F\u0435\u043A\u0442"
        val expectedNevsky = "\u041D\u0435\u0432\u0441\u043A\u0438\u0439 \u043F\u0440-\u043A\u0442"
        assertEquals(expectedNevsky, SmartRouteNamingEngine.compactStreetName(nevsky))

        // "Камергерский переулок" -> "Камергерский пер."
        val kamergersky = "\u041A\u0430\u043C\u0435\u0440\u0433\u0435\u0440\u0441\u043A\u0438\u0439 \u043F\u0435\u0440\u0435\u0443\u043B\u043E\u043A"
        val expectedKamergersky = "\u041A\u0430\u043C\u0435\u0440\u0433\u0435\u0440\u0441\u043A\u0438\u0439 \u043F\u0435\u0440."
        assertEquals(expectedKamergersky, SmartRouteNamingEngine.compactStreetName(kamergersky))

        // "Кремлевская набережная" -> "Кремлевская наб."
        val kremlNab = "\u041A\u0440\u0435\u043C\u043B\u0435\u0432\u0441\u043A\u0430\u044F \u043D\u0430\u0431\u0435\u0440\u0435\u0436\u043D\u0430\u044F"
        val expectedKreml = "\u041A\u0440\u0435\u043C\u043B\u0435\u0432\u0441\u043A\u0430\u044F \u043D\u0430\u0431."
        assertEquals(expectedKreml, SmartRouteNamingEngine.compactStreetName(kremlNab))
    }

    @Test
    fun `formatRouteTitle returns null for empty or blank list`() {
        assertNull(SmartRouteNamingEngine.formatRouteTitle(emptyList()))
        assertNull(SmartRouteNamingEngine.formatRouteTitle(listOf("", "  ")))
    }

    @Test
    fun `formatRouteTitle returns single street for single landmark`() {
        val title = SmartRouteNamingEngine.formatRouteTitle(listOf("Baker Street"))
        assertEquals("Baker St", title)
    }

    @Test
    fun `formatRouteTitle returns linear arrow route for two distinct streets`() {
        val title = SmartRouteNamingEngine.formatRouteTitle(listOf("Broadway", "5th Avenue"))
        assertEquals("Broadway \u2192 5th Ave", title)
    }

    @Test
    fun `formatRouteTitle deduplicates consecutive identical streets`() {
        val title = SmartRouteNamingEngine.formatRouteTitle(listOf("Broadway", "Broadway", "5th Avenue", "5th Avenue"))
        assertEquals("Broadway \u2192 5th Ave", title)
    }

    @Test
    fun `formatRouteTitle returns three streets with arrows for three distinct streets`() {
        val title = SmartRouteNamingEngine.formatRouteTitle(listOf("Broadway", "42nd Street", "5th Avenue"))
        assertEquals("Broadway \u2192 42nd St \u2192 5th Ave", title)
    }

    @Test
    fun `formatRouteTitle detects loop route when start equals end and formats with via`() {
        val title = SmartRouteNamingEngine.formatRouteTitle(listOf("Main Street", "Oak Avenue", "Main Street"))
        assertEquals("Main St Loop (via Oak Ave)", title)
    }

    @Test
    fun `formatRouteTitle for loop route with no distinct middle formats as Start Loop`() {
        val title = SmartRouteNamingEngine.formatRouteTitle(listOf("Main Street", "Main Street", "Main Street"))
        assertEquals("Main St", title)
    }

    @Test
    fun `formatRouteTitle for 4+ unique streets picks start, middle, and finish`() {
        val streets = listOf("Street A", "Street B", "Street C", "Street D", "Street E")
        val title = SmartRouteNamingEngine.formatRouteTitle(streets)
        assertEquals("St A \u2192 St C \u2192 St E", title)
    }
}
