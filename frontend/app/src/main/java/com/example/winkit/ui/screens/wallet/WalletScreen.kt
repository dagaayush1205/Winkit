package com.example.winkit.ui.screens.wallet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.winkit.ui.components.ShiftSafeBottomNav
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalContext
import com.example.winkit.utils.tr

@Composable
fun WalletScreen(
    workerId: String, 
    navController: NavController, 
    viewModel: WalletViewModel
) {
    val context = LocalContext.current
    var initialLoadDone by remember { mutableStateOf(false) }
    var previousBalance by remember { mutableStateOf(0) }

    // 🔥 1. THIS IS THE MAGIC: Fetch data the moment the screen loads!
    LaunchedEffect(workerId) {
        viewModel.fetchRealLedgerData(workerId)
    }

    // 🔥 2. SMART WALLET WATCHER: Automatically fires notification when money is added!
    LaunchedEffect(viewModel.walletBalance) {
        if (!initialLoadDone && viewModel.walletBalance != 0) {
            // Initial Load: Don't fire a notification when fetching data from the DB for the first time
            initialLoadDone = true 
            previousBalance = viewModel.walletBalance
        } else if (initialLoadDone && viewModel.walletBalance > previousBalance) {
            // New Money Added! Calculate how much and fire the notification
            val addedAmount = viewModel.walletBalance - previousBalance
            com.example.winkit.utils.NotificationHelper.showPayoutNotification(context, addedAmount)
            
            // Update the tracker
            previousBalance = viewModel.walletBalance
        }
    }

    Scaffold(
        containerColor = Color(0xFFF8F9FA),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.syncLedgerAndCheckClaims(workerId) }, // Pass ID to Sync
                containerColor = Color(0xFF074768),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Sync, contentDescription = "Sync Ledger")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }

            item { WalletBalanceCard(viewModel.walletBalance, viewModel.totalEarnings, viewModel.totalPayouts) }

            item {
                AnimatedVisibility(visible = viewModel.trackerStep >= 0) {
                    LiveClaimTracker(currentStep = viewModel.trackerStep)
                }
            }
            
            // Show pending manual claims banner
            if (viewModel.pendingManualClaims > 0) {
                item {
                    PendingReviewBanner(count = viewModel.pendingManualClaims)
                }
            }
            
            item {
                Text(tr("Ledger History"), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A), modifier = Modifier.padding(top = 8.dp))
            }

            items(viewModel.transactions, key = { it.id }) { tx ->
                TransactionRow(tx)
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}@Composable
fun WalletBalanceCard(balance: Int, earnings: Int, payouts: Int) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF074768), // Deep Blue
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Header
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(tr("Wallet"), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Surface(color = Color.White.copy(alpha = 0.1f), shape = CircleShape) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFFFFD54F), modifier = Modifier.padding(8.dp).size(20.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // Main Balance
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(tr("Wallet Balance"), color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                Text("₹$balance", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Black)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Sub Stats
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatBox("Total Earnings", "₹$earnings", Modifier.weight(1f))
                StatBox("Total Payouts", "₹$payouts", Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Insurance Status Footer
            Surface(
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(tr("Insurance Status"), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ACTIVE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(tr("Weekly Premium"), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                        Text("₹49/wk", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun StatBox(title: String, amount: String, modifier: Modifier = Modifier) {
    Surface(
        color = Color.White.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(amount, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LiveClaimTracker(currentStep: Int) {
    val steps = listOf(
        "Trigger Confirmed" to "Weather API verified threshold",
        "Eligibility Check" to "Active policy, correct zone",
        "Payout Calculated" to "Amount locked based on severity",
        "Transfer Initiated" to "UPI / IMPS processing",
        "Record Updated" to "Core systems reconciled"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Autorenew, contentDescription = null, tint = Color(0xFF074768))
                Spacer(modifier = Modifier.width(8.dp))
                Text(tr("Live Claim Automation"), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1A1A2E))
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            steps.forEachIndexed { index, step ->
                val isCompleted = currentStep > index
                val isActive = currentStep == index
                
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Status Indicator
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isCompleted -> Color(0xFF00E676)
                                    isActive -> Color(0xFFFFC107)
                                    else -> Color(0xFFE0E0E0)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // Text Details
                    Column {
                        Text(
                            text = step.first,
                            fontWeight = if (isActive || isCompleted) FontWeight.Bold else FontWeight.Normal,
                            color = if (isActive || isCompleted) Color(0xFF1A1A2E) else Color.Gray,
                            fontSize = 14.sp
                        )
                        if (isActive || isCompleted) {
                            Text(text = step.second, color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                }
                
                // Connecting Line
                if (index < steps.size - 1) {
                    Box(
                        modifier = Modifier
                            .padding(start = 11.dp)
                            .width(2.dp)
                            .height(16.dp)
                            .background(if (isCompleted) Color(0xFF00E676) else Color(0xFFE0E0E0))
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionRow(tx: Transaction) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier.size(48.dp).background(tx.iconBgColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(tx.icon, contentDescription = null, tint = tx.iconTintColor)
            }
            
            Spacer(modifier = Modifier.width(16.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(tx.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A1A))
                Text(tx.type, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 0.5.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(tx.timestamp, fontSize = 11.sp, color = Color.Gray)
                }
            }

            // Amount
            Text(
                text = "${if (tx.isPositive) "+" else "-"}₹${tx.amount}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = if (tx.isPositive) Color(0xFF00C853) else Color(0xFF424242)
            )
        }
    }
}
@Composable
fun PendingReviewBanner(count: Int) {
    Surface(
        color = Color(0xFFFFF8E1), // Light Amber/Warning Background
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFFFE082)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp), 
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = Color(0xFFF57C00))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "$count Pending Manual Review${if (count > 1) "s" else ""}", 
                    fontWeight = FontWeight.Bold, 
                    color = Color(0xFFE65100), 
                    fontSize = 14.sp
                )
                Text(
                    text = tr("Your hazard report is being verified by our team. Payouts process within 24hrs."), 
                    color = Color(0xFFE65100).copy(alpha = 0.8f), 
                    fontSize = 12.sp
                )
            }
        }
    }
}
