package com.smartvision.ai

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartvision.ai.ui.theme.SmartVisionTheme
import kotlin.system.exitProcess

class CrashReportActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val errMsg = intent.getStringExtra("error_msg") ?: "Unknown Error"
        val stackTrace = intent.getStringExtra("stack_trace") ?: "No Stacktrace Available"

        setContent {
            SmartVisionTheme(
                darkTheme = true,
                fontSizeMultiplier = 1.0f,
                highContrast = false,
                colorblindMode = "NONE",
                largeButtons = false,
                voiceGuidance = false,
                aiVoiceGuidance = false
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF03001C) // Deep dark background
                ) {
                    CrashScreen(
                        errorMsg = errMsg,
                        stackTrace = stackTrace,
                        onRestart = {
                            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            }
                            if (launchIntent != null) {
                                startActivity(launchIntent)
                            }
                            exitProcess(0)
                        },
                        onCopy = {
                            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Crash Logs", "$errMsg\n\n$stackTrace")
                            clipboard.setPrimaryClip(clip)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CrashScreen(
    errorMsg: String,
    stackTrace: String,
    onRestart: () -> Unit,
    onCopy: () -> Unit
) {
    val neonPink = Color(0xFFFF2A7A)
    val neonBlue = Color(0xFF00F0FF)
    val glassCard = Color(0x1F2A2A40)
    val glassBorder = Color(0x3D00F0FF)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "SYSTEM MALFUNCTION",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = neonPink,
            letterSpacing = 2.sp
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "An unexpected exception has been intercepted. The quantum thread crashed, but all user settings and profile details remain completely safe.",
            fontSize = 13.sp,
            color = Color.LightGray,
            modifier = Modifier.padding(horizontal = 12.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Diagnostic log card
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(glassCard)
                .border(1.dp, glassBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Text(
                text = "ERROR DIAGNOSTIC",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = neonBlue,
                letterSpacing = 1.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = errorMsg,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 3
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(glassBorder.copy(0.2f))
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stackTrace,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.LightGray.copy(0.8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onCopy,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = glassCard),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, neonBlue.copy(0.5f))
            ) {
                Text("Copy Log 📋", color = neonBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            
            Button(
                onClick = onRestart,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = neonPink),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Reboot App 🔄", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}
