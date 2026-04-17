package com.example.winkit.ui.screens.checkout

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.winkit.data.NetworkModule
import com.example.winkit.data.WeeklyPolicyInsert
import com.example.winkit.BuildConfig
import com.example.winkit.PaymentEventManager // 🔥 Imports our new bridge!
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import com.example.winkit.utils.tr
import java.util.Locale

// 🔥 CASHFREE & NETWORK IMPORTS
import com.cashfree.pg.api.CFPaymentGatewayService
import com.cashfree.pg.core.api.CFSession
import com.cashfree.pg.core.api.webcheckout.CFWebCheckoutTheme
import com.cashfree.pg.core.api.webcheckout.CFWebCheckoutPayment
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

enum class CheckoutStep {
    ANALYZING, OFFER, PROCESSING_PAYMENT, SUCCESS
}

@Composable
fun PolicyCheckoutScreen(workerId: String, onBack: () -> Unit, onPaymentSuccess: () -> Unit) {
    var currentStep by remember { mutableStateOf(CheckoutStep.ANALYZING) }
    var weeklyPremium by remember { mutableStateOf("...") }
    var maxPayout by remember { mutableStateOf("...") }

    // 🔥 LISTENS FOR THE SUCCESS SIGNAL FROM MAINACTIVITY
    val isCashfreeSuccess by PaymentEventManager.paymentSuccess

    LaunchedEffect(isCashfreeSuccess) {
        if (isCashfreeSuccess) {
            PaymentEventManager.paymentSuccess.value = false // Reset it
            currentStep = CheckoutStep.PROCESSING_PAYMENT    // Trigger DB Insert & UI
        }
    }

    LaunchedEffect(Unit) {
        try {
            val responseList = NetworkModule.api.getWorkerCharges(workerId = "eq.$workerId")
            if (responseList.isNotEmpty()) {
                val charge = responseList.first()
                weeklyPremium = charge.premium?.toInt()?.toString() ?: "49"
                maxPayout = "800"
            } else {
                weeklyPremium = "49"
                maxPayout = "800"
            }
            delay(1500)
            currentStep = CheckoutStep.OFFER
        } catch (e: Exception) {
            Log.e("SupabaseError", "Failed to fetch charges: ${e.message}")
            weeklyPremium = "49"
            maxPayout = "800"
            delay(1500)
            currentStep = CheckoutStep.OFFER
        }
    }

    LaunchedEffect(currentStep) {
        when (currentStep) {
            CheckoutStep.PROCESSING_PAYMENT -> {
                delay(2000)
                try {
                    val policyId = "POL-${System.currentTimeMillis().toString().takeLast(4)}"
                    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val cal = Calendar.getInstance()
                    val startDate = formatter.format(cal.time)
                    cal.add(Calendar.DAY_OF_YEAR, 7)
                    val endDate = formatter.format(cal.time)

                    val newPolicy = WeeklyPolicyInsert(
                        policy_id = policyId,
                        worker_id = workerId,
                        week_start_date = startDate,
                        week_end_date = endDate,
                        premium_paid = weeklyPremium.toDouble(),
                        max_daily_coverage = maxPayout.toDouble(),
                        status = "ACTIVE"
                    )

                    NetworkModule.api.insertWeeklyPolicy(newPolicy)
                } catch(e: Exception) {
                    Log.e("SupabaseError", "Failed to insert active policy: ${e.message}")
                }
                currentStep = CheckoutStep.SUCCESS
            }
            else -> { }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F7FA))) {
        Crossfade(targetState = currentStep, animationSpec = tween(800), label = "screen_fade") { step ->
            when (step) {
                CheckoutStep.ANALYZING -> AiAnalyzingView()
                CheckoutStep.OFFER, CheckoutStep.PROCESSING_PAYMENT, CheckoutStep.SUCCESS -> {
                    PolicyOfferView(
                        workerId = workerId,
                        weeklyPremium = weeklyPremium,
                        maxPayout = maxPayout,
                        onBack = onBack,
                        onProceedToProcessing = { currentStep = CheckoutStep.PROCESSING_PAYMENT }
                    )
                }
            }
        }

        if (currentStep == CheckoutStep.PROCESSING_PAYMENT || currentStep == CheckoutStep.SUCCESS) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .pointerInput(Unit) {},
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    modifier = Modifier.width(320.dp).padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (currentStep == CheckoutStep.PROCESSING_PAYMENT) {
                            CircularProgressIndicator(color = Color(0xFF006C7A), modifier = Modifier.size(64.dp), strokeWidth = 6.dp)
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(tr("Setting up Auto-Pay..."), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0A192F))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(tr("Authorizing with UPI"), fontSize = 14.sp, color = Color.Gray)
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Success", tint = Color(0xFF4CAF50), modifier = Modifier.size(80.dp))
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(tr("Auto-Pay Enabled!"), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF4CAF50))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(tr("Your coverage is now active."), fontSize = 14.sp, color = Color.Gray)

                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = onPaymentSuccess,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(tr("Go to Dashboard"), fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AiAnalyzingView() {
    val loadingPhrases = listOf(
        "Connecting to WinkIT AI...",
        "Analyzing local weather patterns...",
        "Checking historical dark store curfews...",
        "Calculating optimal risk premium...",
        "Finalizing policy generation..."
    )
    var phraseIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (phraseIndex < loadingPhrases.size - 1) {
            delay(700)
            phraseIndex++
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 2.5f,
        animationSpec = infiniteRepeatable(animation = tween(1500, easing = LinearOutSlowInEasing), repeatMode = RepeatMode.Restart),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(animation = tween(1500, easing = LinearOutSlowInEasing), repeatMode = RepeatMode.Restart),
        label = "alpha"
    )

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0A192F)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
            Box(modifier = Modifier.size(100.dp).scale(scale).alpha(alpha).background(Color(0xFF00E5A0), CircleShape))
            Box(modifier = Modifier.size(64.dp).background(Color.White, CircleShape)) {
                Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF0A192F), modifier = Modifier.align(Alignment.Center).size(32.dp))
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
        Text("WinkIT AI Engine", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Text(loadingPhrases[phraseIndex], color = Color(0xFF00E5A0), fontSize = 14.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
    }
}

