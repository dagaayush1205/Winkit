package com.example.winkit.ui.screens.dashboard

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.winkit.data.WeatherNetwork
import com.example.winkit.utils.LocationFraudTracker
import kotlinx.coroutines.launch
import com.example.winkit.data.NetworkModule
import com.example.winkit.data.SupabaseTelemetryRow

//we changed ViewModel() to AndroidViewModel(application) to access GPS!
class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    // --- UI State Variables (Weather) ---
    var temperature by mutableStateOf("...")
        private set
    var weatherCondition by mutableStateOf("Loading...")
        private set
    var humidity by mutableStateOf("...")
        private set
    var rainProbability by mutableStateOf("...")
        private set
    var cityName by mutableStateOf("Fetching...")
        private set

    // --- Fraud Tracker Instance ---
    private val fraudTracker = LocationFraudTracker(application)

    init {
        fetchLiveWeather()
    }

    private fun fetchLiveWeather() {
        viewModelScope.launch {
            try {
                val response = WeatherNetwork.api.getCurrentWeather(city = "Chennai")

                // 1. PRINT SUCCESS TO LOGCAT!
                Log.d("WinkitAPI", "✅ SUCCESS! Live Temp is: ${response.main.temp}")

                temperature = response.main.temp.toInt().toString()
                weatherCondition = response.weather.firstOrNull()?.main ?: "Clear"
                humidity = response.main.humidity.toString()
                cityName = response.name

                rainProbability = if (weatherCondition.contains("Rain", ignoreCase = true)) {
                    "85%"
                } else if (weatherCondition.contains("Cloud", ignoreCase = true)) {
                    "30%"
                } else {
                    "5%"
                }

            } catch (e: Exception) {
                // 2. PRINT THE EXACT ERROR SO YOU CAN FIX IT
                Log.e("WinkitAPI", "❌ API FAILED: ${e.message}")

                // 3. CHANGE FALLBACKS TO "ERR" SO YOU KNOW IT BROKE
                temperature = "ERR"
                weatherCondition = "Offline"
                humidity = "ERR"
                rainProbability = "ERR"
                cityName = "Disconnected"
            }
        }
    }

    fun verifyLocationAndClaim(workerId: String) {
        viewModelScope.launch {
            try {
                Log.d("FraudDefense", "Starting 10-second GPS Micro-Batch...")
                val batch = fraudTracker.collectMicroBatch(workerId)

                Log.d("FraudDefense", "✅ Micro-batch collected. Uploading...")

                batch.pings.forEach { ping ->
                    val row = SupabaseTelemetryRow(
                        worker_id = workerId,
                        latitude = ping.lat,
                        longitude = ping.lng,
                        speed_kmh = ping.speedKmh,
                        is_mock_location = ping.isMock,
                        dev_settings_enabled = batch.developerModeEnabled,
                        os_signature_valid = batch.osSignatureValid
                    )
                    NetworkModule.api.publishTelemetry(row)
                }
                Log.d("FraudDefense", "🚀 SUCCESS: Published to Supabase")
            } catch (e: Exception) {
                Log.e("FraudDefense", "❌ FAILED: ${e.message}")
            }
        }
    }
}