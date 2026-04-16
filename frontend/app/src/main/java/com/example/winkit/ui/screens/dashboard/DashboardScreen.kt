package com.example.winkit.ui.screens.dashboard

import android.Manifest
import android.os.Build.VERSION.SDK_INT
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import com.example.winkit.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.winkit.domain.models.EnvironmentType
import com.example.winkit.ui.components.ShiftSafeBottomNav
import com.example.winkit.ui.screens.chatbot.WinkitChatFab
import com.example.winkit.ui.screens.chatbot.WinkitChatbotSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.example.winkit.ui.screens.alerts.RelocationAlertModal
import android.os.Build
import com.example.winkit.utils.tr

// ── COLORS ──────────────────────────────────────────────────────────────────
private val BannerStart  = Color(0xFF5B2D8E)
private val BannerEnd    = Color(0xFF8B3FBF)
private val StarDot      = Color(0xCCFFFFFF)
private val NeonGreen    = Color(0xFF00E5A0)
private val TagModerate  = Color(0xFFFF8C42)
private val TagLow       = Color(0xFF00C48C)
private val TagPoor      = Color(0xFFE53935)
private val TagOpen      = Color(0xFF00ACC1)
private val CardBg       = Color(0xFFFFFFFF)
private val PageBg       = Color(0xFFF3F4F8)
private val TextDark     = Color(0xFF1A1A2E)
private val TextGray     = Color(0xFF8E8E9A)
private val NavSelected  = Color(0xFF5B2D8E)
private val NavUnselected= Color(0xFFB0B0C0)
private val GpsIcon      = Color(0xFF5B2D8E)

@Composable
fun ShiftSafeDashboard(
    workerId: String,
    viewModel: DashboardViewModel,
    navController: NavController,
    onTriggerAlert: () -> Unit,
    onPolicyClick: () -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        // Wait exactly 12 seconds after the dashboard loads
        delay(12000) 
        
        // Fire the engagement notification!
        com.example.winkit.utils.NotificationHelper.showPromoNotification(
            context = context, 
            title = "👋 Long time no see!", 
            message = "Surge pricing is active in your area. Jump online now to earn!"
        )
    }
    val coroutineScope = rememberCoroutineScope()
    var hasPolicy by remember { mutableStateOf<Boolean?>(null) }
    var walletBalance by remember { mutableStateOf(0.0) }
    var workerName by remember { mutableStateOf("Rider") }
    var showManualClaimDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var isActivatingPolicy by remember { mutableStateOf(false) } 
    var showRelocationModal by remember { mutableStateOf(false) }
    // ── CHATBOT STATE ───────────────────────────────────────────────────────
    var showChatbot by remember { mutableStateOf(false) }
    var isVerifyingLocation by remember { mutableStateOf(false) }
    var mapDataJson by remember { mutableStateOf("[]") }
    
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { /* We just need to ask, don't strictly need to handle result for demo */ }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val isGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (isGranted) {
                isVerifyingLocation = true
                viewModel.verifyLocationAndClaim(workerId)
                coroutineScope.launch {
                    delay(11000)
                    isVerifyingLocation = false
                    onTriggerAlert()
                }
            } else {
                Toast.makeText(context, tr("Location required for fraud prevention"), Toast.LENGTH_LONG).show()
            }
        }
    )

