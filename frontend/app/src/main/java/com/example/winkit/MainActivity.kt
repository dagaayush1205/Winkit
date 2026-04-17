package com.example.winkit

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf // 🔥 Added for state bridge
import androidx.compose.ui.Modifier
import com.example.winkit.ui.navigation.AppNavigation
import com.example.winkit.ui.theme.WinkItTheme
import com.example.winkit.utils.Translator
import com.example.winkit.utils.VoiceAssistant
import com.example.winkit.utils.LocalVoiceAssistant

// 🔥 CASHFREE IMPORTS
import com.cashfree.pg.api.CFPaymentGatewayService
import com.cashfree.pg.core.api.callback.CFCheckoutResponseCallback
import com.cashfree.pg.core.api.utils.CFErrorResponse

// 🔥 THIS IS THE BRIDGE BETWEEN MAIN ACTIVITY AND YOUR COMPOSE UI
object PaymentEventManager {
    val paymentSuccess = mutableStateOf(false)
}

class MainActivity : ComponentActivity(), CFCheckoutResponseCallback {

    private lateinit var voiceAssistant: VoiceAssistant

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Load the saved language
        Translator.loadLanguage(this)

        // 2. Initialize Voice Assistant
        voiceAssistant = VoiceAssistant(this)

        // 3. Check Login State
        val sharedPref = getSharedPreferences("WinkitPrefs", Context.MODE_PRIVATE)
        val isLoggedIn = sharedPref.getBoolean("isLoggedIn", false)

        // 4. Register the Cashfree Callback listener
        try {
            CFPaymentGatewayService.getInstance().setCheckoutCallback(this)
        } catch (e: Exception) {
            Log.e("Cashfree", "Failed to set callback: ${e.message}")
        }

        setContent {
            WinkItTheme {
                CompositionLocalProvider(LocalVoiceAssistant provides voiceAssistant) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppNavigation(isLoggedIn = isLoggedIn, sharedPref = sharedPref)
                    }
                }
            }
        }
    }

    // 🔥 Handles the Payment Success Event
    override fun onPaymentVerify(orderID: String) {
        Log.d("Cashfree", "✅ Payment Success for Order: $orderID")
        // 🔥 TELLS THE UI TO MOVE TO THE DASHBOARD!
        PaymentEventManager.paymentSuccess.value = true
    }

    // 🔥 Handles the Payment Failure Event
    override fun onPaymentFailure(cfErrorResponse: CFErrorResponse, orderID: String) {
        Log.e("Cashfree", "❌ Payment Failed: ${cfErrorResponse.message}")
    }

    override fun onDestroy() {
        if (::voiceAssistant.isInitialized) {
            voiceAssistant.shutdown()
        }
        super.onDestroy()
    }
}