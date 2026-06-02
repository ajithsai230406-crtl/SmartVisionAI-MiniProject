package com.smartvision.ai.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.smartvision.ai.ui.theme.extended
import com.smartvision.ai.ui.theme.LocalLargeButtons
import com.smartvision.ai.ui.theme.LocalHighContrast
import com.smartvision.ai.utils.TtsManager

@Composable
fun AccessibleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    content: @Composable RowScope.() -> Unit
) {
    val largeButtons = LocalLargeButtons.current
    val highContrast = LocalHighContrast.current
    val ext = MaterialTheme.extended

    val basePaddingValues = if (largeButtons) PaddingValues(horizontal = 24.dp, vertical = 16.dp) else PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    val minHeight = if (largeButtons) 56.dp else 48.dp

    val baseModifier = if (highContrast) {
        Modifier.background(Color.Transparent)
            .border(2.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(12.dp))
    } else {
        Modifier.background(Brush.linearGradient(listOf(ext.neonBlue, ext.neonPurple)))
            .border(1.dp, Color.White.copy(0.3f), RoundedCornerShape(12.dp))
    }

    Box(
        modifier = modifier
            .heightIn(min = minHeight)
            .clip(RoundedCornerShape(12.dp))
            .then(baseModifier)
            .clickable(
                onClick = onClick,
                onClickLabel = contentDescription
            )
            .padding(basePaddingValues)
            .semantics {
                role = Role.Button
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            content()
        }
    }
}

fun Modifier.clickableWithHaptics(
    haptic: HapticFeedback,
    hapticEnabled: Boolean,
    onClick: () -> Unit
): Modifier = this.clickable {
    if (hapticEnabled) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    onClick()
}

fun Modifier.accessibilityGestures(
    enabled: Boolean,
    textToSpeak: String,
    ttsManager: TtsManager
): Modifier {
    if (!enabled) return this
    return this.pointerInput(Unit) {
        detectTapGestures(
            onDoubleTap = {
                ttsManager.speak(textToSpeak)
            },
            onLongPress = {
                ttsManager.speak("Focused element: $textToSpeak")
            }
        )
    }
}
