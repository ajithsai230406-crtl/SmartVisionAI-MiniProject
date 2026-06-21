package com.smartvision.ai.waste.ui

import android.Manifest
import android.graphics.Bitmap
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.*
import com.smartvision.ai.compose.components.*
import com.smartvision.ai.ui.theme.*
import com.smartvision.ai.waste.domain.*
import com.smartvision.ai.waste.presentation.WasteClassifierViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

// ══════════════════════════════════════════════════════════════════════════════
// WASTE CLASSIFIER — MAIN SCREEN
// ══════════════════════════════════════════════════════════════════════════════

private val EcoGreen   = Color(0xFF00FF88)
private val EcoGreenDim = Color(0xFF00CC66)
private val EcoYellow  = Color(0xFFFFD700)

data class WasteDirectoryItem(
    val name: String,
    val category: String,
    val emoji: String,
    val type: String,
    val material: String,
    val environmentalImpact: String,
    val recyclingPossibility: String,
    val suggestedAction: String,
    val funFact: String,
    val aboutText: String,
    val bestOption: String,
    val doNot: String,
    val whereToDispose: String
)

val wasteDirectory = listOf(
    WasteDirectoryItem(
        name = "Mobile Phone",
        category = "E-Waste",
        emoji = "📱",
        type = "Electronic Waste (Hazardous)",
        material = "Metals, Glass, Silicon, Rare Earth Elements",
        environmentalImpact = "Contains heavy metals (lead, mercury) that leach into soil/groundwater if landfilled.",
        recyclingPossibility = "Highly Recyclable (Take to specialized e-waste collection center)",
        suggestedAction = "Do not throw in general trash. Remove SIM card, factory reset, and recycle at dedicated centers.",
        funFact = "1 ton of mobile phones contains about 80 times more gold than a gold mine!",
        aboutText = "Mobile phones contain precious metals and hazardous chemicals that require specialized recycling.",
        bestOption = "Drop off at certified e-waste recycling points or manufacturer trade-in programs.",
        doNot = "Do not incinerate or throw in standard household trash.",
        whereToDispose = "E-waste collection bin / Authorized recycler"
    ),
    WasteDirectoryItem(
        name = "Laptop",
        category = "E-Waste",
        emoji = "💻",
        type = "Electronic Waste (Hazardous)",
        material = "Aluminium, Plastic, Copper, Lithium-ion Battery",
        environmentalImpact = "Heavy metal pollution and plastic components degrade extremely slowly.",
        recyclingPossibility = "Recyclable (Battery must be removed and recycled separately)",
        suggestedAction = "Donate if working. Otherwise, bring to an electronics recycling center.",
        funFact = "Recycling 1 million laptops saves energy equivalent to the electricity used by 3,500 homes in a year!",
        aboutText = "Laptops are complex electronic devices that contain recyclable metals and hazardous materials.",
        bestOption = "Donate to refurbishment charity or return to manufacturer recycling scheme.",
        doNot = "Do not discard in land bins or expose to high heat.",
        whereToDispose = "Specialized E-waste center / Battery drop-off points"
    ),
    WasteDirectoryItem(
        name = "Table",
        category = "Wood Waste",
        emoji = "🪵",
        type = "Wood/Bulky Waste (Biodegradable)",
        material = "Solid Wood / MDF, Metal Screws, Lacquer finish",
        environmentalImpact = "MDF and varnished wood contain resins that release chemicals. Untreated wood is harmless.",
        recyclingPossibility = "Reusable or Recyclable as wood mulch/scrap",
        suggestedAction = "Upcycle, sand/repaint, or dismantle. Dispose bulky items at municipal bulky waste sites.",
        funFact = "Reusing wooden furniture saves forests and reduces carbon emissions by keeping carbon locked in the wood!",
        aboutText = "Wooden tables can last for decades. They can be easily repaired, upcycled, or converted into wood chips.",
        bestOption = "Upcycle, sell/donate to second-hand markets, or scrap for wood recycling.",
        doNot = "Do not burn treated or varnished wood, as it releases toxic smoke.",
        whereToDispose = "Municipal Bulky Waste Depot / Wood Recycling Center"
    ),
    WasteDirectoryItem(
        name = "Chips Wrapper",
        category = "Plastic Waste",
        emoji = "🍿",
        type = "Plastic Waste (Non-Biodegradable)",
        material = "Multi-layered Plastic (PP + PET + Foil)",
        environmentalImpact = "Takes 100-500 years to decompose. Harmful to soil and marine life.",
        recyclingPossibility = "Recyclable (Check Locally)",
        suggestedAction = "Clean and dry the wrapper if possible. Dispose in dry waste or plastic recycling bin.",
        funFact = "Recycling just 1 kg of plastic can save enough energy to power a 60W bulb for 6 hours!",
        aboutText = "This is a multi-layered plastic wrapper commonly used for packaging chips and snacks. It is not biodegradable and can take hundreds of years to decompose.",
        bestOption = "Recycle through plastic waste collection bins (Check local guidelines)",
        doNot = "Do not litter or burn this waste",
        whereToDispose = "Dry Waste Bin / Recycling Center"
    ),
    WasteDirectoryItem(
        name = "Biscuit Packet",
        category = "Plastic Waste",
        emoji = "🍪",
        type = "Plastic Waste (Non-Biodegradable)",
        material = "Biaxially Oriented Polypropylene (BOPP) Film",
        environmentalImpact = "Accumulates in land/oceans, breaking down into harmful microplastics.",
        recyclingPossibility = "Recyclable (Check for soft plastic collection points)",
        suggestedAction = "Compress to save space. Collect with other soft plastics for supermarket drop-offs.",
        funFact = "Most biscuit wrappers are made from BOPP, which is thin but strong and keeps food fresh!",
        aboutText = "Biscuit packets are soft, flexible plastic film wrappers that keep moisture out but degrade slowly.",
        bestOption = "Collect with soft plastics and drop at participating supermarket recycling hubs.",
        doNot = "Do not mix with rigid plastics if local guidelines forbid soft plastics.",
        whereToDispose = "Soft Plastic Collection Bin / Dry Rubbish"
    ),
    WasteDirectoryItem(
        name = "Plastic Bottle",
        category = "Plastic Waste",
        emoji = "🫙",
        type = "Plastic Waste (Recyclable)",
        material = "Polyethylene Terephthalate (PET #1)",
        environmentalImpact = "Petroleum-based product that stays in ecosystems for up to 450 years.",
        recyclingPossibility = "Highly Recyclable (Widely accepted in curb-side recycling)",
        suggestedAction = "Empty contents, crush to save space, and leave cap on before recycling.",
        funFact = "Recycling 5 PET bottles produces enough fiber to make one polyester T-shirt!",
        aboutText = "PET bottles are the most commonly recycled plastics worldwide and can be turned into new bottles or textiles.",
        bestOption = "Rinse, crush, and place in your blue or yellow recycling bin.",
        doNot = "Do not throw in general trash or waterways.",
        whereToDispose = "Plastic Recycling Bin / Deposit Return Machine"
    ),
    WasteDirectoryItem(
        name = "Glass Bottle",
        category = "Glass Waste",
        emoji = "🍷",
        type = "Glass Waste (Highly Recyclable)",
        material = "Soda-lime glass",
        environmentalImpact = "Inert but takes 1 million years to decompose. Energy intensive to manufacture from scratch.",
        recyclingPossibility = "100% Recyclable (Can be recycled endlessly without loss of quality)",
        suggestedAction = "Remove metal caps. Rinse clean. Separate by color (clear, green, amber) if required.",
        funFact = "Recycling one glass bottle saves enough energy to power a computer for 25 minutes!",
        aboutText = "Glass is made of sand, soda ash, and limestone. It is entirely recyclable and safe for reuse.",
        bestOption = "Wash and reuse at home, or deposit in public bottle banks.",
        doNot = "Do not mix drinking glasses or ceramics with bottle glass recycling.",
        whereToDispose = "Glass Recycling Bin / Bottle Bank"
    ),
    WasteDirectoryItem(
        name = "Cardboard Box",
        category = "Paper Waste",
        emoji = "📦",
        type = "Paper Waste (Biodegradable)",
        material = "Corrugated paper fiber",
        environmentalImpact = "Low impact if recycled, but landfilled organic material creates methane.",
        recyclingPossibility = "Highly Recyclable & Compostable (If not soiled with grease/food)",
        suggestedAction = "Flatten the box. Remove packing tape and plastic wraps before recycling.",
        funFact = "Recycling 1 ton of cardboard saves 46 gallons of oil and 9 cubic yards of landfill space!",
        aboutText = "Corrugated cardboard boxes are widely used for shipping. They can be recycled up to 7 times.",
        bestOption = "Flatten and recycle in the paper bin, or shred for garden compost.",
        doNot = "Do not recycle if heavily soaked in oil, grease, or food residue.",
        whereToDispose = "Paper/Cardboard Recycling Bin / Compost Pile"
    ),
    WasteDirectoryItem(
        name = "Aluminium Can",
        category = "Metal Waste",
        emoji = "🥫",
        type = "Metal Waste (Recyclable)",
        material = "Aluminium alloy",
        environmentalImpact = "Mining bauxite is highly destructive, but recycling saves 95% of the energy needed for new metal.",
        recyclingPossibility = "100% Recyclable (Endlessly recyclable without degradation)",
        suggestedAction = "Rinse to prevent insects. Crush if space is limited and put in recycling.",
        funFact = "An aluminium can you recycle today can be back on store shelves as a new can in just 60 days!",
        aboutText = "Beverage cans made of aluminium are the single most valuable item in the recycling stream.",
        bestOption = "Rinse and place in metal recycling bin or use cash-for-cans machines.",
        doNot = "Do not throw in general rubbish as metal is highly valuable.",
        whereToDispose = "Metal Recycling Bin / Scrap Metal collector"
    ),
    WasteDirectoryItem(
        name = "Food Waste",
        category = "Organic Waste",
        emoji = "🍎",
        type = "Organic Waste (Compostable)",
        material = "Organic matter (fruit peels, leftovers, vegetables)",
        environmentalImpact = "In landfills, it produces methane gas. In compost, it fertilizes soil.",
        recyclingPossibility = "100% Compostable (Can be turned into rich fertilizer)",
        suggestedAction = "Segregate from dry waste. Use a backyard composter or green compost bin.",
        funFact = "Food waste represents about 30% of global municipal solid waste!",
        aboutText = "Kitchen scraps, fruit skins, and uneaten food can be naturally decomposed into organic soil booster.",
        bestOption = "Compost at home or dispose in organic/wet waste collection bins.",
        doNot = "Do not discard in plastic bags in the recycling bin.",
        whereToDispose = "Green Wet Waste Bin / Compost Heap"
    ),
    WasteDirectoryItem(
        name = "Clothes",
        category = "Textile Waste",
        emoji = "👕",
        type = "Textile Waste (Reusable/Recyclable)",
        material = "Cotton, Polyester, Wool blend",
        environmentalImpact = "Fast fashion landfill waste decomposes slowly and releases synthetic chemicals.",
        recyclingPossibility = "Reusable or Recyclable into rags, insulation, or new yarn",
        suggestedAction = "Donate usable clothes. Repurpose old textiles as cleaning rags.",
        funFact = "Over 80% of discarded textiles can be reused or recycled, but most end up in landfills!",
        aboutText = "Textile waste includes old clothes, bed sheets, and fabrics. They should be donated or repurposed.",
        bestOption = "Donate to thrift stores, drop in textile bank, or shred for upcycling.",
        doNot = "Do not clean clothes in general trash.",
        whereToDispose = "Textile Donation Box / Fabric Recycling Bin"
    ),
    WasteDirectoryItem(
        name = "Battery",
        category = "Hazardous Waste",
        emoji = "🔋",
        type = "Hazardous Waste (Toxic)",
        material = "Lithium, Zinc, Manganese, Alkaline",
        environmentalImpact = "Heavy metals can pollute soil and water. Direct fire hazard in garbage trucks.",
        recyclingPossibility = "Recyclable at specialized toxic waste plants",
        suggestedAction = "Never mix with standard trash. Tape the terminals to prevent short circuits.",
        funFact = "99% of lead-acid batteries are recycled, making them one of the most recycled products in the world!",
        aboutText = "Batteries store chemical energy. When discarded, they become hazardous waste due to toxic chemicals.",
        bestOption = "Take to local electronics store or toxic waste depot battery drop box.",
        doNot = "Do not place in general trash or incinerate.",
        whereToDispose = "Dedicated Battery Recycling Container"
    )
)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WasteClassifierScreen(
    onBack: () -> Unit,
    viewModel: WasteClassifierViewModel = hiltViewModel()
) {
    val camPerm      = rememberPermissionState(Manifest.permission.CAMERA)
    val scanState    by viewModel.scanState.collectAsStateWithLifecycle()
    val flashEnabled by viewModel.flashEnabled.collectAsStateWithLifecycle()
    val history      by viewModel.sessionHistory.collectAsStateWithLifecycle()
    val liveResult   by viewModel.liveResult.collectAsStateWithLifecycle()
    val cameraRef    = remember { mutableStateOf<Camera?>(null) }
    var capturedBmp  by remember { mutableStateOf<Bitmap?>(null) }

    val ctx         = LocalContext.current
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var selectedGridIndex by remember { mutableStateOf(3) } // Default to 3: Chips Wrapper

    val galleryLauncher = rememberGalleryPickerLauncher { uri ->
        try {
            val source = android.graphics.ImageDecoder.createSource(ctx.contentResolver, uri)
            val bmp = android.graphics.ImageDecoder.decodeBitmap(source)
            capturedBmp = bmp
        } catch (e: Exception) {
            e.printStackTrace()
        }
        viewModel.analyzeFromUri(ctx, uri)
    }

    LaunchedEffect(Unit) { if (!camPerm.status.isGranted) camPerm.launchPermissionRequest() }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopSpeaking()
        }
    }

    LaunchedEffect(flashEnabled) { cameraRef.value?.cameraControl?.enableTorch(flashEnabled) }

    val isClassified = scanState is WasteScanState.Classified

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF040A0E))) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // ── Top HUD ────────────────────────────────────────────────────────
            WasteTopHud(
                isClassified  = isClassified,
                flashEnabled  = flashEnabled,
                onBack        = {
                    if (isClassified) {
                        viewModel.resetScan()
                        capturedBmp = null
                    } else {
                        onBack()
                    }
                },
                onFlash       = { viewModel.toggleFlash() },
                onGallery     = { galleryLauncher() }
            )

            if (!isClassified) {
                // ─── IDLE MODE (Cleaner camera interface) ───────────────────
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Camera/Image Box Container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.3f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(2.dp, EcoGreen.copy(0.4f), RoundedCornerShape(16.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        if (capturedBmp != null) {
                            Image(
                                bitmap = capturedBmp!!.asImageBitmap(),
                                contentDescription = "Captured Waste",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (camPerm.status.isGranted) {
                            WasteCameraPreview(
                                modifier      = Modifier.fillMaxSize(),
                                onPreviewViewCreated = { previewViewRef = it },
                                onCameraReady = { cameraRef.value = it },
                                onFrame       = { bitmap -> viewModel.processLiveFrame(bitmap) }
                            )
                        } else {
                            NoCamPermView(
                                onRequest = { camPerm.launchPermissionRequest() },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Viewfinder overlay
                        WasteViewfinderOverlay()

                        // Analyzing text overlay
                        if (scanState is WasteScanState.Scanning) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 16.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.Black.copy(0.8f))
                                    .border(1.dp, EcoGreen, RoundedCornerShape(20.dp))
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("🌱", fontSize = 12.sp)
                                    Text("Analyzing object...", color = Color.White, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Controls Row: Gallery, Scan, Torch
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Gallery Button
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(0.06f))
                                .border(1.dp, EcoGreen.copy(0.3f), CircleShape)
                                .clickable { galleryLauncher() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🖼️", fontSize = 20.sp)
                        }

                        // Circular Scan Button
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(EcoGreen, EcoGreenDim)))
                                .border(2.dp, Color.White.copy(0.4f), CircleShape)
                                .clickable {
                                    if (capturedBmp != null) {
                                        viewModel.captureAndClassify(capturedBmp!!)
                                    } else {
                                        val currentBitmap = previewViewRef?.bitmap
                                        if (currentBitmap != null) {
                                            capturedBmp = currentBitmap
                                            viewModel.captureAndClassify(currentBitmap)
                                        } else {
                                            Toast.makeText(ctx, "Camera not ready", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔍", fontSize = 28.sp)
                        }

                        // Torch Button
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(0.06f))
                                .border(1.dp, EcoGreen.copy(0.3f), CircleShape)
                                .clickable { viewModel.toggleFlash() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (flashEnabled) "⚡" else "🔦", fontSize = 20.sp)
                        }
                    }

                    // Recent Scans Section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.7f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Recent Scans",
                            style = MaterialTheme.typography.titleMedium,
                            color = EcoGreen,
                            fontWeight = FontWeight.Bold
                        )

                        if (history.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(0.02f))
                                    .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No recent scans yet",
                                    color = Color.White.copy(0.4f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        } else {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 8.dp)
                            ) {
                                items(history) { item ->
                                    Box(
                                        modifier = Modifier
                                            .width(140.dp)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF0C1622))
                                            .border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(12.dp))
                                            .clickable {
                                                viewModel.resetScan()
                                                val bmp = item.imagePath?.let { path ->
                                                    try { android.graphics.BitmapFactory.decodeFile(path) } catch (e: Exception) { null }
                                                }
                                                capturedBmp = bmp
                                                bmp?.let { viewModel.captureAndClassify(it) }
                                            }
                                            .padding(10.dp)
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Text(item.primaryType.emoji, fontSize = 28.sp)
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = item.exactItemName,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center
                                            )
                                            Text(
                                                text = item.primaryType.displayName,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = EcoGreen,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // ─── CLASSIFIED RESULT VIEW ─────────────────────────────────
                val state = scanState as WasteScanState.Classified
                val selectedIdx = state.selectedIndex
                val results = state.results
                val result = results.getOrElse(selectedIdx) { results[0] }
                val know = WasteKnowledgeBase.getKnowledge(result.primaryType)
                val pct = (result.confidence * 100).toInt()

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Result Header Row: Image on the Left, details on the right
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0C1622))
                            .border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Left Side: Captured/Selected Image
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, EcoGreen.copy(0.3f), RoundedCornerShape(12.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            val cachedBitmap = remember(result.imagePath) {
                                result.imagePath?.let { path ->
                                    try {
                                        android.graphics.BitmapFactory.decodeFile(path)
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                            }
                            val displayBmp = cachedBitmap ?: capturedBmp
                            if (displayBmp != null) {
                                Image(
                                    bitmap = displayBmp.asImageBitmap(),
                                    contentDescription = "Scanned Waste Object",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(result.primaryType.emoji, fontSize = 48.sp)
                            }
                        }

                        // Right Side: Item details & confidence
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = result.exactItemName,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = result.primaryType.displayName + " Waste",
                                style = MaterialTheme.typography.bodyMedium,
                                color = EcoGreen,
                                fontWeight = FontWeight.SemiBold
                            )

                            // Status badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (result.isRecyclable) EcoGreen.copy(0.12f) else Color(0xFF33151A))
                                    .border(1.dp, if (result.isRecyclable) EcoGreen.copy(0.3f) else Color(0xFFFF073A).copy(0.3f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (result.isRecyclable) "♻️ Recyclable" else "🗑️ Non-Recyclable",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (result.isRecyclable) EcoGreen else Color(0xFFFF073A),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(Modifier.height(4.dp))
                            // Confidence progress bar
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    "Conf: $pct%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(0.6f)
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(6.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(0.1f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(result.confidence)
                                            .background(EcoGreen, CircleShape)
                                    )
                                }
                            }
                        }
                    }

                    // Tabs if multiple items detected
                    if (results.size > 1) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Multiple items detected:",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(0.5f)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(results) { idx, item ->
                                    val isSelected = idx == selectedIdx
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) EcoGreen.copy(0.15f) else Color(0xFF0C1622))
                                            .border(
                                                1.dp,
                                                if (isSelected) EcoGreen else Color.White.copy(0.08f),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .clickable { viewModel.selectClassifiedItem(idx) }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(item.primaryType.emoji, fontSize = 14.sp)
                                            Text(
                                                text = item.exactItemName,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isSelected) EcoGreen else Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // About This Item Card
                    EcoCard(title = "About This Item") {
                        Text(
                            text = result.environmentalImpact.ifBlank { know.description },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(0.85f),
                            lineHeight = 20.sp
                        )
                    }

                    // Disposal Instructions Card
                    EcoCard(title = "Disposal Instructions") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            DisposeListItem(
                                icon = "✅",
                                title = "How to Dispose",
                                desc = result.disposalMethod.ifBlank { "Dispose in dry waste / recycling center." }
                            )
                            if (result.safetyPrecautions.isNotBlank()) {
                                DisposeListItem(
                                    icon = "⚠️",
                                    title = "Safety Precautions",
                                    desc = result.safetyPrecautions
                                )
                            }
                            DisposeListItem(
                                icon = "🌍",
                                title = "Environmental Impact",
                                desc = result.environmentalImpact.ifBlank { know.description }
                            )
                            DisposeListItem(
                                icon = "💡",
                                title = "Recycling Tips",
                                desc = result.sustainabilityTip.ifBlank { know.ecoTip }
                            )
                        }
                    }

                    // Bottom action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.resetScan()
                                capturedBmp = null
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            border = BorderStroke(1.5.dp, EcoGreen),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = EcoGreen)
                        ) {
                            Text("📷 Scan Again", fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                Toast.makeText(ctx, "Waste details confirmed & saved!", Toast.LENGTH_SHORT).show()
                                viewModel.resetScan()
                                capturedBmp = null
                            },
                            modifier = Modifier
                                .weight(1.2f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EcoGreen,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("✓ Confirm & Save", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ── Floating Robot Orb at bottom right of the screen ────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = 24.dp)
        ) {
            FloatingRobotOrb(onClick = {
                Toast.makeText(ctx, "Calling Floating AI Helper...", Toast.LENGTH_SHORT).show()
            })
        }

        // ── Error ─────────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible  = scanState is WasteScanState.Error,
            enter    = fadeIn(), exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        ) {
            val msg = (scanState as? WasteScanState.Error)?.message ?: ""
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(NeonPink.copy(0.12f)).border(1.dp, NeonPink.copy(0.3f), RoundedCornerShape(14.dp))
                    .padding(14.dp).clickable { viewModel.dismissError() }
            ) {
                Text("⚠️ $msg", style = MaterialTheme.typography.bodySmall, color = NeonPink)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// VIEWFINDER OVERLAY
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun WasteViewfinderOverlay() {
    Canvas(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        val w = size.width
        val h = size.height
        val len = w * 0.15f
        val str = 3f

        // TL Corner
        drawLine(EcoGreen, Offset(0f, 0f), Offset(len, 0f), strokeWidth = str)
        drawLine(EcoGreen, Offset(0f, 0f), Offset(0f, len), strokeWidth = str)

        // TR Corner
        drawLine(EcoGreen, Offset(w, 0f), Offset(w - len, 0f), strokeWidth = str)
        drawLine(EcoGreen, Offset(w, 0f), Offset(w, len), strokeWidth = str)

        // BL Corner
        drawLine(EcoGreen, Offset(0f, h), Offset(len, h), strokeWidth = str)
        drawLine(EcoGreen, Offset(0f, h), Offset(0f, h - len), strokeWidth = str)

        // BR Corner
        drawLine(EcoGreen, Offset(w, h), Offset(w - len, h), strokeWidth = str)
        drawLine(EcoGreen, Offset(w, h), Offset(w, h - len), strokeWidth = str)
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// CAMERA PREVIEW
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun WasteCameraPreview(
    modifier: Modifier,
    onPreviewViewCreated: (PreviewView) -> Unit,
    onCameraReady: (Camera) -> Unit,
    onFrame: (Bitmap) -> Unit
) {
    val ctx            = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope          = rememberCoroutineScope()
    val executor       = remember { Executors.newSingleThreadExecutor() }
    val processing     = remember { java.util.concurrent.atomic.AtomicBoolean(false) }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            executor.shutdown()
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

    AndroidView(modifier = modifier, factory = { c ->
        val pv = PreviewView(c).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            scaleType    = PreviewView.ScaleType.FILL_CENTER
        }
        onPreviewViewCreated(pv)
        ProcessCameraProvider.getInstance(c).addListener({
            val prov    = ProcessCameraProvider.getInstance(c).get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(pv.surfaceProvider) }
            
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analysis.setAnalyzer(executor) { proxy ->
                if (processing.compareAndSet(false, true)) {
                    scope.launch(kotlinx.coroutines.Dispatchers.Default) {
                        try {
                            val rotation = proxy.imageInfo.rotationDegrees
                            val bitmap = proxy.toBitmap()
                            val rotatedBitmap = if (rotation != 0) {
                                val matrix = android.graphics.Matrix().apply { postRotate(rotation.toFloat()) }
                                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                            } else {
                                bitmap
                            }
                            onFrame(rotatedBitmap)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            proxy.close()
                            processing.set(false)
                        }
                    }
                } else {
                    proxy.close()
                }
            }

            val cam = prov.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            onCameraReady(cam)
        }, ContextCompat.getMainExecutor(c))
        pv
    })
}

// ══════════════════════════════════════════════════════════════════════════════
// TOP HUD
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun WasteTopHud(
    isClassified: Boolean,
    flashEnabled: Boolean,
    onBack: () -> Unit,
    onFlash: () -> Unit,
    onGallery: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(0.5f))
                    .border(1.dp, EcoGreen.copy(0.5f), RoundedCornerShape(10.dp))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Text("←", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Column {
                Text(
                    text = "Smart Waste Classifier",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text("AI • Eco Intelligence", style = MaterialTheme.typography.labelSmall, color = EcoGreen)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(0.5f))
                    .border(1.dp, EcoGreen.copy(0.5f), RoundedCornerShape(10.dp))
                    .clickable(onClick = onGallery)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("🖼️", fontSize = 16.sp)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(0.5f))
                    .border(1.dp, EcoGreen.copy(0.5f), RoundedCornerShape(10.dp))
                    .clickable(onClick = onFlash)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(if (flashEnabled) "⚡" else "🔦", fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun EcoCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0C1622))
            .border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = EcoGreen,
            fontWeight = FontWeight.Bold
        )
        content()
    }
}

@Composable
private fun DetailRow(icon: String, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(EcoGreen.copy(0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 16.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = EcoGreen, fontWeight = FontWeight.Bold)
            Text(value, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f), lineHeight = 16.sp)
        }
    }
}

@Composable
private fun DisposeListItem(icon: String, title: String, desc: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.White.copy(0.04f)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 16.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = EcoGreen, fontWeight = FontWeight.Bold)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.85f), lineHeight = 16.sp)
        }
    }
}

@Composable
private fun FloatingRobotOrb(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(Color(0xFF00FF88), Color(0xFF005F44))))
            .border(2.dp, Color.White.copy(0.4f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text("🤖", fontSize = 28.sp)
    }
}

@Composable
private fun NoCamPermView(onRequest: () -> Unit, modifier: Modifier) {
    Box(modifier = modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("♻️", fontSize = 64.sp)
            Text("Camera permission required", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text("Required to classify waste in real time", color = Color.White.copy(0.6f), style = MaterialTheme.typography.bodySmall)
            Button(
                onClick = onRequest,
                colors = ButtonDefaults.buttonColors(containerColor = EcoGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                Text("Grant Permission", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
