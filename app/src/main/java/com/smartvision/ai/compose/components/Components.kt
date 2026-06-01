package com.smartvision.ai.compose.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.smartvision.ai.ui.theme.*

// ─── GradientBackground ───────────────────────────────────────────────────────
@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val ext = MaterialTheme.extended
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = if (ext.isDark) {
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF0C1840),
                            DeepNavy,
                            Color(0xFF020510)
                        ),
                        center = Offset.Unspecified,
                        radius = 1600f
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFFFFF),
                            Color(0xFFF3F7FF)
                        )
                    )
                }
            ),
        content = content
    )
}

// ─── GlassCard ────────────────────────────────────────────────────────────────
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    glowColor: Color? = null,
    cornerRadius: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val ext = MaterialTheme.extended
    val shape = RoundedCornerShape(cornerRadius)
    val borderBrush = if (glowColor != null) {
        Brush.linearGradient(
            colors = listOf(
                glowColor.copy(alpha = 0.7f),
                ext.glassBorder,
                glowColor.copy(alpha = 0.25f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(ext.glassBorder, ext.glassBorder.copy(alpha = 0.3f))
        )
    }

    val cardBg = if (ext.isDark) ext.cardBackground else ext.glassCard

    val baseModifier = modifier
        .clip(shape)
        .background(cardBg)
        .border(width = 1.dp, brush = borderBrush, shape = shape)

    if (onClick != null) {
        Column(
            modifier = baseModifier.clickable(onClick = onClick).padding(16.dp),
            content = content
        )
    } else {
        Column(
            modifier = baseModifier.padding(16.dp),
            content = content
        )
    }
}

// ─── NeonButton ───────────────────────────────────────────────────────────────
@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null
) {
    val ext = MaterialTheme.extended
    val gradient = Brush.horizontalGradient(
        colors = listOf(ext.neonBlue, ext.neonPurple)
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(
                if (enabled) gradient
                else Brush.horizontalGradient(listOf(Color.Gray, Color.DarkGray))
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (icon != null) {
                icon()
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

// ─── OutlineNeonButton ────────────────────────────────────────────────────────
@Composable
fun OutlineNeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null
) {
    val ext = MaterialTheme.extended
    val borderBrush = Brush.horizontalGradient(
        listOf(ext.neonBlue, ext.neonPurple)
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .border(1.5.dp, borderBrush, RoundedCornerShape(26.dp))
            .background(ext.glassCard)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (icon != null) {
                icon()
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = ext.neonBlue
            )
        }
    }
}

// ─── SmartTextField ───────────────────────────────────────────────────────────
@Composable
fun SmartTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    maxLines: Int = 1
) {
    val ext = MaterialTheme.extended
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = {
            Text(placeholder, color = ext.textHint, style = MaterialTheme.typography.bodyMedium)
        },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        singleLine = singleLine,
        maxLines = maxLines,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor        = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor      = MaterialTheme.colorScheme.onSurface,
            focusedBorderColor      = ext.neonBlue,
            unfocusedBorderColor    = ext.glassBorder,
            cursorColor             = ext.neonBlue,
            focusedContainerColor   = ext.glassCard,
            unfocusedContainerColor = ext.glassCard
        ),
        textStyle = MaterialTheme.typography.bodyMedium
    )
}

// ─── ModuleCard (Dashboard Grid Item) — animated neon glow border ─────────────
@Composable
fun ModuleCard(
    title: String,
    icon: @Composable () -> Unit,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = MaterialTheme.extended
    val shape = RoundedCornerShape(18.dp)

    // Pulsing glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "moduleGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue  = 0.65f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    val scaleAnim by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 1.03f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cardScale"
    )

    val cardBg = if (ext.isDark) ext.cardBackground else ext.glassCard

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer { scaleX = scaleAnim; scaleY = scaleAnim }
            .clip(shape)
            .background(cardBg)
            .border(
                1.5.dp,
                Brush.linearGradient(
                    colors = listOf(
                        accentColor.copy(alpha = glowAlpha),
                        ext.glassBorder.copy(alpha = 0.3f),
                        accentColor.copy(alpha = glowAlpha * 0.5f)
                    )
                ),
                shape
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon circle with radial glow background
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.25f),
                                accentColor.copy(alpha = 0.06f)
                            )
                        )
                    )
                    .border(1.dp, accentColor.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text   = title,
                style  = MaterialTheme.typography.labelMedium,
                color  = MaterialTheme.colorScheme.onSurface,
                textAlign    = TextAlign.Center,
                maxLines     = 2,
                overflow     = TextOverflow.Ellipsis,
                fontWeight   = FontWeight.Medium
            )
        }
    }
}

