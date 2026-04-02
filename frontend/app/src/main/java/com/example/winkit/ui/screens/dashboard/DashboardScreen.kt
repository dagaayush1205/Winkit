package com.example.winkit.ui.screens.dashboard

import android.os.Build.VERSION.SDK_INT
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.example.winkit.domain.models.DashboardState
import com.example.winkit.domain.models.EnvironmentType
import androidx.navigation.NavController
import com.example.winkit.ui.components.ShiftSafeBottomNav
import com.example.winkit.ui.screens.dashboard.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.BorderStroke
import android.view.ViewGroup
import androidx.compose.material.icons.filled.Warning
import android.Manifest
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.foundation.clickable

private val BannerStart  = Color(0xFF5B2D8E)   // deep purple (left)
private val BannerEnd    = Color(0xFF8B3FBF)   // violet (right)
private val StarDot      = Color(0xCCFFFFFF)
private val NeonGreen    = Color(0xFF00E5A0)
private val TagModerate  = Color(0xFFFF8C42)   // orange text
private val TagLow       = Color(0xFF00C48C)   // teal text
private val TagPoor      = Color(0xFFE53935)   // red text
private val TagOpen      = Color(0xFF00ACC1)   // cyan text
private val CardBg       = Color(0xFFFFFFFF)
private val PageBg       = Color(0xFFF3F4F8)
private val TextDark     = Color(0xFF1A1A2E)
private val TextGray     = Color(0xFF8E8E9A)
private val NavSelected  = Color(0xFF5B2D8E)
private val NavUnselected= Color(0xFFB0B0C0)
private val GpsBg        = Color(0xFFF8F9FF)
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
    var isVerifyingLocation by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            val isGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || 
                            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            
            if (isGranted) {
                // 1. Show the dark "Verifying..." overlay
                isVerifyingLocation = true 
                
                // 2. Tell the ViewModel to start the background GPS/API work
                viewModel.verifyLocationAndClaim(workerId)
                
                // 3. Start a totally independent UI timer for the Demo
                coroutineScope.launch {
                    delay(11000) // Wait 11 seconds for the "Fraud Scan" to finish
                    isVerifyingLocation = false // Hide the overlay
                    onTriggerAlert() // 🚨 FORCE THE MODAL TO OPEN!
                }
            } else {
                Toast.makeText(context, "Location required for fraud prevention", Toast.LENGTH_LONG).show()
            }
        }
    )
    Scaffold(
        bottomBar = { ShiftSafeBottomNav(navController = navController) },
        containerColor = PageBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // ── 1. Purple weather banner ──────────────────────────────────
            WeatherBanner(
                temp = viewModel.temperature,
                condition = viewModel.weatherCondition
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ── 2. Live Risk Metrics ──────────────────────────────────────
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

            // ── 3. Live GPS Tracking (Interactive Map) ────────────────────
            // You can also trigger the "Claim" from inside here if needed
            GpsTrackingSection(
                onReportClick = {
                    // This manually triggers the alert/claim flow
                    onTriggerAlert() 
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
            HazardReportCard(
                onReportClick = {
                    // This launches the permission check we set up
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION, 
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
            // ── 4. Active Policies ────────────────────────────────────────
            ActivePoliciesSection(onPolicyClick = onPolicyClick)

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}// ── Weather banner ─────────────────────────────────────────────────────────
@Composable
fun WeatherBanner(temp: String, condition: String) {
val currentTime = remember { 
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()) 
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(BannerStart, BannerEnd)
                ),
                shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
            )
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
    ) {
        // Scattered star dots (static decorative layer)
        StarField()

        // Right side: profile avatar circle
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFF3A6B6B))
        )

        // Left: text content
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 20.dp, top = 12.dp)
        ) {
            Text(
                text       = "Welcome back,",
                color      = Color.White.copy(alpha = 0.85f),
                fontSize   = 13.sp,
                fontWeight = FontWeight.Normal
            )
            Text(
                text       = "Rahul Kumar",          // keep static — wire to state if needed
                color      = Color.White,
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector        = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint               = Color.White.copy(alpha = 0.8f),
                    modifier           = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text     = currentTime,
                    color    = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
           Text(
                text       = "$temp°C", // LIVE DATA!
                color      = Color.White,
                fontSize   = 52.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 56.sp
            )
            Text(
                text       = condition, // LIVE DATA!
                color      = Color.White.copy(alpha = 0.9f),
                fontSize   = 16.sp,
                fontWeight = FontWeight.Medium
            )       
      }

        // Right side lower: cloudy moon illustration placeholder + coverage pill
        Column(
            modifier           = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp, top = 40.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Moon + cloud shape (simplified with overlapping circles)
            MoonCloudIllustration()

            Spacer(modifier = Modifier.height(10.dp))

            // "Coverage • Active" pill
            CoveragePill()
        }
    }
}

