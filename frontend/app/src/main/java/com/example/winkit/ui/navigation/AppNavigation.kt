package com.example.winkit.ui.navigation

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.winkit.domain.models.DashboardState
import com.example.winkit.domain.models.EnvironmentType
import com.example.winkit.ui.screens.alerts.RelocationAlertModal
import com.example.winkit.ui.screens.checkout.PolicyCheckoutScreen // <-- ADDED THIS IMPORT
import com.example.winkit.ui.screens.dashboard.ShiftSafeDashboard
import com.example.winkit.ui.screens.onboarding.IntegrationScreen
import com.example.winkit.ui.screens.onboarding.LoginScreen
import com.example.winkit.ui.screens.onboarding.ScheduleScreen
import com.example.winkit.ui.screens.wallet.WalletScreen
import com.example.winkit.ui.screens.wallet.WalletViewModel
import com.example.winkit.ui.screens.dashboard.DashboardViewModel // Add import
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.navigation.compose.dialog

@Composable
fun AppNavigation(isLoggedIn: Boolean, sharedPref: SharedPreferences) {
    val navController = rememberNavController()
    // Create the global ViewModel here
    val walletViewModel: WalletViewModel = viewModel()
    val dashboardViewModel: DashboardViewModel = viewModel()

    var activeWorkerId by remember { mutableStateOf("") }

    // 🔴 HARDCODED FOR DEMO: Always boot to the login screen!
    val startDest = "login"

    NavHost(navController = navController, startDestination = startDest) {

        // --- SCREEN 1: LOGIN ---
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("integration")
                }
            )
        }

        // --- SCREEN 2: INTEGRATION ---
        composable("integration") {
            IntegrationScreen(
                onBack = { navController.popBackStack() },
                onNext = { enteredId ->
                    activeWorkerId = enteredId // Save it to memory!
                    navController.navigate("schedule")
                }
            )
        }

        // --- SCREEN 3: SCHEDULE ---
        composable("schedule") {
            ScheduleScreen(
                onBack = { navController.popBackStack() },
                onFinish = {
                    // <-- FIXED: Go to checkout next, not dashboard!
                    navController.navigate("checkout")
                }
            )
        }

        // --- SCREEN 4: CHECKOUT (AI & PAYMENT) ---
        composable("checkout") {
            PolicyCheckoutScreen(
                workerId = activeWorkerId, // Pass it down!
                onBack = { navController.popBackStack() },
                onPaymentSuccess = {
                    // 🔴 DISABLED FOR DEMO: We aren't saving the persistent login state
                    // sharedPref.edit().putBoolean("isLoggedIn", true).apply()

                    navController.navigate("dashboard") {
                        // Clear the backstack so pressing "back" exits the app instead of going to login/checkout
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        // --- SCREEN 5: DASHBOARD ---
        composable("dashboard") {
            ShiftSafeDashboard(
                workerId = activeWorkerId, // 🔥 Pass dynamic ID here too!
                viewModel = dashboardViewModel,
                navController = navController,
                onTriggerAlert = { navController.navigate("alert") }
            )
        }

        // --- SCREEN 6: WALLET ---
        composable("wallet") {
            WalletScreen(navController = navController, viewModel = walletViewModel)
        }

        // --- SCREEN 7: DISASTER ALERT ---
        dialog("alert") {
            RelocationAlertModal(
                onAccept = {
                    navController.popBackStack()
                    // You can navigate to a map screen here later!
                },
                onDismiss = {
                    navController.popBackStack()
                }
            )
        }
    }
}