// ─── FloatingAiOrb — animated AI assistant button ────────────────────────────
@Composable
fun FloatingAiOrb(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "aiOrb")

    val ring1Scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 1.25f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring1"
    )
    val ring2Scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 1.45f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring2"
    )
    val rotDeg by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 360f,
        animationSpec = infiniteRepeatable(
            animation  = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = modifier.size(68.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing ring 2
        Box(
            modifier = Modifier
                .size(68.dp)
                .graphicsLayer { scaleX = ring2Scale; scaleY = ring2Scale; alpha = 0.15f }
                .clip(CircleShape)
                .background(NeonPurple)
        )
        // Outer pulsing ring 1
        Box(
            modifier = Modifier
                .size(68.dp)
                .graphicsLayer { scaleX = ring1Scale; scaleY = ring1Scale; alpha = 0.25f }
                .clip(CircleShape)
                .background(NeonBlue)
        )
        // Rotating sweep gradient ring
        Box(
            modifier = Modifier
                .size(62.dp)
                .graphicsLayer { rotationZ = rotDeg }
                .clip(CircleShape)
                .background(
                    Brush.sweepGradient(
                        listOf(NeonBlue.copy(0.9f), NeonPurple.copy(0.9f), NeonCyan.copy(0.7f), NeonBlue.copy(0.9f))
                    )
                )
        )
        // Core button
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(NeonPurple, Color(0xFF3B0D8A))
                    )
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text("🤖", fontSize = 26.sp)
        }
    }
}

// ─── SectionHeader ────────────────────────────────────────────────────────────
@Composable
fun SectionHeader(
    title: String,
    actionLabel: String = "See All",
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val ext = MaterialTheme.extended
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text  = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold
        )
        if (onActionClick != null) {
            Text(
                text     = actionLabel,
                style    = MaterialTheme.typography.labelMedium,
                color    = ext.neonBlue,
                modifier = Modifier.clickable(onClick = onActionClick)
            )
        }
    }
}

// ─── HistoryItemRow ──────────────────────────────────────────────────────────
@Composable
fun HistoryItemRow(
    type: String,
    title: String,
    subtitle: String,
    accentColor: Color,
    timestamp: String,
    onClick: (() -> Unit)? = null
) {
    val ext = MaterialTheme.extended
    val cardBg = if (ext.isDark) ext.cardBackground else ext.glassCard
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(1.dp, ext.glassBorder, RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Type badge
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accentColor.copy(alpha = 0.15f))
                .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = type.take(2).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = title,
                style    = MaterialTheme.typography.titleSmall,
                color    = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text     = subtitle,
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text  = timestamp,
                style = MaterialTheme.typography.labelSmall,
                color = ext.textHint
            )
            Text(
                text  = "›",
                fontSize = 16.sp,
                color = ext.textHint
            )
        }
    }
}

// ─── PulsingOrb (used in splash / decorative) ────────────────────────────────
@Composable
fun PulsingOrb(
    color: Color,
    size: Dp = 100.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue  = 1.15f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbScale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbAlpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size)
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha * 0.35f }
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f))
        )
        Box(
            modifier = Modifier
                .size(size * 0.75f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(color.copy(0.5f), color.copy(0.1f))
                    )
                )
        )
        Box(
            modifier = Modifier
                .size(size * 0.5f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.White.copy(0.9f), color)
                    )
                )
        )
    }
}

// ─── SmartTopBar ──────────────────────────────────────────────────────────────
@Composable
fun SmartTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val ext = MaterialTheme.extended
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ext.glassCard)
                    .border(1.dp, ext.glassBorder, RoundedCornerShape(12.dp))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Text("←", color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp)
            }
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text     = title,
            style    = MaterialTheme.typography.titleLarge,
            color    = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        Row(content = actions)
    }
}

// ─── SmartSearchBar ───────────────────────────────────────────────────────────
@Composable
fun SmartSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Search or ask anything...",
    modifier: Modifier = Modifier,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val ext = MaterialTheme.extended
    val cardBg = if (ext.isDark) ext.cardBackground else ext.glassCard
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(cardBg)
            .border(1.dp, ext.glassBorder, RoundedCornerShape(28.dp))
            .padding(horizontal = 16.dp, vertical = 0.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicTextField(
            value         = query,
            onValueChange = onQueryChange,
            modifier      = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            singleLine = true,
            decorationBox = { innerTextField ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "🔍",
                        modifier = Modifier.padding(end = 10.dp),
                        fontSize = 16.sp
                    )
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                placeholder,
                                color = ext.textHint,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        innerTextField()
                    }
                    trailingIcon?.invoke()
                }
            }
        )
    }
}

