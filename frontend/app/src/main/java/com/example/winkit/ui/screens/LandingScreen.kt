package com.example.winkit.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.winkit.R // Ensure you have a logo in res/drawable!
import com.example.winkit.utils.Translator
import com.example.winkit.utils.tr

@Composable
fun LandingScreen(navController: NavController) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F4F8))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Logo or Title
        Text(
            text = "WinkIT",
            fontSize = 40.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF5B2D8E)
        )
        Text(
            text = "Parametric Insurance for Everyone",
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(60.dp))

        Text(
            text = "Choose your language",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A2E)
        )
        Spacer(modifier = Modifier.height(24.dp))

        // Language Buttons Grid
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LangButton("English", "en", Modifier.weight(1f))
                LangButton("हिंदी", "hi", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LangButton("ಕನ್ನಡ", "kn", Modifier.weight(1f))
                LangButton("தமிழ்", "ta", Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(60.dp))

        // Continue Button
        Button(
            onClick = { navController.navigate("login") }, // Routes to your login/signup
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5A0))
        ) {
            Text(tr("Continue"), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
fun LangButton(label: String, langCode: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val isSelected = Translator.currentLang == langCode

    Button(
        onClick = { Translator.setLanguage(context, langCode) },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF5B2D8E) else Color.White,
            contentColor = if (isSelected) Color.White else Color.Black
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(if (isSelected) 4.dp else 0.dp),
        modifier = modifier.height(56.dp)
    ) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
