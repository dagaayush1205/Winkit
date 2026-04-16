package com.example.winkit.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.AccountBalanceWallet // Using your exact icon
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.winkit.ui.screens.dashboard.DashboardViewModel
import com.example.winkit.ui.screens.dashboard.ShiftSafeDashboard
import com.example.winkit.ui.screens.profile.ProfileScreen
import com.example.winkit.ui.screens.wallet.WalletScreen
import com.example.winkit.ui.screens.wallet.WalletViewModel
import com.example.winkit.utils.tr
import com.example.winkit.utils.LocalVoiceAssistant // 🔥 JARVIS IMPORT RESTORED
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainPagerScreen(
    workerId: String,
    navController: NavController,
    dashboardViewModel: DashboardViewModel,
    walletViewModel: WalletViewModel
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    
    // 🔥 Summon JARVIS
    val voice = LocalVoiceAssistant.current 

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp // Added your elevation back
            ) {
                
                // --- TAB 0: DASHBOARD ---
                NavigationBarItem(
                    selected = pagerState.currentPage == 0,
                    onClick = { 
                        if (pagerState.currentPage != 0) {
                            voice.speak("Opening Dashboard") // 🔥 JARVIS Speaks
                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                        }
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text(tr("Home")) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF5B2D8E), 
                        selectedTextColor = Color(0xFF5B2D8E),
                        indicatorColor = Color(0xFFF3F4F8)
                    )
                )
                
                // --- TAB 1: WALLET ---
                NavigationBarItem(
                    selected = pagerState.currentPage == 1,
                    onClick = { 
                        if (pagerState.currentPage != 1) {
                            voice.speak("Opening Wallet Balance") // 🔥 JARVIS Speaks
                            coroutineScope.launch { pagerState.animateScrollToPage(1) }
                        }
                    },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Wallet") },
                    label = { Text(tr("Wallet")) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF5B2D8E), 
                        selectedTextColor = Color(0xFF5B2D8E),
                        indicatorColor = Color(0xFFF3F4F8)
                    )
                )
                
                // --- TAB 2: PROFILE ---
                NavigationBarItem(
                    selected = pagerState.currentPage == 2,
                    onClick = { 
                        if (pagerState.currentPage != 2) {
                            voice.speak("Opening Profile") // 🔥 JARVIS Speaks
                            coroutineScope.launch { pagerState.animateScrollToPage(2) }
                        }
                    },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text(tr("Profile")) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF5B2D8E), 
                        selectedTextColor = Color(0xFF5B2D8E),
                        indicatorColor = Color(0xFFF3F4F8)
                    )
                )
            }
        }
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { page ->
            when (page) {
                0 -> ShiftSafeDashboard(
                    workerId = workerId,
                    viewModel = dashboardViewModel,
                    navController = navController,
                    onTriggerAlert = { navController.navigate("alert") },
                    onPolicyClick = { navController.navigate("active_policy_screen") }
                )
                1 -> WalletScreen(
                    workerId = workerId, 
                    navController = navController, 
                    viewModel = walletViewModel
                )
                2 -> ProfileScreen(
                    workerId = workerId, 
                    navController = navController
                )
            }
        }
    }
}
