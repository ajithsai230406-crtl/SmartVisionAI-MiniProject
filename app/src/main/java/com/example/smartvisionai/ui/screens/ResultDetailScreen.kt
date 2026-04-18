package com.example.smartvisionai.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartvisionai.ui.theme.*

@Composable
fun ResultDetailScreen(
    result: String,
    moduleId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    val accentColor = when (moduleId) {
        "object"    -> CyanAccent
        "ocr"       -> PurpleAccent
        "translate" -> OrangeAccent
        "student"   -> Color(0xFF2196F3)
        "medical"   -> RedAccent
        "waste"     -> GreenAccent
        else        -> CyanAccent
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Back
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .clickable { onBack() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Default.ArrowBack, "Back", tint = TextSecondary, modifier = Modifier.size(20.dp))
            Text("Back", fontSize = 14.sp, color = TextSecondary)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // Header pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = accentColor.copy(.12f),
                border = BorderStroke(1.dp, accentColor.copy(.4f)),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(
                    moduleId.replaceFirstChar { it.uppercase() },
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    fontSize = 12.sp,
                    color = accentColor,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Result text card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardDark)
                    .border(
                        1.dp,
                        Brush.linearGradient(listOf(accentColor.copy(.3f), CardBorder)),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(20.dp)
            ) {
                // Accent top line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(
                            Brush.horizontalGradient(listOf(accentColor.copy(.8f), Color.Transparent))
                        )
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    result,
                    fontSize   = 15.sp,
                    color      = TextPrimary,
                    lineHeight = 24.sp
                )
            }
        }

        // Action bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Copy
            ActionChip(
                icon    = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                label   = if (copied) "Copied!" else "Copy",
                color   = if (copied) GreenAccent else TextSecondary,
                modifier = Modifier.weight(1f),
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Smart Vision Result", result))
                    copied = true
                }
            )

            // Share
            ActionChip(
                icon    = Icons.Default.Share,
                label   = "Share",
                color   = TextSecondary,
                modifier = Modifier.weight(1f),
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, result)
                    }
                    context.startActivity(Intent.createChooser(intent, "Share result"))
                }
            )

            // Speak
            ActionChip(
                icon    = Icons.Default.VolumeUp,
                label   = "Speak",
                color   = accentColor,
                modifier = Modifier.weight(1f),
                onClick = { /* VoiceManager.speak() called here */ }
            )
        }
    }
}

@Composable
fun ActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick  = onClick,
        modifier = modifier,
        shape    = RoundedCornerShape(12.dp),
        color    = CardDark,
        border   = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Text(label, fontSize = 11.sp, color = color)
        }
    }
}