LaunchedEffect(workerId) {
        try {
            val profile = com.example.winkit.data.NetworkModule.api.getWorkerProfile("eq.$workerId")
            if (profile.isNotEmpty()) {
                workerName = profile.first().name ?: "Rider"
            }

            val policies = com.example.winkit.data.NetworkModule.api.getActivePolicies(
                workerId = "eq.$workerId", 
                status = "eq.ACTIVE"
            )
            hasPolicy = policies.isNotEmpty()

            walletBalance = com.example.winkit.data.SupabaseAuthHelper.getWalletBalance(workerId)

            val zones = com.example.winkit.data.NetworkModule.api.getZoneData()
            val formattedJson = zones.joinToString(prefix = "[", postfix = "]") { zone ->
                val hexId = zone["hex_id"].toString()
                val risk = (zone["v_zone_score"] as? Double) ?: 0.0
                val height = (risk * 50).toInt() + 5 // Calculates the 3D height based on risk
                "{ hex: '$hexId', risk: $risk, height: $height }"
            }
            mapDataJson = formattedJson

        } catch (e: Exception) {
            hasPolicy = false
        }
    }


    Scaffold(
        containerColor = PageBg,
        floatingActionButton = { WinkitChatFab(onClick = { showChatbot = true }) },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->

        // while the main content stays inside the scrollable Column.
        Box(modifier = Modifier.fillMaxSize()) {

            // ── MAIN SCROLLABLE COLUMN ──
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                WeatherBanner(
                    name = workerName,
                    walletBalance = walletBalance,
                    temp = viewModel.temperature,
                    condition = viewModel.weatherCondition,
                    onTermsClick = { 
                        isActivatingPolicy = false // Just viewing
                        showTermsDialog = true 
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                when (hasPolicy) {
                    null -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    false -> {
                        FirstTimeActivationCard(
                            onActivate = { 
                                isActivatingPolicy = true 
                                showTermsDialog = true 
                            }
                        )
                    }
                    true -> {
                        ActivePoliciesSection(onPolicyClick = onPolicyClick)
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = tr("Live Risk Metrics"),
                            color = TextDark,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        RiskMetricsGrid(
                            temp = viewModel.temperature,
                            rainProb = viewModel.rainProbability,
                            humidity = viewModel.humidity
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        GpsTrackingSection(
                            zoneDataJson = mapDataJson, 
                            onReportClick = { onTriggerAlert() }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        /*HazardReportCard(
                            onReportClick = {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        )*/ 
                        HazardReportCard(
                            onReportClick = { showManualClaimDialog = true }
                        )

                      Spacer(modifier = Modifier.height(32.dp))
                        
                        // ── HACKATHON DEMO PANEL ──
                        Text(tr("Demo Triggers"), color = Color.LightGray, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp))
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { showRelocationModal = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(tr("Trigger Relocation"), fontSize = 10.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            } // <--- End of main scrollable Column
            // ── MANUAL CLAIM DIALOG ──
            if (showManualClaimDialog) {
                ManualClaimDialog(
                    onDismiss = { showManualClaimDialog = false },
                    onSubmit = { hazardType, description ->
                        showManualClaimDialog = false
                        
                        // Launch coroutine to push to Supabase
                        coroutineScope.launch {
                            Toast.makeText(context, tr("Submitting claim..."), Toast.LENGTH_SHORT).show()
                            
                            val success = com.example.winkit.data.SupabaseAuthHelper.fileManualClaim(
                                workerId = workerId,
                                lat = 12.9815, // Use real GPS coordinates if available!
                                lng = 80.2230,
                                hazardType = hazardType,
                                description = description
                            )
                            
                            if (success) {
                                Toast.makeText(context, tr("Claim submitted for review!"), Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, tr("Failed to submit claim"), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
          // ── TERMS AND CONDITIONS DIALOG ──
            if (showTermsDialog) {
                TermsDialog(
                    onDecline = { showTermsDialog = false },
                    onAccept = {
                        showTermsDialog = false
                        
                        if (isActivatingPolicy) { // ONLY charge if they clicked from the green card
                            coroutineScope.launch {
                                val success = com.example.winkit.data.SupabaseAuthHelper.activateFirstPolicy(workerId)
                                if (success) {
                                    hasPolicy = true
                                    walletBalance = com.example.winkit.data.SupabaseAuthHelper.getWalletBalance(workerId)
                                    Toast.makeText(context, tr("Protection Activated!"), Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, tr("Activation failed"), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                )
            }
            // ── RELOCATION MODAL ──
            if (showRelocationModal) {
                com.example.winkit.ui.screens.alerts.RelocationAlertModal(
                    onAccept = {
                        showRelocationModal = false
                        
                        // 🔥 FIX: Force network call to the Background Thread (IO) so the UI doesn't hang!
                        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                val relocationEvent = com.example.winkit.data.RelocationInsert(
                                    worker_id = workerId,
                                    from_zone = "Velachery",
                                    to_zone = "Adyar Hub",
                                    bonus_amount = 150
                                )
                                // This now runs safely in the background
                                com.example.winkit.data.NetworkModule.api.insertRelocationEvent(relocationEvent)
                                
                                // Switch back to Main thread ONLY to show the Toast
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    Toast.makeText(context, tr("routing to adyar. ₹150 bonus locked!"), Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                    Toast.makeText(context, tr("error accepting route:") + " ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    onDismiss = { showRelocationModal = false }
                )
            }      // ── CHATBOT SHEET (Floats on top of the UI) ──
            if (showChatbot) {
                WinkitChatbotSheet(onDismiss = { showChatbot = false })
            }
        }
    }
}

// ── Weather banner ─────────────────────────────────────────────────────────
@Composable
fun WeatherBanner(name: String, walletBalance: Double, temp: String, condition: String, onTermsClick: () -> Unit) {
    val currentTime = remember {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
    }
    
    // 1. Get the current hour (0-23)
    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    
    val tempInt = temp.toIntOrNull() ?: 25
    
    // 2. Define Time of Day
    val isDay = currentHour in 6..16 // 6 AM to 4 PM
    val isEvening = currentHour in 17..18 // 5 PM to 6 PM
    val isNight = currentHour >= 19 || currentHour < 6 // 7 PM to 5 AM

    // 3. Define Weather Overrides
    val isRaining = condition.contains("Rain", ignoreCase = true) || condition.contains("Storm", ignoreCase = true)
    val isExtremeHeat = tempInt >= 35

    // 4. Set the gradient colors based on time & weather
    val dynamicGradient = when {
        isRaining -> listOf(Color(0xFF37474F), Color(0xFF546E7A)) // Stormy Grey
        isExtremeHeat -> listOf(Color(0xFFD32F2F), Color(0xFFFF7043)) // Heatwave Red
        isDay -> listOf(Color(0xFF1E88E5), Color(0xFF64B5F6)) // Bright Day Sky Blue
        isEvening -> listOf(Color(0xFFE65100), Color(0xFFFFB74D)) // Sunset Orange
        else -> listOf(BannerStart, BannerEnd) // Night Premium Purple (Your default)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(
                brush = Brush.linearGradient(colors = dynamicGradient),
                shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
            )
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
    ) {
        // Only show stars at night, and not during rain
        if (isNight && !isRaining) {
            StarField()
        }
        
        Text(
            text = tr("Terms & Conditions"),
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .clickable { onTermsClick() }
        )
        
        Surface(
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
            color = Color.White.copy(alpha = 0.15f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), horizontalAlignment = Alignment.End) {
                Text("WALLET BALANCE", color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(
                    "₹${String.format("%.2f", walletBalance)}",
                    color = if (walletBalance < 0) Color(0xFFFFCDD2) else NeonGreen,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Column(modifier = Modifier.align(Alignment.CenterStart).padding(start = 20.dp, top = 12.dp)) {
            Text(tr("Welcome back,"), color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
            Text(name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccessTime, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(currentTime, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("$temp°C", color = Color.White, fontSize = 52.sp, fontWeight = FontWeight.Black)
            Text(condition, color = Color.White.copy(alpha = 0.9f), fontSize = 16.sp)
        }

        Column(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp, top = 40.dp), horizontalAlignment = Alignment.End) {
            // Dynamic Illustration Logic
            if (!isExtremeHeat) {
                when {
                    isDay -> SunCloudIllustration(sunColor = Color(0xFFFFD54F)) // Yellow Sun
                    isEvening -> SunCloudIllustration(sunColor = Color(0xFFFF8A65)) // Orange Sunset
                    else -> MoonCloudIllustration() // Night Moon
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            CoveragePill()
        }
    }
}

// ── Add this new Composable right below MoonCloudIllustration! ──
@Composable
fun SunCloudIllustration(sunColor: Color) {
    Box(modifier = Modifier.size(width = 100.dp, height = 70.dp)) {
        // The Sun
        Box(
            modifier = Modifier
                .size(56.dp)
                .align(Alignment.TopCenter)
                .clip(CircleShape)
                .background(sunColor)
        )
        // The Cloud (Slightly brighter for daytime)
        Box(
            modifier = Modifier
                .width(90.dp)
                .height(32.dp)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.9f))
        )
    }
}@Composable
fun StarField() {
    Box(modifier = Modifier.fillMaxSize()) {
        val dots = listOf(0.15f to 0.08f, 0.7f to 0.05f, 0.85f to 0.15f, 0.05f to 0.5f, 0.6f to 0.35f, 0.9f to 0.55f)
        dots.forEach { (xFrac, yFrac) ->
            Box(modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.TopStart)) {
                Box(modifier = Modifier.offset(x = (xFrac * 300).dp, y = (yFrac * 240).dp).size(3.dp).clip(CircleShape).background(StarDot))
            }
        }
    }
}

@Composable
fun MoonCloudIllustration() {
    Box(modifier = Modifier.size(width = 100.dp, height = 70.dp)) {
        Box(modifier = Modifier.size(64.dp).align(Alignment.TopCenter).clip(CircleShape).background(Color.White.copy(alpha = 0.88f)))
        Box(modifier = Modifier.width(90.dp).height(32.dp).align(Alignment.BottomCenter).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.55f)))
    }
}

@Composable
fun CoveragePill() {
    Row(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.18f)).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(NeonGreen))
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(tr("Coverage"), color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
            Text(tr("Active"), color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RiskMetricsGrid(temp: String, rainProb: String, humidity: String) {
    val tempInt = temp.toIntOrNull() ?: 24
    val wetBulbRisk = if (tempInt > 30) "HIGH" else "MODERATE"
    val wetBulbColor = if (tempInt > 30) TagPoor else TagModerate
    val mockAqi = 85
    val aqiRisk  = if (mockAqi > 300) "POOR" else if (mockAqi > 150) "MODERATE" else "GOOD"
    val aqiColor = if (mockAqi > 300) TagPoor else if (mockAqi > 150) TagModerate else TagLow

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RiskCard(Modifier.weight(1f), Icons.Default.Thermostat, Color(0xFFFF7043), "LIVE", TagModerate, "$temp°C", "Est. Wet Bulb Temp")
            RiskCard(Modifier.weight(1f), Icons.Default.WaterDrop, Color(0xFF5C9EE8), "UPDATED", TagLow, rainProb, "Rain Probability")
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RiskCard(Modifier.weight(1f), Icons.Default.Air, Color(0xFF78909C), aqiRisk, aqiColor, "$mockAqi", "Air Quality Index")
            RiskCard(Modifier.weight(1f), Icons.Default.Store, Color(0xFF26A69A), "ONLINE", TagOpen, "Velachery", "Primary Hub")
        }
    }
}

@Composable
fun RiskCard(modifier: Modifier, iconVector: ImageVector, iconTint: Color, tag: String, tagColor: Color, value: String, label: String) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = CardBg), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Icon(iconVector, null, tint = iconTint, modifier = Modifier.size(22.dp))
                Text(tag, color = tagColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(tagColor.copy(alpha = 0.12f)).padding(horizontal = 6.dp, vertical = 2.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(value, color = TextDark, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text(label, color = TextGray, fontSize = 12.sp)
        }
    }
}

@Composable
fun GpsTrackingSection(
    liveLat: Double = 12.9815,
    liveLng: Double = 80.2230,
    zoneDataJson: String = "[]", // 🔥 WE WILL PASS REAL DATA HERE
    onReportClick: () -> Unit = {}
){
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Live Risk Telemetry"), color = Color(0xFF1A1A2E), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFF00E5A0).copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MyLocation, null, tint = Color(0xFF00C48C), modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(tr("3D Sync Active"), color = Color(0xFF00C48C), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        
        Card(
            shape = RoundedCornerShape(16.dp), 
            elevation = CardDefaults.cardElevation(6.dp), 
            modifier = Modifier.fillMaxWidth().height(280.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                
                // 🔥 THE NATIVE DECK.GL ENGINE
                AndroidView(factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                        webViewClient = WebViewClient()
                        
                        // We inject the JSON string directly into the JS variable 'backendData'
                        // We inject the JSON string directly into the JS variable 'backendData'
                        val htmlData = """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
                                <script src="https://unpkg.com/maplibre-gl@3.3.1/dist/maplibre-gl.js"></script>
                                <link href="https://unpkg.com/maplibre-gl@3.3.1/dist/maplibre-gl.css" rel="stylesheet" />
                                <script src="https://unpkg.com/deck.gl@8.9.0/dist.min.js"></script>
                                <style>
                                    body { margin: 0; padding: 0; background: #0f172a; overflow: hidden; }
                                    #map-container { width: 100vw; height: 100vh; }
                                    .pulse-dot {
                                        width: 18px; height: 18px;
                                        background-color: #00E5A0;
                                        border-radius: 50%;
                                        box-shadow: 0 0 15px rgba(0, 229, 160, 0.8);
                                        animation: pulse 1.5s infinite cubic-bezier(0.66, 0, 0, 1);
                                        border: 2px solid #FFFFFF; /* Added a white border to make it pop! */
                                    }
                                    @keyframes pulse {
                                        to { box-shadow: 0 0 0 40px rgba(0, 229, 160, 0); }
                                    }
                                    .leaflet-control-attribution { display: none; }
                                </style>
                            </head>
                            <body>
                                <div id="map-container"></div>
                                <script>
                                    const { DeckGL, H3HexagonLayer } = deck;
                                    
                                    const backendData = $zoneDataJson;

                                    const mapData = backendData.length > 0 ? backendData : [
                                        { hex: '8961892a03bffff', risk: 0.8, height: 40 },
                                        { hex: '8961892a03bffff', risk: 0.2, height: 10 }
                                    ];

                                    const hexagonLayer = new H3HexagonLayer({
                                        id: 'h3-layer',
                                        data: mapData,
                                        pickable: true,
                                        wireframe: false,
                                        filled: true,
                                        extruded: true,
                                        elevationScale: 20,
                                        getHexagon: d => d.hex,
                                        getFillColor: d => {
                                            const risk = d.risk;
                                            return [
                                                Math.round(16 + (225 - 16) * risk),  // R
                                                Math.round(185 + (26 - 185) * risk), // G
                                                Math.round(129 + (80 - 129) * risk), // B
                                                200 // Alpha
                                            ];
                                        },
                                        getElevation: d => d.height
                                    });

                                    // 🔥 FIX 1: Assign DeckGL to a variable
                                    const deckglInstance = new DeckGL({
                                        container: 'map-container',
                                        mapStyle: 'https://basemaps.cartocdn.com/gl/dark-matter-gl-style/style.json',
                                        initialViewState: {
                                            longitude: $liveLng,
                                            latitude: $liveLat,
                                            zoom: 13.5, // 🔥 Zoomed in a tiny bit more so the dot looks better
                                            pitch: 55,
                                            bearing: 0
                                        },
                                        controller: true,
                                        layers: [hexagonLayer]
                                    });

                                    // 🔥 FIX 2: Use the official getMapboxMap() method
                                    setTimeout(() => {
                                        const map = deckglInstance.getMapboxMap();
                                        if(map) {
                                            const el = document.createElement('div');
                                            el.className = 'pulse-dot';
                                            new maplibregl.Marker({element: el})
                                                .setLngLat([$liveLng, $liveLat])
                                                .addTo(map);
                                        }
                                    }, 1000);
                                </script>
                            </body>
                            </html>
                        """.trimIndent()                   
                        loadDataWithBaseURL("https://app.local/", htmlData, "text/html", "UTF-8", null)
                    }
                }, modifier = Modifier.fillMaxSize())

                // ── UI OVERLAYS ──
                Surface(color = Color(0xFF0F172A).copy(alpha = 0.85f), shape = RoundedCornerShape(8.dp), modifier = Modifier.padding(12.dp).align(Alignment.TopStart)) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("3D GRID STATUS", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFF10B981), CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tr("Safe Zone"), color = Color.White, fontSize = 10.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(Color(0xFFE11D48), CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tr("High Risk"), color = Color.White, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun ActivePoliciesSection(onPolicyClick: () -> Unit){
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(tr("Active Policies"), color = Color(0xFF1A1A2E), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(tr("View All"), color = Color(0xFF5B2D8E), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFE0E6ED)), modifier = Modifier.fillMaxWidth().clickable { onPolicyClick() }){
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFF3F4F8)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Description, null, tint = Color(0xFF5A7184), modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Gig Protect Weekly", color = Color(0xFF1A1A2E), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Auto-renews on 22 Mar", color = Color(0xFF8E8E9A), fontSize = 12.sp)
                    }
                }
                Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF8E8E9A))
            }
        }
    }
}

@Composable
fun HazardReportCard(onReportClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Color(0xFFFFEBEE))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Warning Icon Box
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFEBEE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Warning, null, tint = Color(0xFFD32F2F), modifier = Modifier.size(24.dp))
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Text Content
            Column(modifier = Modifier.weight(1f)) {
                Text(tr("Stuck in a Hazard?"), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1A1A2E))
                Text(tr("Report flooded roads or curfew to initiate manual review."), fontSize = 12.sp, color = Color.Gray, lineHeight = 16.sp)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // The Button
            Button(
                onClick = onReportClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(tr("Report"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}
@Composable
fun FirstTimeActivationCard(onActivate: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF1A0A3B), Color(0xFF5B2D8E)) // Deep premium purple
                    )
                )
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Shiny Icon Badge
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        Icons.Default.VerifiedUser,
                        contentDescription = "Secure",
                        tint = Color(0xFF00E5A0), // Neon Green
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Activate Income Protection",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Secure your earnings against floods, heatwaves, and extreme weather. Start your first week of coverage now.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onActivate,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5A0)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text(tr("Activate Now • ₹49/wk"), color = Color(0xFF1A0A3B), fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualClaimDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var selectedHazard by remember { mutableStateOf("Flood / Waterlogging") }
    val hazards = listOf("Flood / Waterlogging", "Extreme Heatwave", "Curfews/Government", "Others")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text(tr("File Manual Claim"), fontWeight = FontWeight.Bold, color = Color(0xFF1A1A2E)) },
        text = {
            Column {
                Text(tr("Select Hazard Type"), fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                
                // Simple dropdown simulation (Scrollable Row of chips for hackathon speed)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    hazards.forEach { hazard ->
                        FilterChip(
                            selected = selectedHazard == hazard,
                            onClick = { selectedHazard = hazard },
                            label = { Text(hazard, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFE8F5E9),
                                selectedLabelColor = Color(0xFF2E7D32)
                            ),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Text(tr("Description (Optional)"), fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text(tr("E.g., Road blocked near Velachery bridge"), fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(selectedHazard, description) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF5222D)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(tr("Submit Claim"), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Cancel"), color = Color.Gray)
            }
        }
    )
}

@Composable
fun TermsDialog(onAccept: () -> Unit, onDecline: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDecline,
        containerColor = Color.White,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF5B2D8E))
                Spacer(modifier = Modifier.width(8.dp))
                Text(tr("Terms of Protection"), fontWeight = FontWeight.Bold, color = Color(0xFF1A1A2E), fontSize = 18.sp)
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(tr("1. Parametric Triggers"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(tr("Payouts are triggered automatically when IMD or independent weather APIs confirm conditions exceed the severe threshold in your designated H3 Hexagon."), fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(tr("2. Fraud Prevention"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(tr("Using mock GPS locations, rooted devices, or submitting false manual claims will result in immediate suspension and forfeiture of the premium."), fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(tr("3. Payout Limits"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(tr("Maximum daily coverage is capped at ₹800. This covers lost potential earnings and does not constitute vehicle or medical insurance."), fontSize = 12.sp, color = Color.Gray)
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5B2D8E)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(tr("I Accept"), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) {
                Text(tr("Decline"), color = Color.Gray)
            }
        }
    )
}
