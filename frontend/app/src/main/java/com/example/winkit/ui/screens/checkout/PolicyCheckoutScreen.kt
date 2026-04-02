package com.example.winkit.ui.screens.checkout

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import com.example.winkit.data.NetworkModule
import com.example.winkit.data.WeeklyPolicyInsert
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class CheckoutStep {
    ANALYZING, OFFER, PROCESSING_PAYMENT, SUCCESS
}

@Composable
fun PolicyCheckoutScreen(workerId: String, onBack: () -> Unit, onPaymentSuccess: () -> Unit) {
    var currentStep by remember { mutableStateOf(CheckoutStep.ANALYZING) }
    var weeklyPremium by remember { mutableStateOf("...") }
    var maxPayout by remember { mutableStateOf("...") }

    LaunchedEffect(Unit) {
        try {
            val responseList = NetworkModule.api.getWorkerCharges(workerId = "eq.$workerId")
            if (responseList.isNotEmpty()) {
                val charge = responseList.first()
                weeklyPremium = charge.premium?.toInt()?.toString() ?: "132"
                maxPayout = "800"
            } else {
                weeklyPremium = "132"
                maxPayout = "800"
            }
            delay(1500)
            currentStep = CheckoutStep.OFFER
        } catch (e: Exception) {
            Log.e("SupabaseError", "Failed to fetch charges: ${e.message}")
            weeklyPremium = "132"
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
                    // 1. Generate the unique ID and Dates
                    val policyId = "POL-${System.currentTimeMillis().toString().takeLast(4)}"
                    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val cal = Calendar.getInstance()
                    val startDate = formatter.format(cal.time)
                    cal.add(Calendar.DAY_OF_YEAR, 7)
                    val endDate = formatter.format(cal.time)

                    // 2. Use the updated 7-parameter Data Class
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

    // MAIN UI BOX
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F7FA))) {

        Crossfade(targetState = currentStep, animationSpec = tween(800), label = "screen_fade") { step ->
            when (step) {
                CheckoutStep.ANALYZING -> {
                    AiAnalyzingView()
                }
                CheckoutStep.OFFER, CheckoutStep.PROCESSING_PAYMENT, CheckoutStep.SUCCESS -> {
                    PolicyOfferView(
                        weeklyPremium = weeklyPremium,
                        maxPayout = maxPayout,
                        onBack = onBack,
                        onSwipeComplete = { currentStep = CheckoutStep.PROCESSING_PAYMENT }
                    )
                }
            }
        }

        // OVERLAY: PAYMENT CONFIRMATION
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
                            Text("Setting up Auto-Pay...", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0A192F))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Authorizing with UPI", fontSize = 14.sp, color = Color.Gray)
                        } else {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Success", tint = Color(0xFF4CAF50), modifier = Modifier.size(80.dp))
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("Auto-Pay Enabled!", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF4CAF50))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Your coverage is now active.", fontSize = 14.sp, color = Color.Gray)

                            // 🔥 FALLBACK BUTTON ADDED HERE 🔥
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = onPaymentSuccess, // THIS TRIGGERS THE NAVIGATION!
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Go to Dashboard", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── 1. THE AI ANALYZING ANIMATION ─────────────────────────────────────────
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
        initialValue = 0.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(animation = tween(1500, easing = LinearOutSlowInEasing), repeatMode = RepeatMode.Restart),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(animation = tween(1500, easing = LinearOutSlowInEasing), repeatMode = RepeatMode.Restart),
        label = "alpha"
    )

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0A192F)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(scale)
                    .alpha(alpha)
                    .background(Color(0xFF00E5A0), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color.White, CircleShape)
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF0A192F), modifier = Modifier.align(Alignment.Center).size(32.dp))
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
        Text("WinkIT AI Engine", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Text(loadingPhrases[phraseIndex], color = Color(0xFF00E5A0), fontSize = 14.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
    }
}

// ─── 2. THE POLICY OFFER UI ────────────────────────────────────────────────
@Composable
fun PolicyOfferView(weeklyPremium: String, maxPayout: String, onBack: () -> Unit, onSwipeComplete: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        val gradientBrush = Brush.verticalGradient(listOf(Color(0xFF0A2A59), Color(0xFF006C7A)))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.35f)
                .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                .background(gradientBrush)
                .padding(24.dp)
                .systemBarsPadding()
        ) {
            Column {
                IconButton(onClick = onBack, modifier = Modifier.offset(x = (-12).dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Your Policy Offer", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Based on your risk profile, here is your weekly coverage plan.", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 160.dp)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFF8F9FA),
                                border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("WEEKLY PREMIUM", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("₹$weeklyPremium", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color(0xFF0A192F))
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFE8F5E9),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("MAX PAYOUT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("₹$maxPayout", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color(0xFF4CAF50))
                                    Text("per incident", fontSize = 12.sp, color = Color(0xFF4CAF50))
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

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE3F2FD),
                    border = BorderStroke(1.dp, Color(0xFFBBDEFB)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF1976D2), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "This policy covers income loss only. It does not cover vehicle damage or medical expenses.",
                            color = Color(0xFF0D47A1),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp), contentAlignment = Alignment.Center) {
                SwipeToPaySlider(weeklyPremium = weeklyPremium, onSwipeComplete = onSwipeComplete)
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

// ─── 3. SWIPE TO PAY LOGIC ─────────────────────────────────────────────────
@Composable
fun SwipeToPaySlider(weeklyPremium: String, onSwipeComplete: () -> Unit){
    val sliderWidth = 320.dp
    val thumbSize = 56.dp
    val sliderWidthPx = with(LocalDensity.current) { sliderWidth.toPx() }
    val thumbSizePx = with(LocalDensity.current) { thumbSize.toPx() }
    val maxDragPx = sliderWidthPx - thumbSizePx

    val offsetX = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .width(sliderWidth)
            .height(thumbSize)
            .background(Color(0xFF0A192F), RoundedCornerShape(32.dp)),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = "Swipe to Auto-Pay ₹$weeklyPremium >>>",
            color = Color.White.copy(alpha = 0.6f),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .size(thumbSize)
                .padding(4.dp)
                .background(Color.White, RoundedCornerShape(32.dp))
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                if (offsetX.value > maxDragPx * 0.7f) {
                                    offsetX.animateTo(maxDragPx)
                                    onSwipeComplete()
                                } else {
                                    offsetX.animateTo(0f)
                                }
                            }
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch {
                            val newOffset = (offsetX.value + dragAmount).coerceIn(0f, maxDragPx)
                            offsetX.snapTo(newOffset)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text("→", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0A192F))
        }
    }
}