@Composable
fun PolicyOfferView(workerId: String, weeklyPremium: String, maxPayout: String, onBack: () -> Unit, onProceedToProcessing: () -> Unit) {
    val context = LocalContext.current
    var isCashfreeLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        val gradientBrush = Brush.verticalGradient(listOf(Color(0xFF0A2A59), Color(0xFF006C7A)))

        Box(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.35f)
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(gradientBrush).padding(24.dp).systemBarsPadding()
        ) {
            Column {
                IconButton(onClick = onBack, modifier = Modifier.offset(x = (-12).dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(tr("Your Policy Offer"), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text(tr("Based on your risk profile, here is your weekly coverage plan."), color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(top = 160.dp).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Surface(shape = RoundedCornerShape(24.dp), color = Color.White, shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFF8F9FA), border = BorderStroke(1.dp, Color(0xFFE0E0E0)), modifier = Modifier.weight(1f)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(tr("WEEKLY PREMIUM"), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("₹$weeklyPremium", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color(0xFF0A192F))
                                }
                            }
                            Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFE8F5E9), modifier = Modifier.weight(1f)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(tr("MAX PAYOUT"), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("₹$maxPayout", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color(0xFF4CAF50))
                                    Text(tr("per incident"), fontSize = 12.sp, color = Color(0xFF4CAF50))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        CoverageRow("Covers extreme weather (heavy rain, heatwaves)")
                        Spacer(modifier = Modifier.height(12.dp))
                        CoverageRow("Covers dark store shutdowns & curfews")
                        Spacer(modifier = Modifier.height(12.dp))
                        CoverageRow("Instant payout to wallet")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFE3F2FD), border = BorderStroke(1.dp, Color(0xFFBBDEFB)), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF1976D2), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = tr("This policy covers income loss only. It does not cover vehicle damage or medical expenses."), color = Color(0xFF0D47A1), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                // 1. DEV BYPASS BUTTON
                Button(
                    onClick = onProceedToProcessing,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E6ED)),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Dev Bypass (Mock Success)", color = Color(0xFF5A7184), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))

                // 🔥 2. DIRECT CASHFREE INTEGRATION
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isCashfreeLoading = true
                            try {
                                val sessionData = withContext(Dispatchers.IO) {
                                    val generatedOrderId = "ORDER_${workerId}_${System.currentTimeMillis()}"

                                    val jsonPayload = JSONObject().apply {
                                        put("order_amount", weeklyPremium.toDouble())
                                        put("order_currency", "INR")
                                        put("order_id", generatedOrderId)
                                        put("customer_details", JSONObject().apply {
                                            put("customer_id", workerId)
                                            put("customer_phone", "9999999999")
                                        })
                                        put("order_meta", JSONObject().apply {
                                            put("return_url", "https://winkit.local/payment")
                                        })
                                    }

                                    val client = OkHttpClient()
                                    val request = Request.Builder()
                                        .url("https://sandbox.cashfree.com/pg/orders")
                                        .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
                                        .addHeader("x-client-id", BuildConfig.CASHFREE_APP_ID)
                                        .addHeader("x-client-secret", BuildConfig.CASHFREE_SECRET_KEY)
                                        .addHeader("x-api-version", "2023-08-01")
                                        .build()

                                    val response = client.newCall(request).execute()
                                    val responseBody = response.body?.string()

                                    if (response.isSuccessful && responseBody != null) {
                                        val jsonResponse = JSONObject(responseBody)
                                        Pair(jsonResponse.getString("payment_session_id"), generatedOrderId)
                                    } else {
                                        throw Exception("Cashfree Error: $responseBody")
                                    }
                                }

                                val cfSession = CFSession.CFSessionBuilder()
                                    .setEnvironment(CFSession.Environment.SANDBOX)
                                    .setPaymentSessionID(sessionData.first)
                                    .setOrderId(sessionData.second)
                                    .build()

                                val cfTheme = CFWebCheckoutTheme.CFWebCheckoutThemeBuilder()
                                    .setNavigationBarBackgroundColor("#5B2D8E")
                                    .setNavigationBarTextColor("#FFFFFF")
                                    .build()

                                val cfWebCheckoutPayment = CFWebCheckoutPayment.CFWebCheckoutPaymentBuilder()
                                    .setSession(cfSession)
                                    .setCFWebCheckoutUITheme(cfTheme)
                                    .build()

                                CFPaymentGatewayService.getInstance().doPayment(context as Activity, cfWebCheckoutPayment)

                            } catch (e: Exception) {
                                Log.e("Cashfree", "Direct API Call failed: ${e.message}")
                                Toast.makeText(context, "Network issue fetching session. Use Dev Bypass.", Toast.LENGTH_LONG).show()
                            } finally {
                                isCashfreeLoading = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5A0)),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isCashfreeLoading) {
                        CircularProgressIndicator(color = Color(0xFF1A1A2E), modifier = Modifier.size(24.dp))
                    } else {
                        Text("Pay ₹$weeklyPremium via Cashfree", color = Color(0xFF1A1A2E), fontSize = 16.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun CoverageRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = Color(0xFFE8F5E9), modifier = Modifier.size(20.dp)) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.padding(2.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, color = Color.DarkGray, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
    }
}