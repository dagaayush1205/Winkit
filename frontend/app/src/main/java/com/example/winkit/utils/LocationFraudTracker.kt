package com.example.winkit.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Looper
import android.provider.Settings
import com.example.winkit.data.FraudCheckPayload
import com.example.winkit.data.LocationPing
import com.google.android.gms.location.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LocationFraudTracker(private val context: Context) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission") // We will handle permissions in the UI
    suspend fun collectMicroBatch(workerId: String): FraudCheckPayload = suspendCancellableCoroutine { continuation ->
        
        val pings = mutableListOf<LocationPing>()

        // Request high accuracy, pinging roughly every 3.3 seconds
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3300)
            .setMinUpdateIntervalMillis(3000)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    
                    val isMock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        location.isMock
                    } else {
                        @Suppress("DEPRECATION")
                        location.isFromMockProvider
                    }

                    pings.add(LocationPing(
                        lat = location.latitude,
                        lng = location.longitude,
                        timestamp = location.time,
                        speedKmh = location.speed * 3.6f, // Convert m/s to km/h
                        isMock = isMock
                    ))

                    // Stop at 3 pings and return payload
                    if (pings.size >= 3) {
                        fusedLocationClient.removeLocationUpdates(this)
                        
                        val payload = FraudCheckPayload(
                            workerId = workerId,
                            pings = pings,
                            developerModeEnabled = isDeveloperModeEnabled(context),
                            osSignatureValid = isOsSignatureValid() 
                        )
                        
                        if (continuation.isActive) continuation.resume(payload)
                    }
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

        continuation.invokeOnCancellation {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    private fun isDeveloperModeEnabled(context: Context): Boolean {
        return Settings.Secure.getInt(
            context.contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0
        ) != 0
    }

    private fun isOsSignatureValid(): Boolean {
        val buildTags = Build.TAGS
        return buildTags == null || !buildTags.contains("test-keys")
    }
}
