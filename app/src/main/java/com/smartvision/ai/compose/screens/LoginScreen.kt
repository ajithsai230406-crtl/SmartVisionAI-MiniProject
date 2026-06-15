package com.smartvision.ai.compose.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartvision.ai.compose.components.*
import com.smartvision.ai.presentation.auth.AuthViewModel
import com.smartvision.ai.presentation.auth.AuthState
import com.smartvision.ai.ui.theme.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.smartvision.ai.R
import kotlin.math.*

// ══════════════════════════════════════════════════════════════════════════════
// SMART VISION AI — PREMIUM LOGIN SCREEN
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val message by viewModel.message.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                viewModel.connectGoogle(account) { onLoginSuccess() }
            } catch (e: ApiException) {
                val msg = when (e.statusCode) {
                    10 -> "Invalid Configuration: Web Client ID or signing certificate fingerprint mismatch in Firebase console (Developer Error 10)."
                    7 -> "Network Error: Please check your internet connection and try again."
                    12500 -> "Google Sign-In failed. Please update Google Play Services and try again."
                    12501 -> "Sign-In was cancelled by the user."
                    12502 -> "Google Sign-In is already in progress."
                    else  -> "Google Sign-In Error (code ${e.statusCode}): ${e.localizedMessage ?: "Unknown error"}"
                }
                viewModel.setErrorMessage(msg)
            } catch (e: Exception) {
                viewModel.setErrorMessage("Firebase/Auth error: ${e.localizedMessage}")
            }
        }
    )

    var showEmailForm by remember { mutableStateOf(false) }
    var isSignup      by remember { mutableStateOf(false) }
    var email         by remember { mutableStateOf("") }
    var password      by remember { mutableStateOf("") }
    var name          by remember { mutableStateOf("") }
    var showPwd       by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let { snackbarHostState.showSnackbar(it) }
    }

    // ── Infinite ambient animation ─────────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "login_ambient")
    val orb1Offset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)),
        label = "orb1"
    )
    val orb2Offset by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(11000, easing = LinearEasing)),
        label = "orb2"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(12000, easing = LinearEasing)),
        label = "ring"
    )

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData   = data,
                    containerColor = NavyCard,
                    contentColor   = TextPrimary,
                    actionColor    = NeonBlue
                )
            }
        },
        containerColor = DeepNavy
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Animated ambient background ────────────────────────────────
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Orb 1 — top-left to top-right drift
                val o1x = w * 0.1f + w * 0.5f * sin(orb1Offset * 2 * PI.toFloat())
                val o1y = h * 0.2f + h * 0.1f * cos(orb1Offset * 2 * PI.toFloat())
                drawCircle(
                    brush  = Brush.radialGradient(
                        colors = listOf(NeonBlue.copy(0.18f), Color.Transparent),
                        center = Offset(o1x, o1y),
                        radius = 320f
                    ),
                    radius = 320f,
                    center = Offset(o1x, o1y)
                )

                // Orb 2 — bottom drift
                val o2x = w * 0.7f + w * 0.25f * sin(orb2Offset * 2 * PI.toFloat())
                val o2y = h * 0.75f + h * 0.08f * cos(orb2Offset * 2 * PI.toFloat())
                drawCircle(
                    brush  = Brush.radialGradient(
                        colors = listOf(NeonPurple.copy(0.20f), Color.Transparent),
                        center = Offset(o2x, o2y),
                        radius = 280f
                    ),
                    radius = 280f,
                    center = Offset(o2x, o2y)
                )

                // Subtle grid lines
                val gridAlpha = 0.03f
                val step = 60f
                var x = 0f
                while (x <= w) {
                    drawLine(NeonBlue.copy(gridAlpha), Offset(x, 0f), Offset(x, h), 0.5f)
                    x += step
                }
                var y = 0f
                while (y <= h) {
                    drawLine(NeonBlue.copy(gridAlpha), Offset(0f, y), Offset(w, y), 0.5f)
                    y += step
                }
            }

            // ── Main scrollable content ────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(56.dp))

                // ── Logo + animated rings ──────────────────────────────────
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .rotate(ringRotation),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer rotating dashed ring
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val r = size.minDimension / 2f - 4f
                        // Dashed arc segments
                        for (i in 0 until 12) {
                            val startAngle = i * 30f
                            drawArc(
                                color       = NeonBlue.copy(pulseAlpha * 0.4f),
                                startAngle  = startAngle,
                                sweepAngle  = 18f,
                                useCenter   = false,
                                style       = Stroke(width = 2.5f, cap = StrokeCap.Round),
                                topLeft     = Offset(size.width / 2f - r, size.height / 2f - r),
                                size        = Size(r * 2, r * 2)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .offset(y = (-100).dp)
                        .size(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Solid glow circle
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(NeonBlue.copy(0.35f), NeonPurple.copy(0.20f), DeepNavy)
                                )
                            )
                            .border(
                                2.dp,
                                Brush.horizontalGradient(listOf(NeonBlue, NeonPurple)),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Eye logo
                        Canvas(Modifier.size(52.dp)) {
                            val w = size.width; val h = size.height
                            val eyePath = Path().apply {
                                moveTo(0f, h / 2f)
                                quadraticTo(w / 2f, 0f, w, h / 2f)
                                quadraticTo(w / 2f, h, 0f, h / 2f)
                                close()
                            }
                            drawPath(eyePath, Color.White)
                            drawCircle(NeonBlue, w / 5.5f, Offset(w / 2f, h / 2f))
                            drawCircle(DeepNavy, w / 10f, Offset(w / 2f, h / 2f))
                            drawCircle(Color.White, w / 26f, Offset(w / 2f - w / 14f, h / 2f - h / 14f))
                        }
                    }
                }

                Spacer(Modifier.height((-52).dp))

                // ── App name ───────────────────────────────────────────────
                Text(
                    "Smart Vision AI",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        brush = Brush.horizontalGradient(listOf(NeonBlue, NeonPurple))
                    ),
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Your AI-powered visual assistant",
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(40.dp))

                // ── GOOGLE SIGN IN — Primary CTA ──────────────────────────
                GoogleSignInButton(
                    onClick = { googleSignInLauncher.launch(googleSignInClient.signInIntent) }
                )

                Spacer(Modifier.height(14.dp))

                // ── OR divider ─────────────────────────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color    = GlassBorder.copy(alpha = 0.8f)
                    )
                    Text(
                        "  OR  ",
                        color = TextHint,
                        style = MaterialTheme.typography.labelSmall
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color    = GlassBorder.copy(alpha = 0.8f)
                    )
                }

                Spacer(Modifier.height(14.dp))

                // ── Email toggle button ────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(NavyCard)
                        .border(
                            1.dp,
                            Brush.horizontalGradient(listOf(NeonBlue.copy(0.3f), NeonPurple.copy(0.2f))),
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { showEmailForm = !showEmailForm },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("✉️", fontSize = 20.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (showEmailForm) "Hide email form" else "Continue with Email",
                            color      = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            style      = MaterialTheme.typography.titleSmall
                        )
                    }
                }

                // ── Expandable email form ──────────────────────────────────
                if (showEmailForm) {
                    Spacer(Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(NavyCard)
                            .border(
                                1.dp,
                                Brush.linearGradient(listOf(NeonBlue.copy(0.2f), NeonPurple.copy(0.15f))),
                                RoundedCornerShape(20.dp)
                            )
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                listOf("Sign In", "Sign Up").forEachIndexed { idx, label ->
                                    val active = (idx == 1) == isSignup
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (active) NeonBlue.copy(0.15f) else Color.Transparent)
                                            .border(if (active) 1.dp else 0.dp, NeonBlue.copy(0.4f), RoundedCornerShape(12.dp))
                                            .clickable { isSignup = idx == 1 }
                                            .padding(horizontal = 20.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            label,
                                            color      = if (active) NeonBlue else TextSecondary,
                                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                            style      = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                    if (idx == 0) Spacer(Modifier.width(8.dp))
                                }
                            }

                            if (isSignup) {
                                SmartTextField(
                                    value = name, onValueChange = { name = it },
                                    placeholder = "Full name",
                                    leadingIcon = { Text("👤", fontSize = 18.sp, modifier = Modifier.padding(start = 8.dp)) }
                                )
                            }
                            SmartTextField(
                                value = email, onValueChange = { email = it },
                                placeholder = "Email address",
                                leadingIcon = { Text("✉️", fontSize = 18.sp, modifier = Modifier.padding(start = 8.dp)) }
                            )
                            SmartTextField(
                                value = password, onValueChange = { password = it },
                                placeholder = "Password",
                                leadingIcon = { Text("🔒", fontSize = 18.sp, modifier = Modifier.padding(start = 8.dp)) },
                                trailingIcon = {
                                    Text(
                                        if (showPwd) "🙈" else "👁",
                                        fontSize = 18.sp,
                                        modifier = Modifier.padding(end = 8.dp).clickable { showPwd = !showPwd }
                                    )
                                },
                                singleLine = true
                            )
                            if (!isSignup) {
                                Text(
                                    "Forgot Password?",
                                    color = NeonBlue,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .align(Alignment.End)
                                        .clickable { viewModel.sendPasswordResetEmail(email) { } }
                                )
                            }
                            NeonButton(
                                text = if (isSignup) "Create Account" else "Sign In",
                                onClick = {
                                    if (isSignup) viewModel.signup(name, email, password) { onLoginSuccess() }
                                    else viewModel.login(email, password) { onLoginSuccess() }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Guest button ───────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, GlassBorder.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .clickable { viewModel.loginAsGuest { onLoginSuccess() } },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("👤", fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Continue as Guest",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ── Terms row ──────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text("By continuing, you agree to our ", style = MaterialTheme.typography.bodySmall, color = TextHint)
                    Text(
                        "Terms",
                        style      = MaterialTheme.typography.bodySmall,
                        color      = NeonBlue,
                        fontWeight = FontWeight.Bold,
                        modifier   = Modifier.clickable { onNavigate(com.smartvision.ai.compose.navigation.Routes.TERMS_CONDITIONS) }
                    )
                    Text(" & ", style = MaterialTheme.typography.bodySmall, color = TextHint)
                    Text(
                        "Privacy",
                        style      = MaterialTheme.typography.bodySmall,
                        color      = NeonBlue,
                        fontWeight = FontWeight.Bold,
                        modifier   = Modifier.clickable { onNavigate(com.smartvision.ai.compose.navigation.Routes.PRIVACY_POLICY) }
                    )
                }

                Spacer(Modifier.height(40.dp))
            }

            // ── Auth Loading and Status Overlay ─────────────────────────────
            if (authState !is AuthState.Idle) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(OverlayDark)
                        .clickable(enabled = false) {} // block click propagation
                        .wrapContentSize(Alignment.Center)
                ) {
                    Box(
                        modifier = Modifier
                            .width(280.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(GlassCard)
                            .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            when (val state = authState) {
                                is AuthState.Loading -> {
                                    CircularProgressIndicator(
                                        color = NeonBlue,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Text(
                                        text = state.message,
                                        color = TextPrimary,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                is AuthState.Success -> {
                                    Text("✔️", fontSize = 36.sp)
                                    Text(
                                        text = "Success",
                                        color = NeonGreen,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                is AuthState.Error -> {
                                    Text("❌", fontSize = 36.sp)
                                    Text(
                                        text = "Failed",
                                        color = NeonPink,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = state.message,
                                        color = TextSecondary,
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center,
                                        maxLines = 5
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    NeonButton(
                                        text = "Dismiss",
                                        onClick = { viewModel.clearAuthState() },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Premium Google Sign-In Button ────────────────────────────────────────────
@Composable
private fun GoogleSignInButton(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "google_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
    ) {
        // Glow shadow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF4285F4).copy(glowAlpha * 0.25f),
                            Color(0xFF34A853).copy(glowAlpha * 0.15f)
                        )
                    )
                )
                .blur(12.dp)
        )
        // Main button
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White)
                .border(
                    2.dp,
                    Brush.horizontalGradient(
                        listOf(Color(0xFF4285F4).copy(0.4f), Color(0xFF34A853).copy(0.3f))
                    ),
                    RoundedCornerShape(18.dp)
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Google G logo (4-color)
                Canvas(Modifier.size(26.dp)) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val r  = size.minDimension / 2f
                    // Blue top-right arc
                    drawArc(Color(0xFF4285F4), -90f, 180f, false, style = Stroke(r * 0.4f), topLeft = Offset(cx - r, cy - r), size = Size(r * 2, r * 2))
                    // Red top-left arc
                    drawArc(Color(0xFFEA4335), -270f, 90f, false, style = Stroke(r * 0.4f), topLeft = Offset(cx - r, cy - r), size = Size(r * 2, r * 2))
                    // Yellow bottom-left arc
                    drawArc(Color(0xFFFBBC05), 180f, 90f, false, style = Stroke(r * 0.4f), topLeft = Offset(cx - r, cy - r), size = Size(r * 2, r * 2))
                    // Green bottom-right arc
                    drawArc(Color(0xFF34A853), 90f, 90f, false, style = Stroke(r * 0.4f), topLeft = Offset(cx - r, cy - r), size = Size(r * 2, r * 2))
                    // White horizontal bar (the G cut-in)
                    drawRect(Color.White, topLeft = Offset(cx, cy - r * 0.22f), size = Size(r * 0.65f, r * 0.44f))
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "Sign in with Google",
                    color      = Color(0xFF202124),
                    fontWeight = FontWeight.SemiBold,
                    style      = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