// ─── NeonBadge ────────────────────────────────────────────────────────────────
@Composable
fun NeonBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = text,
            style      = MaterialTheme.typography.labelSmall,
            color      = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ─── SmartBottomNavBar ────────────────────────────────────────────────────────
@Composable
fun SmartBottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    items: List<com.smartvision.ai.compose.navigation.BottomNavItem>
) {
    val ext = MaterialTheme.extended
    val cardBg = if (ext.isDark) {
        Brush.horizontalGradient(listOf(Color(0xFF0D1530), Color(0xFF08101F)))
    } else {
        Brush.horizontalGradient(listOf(Color.White, Color(0xFFF3F7FF)))
    }

    // Animated indicator
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(cardBg)
            .border(1.dp, ext.glassBorder, RoundedCornerShape(28.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route
                val itemAlpha by animateFloatAsState(
                    targetValue   = if (isSelected) 1f else 0.55f,
                    animationSpec = tween(250),
                    label         = "navAlpha_${item.label}"
                )

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onNavigate(item.route) }
                        .padding(horizontal = 18.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Neon indicator bar above icon when selected
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .height(3.dp)
                                .width(28.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(ext.neonBlue, ext.neonPurple)
                                    )
                                )
                        )
                        Spacer(Modifier.height(3.dp))
                    } else {
                        Spacer(Modifier.height(6.dp))
                    }

                    Text(
                        text     = item.icon,
                        fontSize = 22.sp,
                        modifier = Modifier.graphicsLayer(alpha = itemAlpha)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text       = item.label,
                        style      = MaterialTheme.typography.labelSmall,
                        color      = if (isSelected) ext.neonBlue
                                     else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier   = Modifier.graphicsLayer(alpha = itemAlpha)
                    )
                }
            }
        }
    }
}

// ─── SemanticMeaningCard ──────────────────────────────────────────────────────
@Composable
fun SemanticMeaningCard(
    meaning: com.smartvision.ai.translator.domain.SemanticMeaning,
    sourceLangName: String,
    targetLangName: String,
    modifier: Modifier = Modifier
) {
    val ext = MaterialTheme.extended
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(ext.glassCard)
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(listOf(NeonBlue.copy(0.4f), NeonPurple.copy(0.3f))),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🤖", fontSize = 16.sp)
                    Text(
                        if (meaning.isOfflineMode) "Offline AI Semantic" else "AI Semantic Breakdown",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (meaning.isOfflineMode) Color(0xFF4CAF50) else NeonBlue,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (meaning.isOfflineMode) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1B5E20).copy(0.15f))
                            .border(1.dp, Color(0xFF4CAF50).copy(0.35f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val alpha by infiniteTransition.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulseAlpha"
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50).copy(alpha = alpha))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Offline Mode Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF81C784),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 9.sp
                        )
                    }
                }
            }
            HorizontalDivider(color = if (meaning.isOfflineMode) Color(0xFF4CAF50).copy(0.2f) else NeonBlue.copy(0.15f))

            // Meaning in English
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🇺🇸", fontSize = 12.sp)
                    Text("Meaning in English:", style = MaterialTheme.typography.labelSmall, color = NeonBlue.copy(0.85f), fontWeight = FontWeight.Bold)
                }
                Text(
                    text = meaning.meaningEnglish,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )
            }

            // Meaning in Original Language
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("💡", fontSize = 12.sp)
                    Text("Meaning in $sourceLangName:", style = MaterialTheme.typography.labelSmall, color = NeonPurple.copy(0.85f), fontWeight = FontWeight.Bold)
                }
                Text(
                    text = meaning.meaningOriginal,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                    lineHeight = 20.sp
                )
            }

            // Contextual explanation
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🎭", fontSize = 12.sp)
                    Text("Context & Nuance:", style = MaterialTheme.typography.labelSmall, color = NeonCyan.copy(0.85f), fontWeight = FontWeight.Bold)
                }
                Text(
                    text = meaning.contextExplanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    lineHeight = 18.sp
                )
            }

            // Usage explanation
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("📝", fontSize = 12.sp)
                    Text("Usage & Examples:", style = MaterialTheme.typography.labelSmall, color = NeonPink.copy(0.85f), fontWeight = FontWeight.Bold)
                }
                Text(
                    text = meaning.usageExplanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    lineHeight = 18.sp
                )
            }

            // Pronunciation
            meaning.pronunciation?.let { pron ->
                HorizontalDivider(color = NeonBlue.copy(0.1f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("🗣", fontSize = 12.sp)
                    Text(
                        text = "Romanized Pronunciation: ",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonBlue.copy(alpha = 0.7f)
                    )
                    Text(
                        text = pron,
                        style = MaterialTheme.typography.bodySmall,
                        color = NeonCyan,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
