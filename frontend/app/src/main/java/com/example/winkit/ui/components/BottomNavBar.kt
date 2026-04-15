package com.example.winkit.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.winkit.utils.LocalVoiceAssistant // JARVIS IMPORT

@Composable
fun ShiftSafeBottomNav(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Summon JARVIS
    val voice = LocalVoiceAssistant.current 

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        // --- 1. HOME / DASHBOARD TAB ---
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") },
            selected = currentRoute == "dashboard",
            onClick = {
                if (currentRoute != "dashboard") {
                    voice.speak("Opening Dashboard") // JARVIS Speaks
                    navController.navigate("dashboard") {
                        popUpTo("dashboard") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF074768),
                selectedTextColor = Color(0xFF074768),
                indicatorColor = Color(0xFFE3F2FD),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )

        // --- 2. WALLET TAB ---
        NavigationBarItem(
            icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Wallet") },
            label = { Text("Wallet") },
            selected = currentRoute == "wallet",
            onClick = {
                if (currentRoute != "wallet") {
                    voice.speak("Opening Wallet Balance") // JARVIS Speaks
                    navController.navigate("wallet") {
                        popUpTo("dashboard")
                        launchSingleTop = true
                    }
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF074768),
                selectedTextColor = Color(0xFF074768),
                indicatorColor = Color(0xFFE3F2FD),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )

        // --- 3. PROFILE TAB ---
        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
            label = { Text("Profile") },
            selected = currentRoute == "profile",
            onClick = {
                if (currentRoute != "profile") {
                    voice.speak("Opening Profile") // JARVIS Speaks
                    navController.navigate("profile") {
                        popUpTo("dashboard")
                        launchSingleTop = true
                    }
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF074768),
                selectedTextColor = Color(0xFF074768),
                indicatorColor = Color(0xFFE3F2FD),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )
    }
}
