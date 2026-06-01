package com.smartvision.ai.compose.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.smartvision.ai.compose.components.*
import com.smartvision.ai.ui.theme.*
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val emoji: String,
    val title: String,
    val subtitle: String,
    val accentColor: Color
)

private val pages = listOf(
    OnboardingPage(
        "👁", "Smart Vision AI",
        "Scan text, objects, medicines, and QR codes using your camera with cutting-edge AI models.",
        NeonBlue
    ),
    OnboardingPage(
        "🌐", "Translate & Speak",
        "Instantly translate between 8 languages and listen to translations with natural text-to-speech.",
        NeonPurple
    ),
    OnboardingPage(
        "🤖", "AI-Powered Assistance",
        "Get step-by-step solutions, waste disposal tips, and personalized AI chat — all offline-capable.",
        NeonCyan
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val ext = MaterialTheme.extended
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    GradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Skip button
            Box(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp)) {
                Text(
                    "Skip",
                    modifier = Modifier.align(Alignment.TopEnd).clickable(onClick = onFinished),
                    color = ext.textHint,
                    style = MaterialTheme.typography.labelLarge
                )
            }

            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPageContent(pages[page])
            }

            // Page indicator dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pages.size) { index ->
                    val isActive = pagerState.currentPage == index
                    val width by animateDpAsState(
                        targetValue = if (isActive) 28.dp else 8.dp,
                        animationSpec = tween(300),
                        label = "dotWidth"
                    )
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(
                                if (isActive)
                                    Brush.horizontalGradient(listOf(ext.neonBlue, ext.neonPurple))
                                else
                                    Brush.horizontalGradient(listOf(ext.glassBorder, ext.glassBorder))
                            )
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Next / Get Started button
            NeonButton(
                text = if (pagerState.currentPage == pages.lastIndex) "Get Started 🚀" else "Next →",
                onClick = {
                    if (pagerState.currentPage == pages.lastIndex) {
                        onFinished()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            if (pagerState.currentPage < pages.lastIndex) {
                OutlineNeonButton(
                    text = "Skip to Login",
                    onClick = onFinished,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    val infiniteTransition = rememberInfiniteTransition(label = "onboarding")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pageScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        // Animated icon circle
        Box(
            modifier = Modifier
                .size(160.dp)
                .graphicsLayer(scaleX = scale, scaleY = scale),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(page.accentColor.copy(alpha = 0.1f))
            )
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(page.accentColor.copy(alpha = 0.15f))
                    .border(2.dp, page.accentColor.copy(alpha = 0.4f), CircleShape)
            )
            Text(page.emoji, fontSize = 56.sp)
        }

        Spacer(Modifier.height(40.dp))

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = page.subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
