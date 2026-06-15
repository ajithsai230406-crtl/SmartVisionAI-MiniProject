package com.smartvision.ai.detector.presentation

import android.content.Context
import android.graphics.Bitmap
import android.media.Image
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartvision.ai.data.repository.FirebaseRepository
import com.smartvision.ai.data.repository.HistoryRepository
import com.smartvision.ai.detector.data.MlKitObjectDetectionRepository
import com.smartvision.ai.detector.data.ObjectDetectionRepository
import com.smartvision.ai.detector.domain.*
import com.smartvision.ai.data.repository.OpenRouterManager
import org.json.JSONObject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ObjectDetectorViewModel @Inject constructor(
    private val detectionRepo:   ObjectDetectionRepository,
    private val mlKitRepo:       MlKitObjectDetectionRepository,
    private val historyRepo:     HistoryRepository,
    private val firebase:        FirebaseRepository,
    private val openRouterManager: OpenRouterManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // ── Detection state ───────────────────────────────────────────────────────
    private val _detectionState = MutableStateFlow<DetectionState>(DetectionState.Idle)
    val detectionState: StateFlow<DetectionState> = _detectionState.asStateFlow()

    // ── Selected object for detail card ───────────────────────────────────────
    private val _selectedItem = MutableStateFlow<DetectedItem?>(null)
    val selectedItem: StateFlow<DetectedItem?> = _selectedItem.asStateFlow()

    // ── Knowledge for selected object ─────────────────────────────────────────
    private val _knowledge = MutableStateFlow<ObjectKnowledge?>(null)
    val knowledge: StateFlow<ObjectKnowledge?> = _knowledge.asStateFlow()

    // ── Detection history (current session) ───────────────────────────────────
    private val _sessionHistory = MutableStateFlow<List<DetectionHistoryEntry>>(emptyList())
    val sessionHistory: StateFlow<List<DetectionHistoryEntry>> = _sessionHistory.asStateFlow()

    // ── Flash & overlay toggles ───────────────────────────────────────────────
    private val _flashEnabled   = MutableStateFlow(false)
    val flashEnabled: StateFlow<Boolean> = _flashEnabled.asStateFlow()

    private val _showOverlay    = MutableStateFlow(true)
    val showOverlay: StateFlow<Boolean> = _showOverlay.asStateFlow()

    private val _isLive         = MutableStateFlow(true)
    val isLive: StateFlow<Boolean> = _isLive.asStateFlow()

    // ── FPS counter ───────────────────────────────────────────────────────────
    private val _fps = MutableStateFlow(0)
    val fps: StateFlow<Int> = _fps.asStateFlow()

    private var analysisJob:   Job? = null
    private var geminiJob:     Job? = null
    private var fpsFrameCount  = 0
    private var fpsLastTime    = System.currentTimeMillis()
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context) { /* ready */ }
        startFpsCounter()
    }

    private fun getCenterDistance(box1: DetectionBox, box2: DetectionBox): Float {
        val cx1 = (box1.left + box1.right) / 2f
        val cy1 = (box1.top + box1.bottom) / 2f
        val cx2 = (box2.left + box2.right) / 2f
        val cy2 = (box2.top + box2.bottom) / 2f
        val dx = cx1 - cx2
        val dy = cy1 - cy2
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    // ── Live frame analysis (called every camera frame) ───────────────────────
    suspend fun analyzeFrame(bitmap: Bitmap) = withContext(Dispatchers.Default) {
        if (!_isLive.value) return@withContext
        val t0 = System.currentTimeMillis()

        // Use custom TFLite object detector
        val items = detectionRepo.detectFromBitmap(bitmap).getOrDefault(emptyList())

        updateFps(t0)
        if (items.isEmpty()) {
            _detectionState.value = DetectionState.NoObjects
        } else {
            val inferenceTime = System.currentTimeMillis() - t0
            _detectionState.value = DetectionState.Active(
                items,
                inferenceTime,
                bitmap.width,
                bitmap.height
            )

            // Track and update the selected item with latest model outputs instantly
            val currentSelected = _selectedItem.value
            if (currentSelected != null) {
                val matchingItem = items
                    .filter { it.label == currentSelected.label }
                    .minByOrNull { getCenterDistance(it.box, currentSelected.box) }
                if (matchingItem != null) {
                    _selectedItem.value = matchingItem
                }
            } else {
                // Auto-select first item if none selected
                selectItem(items.first())
            }
        }
    }

    /** Detect from gallery URI */
    fun analyzeFromUri(context: Context, uri: Uri) = viewModelScope.launch {
        _isLive.value         = false
        _detectionState.value = DetectionState.Scanning

        var galleryBitmap: Bitmap? = null
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                galleryBitmap = android.graphics.BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val bitmap = galleryBitmap
        if (bitmap == null) {
            _detectionState.value = DetectionState.Error("Failed to load image from gallery")
            return@launch
        }

        mlKitRepo.detectFromBitmap(bitmap)
            .onSuccess { items ->
                if (items.isEmpty()) {
                    _detectionState.value = DetectionState.NoObjects
                } else {
                    _detectionState.value = DetectionState.Active(items, 0L, bitmap.width, bitmap.height)
                    selectItem(items.first(), bitmap)
                    addToSessionHistory(items)
                }
            }
            .onFailure { _detectionState.value = DetectionState.Error(it.message ?: "Failed to analyze image") }
    }

    /** Select a detected object to show detail card, with optional Gemini Vision integration */
    fun selectItem(item: DetectedItem, screenBitmap: Bitmap? = null) {
        _selectedItem.value = item
        val baseKnow = ObjectKnowledgeBase.getKnowledge(item.label)
        _knowledge.value    = baseKnow

        geminiJob?.cancel()
        if (screenBitmap == null) return

        geminiJob = viewModelScope.launch {
            try {
                // Crop the bitmap to the bounding box of the selected item!
                val srcW = screenBitmap.width
                val srcH = screenBitmap.height
                val cropLeft   = (item.box.left   * srcW).toInt().coerceIn(0, srcW - 1)
                val cropTop    = (item.box.top    * srcH).toInt().coerceIn(0, srcH - 1)
                val cropRight  = (item.box.right  * srcW).toInt().coerceIn(0, srcW - 1)
                val cropBottom = (item.box.bottom * srcH).toInt().coerceIn(0, srcH - 1)
                
                val cropW = (cropRight - cropLeft).coerceAtLeast(1)
                val cropH = (cropBottom - cropTop).coerceAtLeast(1)
                
                val cropped = Bitmap.createBitmap(screenBitmap, cropLeft, cropTop, cropW, cropH)

                val prompt = """
                    You are an advanced AI visual scanner. You are analyzing this cropped photo of a detected object.
                    Determine EXACTLY what the object is, and give a highly informative, premium AI overview.
                    Never return generic labels like "object", "thing", "item", "other", or "unknown".
                    
                    Provide your response EXACTLY as a JSON object with these keys:
                    {
                      "label": "exact name, e.g., Wireless Earbuds Charging Case, Scientific Calculator, Plastic Water Bottle",
                      "description": "Premium 1-2 sentence explanation of what this object is and does",
                      "uses": ["use 1", "use 2", "use 3"],
                      "safety": "Important safety warning or child safety advisory for this object",
                      "maintenanceTips": ["tip 1", "tip 2", "tip 3"],
                      "buyingSuggestions": ["suggestion 1", "suggestion 2"],
                      "environmental": "Eco impact, carbon footprint details, or recycling disposal instructions",
                      "relatedRecommendations": ["related item 1", "related item 2"],
                      "specializedActionType": "medicine" or "food" or "textbook" or "waste" or "electronics" or null (strictly classify it based on item purpose),
                      "specializedActionData": "custom dynamic data (e.g. detailed nutrition estimates 'Calories: 95 kcal · Carbs: 25g' if food, specific diagnostic metrics if electronics, paracetamol dosage warning if medicine, book chapter guide if textbook)"
                    }
                """.trimIndent()

                val jsonText = try {
                    openRouterManager.generateContent(
                        prompt = prompt,
                        image = cropped,
                        modelName = "openrouter/auto"
                    )
                } catch (e: Exception) {
                    null
                }

                if (jsonText != null) {
                    val cleanJson = jsonText.substringAfter("{").substringBeforeLast("}")
                    val fullJson = "{$cleanJson}"
                    val json = JSONObject(fullJson)

                    val label = json.optString("label", baseKnow.label)
                    val description = json.optString("description", baseKnow.description)
                    val safety = json.optString("safety", baseKnow.safetyInfo)
                    val environmental = json.optString("environmental", baseKnow.environmentalImpact)
                    val funFact = json.optString("funFact", baseKnow.funFact)
                    val specializedActionType = if (json.has("specializedActionType")) json.optString("specializedActionType") else baseKnow.specializedActionType
                    val specializedActionData = if (json.has("specializedActionData")) json.optString("specializedActionData") else baseKnow.specializedActionData

                    val usesJson = json.optJSONArray("uses")
                    val usesList = if (usesJson != null) {
                        List(usesJson.length()) { usesJson.getString(it) }
                    } else {
                        baseKnow.uses
                    }

                    val maintenanceJson = json.optJSONArray("maintenanceTips")
                    val maintenanceList = if (maintenanceJson != null) {
                        List(maintenanceJson.length()) { maintenanceJson.getString(it) }
                    } else {
                        baseKnow.maintenanceTips
                    }

                    val buyingJson = json.optJSONArray("buyingSuggestions")
                    val buyingList = if (buyingJson != null) {
                        List(buyingJson.length()) { buyingJson.getString(it) }
                    } else {
                        baseKnow.buyingSuggestions
                    }

                    val relatedJson = json.optJSONArray("relatedRecommendations")
                    val relatedList = if (relatedJson != null) {
                        List(relatedJson.length()) { relatedJson.getString(it) }
                    } else {
                        baseKnow.relatedRecommendations
                    }

                    if (_selectedItem.value?.id == item.id) {
                        val newCategory = label.toCategory()
                        val updatedItem = item.copy(
                            label = label,
                            category = newCategory,
                            accentColor = newCategory.colorArgb
                        )
                        _selectedItem.value = updatedItem
                        _knowledge.value = ObjectKnowledge(
                            label = label,
                            category = newCategory,
                            description = description,
                            uses = usesList,
                            safetyInfo = safety,
                            environmentalImpact = environmental,
                            funFact = funFact,
                            emoji = baseKnow.emoji,
                            maintenanceTips = maintenanceList,
                            buyingSuggestions = buyingList,
                            relatedRecommendations = relatedList,
                            specializedActionType = if (specializedActionType == "null" || specializedActionType.isNullOrEmpty()) null else specializedActionType,
                            specializedActionData = if (specializedActionData == "null" || specializedActionData.isNullOrEmpty()) null else specializedActionData
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // In case of error (e.g. 403 permission denied), we safely keep the enriched local database knowledge!
                if (_selectedItem.value?.id == item.id) {
                    val fallbackKnow = ObjectKnowledgeBase.getKnowledge(item.label)
                    val newCategory = fallbackKnow.category
                    val updatedItem = item.copy(
                        label = fallbackKnow.label,
                        category = newCategory,
                        accentColor = newCategory.colorArgb
                    )
                    _selectedItem.value = updatedItem
                    _knowledge.value = fallbackKnow
                }
            }
        }
    }

    fun dismissDetail() {
        _selectedItem.value = null
        _knowledge.value    = null
    }

    fun resumeLive() {
        _isLive.value         = true
        _selectedItem.value   = null
        _knowledge.value      = null
        _detectionState.value = DetectionState.Idle
    }

    /** Speak the object name and description */
    fun speakObject(item: DetectedItem) {
        val know = ObjectKnowledgeBase.getKnowledge(item.label)
        val text = "${item.label}. ${know.description}"
        tts?.language = Locale.ENGLISH
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        viewModelScope.launch {
            // Generic history entry (for in-app History tab)
            historyRepo.save(
                "Object",
                "${item.label} detected (${(item.confidence * 100).toInt()}%)",
                know.description.take(220)
            )
            // Structured Firebase entry with rich object detection data
            firebase.saveObjectDetection(
                objectName  = item.label,
                confidence  = item.confidence,
                category    = item.category.name,
                description = know.description,
                uses        = know.uses.joinToString(", ")
            )
        }
    }

    fun toggleFlash()   { _flashEnabled.update { !it } }
    fun toggleOverlay() { _showOverlay.update  { !it } }
    fun dismissError()  { _detectionState.value = DetectionState.Idle }

    // ── Session history helpers ────────────────────────────────────────────────
    private fun addToSessionHistory(items: List<DetectedItem>) {
        val entries = items.map {
            DetectionHistoryEntry(
                label      = it.label,
                confidence = it.confidence,
                category   = it.category,
                timestamp  = System.currentTimeMillis()
            )
        }
        _sessionHistory.update { (entries + it).take(30) }
    }

    private fun updateFps(frameStartMs: Long) {
        fpsFrameCount++
        val elapsed = System.currentTimeMillis() - fpsLastTime
        if (elapsed >= 1000L) {
            _fps.value    = fpsFrameCount
            fpsFrameCount = 0
            fpsLastTime   = System.currentTimeMillis()
        }
    }

    private fun startFpsCounter() {
        viewModelScope.launch {
            while (true) {
                delay(1000)
                // fps updated inside updateFps()
            }
        }
    }

    fun stopSpeaking() {
        tts?.stop()
    }

    override fun onCleared() {
        super.onCleared()
        tts?.stop()
        tts?.shutdown()
        analysisJob?.cancel()
    }
}

// ─── Session history entry ────────────────────────────────────────────────────
data class DetectionHistoryEntry(
    val label:      String,
    val confidence: Float,
    val category:   ObjectCategory,
    val timestamp:  Long
)
