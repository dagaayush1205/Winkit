package com.example.winkit

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider 
import androidx.compose.ui.Modifier
import com.example.winkit.ui.navigation.AppNavigation
import com.example.winkit.ui.theme.WinkItTheme
import com.example.winkit.utils.Translator // 🔥 ADDED: Import the Translator
import com.example.winkit.utils.VoiceAssistant
import com.example.winkit.utils.LocalVoiceAssistant 

class MainActivity : ComponentActivity() {

    private lateinit var voiceAssistant: VoiceAssistant

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 🔥 1. Load the saved language the exact millisecond the app opens
        Translator.loadLanguage(this)
        
        // 🔥 2. Initialize Voice Assistant
        voiceAssistant = VoiceAssistant(this)

        // 🔥 3. Check Login State
        val sharedPref = getSharedPreferences("WinkitPrefs", Context.MODE_PRIVATE)
        val isLoggedIn = sharedPref.getBoolean("isLoggedIn", false)

        setContent {
            WinkItTheme {
                CompositionLocalProvider(LocalVoiceAssistant provides voiceAssistant) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        // Pass login state to decide if we show Landing -> Login OR Dashboard
                        AppNavigation(isLoggedIn = isLoggedIn, sharedPref = sharedPref)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        if (::voiceAssistant.isInitialized) {
            voiceAssistant.shutdown()
        }
        super.onDestroy()
    }
}
