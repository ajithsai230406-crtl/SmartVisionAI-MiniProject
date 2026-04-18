package com.example.smartvisionai.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartvisionai.ui.theme.*

/**
 * Generic smart card with optional top accent line, gradient background,
 * and click handler. Used throughout the app for consistent look.
 */
@Composable
fun SmartCard(
    modifier: Modifier = Modifier,
    accentColor: Color = CyanAccent,
    showAccentLine: Boolean = true,
    cornerRadius: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val baseModifier = modifier
        .clip(RoundedCornerShape(cornerRadius))
        .background(
            Brush.linearGradient(listOf(CardDark, Color(0xFF0D1520)))
        )
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(
                listOf(accentColor.copy(alpha = 0.25f), CardBorder)
            ),
            shape = RoundedCornerShape(cornerRadius)
        )
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)

    Column(modifier = baseModifier) {
        // Top accent line
        if (showAccentLine) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(accentColor.copy(0.8f), Color.Transparent)
                        )
                    )
            )
        }
        content()
    }
}

/**
 * Section header label used in detail screens.
 */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = TextMuted,
        letterSpacing = 3.sp,
        modifier = modifier.padding(start = 6.dp, bottom = 8.dp)
    )
}

/**
 * Confidence badge chip.
 */
@Composable
fun ConfidenceBadge(confidence: Int, accentColor: Color = CyanAccent) {
    if (confidence <= 0) return
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(accentColor.copy(alpha = 0.15f))
            .border(1.dp, accentColor.copy(0.4f), RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            "$confidence%",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = accentColor
        )
    }
}

/**
 * Tech pill used in ModuleDetail screen.
 */
@Composable
fun TechPill(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(label, fontSize = 12.sp, color = TextSecondary)
    }
}
