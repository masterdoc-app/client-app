package pro.masterdoc.client.tracking

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

private object AndroidLocationPermission {
    private var activity: ComponentActivity? = null
    private var onResult: ((Boolean) -> Unit)? = null
    private val launcher by lazy {
        requireNotNull(activity).registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { grants ->
            onResult?.invoke(
                grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true,
            )
            onResult = null
        }
    }

    fun configure(host: ComponentActivity) {
        activity = host
    }

    fun hostOrNull(): ComponentActivity? = activity

    suspend fun ensureGranted(): Boolean {
        val host = activity ?: return false
        if (
            ContextCompat.checkSelfPermission(host, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(host, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return suspendCancellableCoroutine { continuation ->
            onResult = { granted ->
                if (continuation.isActive) continuation.resume(granted)
            }
            launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            continuation.invokeOnCancellation { onResult = null }
        }
    }
}

fun configureEngineerLocationTracking(activity: ComponentActivity) {
    AndroidLocationPermission.configure(activity)
}

actual fun createEngineerLocationPingSource(): EngineerLocationPingSource = AndroidEngineerLocationPingSource

private object AndroidEngineerLocationPingSource : EngineerLocationPingSource {
    override suspend fun currentLocation(): EngineerLocationPoint? {
        val context = AndroidLocationPermission.hostOrNull() ?: return null
        if (!AndroidLocationPermission.ensureGranted()) return null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val provider =
            when {
                manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                else -> return null
            }
        return manager.currentLocation(provider)?.toPoint()
    }
}

private suspend fun LocationManager.currentLocation(provider: String): Location? =
    getLastKnownLocation(provider)
        ?: suspendCancellableCoroutine { continuation ->
            val listener =
                object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        if (continuation.isActive) continuation.resume(location)
                        removeUpdates(this)
                    }
                }
            requestSingleUpdate(provider, listener, null)
            continuation.invokeOnCancellation { removeUpdates(listener) }
        }

private fun Location.toPoint(): EngineerLocationPoint =
    EngineerLocationPoint(
        lat = latitude,
        lon = longitude,
        accuracyM = if (hasAccuracy()) accuracy.toDouble() else null,
    )