// ── Decorative star field ──────────────────────────────────────────────────
@Composable
fun StarField() {
    // Static star positions approximated from the screenshot
    Box(modifier = Modifier.fillMaxSize()) {
        val dots = listOf(
            0.15f to 0.08f, 0.7f to 0.05f, 0.85f to 0.15f,
            0.05f to 0.5f,  0.6f to 0.35f, 0.9f to 0.55f,
            0.4f to 0.72f,  0.12f to 0.82f,0.75f to 0.8f,
            0.52f to 0.9f,  0.30f to 0.20f,0.95f to 0.38f
        )
        dots.forEach { (xFrac, yFrac) ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.TopStart)
            ) {
                Box(
                    modifier = Modifier
                        .offset(
                            x = (xFrac * 300).dp,
                            y = (yFrac * 240).dp
                        )
                        .size(3.dp)
                        .clip(CircleShape)
                        .background(StarDot)
                )
            }
        }
    }
}

// ── Moon + cloud illustration ──────────────────────────────────────────────
@Composable
fun MoonCloudIllustration() {
    Box(
        modifier = Modifier
            .size(width = 100.dp, height = 70.dp)
    ) {
        // Moon circle (large white glowing)
        Box(
            modifier = Modifier
                .size(64.dp)
                .align(Alignment.TopCenter)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.88f))
        )
        // Cloud body (wide ellipse at bottom)
        Box(
            modifier = Modifier
                .width(90.dp)
                .height(32.dp)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.55f))
        )
    }
}

// ── Coverage active pill ───────────────────────────────────────────────────
@Composable
fun CoveragePill() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(NeonGreen)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                text       = "Coverage",
                color      = Color.White.copy(alpha = 0.8f),
                fontSize   = 10.sp,
                fontWeight = FontWeight.Normal
            )
            Text(
                text       = "Active",
                color      = NeonGreen,
                fontSize   = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ── 2×2 Risk Metrics Grid ─────────────────────────────────────────────────
@Composable
fun RiskMetricsGrid(temp: String, rainProb: String, humidity: String) {
    // Safely convert the live string temp to an Int for our logic
    val tempInt = temp.toIntOrNull() ?: 24 
    
    val wetBulbRisk = if (tempInt > 30) "HIGH" else "MODERATE"
    val wetBulbColor = if (tempInt > 30) TagPoor else TagModerate
    
    // Mock AQI for the demo (since OpenWeather free doesn't include it)
    val mockAqi = 85
    val aqiRisk  = if (mockAqi > 300) "POOR" else if (mockAqi > 150) "MODERATE" else "GOOD"
    val aqiColor = if (mockAqi > 300) TagPoor else if (mockAqi > 150) TagModerate else TagLow

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Wet Bulb Temp card
            RiskCard(
                modifier  = Modifier.weight(1f),
                iconVector = Icons.Default.Thermostat,
                iconTint  = Color(0xFFFF7043),
                tag       = "LIVE",
                tagColor  = TagModerate,
                value     = "$temp°C", // LIVE DATA!
                label     = "Est. Wet Bulb Temp"
            )
            // Rain Probability card
            RiskCard(
                modifier  = Modifier.weight(1f),
                iconVector = Icons.Default.WaterDrop,
                iconTint  = Color(0xFF5C9EE8),
                tag       = "UPDATED",
                tagColor  = TagLow,
                value     = rainProb, // LIVE DATA!
                label     = "Rain Probability"
            )       
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Air Quality card
            RiskCard(
                modifier  = Modifier.weight(1f),
                iconVector = Icons.Default.Air,
                iconTint  = Color(0xFF78909C),
                tag       = aqiRisk,
                tagColor  = aqiColor,
                value     = "$mockAqi", // Mocked for demo
                label     = "Air Quality Index"
            )
            // Primary Hub card
            RiskCard(
                modifier  = Modifier.weight(1f),
                iconVector = Icons.Default.Store,
                iconTint  = Color(0xFF26A69A),
                tag       = "ONLINE", // Mocked for demo
                tagColor  = TagOpen,
                value     = "Velachery", // Hardcoded to match our static map
                label     = "Primary Hub"
            )
        }
    }
}

