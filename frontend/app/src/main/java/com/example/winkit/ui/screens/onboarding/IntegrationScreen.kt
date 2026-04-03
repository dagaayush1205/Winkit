package com.example.winkit.ui.screens.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.winkit.utils.tr

@Composable
fun IntegrationScreen(onBack: () -> Unit, onNext: (String) -> Unit) {
    var selectedPlatform by remember { mutableStateOf("") }
    var partnerId by remember { mutableStateOf("") }
    
    // Verification States
    var isVerifying by remember { mutableStateOf(false) }
    var isVerified by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
            .systemBarsPadding()
    ) {
        // Top Bar & Progress
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Spacer(modifier = Modifier.width(16.dp))
            LinearProgressIndicator(
                progress = { 0.25f },
                modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF006C7A),
                trackColor = Color(0xFFE0E0E0)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text("25%", color = Color(0xFF006C7A), fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 🔥 FIXED: Corrected string concatenation and tr() usage
        Text(
            text = tr("Link Your") + "\n" + tr("Delivery Profile"), 
            fontSize = 32.sp, 
            fontWeight = FontWeight.ExtraBold, 
            color = Color(0xFF0A192F), 
            lineHeight = 40.sp
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        Text(tr("Select your primary platform to sync your active shifts and verify your identity."), color = Color.Gray, fontSize = 14.sp)

        Spacer(modifier = Modifier.height(32.dp))

        // Platform Grid
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PlatformCard(
                name = "Blinkit", code = "BL", brandColor = Color(0xFFF8CB46),
                isSelected = selectedPlatform == "Blinkit",
                modifier = Modifier.weight(1f)
            ) { 
                selectedPlatform = "Blinkit"
                isVerified = false; partnerId = "" 
            }
            
            PlatformCard(
                name = "Zepto", code = "ZE", brandColor = Color(0xFFFF3269),
                isSelected = selectedPlatform == "Zepto",
                modifier = Modifier.weight(1f)
            ) { 
                selectedPlatform = "Zepto"
                isVerified = false; partnerId = ""
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ID Input & Verification
        if (selectedPlatform.isNotEmpty()) {
            Text("${selectedPlatform.uppercase()} PARTNER ID", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = partnerId,
                onValueChange = { partnerId = it; isVerified = false },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("e.g. ${if (selectedPlatform == "Zepto") "ZEP" else "BKT"}-8849") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF006C7A),
                    unfocusedBorderColor = if (isVerified) Color(0xFF006C7A) else Color.LightGray
                ),
                trailingIcon = {
                    if (isVerified) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Verified", tint = Color(0xFF006C7A))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(tr("Verified"), color = Color(0xFF006C7A), fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = {
                                if (partnerId.startsWith(if (selectedPlatform == "Zepto") "ZEP" else "BKT", ignoreCase = true)) {
                                    coroutineScope.launch {
                                        isVerifying = true
                                        delay(1500) // Fake API Call
                                        isVerifying = false
                                        isVerified = true
                                    }
                                }
                            },
                            enabled = partnerId.length > 4 && !isVerifying,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006C7A)),
                            modifier = Modifier.padding(end = 8.dp).height(36.dp)
                        ) {
                            if (isVerifying) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            else Text(tr("Verify"))
                        }
                    }
                }
            )

            if (isVerified) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = Color(0xFFE0F2F1),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF006C7A), modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        // 🔥 FIXED: Variable extraction for translation
                        Text(
                            text = tr("ID successfully verified with") + " $selectedPlatform " + tr("backend. Your active shifts will be synced automatically."), 
                            color = Color(0xFF004D40), 
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { onNext(partnerId) },
            enabled = isVerified,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0A192F), 
                disabledContainerColor = Color.LightGray
            )
        ) {
            Text(tr("Continue"), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PlatformCard(name: String, code: String, brandColor: Color, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable { onClick() }.height(120.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, if (isSelected) brandColor else Color(0xFFF0F0F0)),
        color = Color.White,
        shadowElevation = if (isSelected) 8.dp else 0.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle, 
                    contentDescription = null, 
                    tint = brandColor,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(20.dp)
                )
            }
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.size(48.dp).background(if (isSelected) brandColor else Color(0xFFF0F0F0), CircleShape), contentAlignment = Alignment.Center) {
                    Text(code, color = if (isSelected) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(name, fontWeight = FontWeight.Bold, color = if (isSelected) brandColor else Color.DarkGray)
            }
        }
    }
}
