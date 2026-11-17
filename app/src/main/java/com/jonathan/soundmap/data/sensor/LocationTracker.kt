package com.jonathan.soundmap.data.sensor

import android.Manifest
import android.annotation.SuppressLint
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

data class Coordinates(val latitude: Double, val longitude: Double)

/** Obtiene la ubicación actual con Fused Location Provider (Google Play Services). */
@Singleton
class LocationTracker @Inject constructor(
    private val client: FusedLocationProviderClient,
) {

    /**
     * Pide una ubicación fresca de alta precisión (hasta 15 s) y, si no llega, usa la última
     * conocida. Devuelve null si no hay ninguna (GPS apagado o sin señal).
     */
    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    suspend fun currentLocation(): Coordinates? {
        val cancel = CancellationTokenSource()
        val fresh = runCatching {
            withTimeoutOrNull(15_000) {
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancel.token).await()
            }
        }.getOrNull()
        cancel.cancel()
        val location = fresh ?: runCatching { client.lastLocation.await() }.getOrNull()
        return location?.let { Coordinates(it.latitude, it.longitude) }
    }
}
