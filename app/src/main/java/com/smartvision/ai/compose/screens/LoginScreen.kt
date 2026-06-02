package com.smartvision.ai.compose.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartvision.ai.compose.components.*
import com.smartvision.ai.presentation.auth.AuthViewModel
import com.smartvision.ai.ui.theme.*

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val message by viewModel.message.collectAsStateWithLifecycle()
    val ext     = MaterialTheme.extended

    var isSignup by remember { mutableStateOf(false) }
    var showEmailForm by remember { mutableStateOf(false) }
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name     by remember { mutableStateOf("") }
    var showPwd  by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let { snackbarHostState.showSnackbar(it) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepNavy)
    ) {
        // Ambient glow blobs
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset((-60).dp, (-40).dp)
                .clip(CircleShape)
                .background(NeonPurple.copy(alpha = 0.08f))
        )
        Box(
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.BottomEnd)
                .offset(50.dp, 50.dp)
                .clip(CircleShape)
                .background(NeonBlue.copy(alpha = 0.07f))
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
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(72.dp))

                // ── App Logo mini ─────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(listOf(NeonBlue.copy(0.3f), NeonPurple.copy(0.2f)))
                        )
                        .border(2.dp, Brush.horizontalGradient(listOf(NeonBlue, NeonPurple)), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👁", fontSize = 30.sp)
                }

                Spacer(Modifier.height(20.dp))

                // ── Heading ───────────────────────────────────────────────
                Text(
                    if (isSignup) "Create Account 🎉" else "Welcome Back! 👋",
                    style      = MaterialTheme.typography.headlineMedium,
                    color      = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    textAlign  = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (isSignup) "Join Smart Vision AI today" else "Sign in to continue",
                    style     = MaterialTheme.typography.bodyMedium,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(32.dp))

                // ── Glass Card ────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(NavyCard)
                        .border(
                            1.dp,
                            Brush.linearGradient(
                                listOf(NeonBlue.copy(0.3f), NeonPurple.copy(0.2f), NeonBlue.copy(0.1f))
                            ),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                        // ── Google Button ──────────────────────────────────
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(26.dp))
                                .background(Color.White)
                                .clickable {
                                    viewModel.connectGoogle(null) { onLoginSuccess() }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                // Google G logo simulation
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "G",
                                        color      = Color(0xFF4285F4),
                                        fontSize   = 18.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "Continue with Google",
                                    color      = Color(0xFF202124),
                                    style      = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // ── Email Button ───────────────────────────────────
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(26.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF1A237E), Color(0xFF311B92))
                                    )
                                )
                                .border(1.dp, NeonPurple.copy(0.3f), RoundedCornerShape(26.dp))
                                .clickable { showEmailForm = !showEmailForm },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("✉️", fontSize = 20.sp)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "Continue with Email",
                                    color      = Color.White,
                                    style      = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // ── Expandable email/password form ─────────────────
                        if (showEmailForm) {
                            Divider(
                                color     = ext.glassBorder.copy(alpha = 0.5f),
                                thickness = 0.8.dp
                            )
                            Text(
                                "OR",
                                modifier  = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                                style     = MaterialTheme.typography.labelMedium
                            )

                            if (isSignup) {
                                SmartTextField(
                                    value         = name,
                                    onValueChange = { name = it },
                                    placeholder   = "Full name",
                                    leadingIcon   = {
                                        Text(
                                            "👤",
                                            fontSize = 18.sp,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                )
                            }
                            SmartTextField(
                                value         = email,
                                onValueChange = { email = it },
                                placeholder   = "Email address",
                                leadingIcon   = {
                                    Text(
                                        "✉️",
                                        fontSize = 18.sp,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            )
                            SmartTextField(
                                value         = password,
                                onValueChange = { password = it },
                                placeholder   = "Password",
                                leadingIcon   = {
                                    Text(
                                        "🔒",
                                        fontSize = 18.sp,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                },
                                trailingIcon  = {
                                    Text(
                                        if (showPwd) "🙈" else "👁",
                                        fontSize = 18.sp,
                                        modifier = Modifier
                                            .padding(end = 8.dp)
                                            .clickable { showPwd = !showPwd }
                                    )
                                },
                                singleLine    = true
                            )

                            NeonButton(
                                text     = if (isSignup) "Create Account" else "Sign In",
                                onClick  = {
                                    if (isSignup) viewModel.signup(name, email, password) { onLoginSuccess() }
                                    else viewModel.login(email, password) { onLoginSuccess() }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                if (isSignup) "Already have an account? Sign in"
                                else "Don't have an account? Sign up",
                                modifier  = Modifier
                                    .fillMaxWidth()
                                    .clickable { isSignup = !isSignup },
                                textAlign = TextAlign.Center,
                                color     = ext.neonBlue,
                                style     = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Guest Button ──────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .border(
                            1.5.dp,
                            Brush.horizontalGradient(listOf(ext.glassBorder, ext.glassBorder)),
                            RoundedCornerShape(26.dp)
                        )
                        .background(NavyCard)
                        .clickable { viewModel.loginAsGuest { onLoginSuccess() } },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("👤", fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Continue as Guest",
                            color      = MaterialTheme.colorScheme.onSurface,
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Terms ─────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "By continuing, you agree to our ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Terms",
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.neonBlue,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onNavigate(com.smartvision.ai.compose.navigation.Routes.TERMS_CONDITIONS) }
                    )
                    Text(
                        " & ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Privacy Policy",
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.neonBlue,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onNavigate(com.smartvision.ai.compose.navigation.Routes.PRIVACY_POLICY) }
                    )
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
