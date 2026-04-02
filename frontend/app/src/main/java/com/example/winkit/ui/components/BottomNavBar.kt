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

@Composable
fun ShiftSafeBottomNav(navController: NavController) {
    // Observes the current navigation state to highlight the correct icon
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        // --- HOME / DASHBOARD TAB ---
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") },
            selected = currentRoute == "dashboard",
            onClick = {
                if (currentRoute != "dashboard") {
                    navController.navigate("dashboard") {
                        // Clears backstack to dashboard to prevent "back button loops"
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

        // --- WALLET TAB ---
        NavigationBarItem(
            icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Wallet") },
            label = { Text("Wallet") },
            selected = currentRoute == "wallet",
            onClick = {
                if (currentRoute != "wallet") {
                    navController.navigate("wallet") {
                        // Ensures we don't build multiple instances of the same screen
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

        // --- PROFILE TAB ---
        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
            label = { Text("Profile") },
            selected = currentRoute == "profile",
            onClick = {
                if (currentRoute != "profile") {
                    navController.navigate("profile") {
                        // Standard professional behavior: root to dashboard
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