package com.smartvision.ai.medicine.data

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.speech.tts.TextToSpeech
import com.smartvision.ai.BuildConfig
import com.smartvision.ai.data.repository.FirebaseRepository
import com.smartvision.ai.data.repository.HistoryRepository
import com.smartvision.ai.data.repository.FallbackApiClient
import com.smartvision.ai.data.repository.OpenRouterManager
import com.smartvision.ai.medicine.domain.DISCLAIMER
import com.smartvision.ai.medicine.domain.MedicineDatabase
import com.smartvision.ai.medicine.domain.MedicineInfo
import com.smartvision.ai.medicine.domain.MedicineScanRecord
import com.smartvision.ai.medicine.domain.MedicineType
import com.smartvision.ai.medicine.domain.SafetyLevel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import com.smartvision.ai.data.db.GeminiCacheDao
import com.smartvision.ai.data.db.GeminiCacheEntity
import com.smartvision.ai.util.retryWithDelay
import java.util.Locale
import android.graphics.Bitmap
import com.smartvision.ai.ocr.data.OcrRecognitionRepository
import com.smartvision.ai.ocr.data.OcrScript
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicineScannerRepository @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val historyRepository: HistoryRepository,
        private val firebase:          FirebaseRepository,
    private val cacheDao: GeminiCacheDao,
    private val fallbackApiClient: FallbackApiClient,
    private val ocrRecognitionRepository: OcrRecognitionRepository,
    private val openRouterManager: OpenRouterManager
) {

    // ── TextToSpeech ──────────────────────────────────────────────────────────
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    fun initTts(onReady: () -> Unit) {
        if (tts != null) { if (ttsReady) onReady(); return }
        tts = TextToSpeech(ctx) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) {
                tts?.language = Locale.ENGLISH
                onReady()
            }
        }
    }

    fun speak(text: String) {
        if (!ttsReady) return
        tts?.stop()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "medicine_tts")
    }

    fun stopSpeaking() { tts?.stop() }
    fun shutdownTts()  { tts?.shutdown(); tts = null; ttsReady = false }

    // ── Scan history (in-memory + SharedPreferences for persistence) ──────────
    private val prefs: SharedPreferences =
        ctx.getSharedPreferences("medicine_history", Context.MODE_PRIVATE)

    private val _history = MutableStateFlow<List<MedicineScanRecord>>(emptyList())
    val history: StateFlow<List<MedicineScanRecord>> = _history

    private val _favorites = MutableStateFlow<List<MedicineScanRecord>>(emptyList())
    val favorites: StateFlow<List<MedicineScanRecord>> = _favorites

    init {
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            val loadedHistory = loadHistory()
            _history.value = loadedHistory
            _favorites.value = loadedHistory.filter { it.isFavorite }
        }
    }

    // ── Connectivity check ──
    private fun isOnline(): Boolean {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    data class BrandGenericResult(val brandName: String?, val genericName: String?)

    fun identifyBrandAndGeneric(ocrText: String): BrandGenericResult {
        val cleaned = ocrText.lowercase().trim()
        
        // 1. Scan the DailyMedicinesDatabase first for matches
        for (med in DailyMedicinesDatabase.dailyMedicines) {
            val name = med.name.lowercase()
            val brand = med.brandName.lowercase()
            val generic = med.genericName.lowercase()
            
            // Check for exact word matches or clear subsegment containing
            if (cleaned.contains(generic) || cleaned.contains(brand) || cleaned.contains(name)) {
                return BrandGenericResult(med.brandName, med.genericName)
            }
        }
        
        // 2. Scan the procedural MedicineDatabase
        for (med in MedicineDatabase.medicines) {
            val name = med.name.lowercase()
            val brand = med.brandName.lowercase()
            val generic = med.genericName.lowercase()
            
            if (cleaned.contains(generic) || cleaned.contains(brand) || cleaned.contains(name)) {
                return BrandGenericResult(med.brandName, med.genericName)
            }
        }
        
        return BrandGenericResult(null, null)
    }

    private suspend fun identifyBrandAndGenericOnline(ocrText: String): BrandGenericResult = withContext(Dispatchers.IO) {
        if (!isOnline()) {
            return@withContext identifyBrandAndGeneric(ocrText)
        }
        
        val prompt = """
            You are a medical product OCR analyzer. Extract the brand name and generic/active ingredient name of the medicine from this raw OCR text:
            "$ocrText"
            
            Guidelines:
            - Brand Name is the commercial name (e.g. Tylenol, Advil, Lipitor).
            - Generic Name is the chemical active ingredient (e.g. Acetaminophen, Ibuprofen, Atorvastatin).
            - If one or both are not present in the text, return null for that field.
            
            Respond strictly in valid JSON (no markdown, no backticks):
            {
              "brandName": "extracted brand name or null",
              "genericName": "extracted generic name or null"
            }
        """.trimIndent()
        
        try {
            val promptHash = prompt.hashCode()
            val cachedResult = cacheDao.getCache(promptHash)
            val jsonText = if (cachedResult != null) {
                cachedResult.response
            } else {
                val responseText = openRouterManager.generateContent(prompt, modelName = "openrouter/auto")
                cacheDao.insertCache(GeminiCacheEntity(promptHash, prompt, responseText))
                responseText
            }

            if (!jsonText.isNullOrBlank()) {
                val cleanedJson = jsonText.trim()
                    .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val j = JSONObject(cleanedJson)
                val brand = j.optString("brandName", "").takeIf { it.isNotBlank() && it != "null" }
                val generic = j.optString("genericName", "").takeIf { it.isNotBlank() && it != "null" }
                return@withContext BrandGenericResult(brand, generic)
            }
        } catch (e: Exception) {
            // fallback
        }
        identifyBrandAndGeneric(ocrText)
    }

    // ── Medicine Detection Heuristic ───────────────────────────────────────────
    private val MEDICINE_KEYWORDS = setOf(
        "mg", "ml", "mcg", "tablet", "tablets", "capsule", "capsules", "syrup",
        "injection", "ointment", "drops", "inhaler", "patch", "powder", "suppository",
        "dosage", "dose", "composition", "each", "contains", "active ingredient",
        "excipient", "rx", "prescription", "otc", "medicine", "drug", "pharmaceutical",
        "expiry", "exp", "mfg", "manufactured", "batch", "lot", "storage", "store below",
        "keep out of reach", "side effects", "contraindications", "indications",
        "warnings", "precautions", "directions", "use as directed",
        "paracetamol", "ibuprofen", "amoxicillin", "cetirizine", "omeprazole",
        "metformin", "atorvastatin", "pantoprazole", "azithromycin", "aspirin",
        "acetaminophen", "antibiotic", "analgesic", "antipyretic", "antacid",
        "usp", "ip", "bp", "nf", "pharmacopoeia"
    )

    fun isMedicineOrProduct(ocrText: String): Boolean {
        val lower = ocrText.lowercase()
        var matchCount = 0
        for (keyword in MEDICINE_KEYWORDS) {
            if (lower.contains(keyword)) matchCount++
            if (matchCount >= 2) return true
        }
        // Also check if local DB recognizes any brand/generic name
        val parseResult = identifyBrandAndGeneric(ocrText)
        return parseResult.brandName != null || parseResult.genericName != null
    }

    private suspend fun verifyWithAi(ocrText: String): Boolean {
        if (!isOnline()) return false
        return try {
            val prompt = """
                You are an image content classifier. Given this OCR text extracted from a photo, determine if the text comes from a medicine package, pharmaceutical product, health supplement label, or any medical product.
                
                OCR Text: "$ocrText"
                
                Reply ONLY with "YES" if this is from a medicine/health product, or "NO" if it appears to be from a random object, book, sign, receipt, or non-medical item.
            """.trimIndent()
            val response = openRouterManager.generateContent(prompt, modelName = "openrouter/auto")
            response.trim().uppercase().startsWith("YES")
        } catch (e: Exception) {
            false
        }
    }

    // ── Core: Analyze from Bitmap (with preprocessing, OCR & extraction) ──────
    suspend fun analyzeFromBitmap(bitmap: Bitmap, onProgress: (Float) -> Unit): Result<MedicineInfo> =
        withContext(Dispatchers.IO) {
            onProgress(0.1f)
            
            // Upload medicine image to Firebase Storage
            var downloadUrl: String? = null
            try {
                val baos = java.io.ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos)
                val bytes = baos.toByteArray()
                val uid = firebase.getCurrentUid()
                val storagePath = "users/$uid/medicine/med_${System.currentTimeMillis()}.jpg"
                val uploadResult = firebase.uploadFileBytes(storagePath, bytes)
                downloadUrl = uploadResult.getOrNull()
            } catch (e: Exception) {
                // Ignore upload failure for robust scan execution
            }

            // 1. Run multi-script OCR with preprocessing
            val ocrResult = ocrRecognitionRepository.recognizeFromBitmap(bitmap, OcrScript.LATIN, preprocess = true)
                .getOrElse {
                    return@withContext Result.failure(it)
                }
            
            onProgress(0.3f)
            val ocrText = ocrResult.text.trim()

            // 2. Compute average OCR confidence
            var totalConfidence = 0f
            var count = 0
            for (block in ocrResult.textBlocks) {
                for (line in block.lines) {
                    for (element in line.elements) {
                        totalConfidence += element.confidence
                        count++
                    }
                }
            }
            val avgConfidence = if (count > 0) totalConfidence / count else 0f

            // Detailed Log: OCR Extraction
            android.util.Log.i("MedicineScanner", "[OCR Extraction] Raw Text:\n$ocrText\n[OCR Metrics] Image dimensions: ${bitmap.width}x${bitmap.height}, Average Element Confidence: $avgConfidence")

            if (ocrText.isBlank()) {
                return@withContext Result.failure(Exception("No text detected. Please recapture with a clearer image of the medicine package."))
            }

            // Low confidence guard
            if (avgConfidence < 0.45f) {
                return@withContext Result.failure(Exception("OCR confidence is too low (${(avgConfidence * 100).toInt()}%). Please recapture the image in a well-lit area and make sure the text is in focus."))
            }

            onProgress(0.45f)

            // 3. Medicine detection gate — reject non-medicine objects
            val isMedicine = isMedicineOrProduct(ocrText)
            if (!isMedicine) {
                // Second chance: ask AI to verify
                val aiVerified = verifyWithAi(ocrText)
                if (!aiVerified) {
                    return@withContext Result.failure(
                        MedicineRejectionException("No medicine or supported product detected. The scanned image does not appear to contain a medicine label or health product packaging.")
                    )
                }
            }

            onProgress(0.6f)

            // 4. Extract medicine name
            val medName = extractMedicineName(ocrText)
            android.util.Log.i("MedicineScanner", "[OCR Extraction] Identified Medicine Name: '$medName'")

            onProgress(0.8f)

            // 5. Local Database Matching
            var offlineMatch: MedicineInfo? = null
            
            // Search local databases first
            val brand = medName
            offlineMatch = DailyMedicinesDatabase.dailyMedicines.firstOrNull {
                it.brandName.equals(brand, ignoreCase = true) || it.name.contains(brand, true)
            }
            if (offlineMatch == null) {
                offlineMatch = DailyMedicinesDatabase.dailyMedicines.firstOrNull {
                    it.genericName.equals(brand, ignoreCase = true)
                }
            }
            if (offlineMatch == null) {
                offlineMatch = MedicineDatabase.findByName(brand)
            }

            // 6. FDA and Gemini analysis
            val result = analyzeWithFdaAndGemini(medName, rawOcrText = ocrText, offlineMatch = offlineMatch, imageUrl = downloadUrl)
            onProgress(1.0f)
            result
        }

    /** Custom exception for non-medicine rejections so ViewModel can distinguish it */
    class MedicineRejectionException(message: String) : Exception(message)

    private suspend fun extractMedicineName(ocrText: String): String {
        // Try local matching first
        val parseResult = identifyBrandAndGeneric(ocrText)
        if (parseResult.brandName != null) return parseResult.brandName
        if (parseResult.genericName != null) return parseResult.genericName

        // Call Gemini online to identify brand and generic names
        val onlineResult = identifyBrandAndGenericOnline(ocrText)
        val name = onlineResult.brandName ?: onlineResult.genericName
        if (!name.isNullOrBlank()) return name

        // Fallback: first non-empty line of OCR text
        val firstLine = ocrText.lines().map { it.trim() }.firstOrNull { it.isNotBlank() }
        return firstLine ?: ocrText
    }

    // ── Core: Analyze from OCR text ───────────────────────────────────────────
    suspend fun analyzeFromOcrText(ocrText: String): Result<MedicineInfo> =
        withContext(Dispatchers.IO) {
            val cleanedText = ocrText.trim()
            if (cleanedText.isBlank()) return@withContext Result.failure(Exception("No text detected from image"))

            // 1. Identify Brand & Generic names LOCALLY (avoiding online call)
            val parseResult = identifyBrandAndGeneric(cleanedText)
            val brand = parseResult.brandName
            val generic = parseResult.genericName

            // 2. Database Matching (strictly search local databases)
            var offlineMatch: MedicineInfo? = null
            
            // Check DailyMedicinesDatabase first
            if (!brand.isNullOrBlank()) {
                offlineMatch = DailyMedicinesDatabase.dailyMedicines.firstOrNull {
                    it.brandName.equals(brand, ignoreCase = true) || it.name.contains(brand, true)
                }
            }
            if (offlineMatch == null && !generic.isNullOrBlank()) {
                offlineMatch = DailyMedicinesDatabase.dailyMedicines.firstOrNull {
                    it.genericName.equals(generic, ignoreCase = true)
                }
            }

            // Fallback to MedicineDatabase
            if (offlineMatch == null && !brand.isNullOrBlank()) {
                offlineMatch = MedicineDatabase.findByName(brand)
            }
            if (offlineMatch == null && !generic.isNullOrBlank()) {
                offlineMatch = MedicineDatabase.findByName(generic)
            }
            
            if (offlineMatch == null) {
                offlineMatch = DailyMedicinesDatabase.dailyMedicines.firstOrNull { med ->
                    val bLower = Regex.escape(med.brandName.lowercase())
                    val gLower = Regex.escape(med.genericName.lowercase())
                    val cleanedLower = cleanedText.lowercase()
                    cleanedLower.contains(Regex("\\b$bLower\\b")) || cleanedLower.contains(Regex("\\b$gLower\\b"))
                }
                if (offlineMatch == null) {
                    offlineMatch = MedicineDatabase.medicines.firstOrNull { med ->
                        val bLower = Regex.escape(med.brandName.lowercase())
                        val gLower = Regex.escape(med.genericName.lowercase())
                        val cleanedLower = cleanedText.lowercase()
                        cleanedLower.contains(Regex("\\b$bLower\\b")) || cleanedLower.contains(Regex("\\b$gLower\\b"))
                    }
                }
            }

            val queryName = offlineMatch?.name ?: brand ?: generic ?: cleanedText

            // 3. Query FDA API and structure with the fallback-enabled API client
            analyzeWithFdaAndGemini(queryName, rawOcrText = cleanedText, offlineMatch = offlineMatch)
        }

    // ── Core: Analyze by typed name ───────────────────────────────────────────
    suspend fun analyzeByName(name: String): Result<MedicineInfo> =
        withContext(Dispatchers.IO) {
            if (name.isBlank()) return@withContext Result.failure(Exception("Please enter a medicine name"))

            var offlineMatch = DailyMedicinesDatabase.dailyMedicines.firstOrNull {
                it.name.contains(name, ignoreCase = true) || it.brandName.contains(name, ignoreCase = true) || it.genericName.contains(name, ignoreCase = true)
            }
            if (offlineMatch == null) {
                offlineMatch = MedicineDatabase.findByName(name)
            }
            analyzeWithFdaAndGemini(name, rawOcrText = "", offlineMatch = offlineMatch)
        }

    // ── FDA/RxNorm and Gemini AI analysis ──────────────────────────────────────
    private suspend fun analyzeWithFdaAndGemini(
        query: String,
        rawOcrText: String,
        offlineMatch: MedicineInfo?,
        imageUrl: String? = null
    ): Result<MedicineInfo> {

        // ── Priority 1: Use local DB directly (no Gemini needed) ──────────────
        // If we have any offline match, return it immediately.
        // This completely avoids Gemini quota issues for known medicines.
        if (offlineMatch != null) {
            val finalInfo = applyDosageSafetyChecks(
                offlineMatch.copy(rawOcrText = rawOcrText, confidenceScore = 92)
            )
            saveToHistory(finalInfo, imageUrl)
            return Result.success(finalInfo)
        }

        // ── Priority 2: Offline fallback when no connection ───────────────────
        // ── Priority 3: API call as last resort (medicine not in local DB) ────
        if (!isOnline()) {
            return Result.failure(Exception("Medicine could not be identified offline. Please check your connection or type the name manually."))
        }

        return runCatching {
            val cleanedQuery = cleanQueryForApi(query)
            
            // Query RxNorm and FDA
            var fdaData: String? = null
            var fdaFound = false
            try {
                // Log API Request: OpenFDA (Requirement 9)
                android.util.Log.i("MedicineScanner", "[API Request] Querying OpenFDA/RxNorm for: '$cleanedQuery'")
                fdaData = MedicineApiHelper.queryFdaAndRxNorm(cleanedQuery)
                val fdaJson = JSONObject(fdaData)
                if (fdaJson.has("brand_name") || fdaJson.has("warnings") || fdaJson.has("rxnorm_cui")) {
                    fdaFound = true
                }
                // Log API Response: OpenFDA (Requirement 9)
                android.util.Log.i("MedicineScanner", "[API Response] OpenFDA Query completed. fdaFound=$fdaFound, responseLength=${fdaData.length}")
            } catch (e: Exception) {
                // Log API Response: OpenFDA Error (Requirement 9)
                android.util.Log.e("MedicineScanner", "[API Response] OpenFDA Query failed: ${e.message}", e)
            }

            val prompt = if (fdaFound && !fdaData.isNullOrBlank()) {
                buildGeminiPromptWithFda(cleanedQuery, fdaData)
            } else {
                buildGeminiPrompt(cleanedQuery)
            }
            val promptHash = prompt.hashCode()

            // Log API Request: Gemini (Requirement 9)
            android.util.Log.i("MedicineScanner", "[API Request] Sending prompt to Gemini (Hash: $promptHash):\n$prompt")

            // ── Look up local offline cache first ──
            val cachedResult = cacheDao.getCache(promptHash)
            val jsonText = if (cachedResult != null) {
                android.util.Log.i("MedicineScanner", "[API Response] Gemini Cache Hit for Hash: $promptHash")
                cachedResult.response
            } else {
                // ── Remote call with fallback client ──
                val responseText = fallbackApiClient.generateContent(prompt)
                cacheDao.insertCache(GeminiCacheEntity(promptHash, prompt, responseText))
                responseText
            }

            // Log API Response: Gemini (Requirement 9)
            android.util.Log.i("MedicineScanner", "[API Response] Gemini Raw Response:\n$jsonText")

            val info = parseGeminiJson(jsonText, cleanedQuery, rawOcrText)
            val confidence = if (fdaFound) 98 else 70
            val finalInfo = applyDosageSafetyChecks(info.copy(confidenceScore = confidence, rawOcrText = rawOcrText))

            saveToHistory(finalInfo, imageUrl)
            finalInfo
        }.recoverCatching { exception ->
            // If API fails, fall back to offline match
            if (offlineMatch != null) {
                val finalFallback = applyDosageSafetyChecks(offlineMatch.copy(rawOcrText = rawOcrText, confidenceScore = 80))
                saveToHistory(finalFallback, imageUrl)
                finalFallback
            } else {
                throw exception
            }
        }
    }


    private fun cleanQueryForApi(query: String): String {
        var q = query.replace("\n", " ").replace("\r", " ")
        q = q.replace(Regex("(?i)\\b(tablets|capsules|injection|ointment|drops|inhaler|powder|patch|suppository|syrup|ip|usp|bp|mg|mcg|ml)\\b"), "")
        q = q.replace(Regex("\\s+"), " ").trim()
        return q.ifBlank { query }
    }

    private fun buildGeminiPrompt(query: String) = """
You are a medical information AI assistant. Provide complete, accurate, and safe medicine information for: "$query"

CRITICAL: Respond ONLY with valid JSON (no markdown, no backticks). Use this exact structure:
{
  "name": "Full medicine name with dosage",
  "brandName": "Brand name",
  "genericName": "Generic / INN name",
  "composition": "Active ingredients and amounts",
  "category": "Drug category (e.g. Antibiotic, Analgesic)",
  "medicineType": "TABLET",
  "dosage": "Recommended dosage per dose",
  "frequency": "How often to take",
  "ageGroup": "Recommended age group",
  "beforeOrAfterFood": "Before food / After food / With food",
  "howToUse": "Instructions on how to take",
  "uses": ["use1", "use2", "use3"],
  "symptomsItTreats": ["symptom1", "symptom2"],
  "whenToUse": "Brief guidance on when to use",
  "whoShouldAvoid": ["group1", "group2"],
  "sideEffects": ["effect1", "effect2", "effect3"],
  "sideEffectSeverities": {
    "effect1": "Common",
    "effect2": "Uncommon",
    "effect3": "Rare/Severe"
  },
  "allergyWarnings": ["allergy1"],
  "interactions": ["drug1 - reason", "drug2 - reason"],
  "interactionWarnings": ["Warning description 1", "Warning description 2"],
  "warnings": ["⚠️ warning1", "⚠️ warning2"],
  "storageInfo": "Storage conditions",
  "expiryNote": "Expiry note",
  "prescriptionOnly": false,
  "safetyScore": 8,
  "safetyLevel": "SAFE",
  "emergencyWarning": null,
  "aiSummary": "2-3 sentence plain-English summary for patients",
  "manufacturer": "Manufacturer or pharmaceutical company name",
  "alternatives": ["Alternative medicine 1", "Alternative medicine 2", "Alternative medicine 3"]
}

SAFETY RULES:
- Do NOT recommend dangerous dosages
- Do NOT replace medical advice  
- Always include appropriate warnings
- safetyLevel must be one of: SAFE, MODERATE, CAUTION, DANGER
- medicineType must be one of: TABLET, CAPSULE, SYRUP, INJECTION, OINTMENT, DROPS, INHALER, PATCH, POWDER, SUPPOSITORY
- sideEffectSeverities must map each of the sideEffects to either "Common", "Uncommon", or "Rare/Severe".
- manufacturer should be the known pharma company (e.g. GSK, Pfizer, Cipla, Sun Pharma). If unknown, use "Various manufacturers".
- alternatives should list 2-5 similar medicines that serve the same therapeutic purpose.
""".trimIndent()

    private fun buildGeminiPromptWithFda(query: String, fdaData: String) = """
You are a medical information AI assistant. We have retrieved official FDA and RxNorm data for "$query":
$fdaData

Your task is to structure this raw medical data into a clean, complete, and user-friendly JSON response. Keep it highly accurate, and ensure sideEffectSeverities correctly rates adverse effects.

CRITICAL: Respond ONLY with valid JSON (no markdown, no backticks). Use this exact structure:
{
  "name": "Full medicine name with dosage",
  "brandName": "Brand name",
  "genericName": "Generic / INN name",
  "composition": "Active ingredients and amounts",
  "category": "Drug category (e.g. Antibiotic, Analgesic)",
  "medicineType": "TABLET",
  "dosage": "Recommended dosage per dose",
  "frequency": "How often to take",
  "ageGroup": "Recommended age group",
  "beforeOrAfterFood": "Before food / After food / With food",
  "howToUse": "Instructions on how to take",
  "uses": ["use1", "use2", "use3"],
  "symptomsItTreats": ["symptom1", "symptom2"],
  "whenToUse": "Brief guidance on when to use",
  "whoShouldAvoid": ["group1", "group2"],
  "sideEffects": ["effect1", "effect2", "effect3"],
  "sideEffectSeverities": {
    "effect1": "Common",
    "effect2": "Uncommon",
    "effect3": "Rare/Severe"
  },
  "allergyWarnings": ["allergy1"],
  "interactions": ["drug1 - reason", "drug2 - reason"],
  "interactionWarnings": ["Warning description 1", "Warning description 2"],
  "warnings": ["⚠️ warning1", "⚠️ warning2"],
  "storageInfo": "Storage conditions",
  "expiryNote": "Expiry note",
  "prescriptionOnly": false,
  "safetyScore": 8,
  "safetyLevel": "SAFE",
  "emergencyWarning": null,
  "aiSummary": "2-3 sentence plain-English summary for patients",
  "manufacturer": "Manufacturer or pharmaceutical company name",
  "alternatives": ["Alternative medicine 1", "Alternative medicine 2", "Alternative medicine 3"]
}

SAFETY RULES:
- Do NOT recommend dangerous dosages
- Do NOT replace medical advice  
- Always include appropriate warnings
- safetyLevel must be one of: SAFE, MODERATE, CAUTION, DANGER
- medicineType must be one of: TABLET, CAPSULE, SYRUP, INJECTION, OINTMENT, DROPS, INHALER, PATCH, POWDER, SUPPOSITORY
- sideEffectSeverities must map each of the sideEffects to either "Common", "Uncommon", or "Rare/Severe".
- manufacturer should be the known pharma company. If unknown, use "Various manufacturers".
- alternatives should list 2-5 similar medicines that serve the same therapeutic purpose.
""".trimIndent()

    private fun applyDosageSafetyChecks(info: MedicineInfo): MedicineInfo {
        val generic = info.genericName.lowercase()
        val nameLower = info.name.lowercase()
        val dosageLower = info.dosage.lowercase()
        val compositionLower = info.composition.lowercase()
        
        val warnings = info.warnings.toMutableList()
        var safetyLevel = info.safetyLevel
        var emergencyWarning = info.emergencyWarning
        var prescriptionOnly = info.prescriptionOnly

        // 1. Acetaminophen/Paracetamol dosage checks
        if (generic.contains("acetaminophen") || generic.contains("paracetamol") ||
            nameLower.contains("paracetamol") || nameLower.contains("acetaminophen")) {
            val has1000mg = nameLower.contains("1000mg") || nameLower.contains("1000 mg") || 
                           dosageLower.contains("1000mg") || dosageLower.contains("1000 mg") ||
                           compositionLower.contains("1000mg") || compositionLower.contains("1000 mg") ||
                           nameLower.contains("1g") || dosageLower.contains("1g")
            
            if (has1000mg) {
                val warningMsg = "⚠️ High dose detected (1000mg). Do not take more than 1 tablet at a time and never exceed 4 tablets (4000mg) in 24 hours to prevent acute liver failure."
                if (!warnings.any { it.contains("1000mg") || it.contains("liver") }) {
                    warnings.add(0, warningMsg)
                }
                safetyLevel = SafetyLevel.CAUTION
                emergencyWarning = "⚠️ High single-dose Acetaminophen. Limit daily intake strictly to 4g to avoid liver injury."
            }
        }

        // 2. High-Risk / Opioid drugs
        if (generic.contains("fentanyl") || generic.contains("morphine") || generic.contains("oxycodone") || generic.contains("methadone") ||
            nameLower.contains("fentanyl") || nameLower.contains("morphine") || nameLower.contains("oxycodone")) {
            safetyLevel = SafetyLevel.DANGER
            prescriptionOnly = true
            emergencyWarning = "⚠️ HIGH RISK OF ADDICTION, RESPIRATORY DEPRESSION, AND OVERDOSE. Use strictly as prescribed. Keep away from children. Naloxone is the antidote for overdose."
            if (!warnings.any { it.contains("ADDICTION") || it.contains("OVERDOSE") }) {
                warnings.add(0, "⚠️ Warning: Schedule II Controlled Substance. High potential for abuse and dependence.")
            }
        }

        // 3. Digoxin (narrow therapeutic index)
        if (generic.contains("digoxin") || nameLower.contains("digoxin") || nameLower.contains("lanoxin")) {
            safetyLevel = SafetyLevel.DANGER
            prescriptionOnly = true
            emergencyWarning = "⚠️ Narrow therapeutic index drug. Toxicity can occur easily. Monitor heart rate and watch for nausea, vomiting, or visual changes (yellow-green halos)."
            if (!warnings.any { it.contains("toxicity") || it.contains("Narrow") }) {
                warnings.add(0, "⚠️ Warning: Digoxin toxicity risk. Keep doses exact.")
            }
        }

        // 4. Warfarin (anticoagulant)
        if (generic.contains("warfarin") || nameLower.contains("warfarin") || nameLower.contains("coumadin")) {
            safetyLevel = SafetyLevel.DANGER
            prescriptionOnly = true
            emergencyWarning = "⚠️ High risk of bleeding. Requires regular INR blood tests. Avoid sudden dietary changes in Vitamin K (leafy greens) and consult doctor before taking any pain reliever."
            if (!warnings.any { it.contains("bleeding") }) {
                warnings.add(0, "⚠️ Warning: Major bleeding hazard. Avoid NSAIDs (Ibuprofen, Aspirin) while on Warfarin.")
            }
        }

        // 5. Methotrexate
        if (generic.contains("methotrexate") || nameLower.contains("methotrexate")) {
            safetyLevel = SafetyLevel.DANGER
            prescriptionOnly = true
            emergencyWarning = "⚠️ Highly toxic. Usually taken ONCE WEEKLY for arthritis/psoriasis. Daily dosing can be fatal. Take folic acid as directed."
            if (!warnings.any { it.contains("WEEKLY") }) {
                warnings.add(0, "⚠️ Warning: ONCE-WEEKLY dosage schedule. Do not take daily.")
            }
        }

        // 6. Insulin
        if (generic.contains("insulin") || nameLower.contains("insulin") || nameLower.contains("humalog") || nameLower.contains("lantus")) {
            safetyLevel = SafetyLevel.CAUTION
            prescriptionOnly = true
            emergencyWarning = "⚠️ Risk of severe hypoglycemia. Always keep fast-acting glucose or sugar nearby in case of shakiness, sweating, or confusion."
        }

        return info.copy(
            warnings = warnings,
            safetyLevel = safetyLevel,
            emergencyWarning = emergencyWarning,
            prescriptionOnly = prescriptionOnly
        )
    }

    // ── JSON Parser ───────────────────────────────────────────────────────────
    private fun parseGeminiJson(jsonText: String, fallbackName: String, rawOcrText: String): MedicineInfo {
        val cleaned = jsonText.trim()
            .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val j = JSONObject(cleaned)

        fun arr(key: String): List<String> = runCatching {
            val a = j.optJSONArray(key) ?: return emptyList()
            (0 until a.length()).map { a.getString(it) }
        }.getOrDefault(emptyList())

        val safetyLevelStr = j.optString("safetyLevel", "MODERATE")
        val safetyLevel = runCatching { SafetyLevel.valueOf(safetyLevelStr) }.getOrDefault(SafetyLevel.MODERATE)
        val typeStr = j.optString("medicineType", "TABLET")
        val medType = runCatching { MedicineType.valueOf(typeStr) }.getOrDefault(MedicineType.TABLET)
        val emergencyWarning = j.optString("emergencyWarning", "").takeIf { it.isNotBlank() && it != "null" }

        val sideEffectSeveritiesMap = mutableMapOf<String, String>()
        val sideEffectSevObj = j.optJSONObject("sideEffectSeverities")
        if (sideEffectSevObj != null) {
            val keys = sideEffectSevObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                sideEffectSeveritiesMap[key] = sideEffectSevObj.getString(key)
            }
        }
        val interactionWarningsList = arr("interactionWarnings")

        return MedicineInfo(
            name             = j.optString("name",             fallbackName),
            brandName        = j.optString("brandName",        fallbackName),
            genericName      = j.optString("genericName",      ""),
            composition      = j.optString("composition",      ""),
            category         = j.optString("category",         "Medicine"),
            medicineType     = medType,
            dosage           = j.optString("dosage",           "As directed by physician"),
            frequency        = j.optString("frequency",        "As directed"),
            ageGroup         = j.optString("ageGroup",         "Adults 18+"),
            beforeOrAfterFood= j.optString("beforeOrAfterFood","As directed"),
            howToUse         = j.optString("howToUse",         "As directed by physician"),
            uses             = arr("uses"),
            symptomsItTreats = arr("symptomsItTreats"),
            whenToUse        = j.optString("whenToUse",        ""),
            whoShouldAvoid   = arr("whoShouldAvoid"),
            sideEffects      = arr("sideEffects"),
            allergyWarnings  = arr("allergyWarnings"),
            interactions     = arr("interactions"),
            warnings         = arr("warnings"),
            storageInfo      = j.optString("storageInfo",      "Store in cool dry place"),
            expiryNote       = j.optString("expiryNote",       "Check expiry date on pack"),
            prescriptionOnly = j.optBoolean("prescriptionOnly", false),
            safetyScore      = j.optInt("safetyScore",          7).coerceIn(1, 10),
            safetyLevel      = safetyLevel,
            emergencyWarning = emergencyWarning,
            aiSummary        = j.optString("aiSummary",        ""),
            rawOcrText       = rawOcrText,
            disclaimer       = DISCLAIMER,
            sideEffectSeverities = sideEffectSeveritiesMap,
            interactionWarnings = interactionWarningsList,
            manufacturer     = j.optString("manufacturer",     ""),
            alternatives     = arr("alternatives")
        )
    }



    // ── Offline DB matching from OCR text ─────────────────────────────────────
    private fun matchOfflineDb(text: String): MedicineInfo? {
        val cleaned = text.lowercase().trim()
        if (cleaned.isBlank()) return null
        
        var bestMatch: MedicineInfo? = null
        var bestScore = 0
        
        for (med in MedicineDatabase.medicines) {
            var score = 0
            val brandLower = med.brandName.lowercase()
            val genericLower = med.genericName.lowercase()
            
            if (cleaned.contains(brandLower)) {
                score += 10
            } else {
                val brandWords = brandLower.split(" ").filter { it.length > 2 }
                val overlap = brandWords.count { cleaned.contains(it) }
                score += overlap * 3
            }
            
            if (cleaned.contains(genericLower)) {
                score += 8
            } else {
                val genericWords = genericLower.split(" ").filter { it.length > 2 }
                val overlap = genericWords.count { cleaned.contains(it) }
                score += overlap * 2
            }
            
            if (score > bestScore) {
                bestScore = score
                bestMatch = med
            }
        }
        
        return if (bestScore >= 5) bestMatch else null
    }

    // ── TTS voice summary ─────────────────────────────────────────────────────
    fun speakMedicineSummary(info: MedicineInfo) {
        val text = buildString {
            append("Medicine information for ${info.name}. ")
            append("Generic name: ${info.genericName}. ")
            append("Category: ${info.category}. ")
            append(info.aiSummary.ifBlank { "This medicine is used for ${info.uses.take(2).joinToString(" and ")}." })
            append(" Dosage: ${info.dosage}. ")
            append("Safety level: ${info.safetyLevel.label}. ")
            if (info.warnings.isNotEmpty()) append("Warning: ${info.warnings.first().replace("⚠️", "")}. ")
            append(DISCLAIMER)
        }
        speak(text)
    }

    // ── History management ────────────────────────────────────────────────────
    private suspend fun saveToHistory(info: MedicineInfo, imageUrl: String? = null) {
        val record = MedicineScanRecord(info = info)
        _history.value = listOf(record) + _history.value.filter { it.info.name != info.name }.take(49)
        _favorites.value = _history.value.filter { it.isFavorite }
        persistHistory()

        // Generic history entry (in-app History tab)
        runCatching {
            val medHistJson = com.smartvision.ai.domain.model.HistoryJsonSerializer.serializeMedicine(
                com.smartvision.ai.domain.model.MedicineHistoryDetail(
                    name = info.name,
                    genericName = info.genericName,
                    category = info.category,
                    composition = info.composition,
                    dosage = info.dosage,
                    frequency = info.frequency,
                    uses = info.uses,
                    sideEffects = info.sideEffects,
                    warnings = info.warnings,
                    interactions = info.interactions,
                    safetyScore = info.safetyScore,
                    safetyLevel = info.safetyLevel.name,
                    aiSummary = info.aiSummary,
                    storageInfo = info.storageInfo,
                    prescriptionOnly = info.prescriptionOnly,
                    whoShouldAvoid = info.whoShouldAvoid,
                    emergencyWarning = info.emergencyWarning,
                    manufacturer = info.manufacturer,
                    alternatives = info.alternatives
                )
            )
            historyRepository.save("Medicine", info.name, "Safety ${info.safetyScore}/10 · ${info.category}", medHistJson)
        }

        // Structured Firebase entry — rich medicine data
        runCatching {
            firebase.saveMedicineScan(
                medicineName = info.name,
                genericName  = info.genericName,
                category     = info.category,
                safetyScore  = info.safetyScore,
                composition  = info.composition,
                uses         = info.uses.take(5).joinToString("; "),
                sideEffects  = info.sideEffects.take(5).joinToString("; "),
                confidence   = info.confidenceScore / 100f,
                imageUrl     = imageUrl
            )
        }
    }

    fun toggleFavorite(record: MedicineScanRecord) {
        val updated = record.copy(isFavorite = !record.isFavorite)
        _history.value = _history.value.map { if (it.id == record.id) updated else it }
        _favorites.value = _history.value.filter { it.isFavorite }
        persistHistory()
    }

    fun clearHistory() {
        _history.value = emptyList()
        _favorites.value = emptyList()
        prefs.edit().remove("history_records_json").apply()
    }

    fun deleteRecord(id: String) {
        _history.value = _history.value.filter { it.id != id }
        _favorites.value = _history.value.filter { it.isFavorite }
        persistHistory()
    }

    fun searchHistory(query: String): List<MedicineScanRecord> =
        _history.value.filter { it.info.name.contains(query, true) }

    // ── JSON Persistence ──────────────────────────────────────────────────────
    private fun persistHistory() {
        val arr = JSONArray()
        _history.value.forEach { arr.put(scanRecordToJson(it)) }
        prefs.edit().putString("history_records_json", arr.toString()).apply()
    }

    private fun loadHistory(): List<MedicineScanRecord> = runCatching {
        val jsonStr = prefs.getString("history_records_json", "") ?: ""
        if (jsonStr.isBlank()) return emptyList()
        val arr = JSONArray(jsonStr)
        (0 until arr.length()).map { jsonToScanRecord(arr.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    // ── JSON Serialization Helpers ──
    private fun medicineInfoToJson(info: MedicineInfo): JSONObject {
        val j = JSONObject()
        j.put("id", info.id)
        j.put("name", info.name)
        j.put("brandName", info.brandName)
        j.put("genericName", info.genericName)
        j.put("composition", info.composition)
        j.put("category", info.category)
        j.put("medicineType", info.medicineType.name)
        j.put("dosage", info.dosage)
        j.put("frequency", info.frequency)
        j.put("ageGroup", info.ageGroup)
        j.put("beforeOrAfterFood", info.beforeOrAfterFood)
        j.put("howToUse", info.howToUse)
        
        val usesArr = JSONArray()
        info.uses.forEach { usesArr.put(it) }
        j.put("uses", usesArr)

        val symptomsArr = JSONArray()
        info.symptomsItTreats.forEach { symptomsArr.put(it) }
        j.put("symptomsItTreats", symptomsArr)

        j.put("whenToUse", info.whenToUse)

        val avoidArr = JSONArray()
        info.whoShouldAvoid.forEach { avoidArr.put(it) }
        j.put("whoShouldAvoid", avoidArr)

        val sideEffectsArr = JSONArray()
        info.sideEffects.forEach { sideEffectsArr.put(it) }
        j.put("sideEffects", sideEffectsArr)

        val allergyArr = JSONArray()
        info.allergyWarnings.forEach { allergyArr.put(it) }
        j.put("allergyWarnings", allergyArr)

        val interactionsArr = JSONArray()
        info.interactions.forEach { interactionsArr.put(it) }
        j.put("interactions", interactionsArr)

        val warningsArr = JSONArray()
        info.warnings.forEach { warningsArr.put(it) }
        j.put("warnings", warningsArr)

        j.put("storageInfo", info.storageInfo)
        j.put("expiryNote", info.expiryNote)

        j.put("prescriptionOnly", info.prescriptionOnly)
        j.put("safetyScore", info.safetyScore)
        j.put("safetyLevel", info.safetyLevel.name)
        j.put("emergencyWarning", info.emergencyWarning ?: JSONObject.NULL)
        j.put("aiSummary", info.aiSummary)
        j.put("disclaimer", info.disclaimer)
        j.put("isFavorite", info.isFavorite)
        j.put("scannedAt", info.scannedAt.time)
        j.put("rawOcrText", info.rawOcrText)
        j.put("confidenceScore", info.confidenceScore)

        val sideEffectSeveritiesObj = JSONObject()
        info.sideEffectSeverities.forEach { (k, v) -> sideEffectSeveritiesObj.put(k, v) }
        j.put("sideEffectSeverities", sideEffectSeveritiesObj)

        val interactionWarningsArr = JSONArray()
        info.interactionWarnings.forEach { interactionWarningsArr.put(it) }
        j.put("interactionWarnings", interactionWarningsArr)

        j.put("manufacturer", info.manufacturer)
        val alternativesArr = JSONArray()
        info.alternatives.forEach { alternativesArr.put(it) }
        j.put("alternatives", alternativesArr)

        return j
    }

    private fun jsonToMedicineInfo(j: JSONObject): MedicineInfo {
        fun toList(arr: JSONArray?): List<String> {
            if (arr == null) return emptyList()
            return (0 until arr.length()).map { arr.getString(it) }
        }

        val sideEffectSeveritiesMap = mutableMapOf<String, String>()
        val sevObj = j.optJSONObject("sideEffectSeverities")
        if (sevObj != null) {
            val keys = sevObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                sideEffectSeveritiesMap[key] = sevObj.getString(key)
            }
        }

        val typeStr = j.optString("medicineType", "TABLET")
        val medType = runCatching { MedicineType.valueOf(typeStr) }.getOrDefault(MedicineType.TABLET)
        val safetyStr = j.optString("safetyLevel", "MODERATE")
        val safetyLevel = runCatching { SafetyLevel.valueOf(safetyStr) }.getOrDefault(SafetyLevel.MODERATE)

        return MedicineInfo(
            id = j.optString("id", java.util.UUID.randomUUID().toString()),
            name = j.optString("name", ""),
            brandName = j.optString("brandName", ""),
            genericName = j.optString("genericName", ""),
            composition = j.optString("composition", ""),
            category = j.optString("category", "Medicine"),
            medicineType = medType,
            dosage = j.optString("dosage", ""),
            frequency = j.optString("frequency", ""),
            ageGroup = j.optString("ageGroup", ""),
            beforeOrAfterFood = j.optString("beforeOrAfterFood", ""),
            howToUse = j.optString("howToUse", ""),
            uses = toList(j.optJSONArray("uses")),
            symptomsItTreats = toList(j.optJSONArray("symptomsItTreats")),
            whenToUse = j.optString("whenToUse", ""),
            whoShouldAvoid = toList(j.optJSONArray("whoShouldAvoid")),
            sideEffects = toList(j.optJSONArray("sideEffects")),
            allergyWarnings = toList(j.optJSONArray("allergyWarnings")),
            interactions = toList(j.optJSONArray("interactions")),
            warnings = toList(j.optJSONArray("warnings")),
            storageInfo = j.optString("storageInfo", "Store in a cool, dry place"),
            expiryNote = j.optString("expiryNote", "Check expiry date on package"),
            prescriptionOnly = j.optBoolean("prescriptionOnly", false),
            safetyScore = j.optInt("safetyScore", 7),
            safetyLevel = safetyLevel,
            emergencyWarning = j.optString("emergencyWarning", "").takeIf { it.isNotBlank() && it != "null" },
            aiSummary = j.optString("aiSummary", ""),
            disclaimer = j.optString("disclaimer", DISCLAIMER),
            isFavorite = j.optBoolean("isFavorite", false),
            scannedAt = java.util.Date(j.optLong("scannedAt", System.currentTimeMillis())),
            rawOcrText = j.optString("rawOcrText", ""),
            confidenceScore = j.optInt("confidenceScore", 100),
            sideEffectSeverities = sideEffectSeveritiesMap,
            interactionWarnings = toList(j.optJSONArray("interactionWarnings")),
            manufacturer = j.optString("manufacturer", ""),
            alternatives = toList(j.optJSONArray("alternatives"))
        )
    }

    private fun scanRecordToJson(record: MedicineScanRecord): JSONObject {
        val j = JSONObject()
        j.put("id", record.id)
        j.put("isFavorite", record.isFavorite)
        j.put("scannedAt", record.scannedAt.time)
        j.put("info", medicineInfoToJson(record.info))
        return j
    }

    private fun jsonToScanRecord(j: JSONObject): MedicineScanRecord {
        return MedicineScanRecord(
            id = j.optString("id", java.util.UUID.randomUUID().toString()),
            isFavorite = j.optBoolean("isFavorite", false),
            scannedAt = java.util.Date(j.optLong("scannedAt", System.currentTimeMillis())),
            info = jsonToMedicineInfo(j.getJSONObject("info"))
        )
    }

    // ── Search suggestions ────────────────────────────────────────────────────
    fun getSearchSuggestions(query: String): List<String> {
        if (query.length < 2) return listOf("Paracetamol", "Ibuprofen", "Amoxicillin", "Cetirizine", "Omeprazole", "Metformin")
        // Search across both databases with symptoms + manufacturer matching
        val dailyMatches = DailyMedicinesDatabase.dailyMedicines.filter { med ->
            med.name.contains(query, true) || med.genericName.contains(query, true) ||
            med.brandName.contains(query, true) || med.manufacturer.contains(query, true) ||
            med.uses.any { it.contains(query, true) } ||
            med.symptomsItTreats.any { it.contains(query, true) }
        }.map { it.name }
        val dbMatches = MedicineDatabase.searchAll(query).map { it.name }
        return (dailyMatches + dbMatches).distinct().take(8)
    }
}
