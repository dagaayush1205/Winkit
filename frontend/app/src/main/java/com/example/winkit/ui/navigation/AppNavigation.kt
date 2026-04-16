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

import com.example.winkit.ui.screens.LandingScreen 
import com.example.winkit.ui.screens.MainPagerScreen // 🔥 Added MainPagerScreen import
import com.example.winkit.ui.screens.alerts.RelocationAlertModal
import com.example.winkit.ui.screens.checkout.PolicyCheckoutScreen
import com.example.winkit.ui.screens.dashboard.DashboardViewModel
import com.example.winkit.ui.screens.onboarding.IntegrationScreen
import com.example.winkit.ui.screens.onboarding.LoginScreen
import com.example.winkit.ui.screens.onboarding.ScheduleScreen
import com.example.winkit.ui.screens.onboarding.SignUpScreen
import com.example.winkit.ui.screens.dashboard.ActivePolicyScreen
import com.example.winkit.ui.screens.wallet.WalletViewModel

@Composable
fun AppNavigation(isLoggedIn: Boolean, sharedPref: SharedPreferences) {
    val navController = rememberNavController()
    val dashboardViewModel: DashboardViewModel = viewModel()
    val walletViewModel: WalletViewModel = viewModel()

    val context = androidx.compose.ui.platform.LocalContext.current
    val savedWorkerId = com.example.winkit.utils.AuthManager.getWorkerId(context)

    var activeWorkerId by remember { mutableStateOf(savedWorkerId ?: "") }

    // 🔥 Changed from "dashboard" to "main_tabs"
    val startDest = if (savedWorkerId != null) "main_tabs" else "landing"

    NavHost(navController = navController, startDestination = startDest) {

        // ── SCREEN 0: LANDING (Language Selection) ──────────────────────
        composable("landing") {
            LandingScreen(navController = navController)
        }

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
                    // 🔥 Redirect to main_tabs
                    navController.navigate("main_tabs") {
                        popUpTo("landing") { inclusive = true } 
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
                    // 🔥 Redirect to main_tabs
                    navController.navigate("main_tabs") {
                        popUpTo("signup") { inclusive = true }
                        popUpTo("landing") { inclusive = true } 
                    }
                }
            )
        }

        // ── THE SWIPEABLE MAIN TABS (Replaces Dashboard, Wallet, Profile) ──
        composable("main_tabs") {
            MainPagerScreen(
                workerId = activeWorkerId,
                navController = navController,
                dashboardViewModel = dashboardViewModel,
                walletViewModel = walletViewModel
            )
        }
        
        // ── ACTIVE POLICY SUB-SCREEN ─────────────────────────────────────
        composable("active_policy_screen") {
            ActivePolicyScreen(
                workerId = activeWorkerId,
                onBack = { navController.popBackStack() } 
            )
        }

        // ── DISASTER ALERT (dialog) ───────────────────────────
        dialog("alert") {
            RelocationAlertModal(
                onAccept = { navController.popBackStack() },
                onDismiss = { navController.popBackStack() }
            )
        }
    }
}