// ── Single risk metric card ────────────────────────────────────────────────
@Composable
fun RiskCard(
    modifier   : Modifier,
    iconVector : ImageVector,
    iconTint   : Color,
    tag        : String,
    tagColor   : Color,
    value      : String,
    label      : String
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier            = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment   = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector        = iconVector,
                    contentDescription = null,
                    tint               = iconTint,
                    modifier           = Modifier.size(22.dp)
                )
                // Tag badge
                Text(
                    text       = tag,
                    color      = tagColor,
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(tagColor.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text       = value,
                color      = TextDark,
                fontSize   = 22.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text     = label,
                color    = TextGray,
                fontSize = 12.sp
            )
        }
    }
}

// ── GPS Tracking section ───────────────────────────────────────────────────
@Composable
fun GpsTrackingSection(onReportClick: () -> Unit = {}){
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // Header row
        Row(
            modifier            = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment   = Alignment.CenterVertically
        ) {
            Text(
                text       = "Live GPS Tracking",
                color      = TextDark,
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold
            )
            // Active badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(NeonGreen.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector        = Icons.Default.MyLocation,
                    contentDescription = null,
                    tint               = NeonGreen,
                    modifier           = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text       = "Active",
                    color      = NeonGreen,
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Destination card
        Card(
            shape     = RoundedCornerShape(16.dp),
            colors    = CardDefaults.cardColors(containerColor = CardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier  = Modifier.fillMaxWidth().height(220.dp) // Taller to show the map
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                
                // 1. THE FREE, INTERACTIVE OPENSTREETMAP
                AndroidView(
                    factory = { context ->
                        android.webkit.WebView(context).apply {
                            // FIX 1: FORCE THE SIZE SO IT ISN'T INVISIBLE
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                // FIX 2: Allow it to fetch HTTP tiles on an HTTPS fake domain
                                mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            }
                            
                            webViewClient = android.webkit.WebViewClient()
                            
                            // FIX 3: Route website errors to Logcat so we can see them!
                            webChromeClient = object : android.webkit.WebChromeClient() {
                                override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                                    android.util.Log.e("MapError", "MAP JS: ${consoleMessage?.message()}")
                                    return true
                                }
                            }
                            
                            val htmlData = """
                                <!DOCTYPE html>
                                <html>
                                <head>
                                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
                                    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                                    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                                    <style>
                                        body { padding: 0; margin: 0; background-color: #f3f4f8; }
                                        html, body, #map { height: 100%; width: 100%; }
                                        .leaflet-control-attribution { display: none; }
                                    </style>
                                </head>
                                <body>
                                    <div id="map"></div>
                                    <script>
                                        var map = L.map('map', {zoomControl: false}).setView([12.9815, 80.2230], 14);
                                        
                                        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                                            maxZoom: 19
                                        }).addTo(map);
                                        
                                        var hazardZone = L.circle([12.9815, 80.2230], {
                                            color: '#E65100',
                                            fillColor: '#FF9800',
                                            fillOpacity: 0.4,
                                            radius: 800
                                        }).addTo(map);
                                    </script>
                                </body>
                                </html>
                            """.trimIndent()
                            
                            // Load using the fake secure origin
                            loadDataWithBaseURL("https://app.local/", htmlData, "text/html", "UTF-8", null)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )                // 2. MOCK "TRAFFIC CLOSURE" OVERLAY (Hackathon magic!)
                Surface(
                    color = Color(0xFFFFF3E0), // Light orange warning background
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(12.dp).align(Alignment.TopStart),
                    border = BorderStroke(1.dp, Color(0xFFFFB74D))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFE65100))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "100ft Road Closed (Waterlogging)", 
                            color = Color(0xFFE65100), 
                            fontSize = 10.sp, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 3. YOUR BOTTOM OVERLAY ("CURRENT ZONE")
                Surface(
                    color = Color.White.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = GpsIcon)
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
// ── Bottom Navigation ──────────────────────────────────────────────────────
@Composable
fun WinkitBottomNav() {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = true,
            onClick  = {},
            icon     = {
                Icon(Icons.Default.Home, contentDescription = "Home")
            },
            label    = { Text("HOME", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            colors   = NavigationBarItemDefaults.colors(
                selectedIconColor   = NavSelected,
                selectedTextColor   = NavSelected,
                unselectedIconColor = NavUnselected,
                unselectedTextColor = NavUnselected,
                indicatorColor      = Color.Transparent
            )
        )
        NavigationBarItem(
            selected = false,
            onClick  = {},
            icon     = {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Wallet")
            },
            label    = { Text("WALLET", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            colors   = NavigationBarItemDefaults.colors(
                selectedIconColor   = NavSelected,
                selectedTextColor   = NavSelected,
                unselectedIconColor = NavUnselected,
                unselectedTextColor = NavUnselected,
                indicatorColor      = Color.Transparent
            )
        )
        NavigationBarItem(
            selected = false,
            onClick  = {},
            icon     = {
                Icon(Icons.Default.Person, contentDescription = "Profile")
            },
            label    = { Text("PROFILE", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            colors   = NavigationBarItemDefaults.colors(
                selectedIconColor   = NavSelected,
                selectedTextColor   = NavSelected,
                unselectedIconColor = NavUnselected,
                unselectedTextColor = NavUnselected,
                indicatorColor      = Color.Transparent
            )
        )
    }
}

// ── AnimatedWeatherBanner kept for backward-compat (unused in new flow) ────
@Composable
fun AnimatedWeatherBanner(type: EnvironmentType) {
    // Kept to avoid breaking any other call sites.
    // The new WeatherBanner(state) is used inside ShiftSafeDashboard above.
}
// ── Active Policies Section ────────────────────────────────────────────────
@Composable
fun ActivePoliciesSection(onPolicyClick: () -> Unit){
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Active Policies",
                color = Color(0xFF1A1A2E), // TextDark
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "View All",
                color = Color(0xFF5B2D8E), // NavSelected (Purple)
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Policy Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE0E6ED)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onPolicyClick() } // <-- ADD CLICK LISTENER HERE
        ){
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Icon Circle
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF3F4F8)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Policy",
                            tint = Color(0xFF5A7184),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // Text
                    Column {
                        Text(
                            text = "Gig Protect Weekly",
                            color = Color(0xFF1A1A2E),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Auto-renews on 22 Mar",
                            color = Color(0xFF8E8E9A),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Chevron Arrow
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Go",
                    tint = Color(0xFF8E8E9A)
                )
            }
        }
    }
}

@Composable
fun HazardReportCard(onReportClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFFFF1F0), // Light Red/Alert Background
        border = BorderStroke(1.dp, Color(0xFFFFCCC7))
    ) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Warning, 
                contentDescription = null, 
                tint = Color(0xFFF5222D),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Hazardous Conditions?",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF820014)
            )
            Text(
                "Report flood or extreme weather to secure your payout.",
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                color = Color(0xFFCF1322),
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // --- THE BUTTON YOU ASKED FOR ---
            Button(
                onClick = onReportClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF5222D)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Report Hazard & Claim Payout", fontWeight = FontWeight.Bold)
            }
        }
    }
}
