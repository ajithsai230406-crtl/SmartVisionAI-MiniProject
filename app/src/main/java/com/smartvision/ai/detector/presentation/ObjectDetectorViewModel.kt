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
import org.json.JSONArray
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
    private val screenContextManager: com.smartvision.ai.data.ScreenContextManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private var selectedCroppedBitmap: Bitmap? = null

    private fun saveBitmapToCache(bitmap: Bitmap): String? {
        return try {
            val file = java.io.File(context.cacheDir, "visual_search_${System.currentTimeMillis()}.jpg")
            java.io.FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            android.util.Log.e("ObjectDetector", "Failed to save bitmap to cache", e)
            null
        }
    }

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
    private var latestBitmap: Bitmap? = null
    private val refinedCache = mutableMapOf<Int, RefinedInfo>()

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
        latestBitmap = bitmap
        val t0 = System.currentTimeMillis()

        // Use custom TFLite object detector
        val items = detectionRepo.detectFromBitmap(bitmap).getOrDefault(emptyList())

        // Map items using spatial matching against refined cache
        val mappedItems = items.map { item ->
            val matchingRefined = refinedCache.values
                .filter { getCenterDistance(item.box, it.lastBox) < 0.38f }
                .minByOrNull { getCenterDistance(item.box, it.lastBox) }
                
            if (matchingRefined != null) {
                matchingRefined.lastBox = item.box
                item.copy(
                    label = matchingRefined.label,
                    category = matchingRefined.category,
                    accentColor = matchingRefined.accentColor
                )
            } else {
                item
            }
        }

        updateFps(t0)
        if (mappedItems.isEmpty()) {
            _detectionState.value = DetectionState.NoObjects
        } else {
            val inferenceTime = System.currentTimeMillis() - t0
            _detectionState.value = DetectionState.Active(
                mappedItems,
                inferenceTime,
                bitmap.width,
                bitmap.height
            )

            // Track and update the selected item with latest model outputs instantly
            val currentSelected = _selectedItem.value
            if (currentSelected != null) {
                // Track spatially: find the item in the new frame closest to the previously selected item's box
                val matchingItem = mappedItems
                    .minByOrNull { getCenterDistance(it.box, currentSelected.box) }
                if (matchingItem != null && getCenterDistance(matchingItem.box, currentSelected.box) < 0.38f) {
                    // Update coordinates and confidence, but preserve refined fields and tracked ID
                    _selectedItem.value = matchingItem.copy(
                        id = currentSelected.id,
                        label = currentSelected.label,
                        category = currentSelected.category,
                        accentColor = currentSelected.accentColor
                    )
                } else {
                    _selectedItem.value = null
                    _knowledge.value = null
                }
            } else {
                // Auto-select the item closest to the center reticle (0.5f, 0.5f)
                val centerBox = DetectionBox(0.5f, 0.5f, 0.5f, 0.5f)
                val closestToCenter = mappedItems.minByOrNull { getCenterDistance(it.box, centerBox) }
                if (closestToCenter != null && getCenterDistance(closestToCenter.box, centerBox) < 0.45f) {
                    selectItem(closestToCenter, bitmap, isAutoSelect = true)
                }
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
    fun selectItem(item: DetectedItem, screenBitmap: Bitmap? = null, isAutoSelect: Boolean = false) {
        _selectedItem.value = item
        val baseKnow = ObjectKnowledgeBase.getKnowledge(item.label)
        _knowledge.value    = baseKnow

        geminiJob?.cancel()
        val bitmapToUse = screenBitmap ?: latestBitmap
        if (bitmapToUse == null) return

        geminiJob = viewModelScope.launch {
            if (isAutoSelect) {
                delay(350)
            }
            try {
                if (_selectedItem.value?.id != item.id) return@launch

                // Crop the bitmap to the bounding box of the selected item!
                val srcW = bitmapToUse.width
                val srcH = bitmapToUse.height
                val cropLeft   = (item.box.left   * srcW).toInt().coerceIn(0, srcW - 1)
                val cropTop    = (item.box.top    * srcH).toInt().coerceIn(0, srcH - 1)
                val cropRight  = (item.box.right  * srcW).toInt().coerceIn(0, srcW - 1)
                val cropBottom = (item.box.bottom * srcH).toInt().coerceIn(0, srcH - 1)
                
                val cropW = (cropRight - cropLeft).coerceAtLeast(1)
                val cropH = (cropBottom - cropTop).coerceAtLeast(1)
                
                val cropped = Bitmap.createBitmap(bitmapToUse, cropLeft, cropTop, cropW, cropH)
                selectedCroppedBitmap = cropped

                val prompt = """
                    You are an advanced Smart Visual Search assistant like Google Lens.
                    Analyze this cropped photo of a detected object and extract highly detailed search and product specifications.
                    Never return generic labels. Be extremely specific.
                    
                    Provide your response EXACTLY as a JSON object with these keys:
                    {
                      "label": "Exact name of the object, product, book, plant, animal, or gadget",
                      "category": "One of: PERSON, ANIMAL, VEHICLE, FOOD, ELECTRONICS, FURNITURE, SPORTS, NATURE, HOUSEHOLD, BOOK, PLANT, CLOTHES, TOOL, MONUMENT, PRODUCT, TOY, PACKAGING, OTHER",
                      "description": "Concise summary of what this object is and does",
                      "uses": ["use case 1", "use case 2"],
                      "safetyInfo": "Important safety warning or care tips for this object",
                      "environmentalImpact": "Eco impact, carbon footprint details, or recycling instructions",
                      "funFact": "An interesting trivia or background fact",
                      "emoji": "Representative emoji",
                      
                      // Visual Search Lens details
                      "aiSummary": "An engaging AI summary of the visual search results",
                      "specifications": {
                        "Specification A": "Value A",
                        "Specification B": "Value B"
                      },
                      "alternatives": ["Alternative brand/model 1", "Alternative 2"],
                      "brandsAndModels": ["Popular Brand X Model Y", "Brand Z Model W"],
                      "manufacturerInfo": "Details about makers, origins or manufacturing of the object",
                      "ratings": "e.g., 4.7/5 stars from reviews",
                      "priceRange": "e.g., $15 - $25, or N/A",
                      
                      // Category specifics
                      "brand": "e.g., Apple, Nike, Heinz, or null",
                      "ingredients": ["Ingredient 1", "Ingredient 2"] or null,
                      "weight": "e.g., 500g, 1.5 kg, or null",
                      "variants": ["Storage/size/color options"] or null,
                      "launchInfo": "Release year, price, or history",
                      "author": "Author name (if book) or null",
                      "scientificName": "Scientific/Latin name (if plant/animal) or null",
                      "characteristics": ["Physical characteristic 1", "characteristic 2"] or null,
                      "habitat": "Native habitat/region or null",
                      "careInfo": "Care instructions, storage conditions, or null",
                      "calories": "e.g., 150 kcal or null",
                      "nutritionalValues": {
                        "Carbs": "20g",
                        "Protein": "3g"
                      } or null,
                      "healthyAlternatives": ["Healthy alternative 1", "Healthy alternative 2"] or null,
                      
                      // Google Lens-style Multiple Search Cards
                      "webResults": [
                        {
                          "title": "Official website/wiki title",
                          "snippet": "Short descriptive snippet about the website content.",
                          "url": "https://www.google.com/search?q=query"
                        }
                      ],
                      "similarImages": [
                        {
                          "title": "Alternative layout",
                          "imageUrl": "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=200",
                          "sourceUrl": "https://www.google.com/search?tbm=isch&q=query"
                        }
                      ],
                      "relatedProducts": [
                        {
                          "name": "Related model X",
                          "brand": "Brand name",
                          "price": "approx. $10",
                          "rating": "4.5★",
                          "url": "https://www.google.com/search?q=buy+related"
                        }
                      ]
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
                    val categoryName = json.optString("category", baseKnow.category.name)
                    val newCategory = runCatching { ObjectCategory.valueOf(categoryName) }.getOrDefault(label.toCategory())
                    
                    val description = json.optString("description", baseKnow.description)
                    val safety = json.optString("safetyInfo", baseKnow.safetyInfo)
                    val environmental = json.optString("environmentalImpact", baseKnow.environmentalImpact)
                    val funFact = json.optString("funFact", baseKnow.funFact)
                    val emoji = json.optString("emoji", baseKnow.emoji)
                    
                    val aiSummary = json.optString("aiSummary", "")
                    val manufacturerInfo = json.optString("manufacturerInfo", "")
                    val ratings = json.optString("ratings", "")
                    val priceRange = json.optString("priceRange", "")
                    val brand = json.optString("brand", "")
                    val weight = json.optString("weight", "")
                    val launchInfo = json.optString("launchInfo", "")
                    val author = json.optString("author", "")
                    val scientificName = json.optString("scientificName", "")
                    val habitat = json.optString("habitat", "")
                    val careInfo = json.optString("careInfo", "")
                    val calories = json.optString("calories", "")
                    
                    val specifications = mutableMapOf<String, String>()
                    val specsObj = json.optJSONObject("specifications")
                    if (specsObj != null) {
                        specsObj.keys().forEach { k -> specifications[k] = specsObj.getString(k) }
                    }
                    
                    val nutritionalValues = mutableMapOf<String, String>()
                    val nutObj = json.optJSONObject("nutritionalValues")
                    if (nutObj != null) {
                        nutObj.keys().forEach { k -> nutritionalValues[k] = nutObj.getString(k) }
                    }
                    
                    val usesJson = json.optJSONArray("uses")
                    val usesList = if (usesJson != null) List(usesJson.length()) { usesJson.getString(it) } else baseKnow.uses
                    
                    val alternatives = if (json.has("alternatives")) {
                        val arr = json.optJSONArray("alternatives")
                        if (arr != null) List(arr.length()) { arr.getString(it) } else emptyList()
                    } else emptyList()
                    
                    val brandsAndModels = if (json.has("brandsAndModels")) {
                        val arr = json.optJSONArray("brandsAndModels")
                        if (arr != null) List(arr.length()) { arr.getString(it) } else emptyList()
                    } else emptyList()
                    
                    val ingredients = if (json.has("ingredients")) {
                        val arr = json.optJSONArray("ingredients")
                        if (arr != null) List(arr.length()) { arr.getString(it) } else emptyList()
                    } else emptyList()
                    
                    val variants = if (json.has("variants")) {
                        val arr = json.optJSONArray("variants")
                        if (arr != null) List(arr.length()) { arr.getString(it) } else emptyList()
                    } else emptyList()
                    
                    val characteristics = if (json.has("characteristics")) {
                        val arr = json.optJSONArray("characteristics")
                        if (arr != null) List(arr.length()) { arr.getString(it) } else emptyList()
                    } else emptyList()
                    
                    val healthyAlternatives = if (json.has("healthyAlternatives")) {
                        val arr = json.optJSONArray("healthyAlternatives")
                        if (arr != null) List(arr.length()) { arr.getString(it) } else emptyList()
                    } else emptyList()
                    
                    val webList = mutableListOf<WebResult>()
                    val webArr = json.optJSONArray("webResults")
                    if (webArr != null) {
                        for (i in 0 until webArr.length()) {
                            val o = webArr.getJSONObject(i)
                            webList.add(WebResult(o.optString("title", ""), o.optString("snippet", ""), o.optString("url", "")))
                        }
                    }
                    
                    val imgList = mutableListOf<SimilarImage>()
                    val imgArr = json.optJSONArray("similarImages")
                    if (imgArr != null) {
                        for (i in 0 until imgArr.length()) {
                            val o = imgArr.getJSONObject(i)
                            imgList.add(SimilarImage(o.optString("title", ""), o.optString("imageUrl", ""), o.optString("sourceUrl", "")))
                        }
                    }
                    
                    val prodList = mutableListOf<RelatedProduct>()
                    val prodArr = json.optJSONArray("relatedProducts")
                    if (prodArr != null) {
                        for (i in 0 until prodArr.length()) {
                            val o = prodArr.getJSONObject(i)
                            prodList.add(RelatedProduct(o.optString("name", ""), o.optString("brand", ""), o.optString("price", ""), o.optString("rating", ""), o.optString("url", "")))
                        }
                    }
                    
                    val searchData = VisualSearchData(
                        specifications = specifications,
                        alternatives = alternatives,
                        brandsAndModels = brandsAndModels,
                        manufacturerInfo = manufacturerInfo,
                        ratings = ratings,
                        priceRange = priceRange,
                        brand = brand,
                        ingredients = ingredients,
                        weight = weight,
                        variants = variants,
                        launchInfo = launchInfo,
                        author = author,
                        scientificName = scientificName,
                        characteristics = characteristics,
                        habitat = habitat,
                        careInfo = careInfo,
                        calories = calories,
                        nutritionalValues = nutritionalValues,
                        healthyAlternatives = healthyAlternatives,
                        webResults = webList,
                        similarImages = imgList,
                        relatedProducts = prodList,
                        aiSummary = aiSummary
                    )

                    if (_selectedItem.value?.id == item.id) {
                        val updatedItem = _selectedItem.value?.copy(
                            label = label,
                            category = newCategory,
                            accentColor = newCategory.colorArgb
                        ) ?: item.copy(
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
                            emoji = emoji,
                            maintenanceTips = baseKnow.maintenanceTips,
                            buyingSuggestions = baseKnow.buyingSuggestions,
                            relatedRecommendations = baseKnow.relatedRecommendations,
                            specializedActionType = null,
                            specializedActionData = null,
                            searchData = searchData
                        )
                        
                        // Emit context for floating AI assistant
                        screenContextManager.updateContext(
                            com.smartvision.ai.data.ScreenContext.ObjectDetectorContext(
                                detectedObjects = listOf(label),
                                confidences = listOf(item.confidence),
                                objectCount = 1,
                                selectedLabel = label,
                                selectedDescription = description,
                                searchSummary = aiSummary
                            ),
                            moduleName = "Visual Search"
                        )
                        
                        // Add to refined cache
                        refinedCache[item.id] = RefinedInfo(
                            label = label,
                            category = newCategory,
                            accentColor = newCategory.colorArgb,
                            lastBox = item.box
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
        refinedCache.clear()
    }

    fun runHighAccuracyAiScan(bitmap: Bitmap) = viewModelScope.launch {
        _isLive.value = false
        _detectionState.value = DetectionState.Scanning
        
        try {
            val prompt = """
                You are an expert AI object detection model specialized in everyday real-world objects.
                Analyze the provided image and identify ALL clearly visible objects.
                Return ONLY a JSON object (no markdown, no extra text):
                {
                  "objects": [
                    {
                      "label": "Metal Water Bottle",
                      "confidence": 0.95,
                      "ymin": 0.45,
                      "xmin": 0.2,
                      "ymax": 0.75,
                      "xmax": 0.8
                    }
                  ]
                }
            """.trimIndent()

            val jsonText = openRouterManager.generateContent(
                prompt = prompt,
                image = bitmap,
                modelName = "openrouter/auto"
            )

            val cleanJson = jsonText.substringAfter("{").substringBeforeLast("}")
            val fullJson = "{$cleanJson}"
            val json = JSONObject(fullJson)
            val jsonArray = json.getJSONArray("objects")
            
            val items = mutableListOf<DetectedItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val label = obj.getString("label")
                val confidence = obj.getDouble("confidence").toFloat()
                val ymin = obj.getDouble("ymin").toFloat().coerceIn(0f, 1f)
                val xmin = obj.getDouble("xmin").toFloat().coerceIn(0f, 1f)
                val ymax = obj.getDouble("ymax").toFloat().coerceIn(0f, 1f)
                val xmax = obj.getDouble("xmax").toFloat().coerceIn(0f, 1f)
                
                val category = label.toCategory()
                val box = DetectionBox(xmin, ymin, xmax, ymax)
                items.add(
                    DetectedItem(
                        id = i,
                        label = label,
                        confidence = confidence,
                        box = box,
                        category = category,
                        accentColor = category.colorArgb
                    )
                )
            }
            
            if (items.isEmpty()) {
                _detectionState.value = DetectionState.NoObjects
            } else {
                _detectionState.value = DetectionState.Active(
                    items = items,
                    frameMs = 0L,
                    frameWidth = bitmap.width,
                    frameHeight = bitmap.height
                )
                // Auto-select the first item
                selectItem(items.first(), bitmap)
                addToSessionHistory(items)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _detectionState.value = DetectionState.Error("AI Scan failed: ${e.message ?: "Unknown error"}")
        }
    }

    /** Speak the object name and description */
    fun speakObject(item: DetectedItem) {
        val know = _knowledge.value ?: ObjectKnowledgeBase.getKnowledge(item.label)
        val text = "${item.label}. ${know.description}"
        tts?.language = Locale.ENGLISH
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        viewModelScope.launch {
            val path = selectedCroppedBitmap?.let { saveBitmapToCache(it) }
            val objHistJson = com.smartvision.ai.domain.model.HistoryJsonSerializer.serializeObject(
                com.smartvision.ai.domain.model.ObjectHistoryDetail(
                    label = know.label,
                    confidence = item.confidence,
                    category = know.category.name,
                    description = know.description,
                    uses = know.uses,
                    safetyInfo = know.safetyInfo,
                    environmentalImpact = know.environmentalImpact,
                    funFact = know.funFact,
                    emoji = know.emoji,
                    maintenanceTips = know.maintenanceTips,
                    buyingSuggestions = know.buyingSuggestions,
                    imagePath = path,
                    searchData = know.searchData
                )
            )
            // Generic history entry (for in-app History tab)
            historyRepo.save(
                "Object",
                "${item.label} detected (${(item.confidence * 100).toInt()}%)",
                know.description.take(220),
                objHistJson
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
        latestBitmap = null
    }
}

// ─── Session history entry ────────────────────────────────────────────────────
data class DetectionHistoryEntry(
    val label:      String,
    val confidence: Float,
    val category:   ObjectCategory,
    val timestamp:  Long
)

private data class RefinedInfo(
    val label: String,
    val category: ObjectCategory,
    val accentColor: Long,
    var lastBox: DetectionBox
)
