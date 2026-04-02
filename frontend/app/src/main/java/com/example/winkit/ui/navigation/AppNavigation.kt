package com.example.winkit.ui.navigation

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import com.example.winkit.ui.screens.alerts.RelocationAlertModal
import com.example.winkit.ui.screens.checkout.PolicyCheckoutScreen
import com.example.winkit.ui.screens.dashboard.DashboardViewModel
import com.example.winkit.ui.screens.dashboard.ShiftSafeDashboard
import com.example.winkit.ui.screens.onboarding.IntegrationScreen
import com.example.winkit.ui.screens.onboarding.LoginScreen
import com.example.winkit.ui.screens.onboarding.ScheduleScreen
import com.example.winkit.ui.screens.onboarding.SignUpScreen
import com.example.winkit.ui.screens.wallet.WalletScreen
import com.example.winkit.ui.screens.dashboard.ActivePolicyScreen
import com.example.winkit.ui.screens.wallet.WalletViewModel

@Composable
fun AppNavigation(isLoggedIn: Boolean, sharedPref: SharedPreferences) {
    val navController = rememberNavController()
    val dashboardViewModel: DashboardViewModel = viewModel()
    val walletViewModel: WalletViewModel = viewModel()

    // workerId is populated after either Sign Up or Login
    var activeWorkerId by remember { mutableStateOf("") }

    // Always boot to signup for demo (shows the full flow)
    val startDest = "signup"

    NavHost(navController = navController, startDestination = startDest) {

        // ── SCREEN 1: SIGN UP (entry point) ─────────────────────────────
        composable("signup") {
            SignUpScreen(
                onSignUpSuccess = { workerId ->
                    activeWorkerId = workerId
                    navController.navigate("integration") {
                        popUpTo("signup") { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate("login")
                }
            )
        }

        // ── SCREEN 2: LOGIN ──────────────────────────────────────────────
        composable("login") {
            LoginScreen(
                onLoginSuccess = { workerId ->
                    activeWorkerId = workerId
                    // Existing users who are already set up go straight to dashboard
                    navController.navigate("dashboard") {
                        popUpTo("signup") { inclusive = true }
                    }
                },
                onNavigateToSignUp = {
                    navController.navigate("signup") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        // ── SCREEN 3: INTEGRATION (platform link) ───────────────────────
        composable("integration") {
            IntegrationScreen(
                onBack = { navController.popBackStack() },
                onNext = { enteredId ->
                    // If the user entered a partner ID during integration, update it.
                    // Otherwise keep the workerId from sign-up.
                    if (enteredId.isNotBlank()) activeWorkerId = enteredId
                    navController.navigate("schedule")
                }
            )
        }

        // ── SCREEN 4: SCHEDULE ───────────────────────────────────────────
        composable("schedule") {
            ScheduleScreen(
                onBack = { navController.popBackStack() },
                onFinish = {
                    navController.navigate("checkout")
                }
            )
        }

        // ── SCREEN 5: CHECKOUT ───────────────────────────────────────────
        composable("checkout") {
            PolicyCheckoutScreen(
                workerId = activeWorkerId,
                onBack = { navController.popBackStack() },
                onPaymentSuccess = {
                    navController.navigate("dashboard") {
                        popUpTo("signup") { inclusive = true }
                    }
                }
            )
        }

        // ── SCREEN 6: DASHBOARD ──────────────────────────────────────────
        composable("dashboard") {
            ShiftSafeDashboard(
                workerId = activeWorkerId,
                viewModel = dashboardViewModel,
                navController = navController,
                onTriggerAlert = { navController.navigate("alert") },
                onPolicyClick = { navController.navigate("active_policy_screen") }
            )
        }
        composable("active_policy_screen") {
            ActivePolicyScreen(
                workerId = activeWorkerId,
                onBack = { navController.popBackStack() } // Goes back to the dashboard
            )
        }
        // ── SCREEN 7: WALLET ─────────────────────────────────────────────
        composable("wallet") {
            WalletScreen(
                workerId = activeWorkerId,
                navController = navController,
                viewModel = walletViewModel
            )
        }

        // ── SCREEN 8: DISASTER ALERT (dialog) ───────────────────────────
        dialog("alert") {
            RelocationAlertModal(
                onAccept = { navController.popBackStack() },
                onDismiss = { navController.popBackStack() }
            )
        }
    }
}
