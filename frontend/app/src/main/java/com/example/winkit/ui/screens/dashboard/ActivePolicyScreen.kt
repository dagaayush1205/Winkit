package com.example.winkit.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.winkit.data.NetworkModule
import com.example.winkit.utils.tr
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivePolicyScreen(workerId: String, onBack: () -> Unit) {
    var isLoading by remember { mutableStateOf(true) }
    var policyDetails by remember { mutableStateOf<Map<String, String>?>(null) }

    LaunchedEffect(Unit) {
        try {
            // Fetch ONLY this worker's ACTIVE policy from weekly_policies
            val response = NetworkModule.api.getActivePolicies(workerId = "eq.$workerId", status = "eq.ACTIVE")
            if (response.isNotEmpty()) {
                val policy = response.first()
                policyDetails = mapOf(
                    "policyId" to (policy.policy_id ?: "POL-UNKNOWN"),
                    "startDate" to (policy.week_start_date ?: "N/A"),
                    "endDate" to (policy.week_end_date ?: "N/A"),
                    "premium" to (policy.premium_paid?.toString() ?: "0"),
                    "coverage" to (policy.max_daily_coverage?.toString() ?: "0")
                )
            }
        } catch (e: Exception) {
            // Fallback for hackathon demo if API fails
            policyDetails = mapOf(
                "policyId" to "POL-9002",
                "startDate" to "2026-03-28",
                "endDate" to "2026-04-04",
                "premium" to "75",
                "coverage" to "800"
            )
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tr("Policy Details"), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF3F4F8))
            )
        },
        containerColor = Color(0xFFF3F4F8)
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color(0xFF5B2D8E),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (policyDetails != null) {
                // The Policy "Ticket" Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        // Header
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE8F5E9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF4CAF50))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Gig Protect Weekly", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF1A1A2E))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("STATUS: ACTIVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Divider(color = Color(0xFFE0E6ED))
                        Spacer(modifier = Modifier.height(24.dp))

                        // Details Grid
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            PolicyDetailItem("POLICY ID", policyDetails!!["policyId"]!!)
                            PolicyDetailItem("PREMIUM PAID", "₹${policyDetails!!["premium"]}")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            PolicyDetailItem("START DATE", policyDetails!!["startDate"]!!)
                            PolicyDetailItem("END DATE", policyDetails!!["endDate"]!!)
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Coverage Banner
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8F9FF),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(tr("MAX DAILY COVERAGE"), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8E8E9A))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("₹${policyDetails!!["coverage"]}", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color(0xFF5B2D8E))
                            }
                        }
                    }
                }
            } else {
                Text(tr("No active policy found."), modifier = Modifier.align(Alignment.Center), color = Color.Gray)
            }
        }
    }
}

@Composable
fun PolicyDetailItem(label: String, value: String) {
    Column {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8E8E9A))
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A1A2E))
    }
}
