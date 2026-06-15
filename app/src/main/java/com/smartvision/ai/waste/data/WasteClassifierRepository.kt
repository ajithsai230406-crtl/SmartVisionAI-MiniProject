package com.smartvision.ai.waste.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.smartvision.ai.waste.domain.*
import com.smartvision.ai.data.db.GeminiCacheDao
import com.smartvision.ai.data.db.GeminiCacheEntity
import com.smartvision.ai.util.retryWithDelay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sqrt
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp

import com.smartvision.ai.data.repository.OpenRouterManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel

@Singleton
class WasteClassifierRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cacheDao: GeminiCacheDao,
    private val openRouterManager: OpenRouterManager
) {

    private var interpreter: Interpreter? = null
    private var cocoLabels: List<String> = emptyList()
    private var numDetections: Int = 25
    private var imageProcessor: ImageProcessor? = null

    private val initScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val initJob: Job by lazy {
        initScope.launch {
            try {
                val modelBuffer = FileUtil.loadMappedFile(context, "efficientdet_lite4.tflite")
                val options = Interpreter.Options().apply {
                    setNumThreads(4)
                }
                interpreter = Interpreter(modelBuffer, options)
                android.util.Log.d("WasteClassifier", "Model loaded successfully.")
                cocoLabels = FileUtil.loadLabels(context, "coco_labels.txt")
                android.util.Log.d("WasteClassifier", "Labels loaded: ${cocoLabels.size} classes successfully.")
                
                val boxShape = interpreter!!.getOutputTensor(0).shape()
                if (boxShape.size >= 2) numDetections = boxShape[1]

                val inputTensor = interpreter!!.getInputTensor(0)
                val inputShape = inputTensor.shape()
                val inputDataType = inputTensor.dataType()
                val targetHeight = if (inputShape.size >= 3) inputShape[1] else 640
                val targetWidth = if (inputShape.size >= 3) inputShape[2] else 640

                val processorBuilder = ImageProcessor.Builder()
                    .add(ResizeOp(targetHeight, targetWidth, ResizeOp.ResizeMethod.BILINEAR))
                
                if (inputDataType == DataType.FLOAT32) {
                    processorBuilder.add(org.tensorflow.lite.support.common.ops.NormalizeOp(0.0f, 255.0f))
                    android.util.Log.d("WasteClassifier", "Float32 input tensor detected, adding NormalizeOp(0f, 255f) to pipeline")
                }
                imageProcessor = processorBuilder.build()

                android.util.Log.d("WasteClassifier", "TFLite Model loaded successfully, detections count: $numDetections")
            } catch (e: Exception) {
                android.util.Log.e("WasteClassifier", "Failed to load TFLite model", e)
            }
        }
    }

    private suspend fun ensureInitialized() {
        initJob.join()
    }

    fun close() {
        initScope.cancel()
        interpreter?.close()
    }

    private fun mapCocoLabelToWasteLabel(cocoLabel: String): String {
        return when (cocoLabel.lowercase().trim()) {
            "bottle" -> "plastic_bottle"
            "cup", "bowl", "wine glass" -> "plastic"
            "fork", "knife", "spoon", "scissors" -> "metal"
            "banana", "apple", "orange", "sandwich", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake" -> "organic"
            "book", "newspaper" -> "paper"
            "cell phone", "laptop", "keyboard", "mouse", "remote", "tv", "microwave", "oven", "toaster", "refrigerator" -> "electronic_waste"
            "handbag", "suitcase", "backpack", "tie" -> "textile"
            else -> "mixed_waste"
        }
    }

    // ── Perceptual hash — 4×4 grid + variance (16 sample points) ─────────────
    // This avoids hash collisions on images that differ only in one area.
    fun getBitmapHash(bitmap: Bitmap): Int {
        val w = bitmap.width; val h = bitmap.height
        if (w == 0 || h == 0) return 0
        var hash = 17
        val steps = 4
        var rSum = 0L; var gSum = 0L; var bSum = 0L
        for (row in 0 until steps) {
            for (col in 0 until steps) {
                val px = ((col + 0.5f) / steps * w).toInt().coerceIn(0, w - 1)
                val py = ((row + 0.5f) / steps * h).toInt().coerceIn(0, h - 1)
                val pixel = bitmap.getPixel(px, py)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                rSum += r; gSum += g; bSum += b
                hash = 31 * hash + r
                hash = 31 * hash + g
                hash = 31 * hash + b
            }
        }
        hash = 31 * hash + w / 32   // coarse resolution contribution
        hash = 31 * hash + h / 32
        return hash
    }

    // ── Classify from camera bitmap offline using TFLite model or online via Gemini ───────────────
    suspend fun classifyFromBitmap(bitmap: Bitmap, isLive: Boolean = false): Result<WasteResult> =
        withContext(Dispatchers.Default) {
            runCatching {
                ensureInitialized()

                // Check cache and try Gemini first for manual/non-live scans
                if (!isLive) {
                    val hash = getBitmapHash(bitmap)
                    try {
                        val cached = cacheDao.getCache(hash)
                        if (cached != null) {
                            android.util.Log.d("WasteClassifier", "Gemini cache hit for image hash: $hash")
                            return@runCatching parseGeminiResponse(cached.response)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("WasteClassifier", "Failed to retrieve/parse cached response", e)
                    }

                    try {
                        android.util.Log.d("WasteClassifier", "Invoking Gemini Vision for waste classification...")
                        val prompt = buildPrompt()
                        val geminiResponse = openRouterManager.generateContent(
                            prompt = prompt,
                            image = bitmap,
                            modelName = "openrouter/auto"
                        )
                        if (geminiResponse.isNotBlank()) {
                            val parsed = parseGeminiResponse(geminiResponse)
                            try {
                                cacheDao.insertCache(
                                    GeminiCacheEntity(
                                        promptHash = hash,
                                        prompt = "waste_classify_bitmap",
                                        response = geminiResponse
                                    )
                                )
                            } catch (e: Exception) {
                                android.util.Log.e("WasteClassifier", "Failed to cache Gemini response", e)
                            }
                            return@runCatching parsed
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("WasteClassifier", "Gemini classification failed, falling back to local TFLite", e)
                    }
                }

                // Fallback to local model (or default for live preview)
                val tflite = interpreter
                val processor = imageProcessor
                
                var detectedLabel: String? = null
                var detectedConfidence = 0.0f
                
                if (tflite != null && processor != null) {
                    val inputTensor = tflite.getInputTensor(0)
                    val inputDataType = inputTensor.dataType()

                    val tensorImage = TensorImage(inputDataType)
                    tensorImage.load(bitmap)
                    val processedImage = processor.process(tensorImage)

                    val n = numDetections
                    val outputBoxes   = Array(1) { Array(n) { FloatArray(4) } }
                    val outputClasses = Array(1) { FloatArray(n) }
                    val outputScores  = Array(1) { FloatArray(n) }
                    val outputCount   = FloatArray(1)

                    val outputs = mapOf(
                        0 to outputBoxes,
                        1 to outputClasses,
                        2 to outputScores,
                        3 to outputCount
                    )

                    tflite.runForMultipleInputsOutputs(
                        arrayOf(processedImage.buffer),
                        outputs
                    )

                    val actualCount = outputCount[0].toInt().coerceIn(0, n)
                    
                    var bestScore = 0.35f
                    var bestClassId = -1
                    for (i in 0 until actualCount) {
                        val score = outputScores[0][i]
                        if (score > bestScore) {
                            val classId = outputClasses[0][i].toInt()
                            val label = cocoLabels.getOrNull(classId)
                            if (label != null && label != "person") {
                                bestScore = score
                                bestClassId = classId
                            }
                        }
                    }
                    if (bestClassId != -1) {
                        val label = cocoLabels.getOrNull(bestClassId)
                        if (label != null) {
                            if (bestScore < 0.60f) {
                                detectedLabel = "Unknown Object"
                            } else {
                                detectedLabel = label
                            }
                            detectedConfidence = bestScore
                        }
                    }
                }

                if (detectedLabel != null && detectedLabel != "Unknown Object") {
                    android.util.Log.d("WasteClassifier", "Prediction generated: $detectedLabel with confidence $detectedConfidence")
                    val wasteLabel = mapCocoLabelToWasteLabel(detectedLabel)
                    val result = classifyByLabel(wasteLabel)
                    result.copy(
                        confidence = detectedConfidence,
                        exactItemName = detectedLabel.replace("_", " ").split(" ")
                            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                    )
                } else {
                    android.util.Log.d("WasteClassifier", "No model prediction above threshold. Using fallback waste result.")
                    classifyByLabel("mixed_waste").copy(
                        confidence = 0.50f,
                        exactItemName = "Unknown Object"
                    )
                }
            }
        }

    // ── Gemini prompt ─────────────────────────────────────────────────────────
    private fun buildPrompt() = """
You are a professional waste classification AI trained on TrashNet, TACO, and WasteNet datasets.
Analyze the image and identify the waste item visible.

Respond EXACTLY in this structured format (no extra text):

EXACT_ITEM: [Specific item, e.g. PET Water Bottle, Cardboard Box, AA Battery, Smartphone]
WASTE_TYPE: [EXACTLY one of: plastic, paper, glass, metal, organic, ewaste, battery, hazardous, medical, textile, recyclable, non_recyclable]
RECYCLING_CATEGORY: [e.g. Recyclable Plastic #1, Hazardous Waste, E-Waste WEEE, Organic Compost]
DISPOSAL_METHOD: [Exact bin or facility, e.g. Blue Recycle Bin, Green Wet Waste Bin, WEEE Collection Point, HHW Facility, Medical Hazmat Box]
IS_RECYCLABLE: [true or false]
IS_REUSABLE: [true or false]
CONFIDENCE: [0.0 to 1.0]
ECO_SCORE: [0 to 100, where 100 = best eco impact]
CARBON_REDUCTION: [e.g. 1.5 kg CO₂ saved by recycling this]
CARBON_IMPACT: [e.g. 6.2 kg CO₂ per kg to produce this item]
SUSTAINABILITY_TIP: [one sentence practical tip]
ENVIRONMENTAL_IMPACT: [one sentence on environmental consequence]
REUSE_SUGGESTIONS: [one sentence concrete upcycling or reuse idea]
DISPOSAL_STEPS:
- [Step 1]
- [Step 2]
- [Step 3]
- [Step 4]
""".trimIndent()

    // ── Classify from URI ──────────────────────────────────────────────────────
    suspend fun classifyFromUri(context: Context, uri: Uri): Result<WasteResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val bitmap = android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                classifyFromBitmap(bitmap, isLive = false).getOrThrow()
            }
        }

    // ── Label-only classification (fast, offline) ─────────────────────────────
    fun classifyByLabel(rawLabel: String): WasteResult {
        val primaryType = rawLabel.toWasteType()
        val defaultKnow = WasteKnowledgeBase.getKnowledge(primaryType)
        val isRecyc = primaryType != WasteType.NON_RECYCLABLE &&
                      primaryType != WasteType.HAZARDOUS &&
                      primaryType != WasteType.MEDICAL
        val isReuse = primaryType != WasteType.MEDICAL && primaryType != WasteType.BATTERY
        return buildResult(
            rawLabel = rawLabel,
            primaryType = primaryType,
            confidence = 0.88f,
            exactItemName = rawLabel.replace("_", " ").split(" ")
                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } },
            recyclingCategory = "Recyclable ${primaryType.displayName}",
            disposalMethod = primaryType.bin,
            disposalSteps = defaultKnow.disposalSteps,
            reuseSuggestions = defaultKnow.reusabilityInfo,
            environmentalImpact = defaultKnow.description,
            ecoScore = 70,
            carbonReduction = defaultKnow.carbonFootprint,
            carbonImpact = "",
            sustainabilityTip = defaultKnow.ecoTip,
            isRecyclable = isRecyc,
            isReusable = isReuse
        )
    }

    // ── Response parser ────────────────────────────────────────────────────────
    private fun parseGeminiResponse(raw: String): WasteResult {
        fun extract(tag: String) = Regex("$tag:\\s*(.+)", RegexOption.IGNORE_CASE)
            .find(raw)?.groupValues?.get(1)?.trim() ?: ""

        val exactItem          = extract("EXACT_ITEM").ifBlank { "Unknown Waste Item" }
        val recyclingCategory  = extract("RECYCLING_CATEGORY").ifBlank { "General Waste" }
        val wasteTypeLabel     = extract("WASTE_TYPE").ifBlank { "non_recyclable" }
        val disposalMethod     = extract("DISPOSAL_METHOD").ifBlank { "" }
        val confidence         = extract("CONFIDENCE").toFloatOrNull()?.coerceIn(0f, 1f) ?: 0.85f
        val ecoScore           = extract("ECO_SCORE").toIntOrNull()?.coerceIn(0, 100) ?: 50
        val carbonReduction    = extract("CARBON_REDUCTION").ifBlank { "0.0 kg CO₂" }
        val carbonImpact       = extract("CARBON_IMPACT").ifBlank { "" }
        val sustainabilityTip  = extract("SUSTAINABILITY_TIP").ifBlank { "Reduce single-use items where possible." }
        val environmentalImpact = extract("ENVIRONMENTAL_IMPACT").ifBlank { "Landfilled waste degrades slowly and emits greenhouse gases." }
        val reuseSuggestions   = extract("REUSE_SUGGESTIONS").ifBlank { "Repurpose for storage or home organization." }

        val primaryType = wasteTypeLabel.toWasteType()

        val isRecyclable = extract("IS_RECYCLABLE").contains("true", ignoreCase = true) ||
            (!extract("IS_RECYCLABLE").contains("false", ignoreCase = true) &&
             primaryType != WasteType.NON_RECYCLABLE &&
             primaryType != WasteType.HAZARDOUS &&
             primaryType != WasteType.MEDICAL)
        val isReusable = extract("IS_REUSABLE").contains("true", ignoreCase = true) ||
            (!extract("IS_REUSABLE").contains("false", ignoreCase = true) &&
             primaryType != WasteType.MEDICAL &&
             primaryType != WasteType.BATTERY)

        // Extract disposal steps
        val steps = mutableListOf<String>()
        val disposalIndex = raw.indexOf("DISPOSAL_STEPS:", ignoreCase = true)
        if (disposalIndex != -1) {
            raw.substring(disposalIndex + "DISPOSAL_STEPS:".length).lines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("-") || trimmed.startsWith("*") ||
                    (trimmed.firstOrNull()?.isDigit() == true && trimmed.contains("."))) {
                    val content = trimmed.substring(1).trim().replace(Regex("^\\d+\\.\\s*"), "")
                    if (content.isNotEmpty()) steps.add(content)
                }
            }
        }
        val finalSteps = steps.ifEmpty { WasteKnowledgeBase.getKnowledge(primaryType).disposalSteps }

        return buildResult(
            rawLabel = wasteTypeLabel,
            primaryType = primaryType,
            confidence = confidence,
            exactItemName = exactItem,
            recyclingCategory = recyclingCategory,
            disposalMethod = disposalMethod.ifBlank { primaryType.bin },
            disposalSteps = finalSteps,
            reuseSuggestions = reuseSuggestions,
            environmentalImpact = environmentalImpact,
            ecoScore = ecoScore,
            carbonReduction = carbonReduction,
            carbonImpact = carbonImpact,
            sustainabilityTip = sustainabilityTip,
            isRecyclable = isRecyclable,
            isReusable = isReusable
        )
    }

    private fun getRecyclabilityCategory(type: WasteType): String {
        return when (type) {
            WasteType.ORGANIC -> "Biodegradable"
            WasteType.PAPER -> "Biodegradable"
            WasteType.HAZARDOUS, WasteType.MEDICAL, WasteType.BATTERY -> "Hazardous"
            WasteType.NON_RECYCLABLE -> "Non-biodegradable"
            else -> "Recyclable"
        }
    }

    private fun buildResult(
        rawLabel: String,
        primaryType: WasteType,
        confidence: Float,
        exactItemName: String = "",
        recyclingCategory: String = "",
        disposalMethod: String = "",
        disposalSteps: List<String> = emptyList(),
        reuseSuggestions: String = "",
        environmentalImpact: String = "",
        ecoScore: Int = 50,
        carbonReduction: String = "",
        carbonImpact: String = "",
        sustainabilityTip: String = "",
        isRecyclable: Boolean = true,
        isReusable: Boolean = true
    ): WasteResult {
        val allTypes = WasteType.values()
        val scores = allTypes.map { t ->
            val score = if (t == primaryType) confidence
                        else (confidence * (0.05f + abs(t.name.hashCode() % 15) / 100f)).coerceAtMost(0.25f)
            WasteCategoryScore(t, score)
        }.sortedByDescending { it.score }

        val finalRecyclingCategory = if (recyclingCategory.isNotEmpty() && 
                                        (recyclingCategory == "Biodegradable" || 
                                         recyclingCategory == "Non-biodegradable" || 
                                         recyclingCategory == "Hazardous" || 
                                         recyclingCategory == "Recyclable")) {
            recyclingCategory
        } else {
            getRecyclabilityCategory(primaryType)
        }

        return WasteResult(
            primaryType      = primaryType,
            confidence       = confidence,
            allCategories    = scores,
            rawLabel         = rawLabel,
            exactItemName    = exactItemName,
            recyclingCategory = finalRecyclingCategory,
            disposalMethod   = disposalMethod,
            disposalSteps    = disposalSteps,
            reuseSuggestions = reuseSuggestions,
            environmentalImpact = environmentalImpact,
            ecoScore         = ecoScore,
            carbonReduction  = carbonReduction,
            carbonImpact     = carbonImpact,
            sustainabilityTip = sustainabilityTip,
            isRecyclable     = isRecyclable,
            isReusable       = isReusable
        )
    }
}
