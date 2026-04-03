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
    val coroutineScope = rememberCoroutineScope()
    var hasPolicy by remember { mutableStateOf<Boolean?>(null) }
    var walletBalance by remember { mutableStateOf(0.0) }
    var workerName by remember { mutableStateOf("Rider") }
    var showManualClaimDialog by remember { mutableStateOf(false) }
    // ── CHATBOT STATE ───────────────────────────────────────────────────────
    var showChatbot by remember { mutableStateOf(false) }
    var isVerifyingLocation by remember { mutableStateOf(false) }

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
                Toast.makeText(context, "Location required for fraud prevention", Toast.LENGTH_LONG).show()
            }
        }
    )

    LaunchedEffect(workerId) {
        try {
            val profile = com.example.winkit.data.NetworkModule.api.getWorkerProfile("eq.$workerId")
            if (profile.isNotEmpty()) {
                workerName = profile.first().name ?: "Rider"
            }

            val policies = com.example.winkit.data.NetworkModule.api.getPoliciesByWorker("eq.$workerId")
            hasPolicy = policies.isNotEmpty()

            walletBalance = com.example.winkit.data.SupabaseAuthHelper.getWalletBalance(workerId)
        } catch (e: Exception) {
            hasPolicy = false
        }
    }

    Scaffold(
        bottomBar = { ShiftSafeBottomNav(navController = navController) },
        containerColor = PageBg,
        floatingActionButton = { WinkitChatFab(onClick = { showChatbot = true }) },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->

        // 🔥 FIXED: Wrap everything in a Box so the Chatbot sheet floats on top,
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
                    condition = viewModel.weatherCondition
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 🔥 FIXED: The 'when' block is now SAFELY INSIDE the Column
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
                                coroutineScope.launch {
                                    val success = com.example.winkit.data.SupabaseAuthHelper.activateFirstPolicy(workerId)
                                    if (success) {
                                        hasPolicy = true
                                        walletBalance = com.example.winkit.data.SupabaseAuthHelper.getWalletBalance(workerId)
                                    } else {
                                        Toast.makeText(context, "Activation failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }

                    true -> {
                        Text(
                            text = "Live Risk Metrics",
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

                        GpsTrackingSection(onReportClick = { onTriggerAlert() })

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

                        Spacer(modifier = Modifier.height(24.dp))

                        ActivePoliciesSection(onPolicyClick = onPolicyClick)
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
                            Toast.makeText(context, "Submitting claim...", Toast.LENGTH_SHORT).show()
                            
                            val success = com.example.winkit.data.SupabaseAuthHelper.fileManualClaim(
                                workerId = workerId,
                                lat = 12.9815, // Use real GPS coordinates if available!
                                lng = 80.2230,
                                hazardType = hazardType,
                                description = description
                            )
                            
                            if (success) {
                                Toast.makeText(context, "Claim submitted for review!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Failed to submit claim", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
            // ── CHATBOT SHEET (Floats on top of the UI) ──
            if (showChatbot) {
                WinkitChatbotSheet(onDismiss = { showChatbot = false })
            }
        }
    }
}

// ── Weather banner ─────────────────────────────────────────────────────────
@Composable
fun WeatherBanner(name: String, walletBalance: Double, temp: String, condition: String) {
    val currentTime = remember {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(
                brush = Brush.linearGradient(colors = listOf(BannerStart, BannerEnd)),
                shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
            )
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
    ) {
        StarField()

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
            Text("Welcome back,", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
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
            MoonCloudIllustration()
            Spacer(modifier = Modifier.height(10.dp))
            CoveragePill()
        }
    }
}

@Composable
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
            Text("Coverage", color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
            Text("Active", color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
fun GpsTrackingSection(onReportClick: () -> Unit = {}){
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Live GPS Tracking", color = TextDark, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(NeonGreen.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MyLocation, null, tint = NeonGreen, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Active", color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = CardBg), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth().height(220.dp)) {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        settings.javaScriptEnabled = true
                        webViewClient = WebViewClient()
                        val htmlData = """
                            <!DOCTYPE html><html><head><meta name="viewport" content="width=device-width, initial-scale=1.0"/><link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" /><script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script><style>body{padding:0;margin:0}#map{height:100vh}</style></head><body><div id="map"></div><script>var map=L.map('map',{zoomControl:false}).setView([12.9815,80.2230],14);L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);L.circle([12.9815,80.2230],{color:'#E65100',fillColor:'#FF9800',fillOpacity:0.4,radius:800}).addTo(map);</script></body></html>
                        """.trimIndent()
                        loadDataWithBaseURL("https://app.local/", htmlData, "text/html", "UTF-8", null)
                    }
                }, modifier = Modifier.fillMaxSize())

                Surface(color = Color(0xFFFFF3E0), shape = RoundedCornerShape(8.dp), modifier = Modifier.padding(12.dp).align(Alignment.TopStart), border = BorderStroke(1.dp, Color(0xFFFFB74D))) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFE65100)))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("100ft Road Closed (Waterlogging)", color = Color(0xFFE65100), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Surface(color = Color.White.copy(alpha = 0.95f), shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp), modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = GpsIcon)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("CURRENT ZONE", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("Velachery, Chennai", color = TextDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
            Text("Active Policies", color = Color(0xFF1A1A2E), fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("View All", color = Color(0xFF5B2D8E), fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
    Surface(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(24.dp), color = Color(0xFFFFF1F0), border = BorderStroke(1.dp, Color(0xFFFFCCC7))) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Warning, null, tint = Color(0xFFF5222D), modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text("Hazardous Conditions?", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF820014))
            Text("Report flood or extreme weather to secure your payout.", textAlign = TextAlign.Center, fontSize = 13.sp, color = Color(0xFFCF1322), modifier = Modifier.padding(vertical = 8.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onReportClick, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF5222D)), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Text("Report Hazard & Claim Payout", fontWeight = FontWeight.Bold)
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
                    Text("Activate Now • ₹49/wk", color = Color(0xFF1A0A3B), fontWeight = FontWeight.Black, fontSize = 16.sp)
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
    val hazards = listOf("Flood / Waterlogging", "Extreme Heatwave", "Dark Store Curfew", "Vehicle Breakdown")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("File Manual Claim", fontWeight = FontWeight.Bold, color = Color(0xFF1A1A2E)) },
        text = {
            Column {
                Text("Select Hazard Type", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
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
                
                Text("Description (Optional)", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("E.g., Road blocked near Velachery bridge", fontSize = 12.sp) },
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
                Text("Submit Claim", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}
