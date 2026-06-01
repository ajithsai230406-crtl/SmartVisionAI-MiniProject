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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartvision.ai.ui.theme.*

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val colors  = smartColors
    val uiState by viewModel.uiState.collectAsState()

    var email        by remember { mutableStateOf("") }
    var password     by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isSignUp     by remember { mutableStateOf(false) }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) onLoginSuccess()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(colors.background, colors.surface)
                )
            )
    ) {
        // Background decoration
        Box(
            modifier = Modifier
                .size(400.dp)
                .offset(x = 100.dp, y = (-80).dp)
                .background(
                    Brush.radialGradient(listOf(colors.primary.copy(0.08f), Color.Transparent)),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(300.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-60).dp, y = 80.dp)
                .background(
                    Brush.radialGradient(listOf(colors.secondary.copy(0.06f), Color.Transparent)),
                    CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Logo / Hero ───────────────────────────────────────────────────
            AnimatedVisibility(visible, enter = fadeIn(tween(700)) + slideInVertically(tween(700)) { -30 }) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .background(
                                Brush.linearGradient(listOf(colors.primary, colors.secondary)),
                                RoundedCornerShape(26.dp)
                            )
                            .shadow(16.dp, RoundedCornerShape(26.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RemoveRedEye,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                    Text(
                        text       = "SmartVision AI",
                        style      = MaterialTheme.typography.displaySmall,
                        color      = colors.onSurface,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text  = "Your next-gen AI scanner",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.subtext
                    )
                }
            }

            Spacer(Modifier.height(40.dp))

            // ── Auth Card ─────────────────────────────────────────────────────
            AnimatedVisibility(visible, enter = fadeIn(tween(700, 200)) + slideInVertically(tween(700, 200)) { 40 }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(colors.card)
                        .border(1.dp, colors.cardBorder, RoundedCornerShape(28.dp))
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Tab: Sign In / Sign Up
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.surface),
                    ) {
                        listOf("Sign In" to false, "Sign Up" to true).forEach { (label, mode) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isSignUp == mode)
                                            Brush.linearGradient(listOf(colors.primary.copy(0.2f), colors.secondary.copy(0.1f)))
                                        else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                    )
                                    .border(
                                        if (isSignUp == mode) 1.dp else 0.dp,
                                        if (isSignUp == mode) colors.primary.copy(0.4f) else Color.Transparent,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { isSignUp = mode }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text  = label,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isSignUp == mode) colors.primary else colors.subtext,
                                    fontWeight = if (isSignUp == mode) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    // Email field
                    AuthTextField(
                        value        = email,
                        onValueChange = { email = it },
                        label        = "Email",
                        icon         = Icons.Rounded.Email,
                        keyboardType = KeyboardType.Email
                    )

                    // Password field
                    AuthTextField(
                        value         = password,
                        onValueChange = { password = it },
                        label         = "Password",
                        icon          = Icons.Rounded.Lock,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon  = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                    null,
                                    tint = colors.subtext
                                )
                            }
                        }
                    )

                    // Error
                    if (uiState.error != null) {
                        Text(
                            text  = uiState.error!!,
                            color = colors.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // Submit button
                    Button(
                        onClick = {
                            if (isSignUp) viewModel.signUp(email, password)
                            else viewModel.signIn(email, password)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape  = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor   = Color.Black
                        ),
                        enabled = email.isNotEmpty() && password.isNotEmpty() && !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(Modifier.size(22.dp), color = Color.Black, strokeWidth = 2.dp)
                        } else {
                            Text(
                                if (isSignUp) "Create Account" else "Sign In",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    // Divider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Divider(modifier = Modifier.weight(1f), color = colors.cardBorder)
                        Text("or", style = MaterialTheme.typography.bodySmall, color = colors.subtext)
                        Divider(modifier = Modifier.weight(1f), color = colors.cardBorder)
                    }

                    // Google Sign-In
                    OutlinedButton(
                        onClick  = { viewModel.signInWithGoogle() },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(16.dp),
                        border   = BorderStroke(1.dp, colors.cardBorder),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = colors.onSurface)
                    ) {
                        Icon(
                            Icons.Rounded.AccountCircle,
                            null,
                            tint = colors.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Continue with Google", fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Skip / Guest
            AnimatedVisibility(visible, enter = fadeIn(tween(700, 400))) {
                TextButton(onClick = onLoginSuccess) {
                    Text("Continue without account", color = colors.subtext, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AUTH TEXT FIELD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AuthTextField(
    value:                String,
    onValueChange:        (String) -> Unit,
    label:                String,
    icon:                 androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType:         KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon:         @Composable (() -> Unit)? = null
) {
    val colors = smartColors
    OutlinedTextField(
        value            = value,
        onValueChange    = onValueChange,
        label            = { Text(label) },
        leadingIcon      = { Icon(icon, null, tint = colors.primary, modifier = Modifier.size(20.dp)) },
        trailingIcon     = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions  = KeyboardOptions(keyboardType = keyboardType),
        singleLine       = true,
        modifier         = Modifier.fillMaxWidth(),
        shape            = RoundedCornerShape(16.dp),
        colors           = OutlinedTextFieldDefaults.colors(
            focusedBorderColor     = colors.primary,
            unfocusedBorderColor   = colors.cardBorder,
            focusedLabelColor      = colors.primary,
            unfocusedLabelColor    = colors.subtext,
            focusedTextColor       = colors.onSurface,
            unfocusedTextColor     = colors.onSurface,
            cursorColor            = colors.primary,
            focusedContainerColor  = colors.card,
            unfocusedContainerColor = colors.card
        )
    )
}
