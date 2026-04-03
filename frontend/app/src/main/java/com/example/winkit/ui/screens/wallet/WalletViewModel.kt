package com.example.winkit.ui.screens.wallet

import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.winkit.data.NetworkModule
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// ── DATA MODEL ─────────────────────────────────────────────────────────────
data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val type: String,
    val amount: Int,
    val isPositive: Boolean,
    val timestamp: String,
    val icon: ImageVector,
    val iconBgColor: Color,
    val iconTintColor: Color
)

// ── VIEW MODEL ─────────────────────────────────────────────────────────────
class WalletViewModel : ViewModel() {

    var walletBalance by mutableStateOf(0)
        private set
    var totalEarnings by mutableStateOf(0)
        private set
    var totalPayouts by mutableStateOf(0)
        private set

    var trackerStep by mutableStateOf(-1)
        private set

    val transactions = mutableStateListOf<Transaction>()

    var pendingManualClaims by mutableStateOf(0)
        private set

    private fun getCurrentTime(): String {
        return SimpleDateFormat("dd MMM 'at' hh:mm a", Locale.getDefault()).format(Date())
    }

    // --- 1. INITIAL LOAD (DYNAMIC WORKER ID) ---
    fun fetchRealLedgerData(workerId: String) {
        viewModelScope.launch {
            try {
                val realClaims = NetworkModule.api.getWorkerClaims(workerId = "eq.$workerId")
                val realActivity = NetworkModule.api.getWorkerActivity(workerId = "eq.$workerId")
                val realPolicies = NetworkModule.api.getPoliciesByWorker(workerId = "eq.$workerId")
                val pendingClaims = NetworkModule.api.getPendingManualClaims(workerId = "eq.$workerId")

                pendingManualClaims = pendingClaims.size
                Log.d("Wallet", "SUCCESS: Loaded ${realClaims.size} claims, ${pendingClaims.size} pending for $workerId!")

                var calculatedBalance = 0
                var calcEarnings = 0
                var calcPayouts = 0

                transactions.clear()

                realActivity.forEach { activity ->
                    val amt = activity.daily_earnings?.toInt() ?: 0
                    calcEarnings += amt
                    calculatedBalance += amt
                    transactions.add(
                        Transaction(
                            title = "Daily Earning (${activity.deliveries_completed ?: 0} orders)",
                            type = "EARNING", amount = amt, isPositive = true, timestamp = activity.log_date ?: getCurrentTime(),
                            icon = Icons.Default.ArrowOutward, iconBgColor = Color(0xFFE8F5E9), iconTintColor = Color(0xFF4CAF50)
                        )
                    )
                }

                realClaims.forEach { claim ->
                    val amt = claim.payout_amt?.toInt() ?: 0
                    calcPayouts += amt
                    calculatedBalance += amt
                    transactions.add(
                        Transaction(
                            title = "Parametric Payout (${claim.status})",
                            type = "INSURANCE CLAIM", amount = amt, isPositive = true, timestamp = claim.created_at?.substring(0, 10) ?: getCurrentTime(),
                            icon = Icons.Default.WaterDrop, iconBgColor = Color(0xFFE3F2FD), iconTintColor = Color(0xFF2196F3)
                        )
                    )
                }

                realPolicies.forEach { policy ->
                    val premiumAmt = policy.premium_paid?.toInt() ?: 0
                    calculatedBalance -= premiumAmt
                    transactions.add(
                        Transaction(
                            title = "Weekly Premium",
                            type = "PREMIUM DEDUCTION",
                            amount = premiumAmt,
                            isPositive = false,
                            timestamp = policy.week_start_date ?: getCurrentTime(),
                            icon = Icons.Default.Security, iconBgColor = Color(0xFFFFEBEE), iconTintColor = Color(0xFFF44336)
                        )
                    )
                }

                walletBalance = calculatedBalance
                totalEarnings = calcEarnings
                totalPayouts = calcPayouts

            } catch (e: Exception) {
                Log.e("Wallet", "Failed to load real DB data: ${e.message}")
            }
        }
    }

    // --- 2. THE LIVE DEMO SYNC (DYNAMIC WORKER ID) ---
    fun syncLedgerAndCheckClaims(workerId: String) {
        if (trackerStep != -1) return

        viewModelScope.launch {
            trackerStep = 0; delay(1200)
            trackerStep = 1; delay(1200)
            trackerStep = 2; delay(1200)
            trackerStep = 3; delay(1200)
            trackerStep = 4

            try {
                val latestClaims = NetworkModule.api.getWorkerClaims(workerId = "eq.$workerId", order = "created_at.desc")
                val newestClaim = latestClaims.firstOrNull()
                Log.d("Wallet", "SUCCESS: Live Sync caught a payout of ₹${newestClaim?.payout_amt} with status: ${newestClaim?.status}")

                if (newestClaim != null) {
                    val realPayoutAmt = newestClaim.payout_amt?.toInt() ?: 0

                    walletBalance += realPayoutAmt
                    totalPayouts += realPayoutAmt

                    transactions.add(0, Transaction(
                        title = "Automated Flood Relief (Live)",
                        type = "PARAMETRIC PAYOUT",
                        amount = realPayoutAmt,
                        isPositive = true,
                        timestamp = getCurrentTime(),
                        icon = Icons.Default.WaterDrop,
                        iconBgColor = Color(0xFFE3F2FD),
                        iconTintColor = Color(0xFF2196F3)
                    ))
                }
            } catch (e: Exception) {
                Log.e("Wallet", "Failed to fetch live claim: ${e.message}")
            }

            delay(1500)
            trackerStep = -1
        }
    }
}