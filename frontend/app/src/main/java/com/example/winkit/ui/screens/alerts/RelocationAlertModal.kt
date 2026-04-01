package com.example.winkit.ui.screens.alerts

import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog

@Composable
fun RelocationAlertModal(onAccept: () -> Unit, onDismiss: () -> Unit) {
    // True popup overlay
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- 1. HEADER (Warning) ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFF1F0))
                        .padding(16.dp)
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("ZONE FLOODING DETECTED", color = Color(0xFFD32F2F), fontSize = 16.sp, fontWeight = FontWeight.Black)
                        Text("Deliveries paused in Velachery.", color = Color(0xFFD32F2F).copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                }

                // --- 2. THE ROUTING MAP ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(Color(0xFFF3F4F8))
                ) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                }
                                webViewClient = WebViewClient()
                                webChromeClient = WebChromeClient()

                                // Leaflet Map showing a route from Hazard to Safe Zone
                                val htmlData = """
                                    <!DOCTYPE html>
                                    <html>
                                    <head>
                                        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
                                        <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                                        <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                                        <style>
                                            body { padding: 0; margin: 0; }
                                            html, body, #map { height: 100%; width: 100%; }
                                            .leaflet-control-attribution { display: none; }
                                        </style>
                                    </head>
                                    <body>
                                        <div id="map"></div>
                                        <script>
                                            var map = L.map('map', {zoomControl: false}).setView([12.9900, 80.2400], 13);
                                            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);
                                            
                                            // Hazard Zone (Velachery)
                                            L.circle([12.9815, 80.2230], {color: '#E65100', fillColor: '#FF9800', fillOpacity: 0.4, radius: 600}).addTo(map);
                                            
                                            // Safe Zone (Adyar)
                                            var safeIcon = L.divIcon({className: 'safe-icon', html: '<div style="background-color:#00E5A0; width:16px; height:16px; border-radius:50%; border:3px solid white; box-shadow: 0 0 4px rgba(0,0,0,0.5);"></div>'});
                                            L.marker([13.0033, 80.2550], {icon: safeIcon}).addTo(map);
                                            
                                            // Route Line
                                            var route = L.polyline([[12.9815, 80.2230], [12.9900, 80.2350], [13.0033, 80.2550]], {color: '#5B2D8E', weight: 4, dashArray: '5, 10'}).addTo(map);
                                            map.fitBounds(route.getBounds(), {padding: [20, 20]});
                                        </script>
                                    </body>
                                    </html>
                                """.trimIndent()
                                loadDataWithBaseURL("https://app.local/", htmlData, "text/html", "UTF-8", null)
                            }
                        }
                    )
                }

                // --- 3. PAYOUT & RELOCATION DETAILS ---
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("RELOCATION OFFER", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFE8F5E9)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.LocalOffer, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("₹150 Surge Bonus", color = Color(0xFF1A1A2E), fontWeight = FontWeight.Black, fontSize = 16.sp)
                            Text("Guaranteed minimum 5 orders", color = Color.Gray, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFF3F4F8)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF5B2D8E), modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Relocate to Adyar Hub", color = Color(0xFF1A1A2E), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("3.2 km away • ~10 mins", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }

                // --- 4. BUTTONS ---
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Button(
                        onClick = onAccept,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5B2D8E)), // Brand Purple
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text("Accept Route & Bonus", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Stay offline in current zone", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
