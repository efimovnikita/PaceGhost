package com.paceghost.app.service.geocode

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.paceghost.app.domain.model.TrackPoint
import com.paceghost.app.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume

interface RouteGeocoder {
    suspend fun resolveLandmark(lat: Double, lon: Double): String?
    suspend fun resolveLandmarks(points: List<TrackPoint>): List<String>
}

class AndroidWalkGeocoder(
    private val context: Context,
    private val locale: Locale = Locale.getDefault()
) : RouteGeocoder {

    private val geocoder: Geocoder by lazy {
        Geocoder(context, locale)
    }

    override suspend fun resolveLandmarks(points: List<TrackPoint>): List<String> {
        val landmarks = mutableListOf<String>()
        for (point in points) {
            val name = resolveLandmark(point.lat, point.lon)
            if (!name.isNullOrBlank()) {
                landmarks.add(name)
            }
        }
        return landmarks
    }

    override suspend fun resolveLandmark(lat: Double, lon: Double): String? {
        if (!Geocoder.isPresent()) {
            AppLogger.w(TAG, "Geocoder service is not present on this device")
            return null
        }

        return try {
            val addresses = getFromLocationCompat(lat, lon, 1)
            val firstAddress = addresses.firstOrNull() ?: return null
            extractLandmark(firstAddress)
        } catch (e: IOException) {
            AppLogger.w(TAG, "Network error resolving coordinates ($lat, $lon): ${e.message}")
            null
        } catch (e: Exception) {
            AppLogger.e(TAG, "Unexpected error resolving coordinates ($lat, $lon)", e)
            null
        }
    }

    private suspend fun getFromLocationCompat(
        lat: Double,
        lon: Double,
        maxResults: Int
    ): List<Address> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                kotlinx.coroutines.withTimeoutOrNull(4000L) {
                    suspendCancellableCoroutine { continuation ->
                        try {
                            geocoder.getFromLocation(lat, lon, maxResults, object : Geocoder.GeocodeListener {
                                override fun onGeocode(addresses: MutableList<Address>) {
                                    if (continuation.isActive) {
                                        continuation.resume(addresses)
                                    }
                                }

                                override fun onError(errorMessage: String?) {
                                    AppLogger.w(TAG, "Geocoder error: $errorMessage")
                                    if (continuation.isActive) {
                                        continuation.resume(emptyList())
                                    }
                                }
                            })
                        } catch (e: Exception) {
                            AppLogger.w(TAG, "Failed to invoke getFromLocation: ${e.message}")
                            if (continuation.isActive) {
                                continuation.resume(emptyList())
                            }
                        }
                    }
                } ?: emptyList()
            } catch (e: Exception) {
                AppLogger.w(TAG, "Exception during getFromLocationCompat: ${e.message}")
                emptyList()
            }
        } else {
            withContext(Dispatchers.IO) {
                try {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(lat, lon, maxResults) ?: emptyList()
                } catch (e: Exception) {
                    AppLogger.w(TAG, "Legacy getFromLocation error: ${e.message}")
                    emptyList()
                }
            }
        }
    }

    private fun extractLandmark(address: Address): String? {
        val thoroughfare = address.thoroughfare?.trim()?.takeIf { it.isNotBlank() }
        if (thoroughfare != null) {
            return thoroughfare
        }

        val feature = address.featureName?.trim()?.takeIf { it.isNotBlank() }
        // If featureName is just numeric street numbers, skip to avoid "12" or "45B"
        if (feature != null && !feature.all { it.isDigit() || it.isWhitespace() || it == '-' || it == '/' }) {
            return feature
        }

        val subLocality = address.subLocality?.trim()?.takeIf { it.isNotBlank() }
        if (subLocality != null) {
            return subLocality
        }

        return null
    }

    companion object {
        private const val TAG = "WalkGeocoder"
    }
}
