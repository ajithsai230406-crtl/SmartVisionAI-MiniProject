package com.smartvision.ai.ui.screens.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
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

// ── Login Screen ──────────────────────────────────────────────────────────────

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, vm: AuthViewModel = hiltViewModel()) {
    val c = svColors
    val s by vm.state.collectAsState()
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name     by remember { mutableStateOf("") }
    var showPw   by remember { mutableStateOf(false) }
    var visible  by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    LaunchedEffect(s.isLoggedIn) { if (s.isLoggedIn) onLoginSuccess() }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(c.background, c.surface)))) {
        // Background glows
        Box(Modifier.size(360.dp).offset(x = 80.dp, y = (-70).dp)
            .background(Brush.radialGradient(listOf(c.primary.copy(.07f), Color.Transparent)), CircleShape))
        Box(Modifier.size(280.dp).align(Alignment.BottomStart).offset(x = (-50).dp, y = 70.dp)
            .background(Brush.radialGradient(listOf(c.secondary.copy(.05f), Color.Transparent)), CircleShape))

        Column(Modifier.fillMaxSize().padding(horizontal = 26.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {

            AnimatedVisibility(visible, enter = fadeIn(tween(700)) + slideInVertically(tween(700)) { -30 }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(80.dp).background(Brush.linearGradient(listOf(c.primary, c.secondary)), RoundedCornerShape(24.dp)), Alignment.Center) {
                        Icon(Icons.Rounded.RemoveRedEye, null, tint = Color.White, modifier = Modifier.size(40.dp))
                    }
                    Text("SmartVision AI", style = MaterialTheme.typography.displaySmall, color = c.onSurface, fontWeight = FontWeight.ExtraBold)
                    Text("Next-gen AI Scanner", style = MaterialTheme.typography.bodyMedium, color = c.subtext)
                }
            }

            Spacer(Modifier.height(36.dp))

            AnimatedVisibility(visible, enter = fadeIn(tween(700, 200)) + slideInVertically(tween(700, 200)) { 40 }) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(c.card)
                    .border(1.dp, c.border, RoundedCornerShape(26.dp)).padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)) {

                    // Tab
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.surface)) {
                        listOf("Sign In" to false, "Sign Up" to true).forEach { (label, isUp) ->
                            Box(Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                .background(if (s.isSignUp == isUp) c.primary.copy(.18f) else Color.Transparent)
                                .clickable { vm.toggleMode() }.padding(vertical = 11.dp), Alignment.Center) {
                                Text(label, style = MaterialTheme.typography.titleMedium,
                                    color = if (s.isSignUp == isUp) c.primary else c.subtext,
                                    fontWeight = if (s.isSignUp == isUp) FontWeight.SemiBold else FontWeight.Normal)
                            }
                        }
                    }

                    // Name field (sign up only)
                    if (s.isSignUp) AuthField(name, { name = it }, "Full Name", Icons.Rounded.Person)

                    // Email
                    AuthField(email, { email = it }, "Email", Icons.Rounded.Email, KeyboardType.Email)

                    // Password
                    AuthField(password, { password = it }, "Password", Icons.Rounded.Lock,
                        visualTransformation = if (showPw) VisualTransformation.None else PasswordVisualTransformation(),
                        trailing = {
                            IconButton(onClick = { showPw = !showPw }) {
                                Icon(if (showPw) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff, null, tint = c.subtext)
                            }
                        })

                    // Error
                    s.error?.let { Text(it, color = c.error, style = MaterialTheme.typography.bodySmall) }

                    // Submit
                    Button(onClick = { if (s.isSignUp) vm.signUp(email, password, name) else vm.signIn(email, password) },
                        modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = c.primary, contentColor = Color.Black),
                        enabled = !s.isLoading) {
                        if (s.isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                        else Text(if (s.isSignUp) "Create Account" else "Sign In", fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium)
                    }

                    // Divider
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        HorizontalDivider(Modifier.weight(1f), color = c.border)
                        Text("or", style = MaterialTheme.typography.bodySmall, color = c.subtext)
                        HorizontalDivider(Modifier.weight(1f), color = c.border)
                    }

                    // Google Sign-In
                    OutlinedButton(onClick = { /* launcher.launch(googleSignInIntent) — wire in Activity */ },
                        modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, c.border),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = c.onSurface)) {
                        Icon(Icons.Rounded.AccountCircle, null, tint = c.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Continue with Google", fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            AnimatedVisibility(visible, enter = fadeIn(tween(700, 400))) {
                TextButton(onClick = { vm.skipLogin() }) {
                    Text("Continue without account", color = c.subtext, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun AuthField(
    value: String, onChange: (String) -> Unit, label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: @Composable (() -> Unit)? = null
) {
    val c = svColors
    OutlinedTextField(value, onChange, label = { Text(label) },
        leadingIcon = { Icon(icon, null, tint = c.primary, modifier = Modifier.size(20.dp)) },
        trailingIcon = trailing, visualTransformation = visualTransformation, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = c.primary, unfocusedBorderColor = c.border,
            focusedLabelColor = c.primary, unfocusedLabelColor = c.subtext,
            focusedTextColor = c.onSurface, unfocusedTextColor = c.onSurface,
            cursorColor = c.primary, focusedContainerColor = c.card, unfocusedContainerColor = c.card))
}
