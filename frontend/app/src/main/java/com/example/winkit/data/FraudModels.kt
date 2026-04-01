package com.example.winkit.data

import com.google.gson.annotations.SerializedName

// 1. A single GPS ping
data class LocationPing(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("speed_kmh") val speedKmh: Float,
    @SerializedName("is_mock") val isMock: Boolean
)

// 2. The final array of 3 pings + hardware flags sent to Python
data class FraudCheckPayload(
    @SerializedName("worker_id") val workerId: String,
    @SerializedName("pings") val pings: List<LocationPing>,
    @SerializedName("developer_mode_enabled") val developerModeEnabled: Boolean,
    @SerializedName("os_signature_valid") val osSignatureValid: Boolean
)
