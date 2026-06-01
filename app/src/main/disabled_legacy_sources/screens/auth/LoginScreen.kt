package com.smartvision.ai.ui.screens.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartvision.ai.ui.theme.*

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val c       = svColors
    val uiState by viewModel.uiState.collectAsState()

    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }
    var isSignUp by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) onLoginSuccess()
    }

    val inf      = rememberInfiniteTransition(label = "orb")
    val pulse   by inf.animateFloat(0.85f, 1.15f, infiniteRepeatable(tween(1600, easing = EaseInOutSine), RepeatMode.Reverse), "p")
    val glowA   by inf.animateFloat(0.3f,  0.7f,  infiniteRepeatable(tween(1200, easing = EaseInOutSine), RepeatMode.Reverse), "g")

    Box(Modifier.fillMaxSize().background(c.background)) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Brush.radialGradient(colors = listOf(Color(0x307B61FF), Color.Transparent),
                center = Offset(size.width * 0.2f, size.height * 0.15f), radius = 300f),
                radius = 300f, center = Offset(size.width * 0.2f, size.height * 0.15f))
            drawCircle(Brush.radialGradient(colors = listOf(Color(0x2000E5FF), Color.Transparent),
                center = Offset(size.width * 0.85f, size.height * 0.35f), radius = 250f),
                radius = 250f, center = Offset(size.width * 0.85f, size.height * 0.35f))
        }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))

            // AI Orb
            Box(Modifier.size(130.dp), Alignment.Center) {
                Box(Modifier.size(130.dp).scale(pulse).background(
                    Brush.radialGradient(colors = listOf(Color(0x407B61FF), Color(0x2000E5FF), Color.Transparent)), CircleShape))
                Box(Modifier.size(80.dp).background(
                    Brush.linearGradient(listOf(SVColors.purple, SVColors.cyan)), CircleShape)
                    .border(2.dp, Brush.linearGradient(listOf(SVColors.cyan, SVColors.purple)), CircleShape),
                    Alignment.Center) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(40.dp))
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Welcome to", style = MaterialTheme.typography.bodyLarge, color = c.subtext, textAlign = TextAlign.Center)
            Text("SmartVision AI", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, color = Color.White)
            Text("Sign in to continue", style = MaterialTheme.typography.bodyMedium, color = c.subtext, textAlign = TextAlign.Center)

            Spacer(Modifier.height(32.dp))

            // Glass card
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                .background(c.card.copy(0.9f))
                .border(1.dp, Brush.linearGradient(listOf(SVColors.cyan.copy(0.4f), SVColors.purple.copy(0.4f))), RoundedCornerShape(24.dp))
                .padding(20.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Email
                    OutlinedTextField(value = email, onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(), label = { Text("Email", color = c.subtext) },
                        leadingIcon = { Icon(Icons.Rounded.Email, null, tint = SVColors.cyan, modifier = Modifier.size(20.dp)) },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SVColors.cyan,
                            unfocusedBorderColor = c.border, focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            cursorColor = SVColors.cyan, focusedContainerColor = c.surface, unfocusedContainerColor = c.surface))

                    // Password
                    OutlinedTextField(value = password, onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(), label = { Text("Password", color = c.subtext) },
                        leadingIcon = { Icon(Icons.Rounded.Lock, null, tint = SVColors.cyan, modifier = Modifier.size(20.dp)) },
                        trailingIcon = { IconButton(onClick = { showPass = !showPass }) {
                            Icon(if (showPass) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, null, tint = c.subtext) }},
                        visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SVColors.cyan,
                            unfocusedBorderColor = c.border, focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            cursorColor = SVColors.cyan, focusedContainerColor = c.surface, unfocusedContainerColor = c.surface))

                    Box(Modifier.fillMaxWidth(), Alignment.CenterEnd) {
                        Text("Forgot Password?", style = MaterialTheme.typography.bodySmall,
                            color = SVColors.cyan, modifier = Modifier.clickable { })
                    }

                    // Login button
                    Button(onClick = { if (isSignUp) viewModel.signUp(email, password) else viewModel.signIn(email, password) },
                        modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp),
                        enabled = email.isNotEmpty() && password.isNotEmpty() && !uiState.isLoading) {
                        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(SVColors.purple, SVColors.cyan)), RoundedCornerShape(14.dp)),
                            Alignment.Center) {
                            if (uiState.isLoading) CircularProgressIndicator(Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                            else Text(if (isSignUp) "Sign Up" else "Login", style = MaterialTheme.typography.titleMedium,
                                color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp), Alignment.CenterVertically) {
                        HorizontalDivider(Modifier.weight(1f), color = c.border)
                        Text("or", color = c.subtext, style = MaterialTheme.typography.bodySmall)
                        HorizontalDivider(Modifier.weight(1f), color = c.border)
                    }

                    OutlinedButton(onClick = { viewModel.signInWithGoogle() },
                        modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, c.border),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = c.surface)) {
                        Row( Modifier.fillMaxWidth(), Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(20.dp).background(
                                Brush.radialGradient(colors = listOf(Color(0xFF4285F4), Color(0xFF34A853))), CircleShape))
                            Text("Continue with Google", color = Color.White, style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxSize()) {
                Text(if (isSignUp) "Already have an account? " else "Don't have an account? ",
                    color = c.subtext, style = MaterialTheme.typography.bodyMedium)
                Text(if (isSignUp) "Sign in" else "Sign up",
                    color = SVColors.cyan, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { isSignUp = !isSignUp })
            }
            uiState.error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = SVColors.error, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
