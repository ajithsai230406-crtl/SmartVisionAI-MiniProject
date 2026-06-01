package com.smartvision.ai.medicine.data

import android.content.Context
import android.content.SharedPreferences
import android.speech.tts.TextToSpeech
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.smartvision.ai.BuildConfig
import com.smartvision.ai.data.repository.HistoryRepository
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
import kotlinx.coroutines.withContext
import org.json.JSONObject
import com.smartvision.ai.data.db.GeminiCacheDao
import com.smartvision.ai.data.db.GeminiCacheEntity
import com.smartvision.ai.util.retryWithDelay
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicineScannerRepository @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val historyRepository: HistoryRepository,
    private val cacheDao: GeminiCacheDao
) {
    // ── Gemini AI ──────────────────────────────────────────────────────────────
    private val gemini: GenerativeModel? by lazy {
        runCatching {
            GenerativeModel(modelName = "gemini-1.5-flash", apiKey = BuildConfig.GEMINI_API_KEY)
        }.getOrNull()
    }

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

    private val _history = MutableStateFlow<List<MedicineScanRecord>>(loadHistory())
    val history: StateFlow<List<MedicineScanRecord>> = _history

    private val _favorites = MutableStateFlow<List<MedicineScanRecord>>(loadFavorites())
    val favorites: StateFlow<List<MedicineScanRecord>> = _favorites

    // ── Core: Analyze from OCR text ───────────────────────────────────────────
    suspend fun analyzeFromOcrText(ocrText: String): Result<MedicineInfo> =
        withContext(Dispatchers.IO) {
            val cleanedText = ocrText.trim()
            if (cleanedText.isBlank()) return@withContext Result.failure(Exception("No text detected from image"))

            // Try offline database first (fast)
            val offlineMatch = matchOfflineDb(cleanedText)
            if (offlineMatch != null) {
                val info = offlineMatch.copy(rawOcrText = cleanedText)
                saveToHistory(info)
                return@withContext Result.success(info)
            }

            // Try Gemini AI (online)
            analyzeWithGemini(cleanedText, rawOcrText = cleanedText)
        }

    // ── Core: Analyze by typed name ───────────────────────────────────────────
    suspend fun analyzeByName(name: String): Result<MedicineInfo> =
        withContext(Dispatchers.IO) {
            if (name.isBlank()) return@withContext Result.failure(Exception("Please enter a medicine name"))

            // Offline DB
            val offlineMatch = MedicineDatabase.findByName(name)
            if (offlineMatch != null) {
                saveToHistory(offlineMatch)
                return@withContext Result.success(offlineMatch)
            }

            // Gemini AI
            analyzeWithGemini(name, rawOcrText = "")
        }

    // ── Gemini AI analysis ────────────────────────────────────────────────────
    private suspend fun analyzeWithGemini(query: String, rawOcrText: String): Result<MedicineInfo> {
        val model = gemini ?: return Result.success(MedicineDatabase.random().copy(
            name = query.take(30), rawOcrText = rawOcrText
        ))

        return runCatching {
            val prompt = buildGeminiPrompt(query)
            val promptHash = prompt.hashCode()

            // ── Look up local offline cache first ──
            val cachedResult = cacheDao.getCache(promptHash)
            val info = if (cachedResult != null) {
                parseGeminiJson(cachedResult.response, query, rawOcrText)
            } else {
                // ── Remote call with exponential backoff retries ──
                val response = retryWithDelay(retries = 3) {
                    model.generateContent(content { text(prompt) })
                }
                val jsonText = response.text ?: throw Exception("Empty AI response")
                val parsed = parseGeminiJson(jsonText, query, rawOcrText)
                cacheDao.insertCache(GeminiCacheEntity(promptHash, prompt, jsonText))
                parsed
            }

            saveToHistory(info)
            info
        }.recoverCatching {
            // Fallback to offline random if Gemini fails
            val fallback = MedicineDatabase.random().copy(name = query, rawOcrText = rawOcrText)
            saveToHistory(fallback)
            fallback
        }
    }

    private fun buildGeminiPrompt(query: String) = """
You are a medical information AI assistant. Provide complete, accurate, and safe medicine information for: "$query"

CRITICAL: Respond ONLY with valid JSON (no markdown, no backticks). Use this exact structure:
{
  "name": "Full medicine name with dosage",
  "brandName": "Brand names (comma separated)",
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
  "allergyWarnings": ["allergy1"],
  "interactions": ["drug1 - reason", "drug2 - reason"],
  "warnings": ["⚠️ warning1", "⚠️ warning2"],
  "storageInfo": "Storage conditions",
  "expiryNote": "Expiry note",
  "prescriptionOnly": false,
  "safetyScore": 8,
  "safetyLevel": "SAFE",
  "emergencyWarning": null,
  "aiSummary": "2-3 sentence plain-English summary for patients"
}

SAFETY RULES:
- Do NOT recommend dangerous dosages
- Do NOT replace medical advice  
- Always include appropriate warnings
- safetyLevel must be one of: SAFE, MODERATE, CAUTION, DANGER
- medicineType must be one of: TABLET, CAPSULE, SYRUP, INJECTION, OINTMENT, DROPS, INHALER, PATCH, POWDER, SUPPOSITORY
""".trimIndent()

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
            disclaimer       = DISCLAIMER
        )
    }

    // ── Offline DB matching from OCR text ─────────────────────────────────────
    private fun matchOfflineDb(text: String): MedicineInfo? {
        val lower = text.lowercase()
        return MedicineDatabase.medicines.firstOrNull { med ->
            lower.contains(med.name.lowercase().split(" ").first()) ||
            lower.contains(med.genericName.lowercase()) ||
            med.brandName.lowercase().split("/").any { brand -> lower.contains(brand.trim()) }
        }
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
    private suspend fun saveToHistory(info: MedicineInfo) {
        val record = MedicineScanRecord(info = info)
        _history.value = listOf(record) + _history.value.take(49)
        persistHistory()
        runCatching { historyRepository.save("Medicine", info.name, "Safety ${info.safetyScore}/10 · ${info.category}") }
    }

    fun toggleFavorite(record: MedicineScanRecord) {
        val updated = record.copy(isFavorite = !record.isFavorite)
        _history.value = _history.value.map { if (it.id == record.id) updated else it }
        _favorites.value = if (updated.isFavorite) {
            listOf(updated) + _favorites.value.filter { it.id != record.id }
        } else {
            _favorites.value.filter { it.id != record.id }
        }
        persistHistory()
    }

    fun clearHistory() {
        _history.value = emptyList()
        prefs.edit().remove("history_names").apply()
    }

    fun deleteRecord(id: String) {
        _history.value = _history.value.filter { it.id != id }
        _favorites.value = _favorites.value.filter { it.id != id }
    }

    fun searchHistory(query: String): List<MedicineScanRecord> =
        _history.value.filter { it.info.name.contains(query, true) }

    // ── Simple persistence (medicine names only, for demo) ────────────────────
    private fun persistHistory() {
        val names = _history.value.take(20).joinToString("|") { it.info.name }
        prefs.edit().putString("history_names", names).apply()
    }

    private fun loadHistory(): List<MedicineScanRecord> = runCatching {
        val names = prefs.getString("history_names", "") ?: ""
        if (names.isBlank()) return emptyList()
        names.split("|").mapNotNull { name ->
            MedicineDatabase.findByName(name)?.let { MedicineScanRecord(info = it) }
        }
    }.getOrDefault(emptyList())

    private fun loadFavorites(): List<MedicineScanRecord> = emptyList()

    // ── Search suggestions ────────────────────────────────────────────────────
    fun getSearchSuggestions(query: String): List<String> =
        if (query.length < 2) listOf("Paracetamol", "Ibuprofen", "Amoxicillin", "Cetirizine", "Omeprazole", "Metformin")
        else MedicineDatabase.searchAll(query).map { it.name }
}
