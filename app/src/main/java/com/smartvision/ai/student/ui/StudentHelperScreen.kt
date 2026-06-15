package com.smartvision.ai.student.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.speech.RecognizerIntent
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.*
import com.smartvision.ai.compose.components.*
import com.smartvision.ai.domain.model.StudyHistoryItem
import com.smartvision.ai.student.presentation.*
import com.smartvision.ai.ui.theme.*
import com.smartvision.ai.utils.ImageUtils

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun StudentHelperScreen(
    onBack: () -> Unit,
    viewModel: StudentHelperViewModel = hiltViewModel()
) {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val questionText by viewModel.questionText.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val firestoreHistoryList by viewModel.firestoreHistoryList.collectAsStateWithLifecycle()
    val isLastPage by viewModel.isLastPage.collectAsStateWithLifecycle()
    val isLoadingHistory by viewModel.isLoadingHistory.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Toggle camera scanning overlay
    var isCameraActive by remember { mutableStateOf(false) }

    // TTS speaker state
    var isSpeaking by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopSpeaking()
        }
    }

    // Gallery Picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.processUriForOcr(uri)
        }
    }

    // Voice Input launcher
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                viewModel.updateQuestion(spokenText)
                viewModel.generateAnswer(spokenText)
            }
        }
    }

    // CameraX tools
    val cameraRef = remember { mutableStateOf<Camera?>(null) }
    val imageCapture = remember { ImageCapture.Builder().build() }

    Box(modifier = Modifier.fillMaxSize().background(DeepNavy)) {
        if (isCameraActive) {
            // Camera scanning interface
            if (cameraPermission.status.isGranted) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    StudentCameraPreview(
                        modifier = Modifier.fillMaxSize(),
                        imageCapture = imageCapture,
                        onCameraReady = { cameraRef.value = it }
                    )

                    var canvasWidth by remember { mutableStateOf(1) }
                    var canvasHeight by remember { mutableStateOf(1) }
                    val density = LocalDensity.current

                    val defaultRect = remember(canvasWidth, canvasHeight) {
                        if (canvasWidth > 1 && canvasHeight > 1) {
                            val cropW = 280.dp.value * density.density
                            val cropH = 180.dp.value * density.density
                            val left = (canvasWidth - cropW) / 2f
                            val top = (canvasHeight - cropH) / 2f
                            Rect(left, top, left + cropW, top + cropH)
                        } else {
                            Rect(150f, 400f, 900f, 1100f)
                        }
                    }

                    var manualRect by remember { mutableStateOf<Rect?>(null) }
                    val cropRect = manualRect ?: defaultRect

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .onGloballyPositioned { coords ->
                                canvasWidth = coords.size.width
                                canvasHeight = coords.size.height
                            }
                    ) {
                        ResizableViewfinder(
                            rect = cropRect,
                            onRectChange = { manualRect = it },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Scan Hint
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ScanHintLabel(
                            message = "🎓 Align question inside crop box",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    // Top Bar for Scanner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { isCameraActive = false },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(0.5f))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel", tint = Color.White)
                        }
                        Text(
                            "Scan Question",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(Modifier.width(40.dp))
                    }

                    // Bottom Capture Controls
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .border(4.dp, Color.White, CircleShape)
                                .padding(6.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable {
                                    captureAndCropPhoto(
                                        context,
                                        imageCapture,
                                        cropRect,
                                        canvasWidth,
                                        canvasHeight
                                    ) { croppedBmp ->
                                        viewModel.processImageForOcr(croppedBmp)
                                        isCameraActive = false
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📸", fontSize = 28.sp)
                        }
                    }
                }
            } else {
                NoCameraPermView(
                    onRequest = { cameraPermission.launchPermissionRequest() },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            // Main Chat Study Assistant screen
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                "AI Study Assistant",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 19.sp
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Go back",
                                    tint = Color.White
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                viewModel.clearAll()
                                isSpeaking = false
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reset",
                                    tint = NeonCyan
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = DeepNavy
                        )
                    )
                },
                containerColor = DeepNavy
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Ask Question Card Form
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = GlassCard),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "Ask Your Homework Question",
                                color = NeonCyan,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedTextField(
                                value = questionText,
                                onValueChange = { viewModel.updateQuestion(it) },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Type, scan, paste, or speak your question...", color = Color.White.copy(0.35f), fontSize = 13.sp) },
                                minLines = 3,
                                maxLines = 5,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = Color.White.copy(0.2f)
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = {
                                    viewModel.generateAnswer()
                                })
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Scanning / voice input shortcuts
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Scan Question
                                    IconButton(
                                        onClick = {
                                            if (cameraPermission.status.isGranted) {
                                                isCameraActive = true
                                            } else {
                                                cameraPermission.launchPermissionRequest()
                                            }
                                        },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(0.05f))
                                            .border(1.dp, Color.White.copy(0.15f), RoundedCornerShape(8.dp))
                                    ) {
                                        Text("📷", fontSize = 16.sp)
                                    }

                                    // Upload Image
                                    IconButton(
                                        onClick = { galleryLauncher.launch("image/*") },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(0.05f))
                                            .border(1.dp, Color.White.copy(0.15f), RoundedCornerShape(8.dp))
                                    ) {
                                        Text("🖼️", fontSize = 16.sp)
                                    }

                                    // Voice Question
                                    IconButton(
                                        onClick = {
                                            try {
                                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault())
                                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask your question...")
                                                }
                                                speechRecognizerLauncher.launch(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Speech recognition not supported on this device", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(0.05f))
                                            .border(1.dp, Color.White.copy(0.15f), RoundedCornerShape(8.dp))
                                    ) {
                                        Text("🎙️", fontSize = 16.sp)
                                    }
                                }

                                // Send Button
                                Button(
                                    onClick = { viewModel.generateAnswer() },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                    contentPadding = PaddingValues()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(Brush.horizontalGradient(listOf(NeonCyan, NeonPurple)))
                                            .padding(horizontal = 20.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Send", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // AI Response Area
                    AnimatedVisibility(
                        visible = uiState !is StudentUiState.Idle,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = NavyCard),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Box(modifier = Modifier.padding(16.dp)) {
                                when (val state = uiState) {
                                    is StudentUiState.Loading -> {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            CircularProgressIndicator(color = NeonCyan)
                                            Text(
                                                "Formulating detailed AI solution...",
                                                color = Color.White.copy(0.7f),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                    is StudentUiState.Success -> {
                                        // Parse the response into Solution and Answer sections
                                        val rawResponse = state.response
                                        val solutionText: String
                                        val answerText: String

                                        // Parse SOLUTION: and ANSWER: blocks
                                        val solutionRegex = Regex("SOLUTION:(?:\\s*\\n)?(.*?)(?=\\s*ANSWER:|$)", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
                                        val answerRegex = Regex("ANSWER:(?:\\s*\\n)?(.*)", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))

                                        val solutionMatch = solutionRegex.find(rawResponse)
                                        val answerMatch = answerRegex.find(rawResponse)

                                        if (solutionMatch != null || answerMatch != null) {
                                            solutionText = solutionMatch?.groupValues?.get(1)?.trim() ?: ""
                                            answerText = answerMatch?.groupValues?.get(1)?.trim() ?: ""
                                        } else {
                                            // Fallback: show entire response as solution
                                            solutionText = rawResponse.trim()
                                            answerText = ""
                                        }

                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            // Action buttons row
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Read Aloud
                                                IconButton(onClick = {
                                                    if (isSpeaking) {
                                                        viewModel.stopSpeaking()
                                                        isSpeaking = false
                                                    } else {
                                                        viewModel.speak(state.response)
                                                        isSpeaking = true
                                                    }
                                                }, modifier = Modifier.size(32.dp)) {
                                                    Icon(
                                                        imageVector = if (isSpeaking) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                                        contentDescription = "Read aloud",
                                                        tint = if (isSpeaking) NeonCyan else Color.White.copy(0.6f),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                // Copy
                                                IconButton(onClick = {
                                                    clipboardManager.setText(AnnotatedString(state.response))
                                                    Toast.makeText(context, "Copied to Clipboard", Toast.LENGTH_SHORT).show()
                                                }, modifier = Modifier.size(32.dp)) {
                                                    Icon(
                                                        imageVector = Icons.Default.ContentCopy,
                                                        contentDescription = "Copy text",
                                                        tint = Color.White.copy(0.6f),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                // Share
                                                IconButton(onClick = {
                                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                        type = "text/plain"
                                                        putExtra(Intent.EXTRA_TEXT, state.response)
                                                    }
                                                    context.startActivity(Intent.createChooser(shareIntent, "Share solution"))
                                                }, modifier = Modifier.size(32.dp)) {
                                                    Icon(
                                                        imageVector = Icons.Default.Share,
                                                        contentDescription = "Share text",
                                                        tint = Color.White.copy(0.6f),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }

                                            // Solution Section
                                            if (solutionText.isNotBlank()) {
                                                Text(
                                                    "Solution:",
                                                    color = NeonCyan,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp
                                                )

                                                // Parse and display each line of the solution
                                                val solutionLines = solutionText.lines()
                                                    .map { it.trim() }
                                                    .filter { it.isNotBlank() }

                                                Column(
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    solutionLines.forEach { line ->
                                                        // Clean up markdown artifacts like ** and ##
                                                        val cleanLine = line
                                                            .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
                                                            .replace(Regex("^#+\\s*"), "")
                                                            .trim()

                                                        if (cleanLine.isNotBlank()) {
                                                            Text(
                                                                text = cleanLine,
                                                                color = Color.White.copy(0.88f),
                                                                fontSize = 14.sp,
                                                                lineHeight = 21.sp,
                                                                fontFamily = FontFamily.SansSerif
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            // Answer Section
                                            if (answerText.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))

                                                Text(
                                                    "Answer:",
                                                    color = NeonPink,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp
                                                )

                                                // Clean the answer text
                                                val cleanAnswer = answerText
                                                    .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
                                                    .replace(Regex("^#+\\s*"), "")
                                                    .trim()

                                                Text(
                                                    text = cleanAnswer,
                                                    color = NeonCyan,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 28.sp,
                                                    lineHeight = 34.sp,
                                                    fontFamily = FontFamily.SansSerif
                                                )
                                            }

                                            // Fallback: if no structured sections were found, show raw
                                            if (solutionText.isBlank() && answerText.isBlank()) {
                                                Text(
                                                    text = rawResponse,
                                                    color = Color.White.copy(0.9f),
                                                    fontSize = 14.sp,
                                                    lineHeight = 22.sp,
                                                    fontFamily = FontFamily.SansSerif
                                                )
                                            }
                                        }
                                    }
                                    is StudentUiState.Error -> {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(NeonPink.copy(0.15f))
                                                .border(1.dp, NeonPink.copy(0.3f), RoundedCornerShape(8.dp))
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text("⚠️", fontSize = 18.sp)
                                            Text(
                                                text = state.message,
                                                color = NeonPink,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                    else -> {}
                                }
                            }
                        }
                    }

                    // History logs
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Study History",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            if (firestoreHistoryList.isNotEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        "Export",
                                        color = NeonCyan,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clickable {
                                                viewModel.exportHistory { res ->
                                                    res.onSuccess { uri ->
                                                        android.widget.Toast.makeText(context, "Exported successfully to Downloads/Documents", android.widget.Toast.LENGTH_LONG).show()
                                                    }.onFailure { ex ->
                                                        android.widget.Toast.makeText(context, "Export failed: ${ex.message}", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                            .padding(4.dp)
                                    )
                                    Text(
                                        "|",
                                        color = Color.White.copy(0.2f),
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        "Clear All",
                                        color = NeonPink,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clickable { viewModel.clearAllHistory() }
                                            .padding(4.dp)
                                    )
                                }
                            }
                        }

                        if (firestoreHistoryList.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No recent study history.",
                                    color = Color.White.copy(0.4f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                firestoreHistoryList.forEach { historyItem ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.reopenHistoryItem(historyItem)
                                                isSpeaking = false
                                            }
                                            .border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(12.dp)),
                                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.03f)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .background(Color.White.copy(0.1f), RoundedCornerShape(4.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = historyItem.subject.uppercase(),
                                                            color = NeonCyan,
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                    Text(
                                                        text = historyItem.question,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Medium,
                                                        fontSize = 14.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                                Text(
                                                    text = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(
                                                        java.util.Date(historyItem.timestamp)
                                                    ),
                                                    color = Color.White.copy(0.4f),
                                                    fontSize = 11.sp
                                                )
                                            }

                                            IconButton(onClick = { viewModel.deleteHistoryItem(historyItem.id) }) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete record",
                                                    tint = Color.White.copy(0.35f)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (!isLastPage) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.loadNextPage() }
                                            .border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(12.dp)),
                                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(0.05f)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (isLoadingHistory) "Loading..." else "Load More",
                                                color = NeonCyan,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// CAMERA PREVIEW COMPOSABLE
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun StudentCameraPreview(
    modifier: Modifier,
    imageCapture: ImageCapture,
    onCameraReady: (Camera) -> Unit
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        onDispose {
            val future = ProcessCameraProvider.getInstance(ctx)
            future.addListener({
                try {
                    future.get().unbindAll()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            val preview = PreviewView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            ProcessCameraProvider.getInstance(context).addListener({
                val provider = ProcessCameraProvider.getInstance(context).get()
                provider.unbindAll()

                val previewUC = Preview.Builder().build()
                    .also { it.setSurfaceProvider(preview.surfaceProvider) }

                val cam = provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    previewUC, imageCapture
                )
                onCameraReady(cam)
            }, ContextCompat.getMainExecutor(context))
            preview
        }
    )
}

// ══════════════════════════════════════════════════════════════════════════════
// RESIZABLE VIEWFINDER OVERLAY
// ══════════════════════════════════════════════════════════════════════════════
@Composable
private fun ResizableViewfinder(
    rect: Rect,
    onRectChange: (Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val handleSize = 44.dp
    val thick = 4.dp
    val rad = 12.dp

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        var isDraggingBox by remember { mutableStateOf(false) }

        val dragTranslationModifier = Modifier.pointerInput(rect) {
            detectDragGestures(
                onDragStart = { startOffset ->
                    isDraggingBox = startOffset.x in rect.left..rect.right &&
                                    startOffset.y in rect.top..rect.bottom
                },
                onDrag = { change, dragAmount ->
                    if (isDraggingBox) {
                        change.consume()
                        val rectWidth = rect.width
                        val rectHeight = rect.height

                        var newLeft = rect.left + dragAmount.x
                        var newTop = rect.top + dragAmount.y

                        if (newLeft < 20f) newLeft = 20f
                        if (newTop < 120f) newTop = 120f

                        val newRight = newLeft + rectWidth
                        val newBottom = newTop + rectHeight

                        if (newRight > widthPx - 20f) {
                            newLeft = widthPx - 20f - rectWidth
                        }
                        if (newBottom > heightPx - 220f) {
                            newTop = heightPx - 220f - rectHeight
                        }

                        onRectChange(Rect(newLeft, newTop, newLeft + rectWidth, newTop + rectHeight))
                    }
                },
                onDragEnd = { isDraggingBox = false },
                onDragCancel = { isDraggingBox = false }
            )
        }

        Canvas(modifier = Modifier.fillMaxSize().then(dragTranslationModifier)) {
            val outerPath = Path().apply {
                addRect(Rect(0f, 0f, size.width, size.height))
            }
            val innerPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = rect,
                        cornerRadius = CornerRadius(rad.toPx(), rad.toPx())
                    )
                )
            }
            val punchedPath = Path.combine(PathOperation.Difference, outerPath, innerPath)
            drawPath(punchedPath, Color.Black.copy(alpha = 0.70f))

            drawRoundRect(
                color = NeonCyan.copy(alpha = 0.45f),
                topLeft = rect.topLeft,
                size = rect.size,
                cornerRadius = CornerRadius(rad.toPx(), rad.toPx()),
                style = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                )
            )

            val len = 24.dp.toPx()
            val thickPx = thick.toPx()
            val radPx = rad.toPx()

            // TL
            val tlPath = Path().apply {
                moveTo(rect.left, rect.top + len)
                lineTo(rect.left, rect.top + radPx)
                quadraticTo(rect.left, rect.top, rect.left + radPx, rect.top)
                lineTo(rect.left + len, rect.top)
            }
            drawPath(tlPath, NeonCyan, style = Stroke(width = thickPx, cap = StrokeCap.Round))

            // TR
            val trPath = Path().apply {
                moveTo(rect.right - len, rect.top)
                lineTo(rect.right - radPx, rect.top)
                quadraticTo(rect.right, rect.top, rect.right, rect.top + radPx)
                lineTo(rect.right, rect.top + len)
            }
            drawPath(trPath, NeonCyan, style = Stroke(width = thickPx, cap = StrokeCap.Round))

            // BL
            val blPath = Path().apply {
                moveTo(rect.left, rect.bottom - len)
                lineTo(rect.left, rect.bottom - radPx)
                quadraticTo(rect.left, rect.bottom, rect.left + radPx, rect.bottom)
                lineTo(rect.left + len, rect.bottom)
            }
            drawPath(blPath, NeonCyan, style = Stroke(width = thickPx, cap = StrokeCap.Round))

            // BR
            val brPath = Path().apply {
                moveTo(rect.right - len, rect.bottom)
                lineTo(rect.right - radPx, rect.bottom)
                quadraticTo(rect.right, rect.bottom, rect.right, rect.bottom - radPx)
                lineTo(rect.right, rect.bottom - len)
            }
            drawPath(brPath, NeonCyan, style = Stroke(width = thickPx, cap = StrokeCap.Round))
        }

        val handleSizePx = with(density) { handleSize.toPx() }

        // Draggable corners
        // Top-Left
        Box(
            modifier = Modifier
                .offset { IntOffset((rect.left - handleSizePx / 2).toInt(), (rect.top - handleSizePx / 2).toInt()) }
                .size(handleSize)
                .pointerInput(rect) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newLeft = (rect.left + dragAmount.x).coerceAtMost(rect.right - 160f).coerceAtLeast(20f)
                        val newTop = (rect.top + dragAmount.y).coerceAtMost(rect.bottom - 160f).coerceAtLeast(120f)
                        onRectChange(Rect(newLeft, newTop, rect.right, rect.bottom))
                    }
                }
        )

        // Top-Right
        Box(
            modifier = Modifier
                .offset { IntOffset((rect.right - handleSizePx / 2).toInt(), (rect.top - handleSizePx / 2).toInt()) }
                .size(handleSize)
                .pointerInput(rect) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newRight = (rect.right + dragAmount.x).coerceAtLeast(rect.left + 160f).coerceAtMost(widthPx - 20f)
                        val newTop = (rect.top + dragAmount.y).coerceAtMost(rect.bottom - 160f).coerceAtLeast(120f)
                        onRectChange(Rect(rect.left, newTop, newRight, rect.bottom))
                    }
                }
        )

        // Bottom-Left
        Box(
            modifier = Modifier
                .offset { IntOffset((rect.left - handleSizePx / 2).toInt(), (rect.bottom - handleSizePx / 2).toInt()) }
                .size(handleSize)
                .pointerInput(rect) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newLeft = (rect.left + dragAmount.x).coerceAtMost(rect.right - 160f).coerceAtLeast(20f)
                        val newBottom = (rect.bottom + dragAmount.y).coerceAtLeast(rect.top + 160f).coerceAtMost(heightPx - 220f)
                        onRectChange(Rect(newLeft, rect.top, rect.right, newBottom))
                    }
                }
        )

        // Bottom-Right
        Box(
            modifier = Modifier
                .offset { IntOffset((rect.right - handleSizePx / 2).toInt(), (rect.bottom - handleSizePx / 2).toInt()) }
                .size(handleSize)
                .pointerInput(rect) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newRight = (rect.right + dragAmount.x).coerceAtLeast(rect.left + 160f).coerceAtMost(widthPx - 20f)
                        val newBottom = (rect.bottom + dragAmount.y).coerceAtLeast(rect.top + 160f).coerceAtMost(heightPx - 220f)
                        onRectChange(Rect(rect.left, rect.top, newRight, newBottom))
                    }
                }
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// CAPTURE & CROP HELPER METHODS
// ══════════════════════════════════════════════════════════════════════════════
private fun captureAndCropPhoto(
    context: Context,
    imageCapture: ImageCapture,
    cropRect: Rect,
    canvasWidth: Int,
    canvasHeight: Int,
    onSuccess: (Bitmap) -> Unit
) {
    imageCapture.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val fullBmp = imageProxyToBitmap(image)
                image.close()

                if (fullBmp != null) {
                    val cropLeft = cropRect.left / canvasWidth
                    val cropTop = cropRect.top / canvasHeight
                    val cropRight = cropRect.right / canvasWidth
                    val cropBottom = cropRect.bottom / canvasHeight

                    val croppedBmp = ImageUtils(com.google.firebase.storage.FirebaseStorage.getInstance()).cropBitmap(
                        fullBmp, cropLeft, cropTop, cropRight, cropBottom
                    )
                    onSuccess(croppedBmp)
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Toast.makeText(context, "Failed to capture image", Toast.LENGTH_SHORT).show()
            }
        }
    )
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    val raw = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

    if (image.imageInfo.rotationDegrees != 0) {
        val matrix = android.graphics.Matrix().apply {
            postRotate(image.imageInfo.rotationDegrees.toFloat())
        }
        return Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
    }
    return raw
}

@Composable
private fun NoCameraPermView(onRequest: () -> Unit, modifier: Modifier) {
    Box(modifier = modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("📷", fontSize = 64.sp)
            Text("Camera permission required", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text("Required to scan textbook or assignment questions.", color = Color.White.copy(0.6f), style = MaterialTheme.typography.bodySmall)
            Button(onClick = onRequest) {
                Text("Grant Permission")
            }
        }
    }
}

@Composable
private fun ScanHintLabel(message: String, modifier: Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "hint")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "hintAlpha"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.65f))
            .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            message,
            color = Color.White.copy(alpha = alpha),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
