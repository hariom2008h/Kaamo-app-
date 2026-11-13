package com.kaamo.app.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** A GPS fix. Kept in memory only; it is sent to the server for one distance query and never stored there. */
data class DeviceLocation(val latitude: Double, val longitude: Double)

/** Great-circle distance in whole km (same formula as the server's distance_km). */
fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    return (6371.0 * 2 * asin(min(1.0, sqrt(a)))).roundToInt()
}

/**
 * Returns a function that asks for location permission if needed (when the user taps a button), then
 * reads the current position with the platform LocationManager (no Google Play services needed).
 * [onResult] gets null when permission is denied, location is switched off, or no fix arrives.
 */
@Composable
fun rememberLocationRequester(onResult: (DeviceLocation?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val latestOnResult by rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.any { it }) readLocation(context) { latestOnResult(it) } else latestOnResult(null)
    }
    return remember(launcher, context) {
        {
            if (hasLocationPermission(context)) {
                readLocation(context) { latestOnResult(it) }
            } else {
                launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }
        }
    }
}

private fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

@SuppressLint("MissingPermission") // only called after hasLocationPermission() or a granted permission result
private fun readLocation(context: Context, done: (DeviceLocation?) -> Unit) {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    if (manager == null) {
        done(null)
        return
    }
    // Network first (fast, works indoors), then GPS.
    val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
        .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
    if (providers.isEmpty()) {
        done(null)
        return
    }
    val last = providers.mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }.maxByOrNull { it.time }
    if (last != null && System.currentTimeMillis() - last.time < 2 * 60_000L) {
        done(DeviceLocation(last.latitude, last.longitude))
        return
    }
    var delivered = false
    LocationManagerCompat.getCurrentLocation(manager, providers.first(), null, ContextCompat.getMainExecutor(context)) { fix ->
        if (delivered) return@getCurrentLocation
        delivered = true
        val best = fix ?: last
        done(best?.let { DeviceLocation(it.latitude, it.longitude) })
    }
}
