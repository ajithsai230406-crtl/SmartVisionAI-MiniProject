package com.smartvision.ai.domain.model

import org.json.JSONArray
import org.json.JSONObject
import com.smartvision.ai.detector.domain.*

// ══════════════════════════════════════════════════════════════════════════════
// HISTORY DETAIL DATA CLASSES — JSON-serializable per-module result snapshots
// ══════════════════════════════════════════════════════════════════════════════

data class TranslationHistoryDetail(
    val originalText: String,
    val translatedText: String,
    val sourceLang: String,
    val targetLang: String,
    val meaningInSource: String? = null,
    val meaningInTarget: String? = null,
    val meaningInEnglish: String? = null,
    val context: String? = null,
    val usageExamples: List<String> = emptyList(),
    val alternativeMeanings: List<String> = emptyList(),
    val pronunciation: String? = null
)

data class VoiceEntryDetail(
    val originalText: String,
    val translatedText: String,
    val sourceLang: String,
    val targetLang: String,
    val isFromUser: Boolean,
    val meaningInSource: String? = null,
    val meaningInTarget: String? = null,
    val meaningInEnglish: String? = null,
    val context: String? = null,
    val usageExamples: List<String> = emptyList(),
    val alternativeMeanings: List<String> = emptyList(),
    val pronunciation: String? = null
)

data class VoiceHistoryDetail(
    val entries: List<VoiceEntryDetail>
)

data class OcrHistoryDetail(
    val extractedText: String,
    val translatedText: String? = null,
    val sourceLang: String,
    val targetLang: String? = null,
    val meaningInSource: String? = null,
    val meaningInTarget: String? = null,
    val meaningInEnglish: String? = null,
    val context: String? = null,
    val usageExamples: List<String> = emptyList(),
    val alternativeMeanings: List<String> = emptyList(),
    val pronunciation: String? = null,
    val wordCount: Int = 0,
    val paragraphCount: Int = 0
)

data class QrHistoryDetail(
    val rawValue: String,
    val displayValue: String,
    val contentType: String,
    val formatName: String,
    val wifiSsid: String? = null,
    val wifiPassword: String? = null,
    val wifiSecurity: String? = null,
    val timestamp: Long = 0L
)

data class ObjectHistoryDetail(
    val label: String,
    val confidence: Float,
    val category: String,
    val description: String,
    val uses: List<String> = emptyList(),
    val safetyInfo: String = "",
    val environmentalImpact: String = "",
    val funFact: String = "",
    val emoji: String = "",
    val maintenanceTips: List<String> = emptyList(),
    val buyingSuggestions: List<String> = emptyList(),
    val imagePath: String? = null,
    val searchData: VisualSearchData? = null
)

data class WasteHistoryDetail(
    val wasteType: String,
    val exactItemName: String,
    val confidence: Float,
    val disposalMethod: String = "",
    val disposalSteps: List<String> = emptyList(),
    val reuseSuggestions: String = "",
    val environmentalImpact: String = "",
    val ecoScore: Int = 50,
    val carbonReduction: String = "",
    val carbonImpact: String = "",
    val isRecyclable: Boolean = true,
    val isReusable: Boolean = true,
    val sustainabilityTip: String = "",
    val imagePath: String? = null,
    val safetyPrecautions: String = ""
)

data class MedicineHistoryDetail(
    val name: String,
    val genericName: String,
    val category: String,
    val composition: String = "",
    val dosage: String = "",
    val frequency: String = "",
    val uses: List<String> = emptyList(),
    val sideEffects: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val interactions: List<String> = emptyList(),
    val safetyScore: Int = 5,
    val safetyLevel: String = "MODERATE",
    val aiSummary: String = "",
    val storageInfo: String = "",
    val prescriptionOnly: Boolean = false,
    val whoShouldAvoid: List<String> = emptyList(),
    val emergencyWarning: String? = null,
    val imagePath: String? = null,
    val manufacturer: String = "",
    val alternatives: List<String> = emptyList()
)

data class StudentHistoryDetail(
    val question: String,
    val answer: String,
    val subject: String = ""
)

data class CareerHistoryDetail(
    val actionType: String, // RESUME_BUILDER, RESUME_ANALYZER, COVER_LETTER, INTERVIEW_PREP
    val title: String,
    val date: String,
    val details: String,
    val atsScore: Int = -1,
    val generatedContent: String = "",
    val interviewQuestionsJson: String = ""
)

// ══════════════════════════════════════════════════════════════════════════════
// JSON SERIALIZATION HELPERS
// ══════════════════════════════════════════════════════════════════════════════

object HistoryJsonSerializer {

    private fun JSONObject.serializeSearchData(d: VisualSearchData?): JSONObject? {
        if (d == null) return null
        return JSONObject().apply {
            val specsObj = JSONObject()
            d.specifications.forEach { (k, v) -> specsObj.put(k, v) }
            put("specifications", specsObj)
            
            putStringList("alternatives", d.alternatives)
            putStringList("brandsAndModels", d.brandsAndModels)
            put("manufacturerInfo", d.manufacturerInfo)
            put("ratings", d.ratings)
            put("priceRange", d.priceRange)
            put("brand", d.brand)
            putStringList("ingredients", d.ingredients)
            put("weight", d.weight)
            putStringList("variants", d.variants)
            put("launchInfo", d.launchInfo)
            put("author", d.author)
            put("scientificName", d.scientificName)
            putStringList("characteristics", d.characteristics)
            put("habitat", d.habitat)
            put("careInfo", d.careInfo)
            put("calories", d.calories)
            
            val nutObj = JSONObject()
            d.nutritionalValues.forEach { (k, v) -> nutObj.put(k, v) }
            put("nutritionalValues", nutObj)
            
            putStringList("healthyAlternatives", d.healthyAlternatives)
            
            val webArr = JSONArray()
            d.webResults.forEach { r ->
                webArr.put(JSONObject().apply {
                    put("title", r.title)
                    put("snippet", r.snippet)
                    put("url", r.url)
                })
            }
            put("webResults", webArr)
            
            val imgArr = JSONArray()
            d.similarImages.forEach { im ->
                imgArr.put(JSONObject().apply {
                    put("title", im.title)
                    put("imageUrl", im.imageUrl)
                    put("sourceUrl", im.sourceUrl)
                })
            }
            put("similarImages", imgArr)
            
            val prodArr = JSONArray()
            d.relatedProducts.forEach { p ->
                prodArr.put(JSONObject().apply {
                    put("name", p.name)
                    put("brand", p.brand)
                    put("price", p.price)
                    put("rating", p.rating)
                    put("url", p.url)
                })
            }
            put("relatedProducts", prodArr)
            
            put("aiSummary", d.aiSummary)
        }
    }

    private fun JSONObject.deserializeSearchData(): VisualSearchData? {
        val j = optJSONObject("searchData") ?: return null
        
        val specsMap = mutableMapOf<String, String>()
        val specsObj = j.optJSONObject("specifications")
        if (specsObj != null) {
            specsObj.keys().forEach { k -> specsMap[k] = specsObj.getString(k) }
        }
        
        val nutMap = mutableMapOf<String, String>()
        val nutObj = j.optJSONObject("nutritionalValues")
        if (nutObj != null) {
            nutObj.keys().forEach { k -> nutMap[k] = nutObj.getString(k) }
        }
        
        val webList = mutableListOf<WebResult>()
        val webArr = j.optJSONArray("webResults")
        if (webArr != null) {
            for (i in 0 until webArr.length()) {
                val o = webArr.getJSONObject(i)
                webList.add(WebResult(o.getString("title"), o.getString("snippet"), o.getString("url")))
            }
        }
        
        val imgList = mutableListOf<SimilarImage>()
        val imgArr = j.optJSONArray("similarImages")
        if (imgArr != null) {
            for (i in 0 until imgArr.length()) {
                val o = imgArr.getJSONObject(i)
                imgList.add(SimilarImage(o.getString("title"), o.getString("imageUrl"), o.optString("sourceUrl", "")))
            }
        }
        
        val prodList = mutableListOf<RelatedProduct>()
        val prodArr = j.optJSONArray("relatedProducts")
        if (prodArr != null) {
            for (i in 0 until prodArr.length()) {
                val o = prodArr.getJSONObject(i)
                prodList.add(RelatedProduct(o.getString("name"), o.getString("brand"), o.getString("price"), o.getString("rating"), o.optString("url", "")))
            }
        }
        
        return VisualSearchData(
            specifications = specsMap,
            alternatives = j.getStringList("alternatives"),
            brandsAndModels = j.getStringList("brandsAndModels"),
            manufacturerInfo = j.optString("manufacturerInfo", ""),
            ratings = j.optString("ratings", ""),
            priceRange = j.optString("priceRange", ""),
            brand = j.optString("brand", ""),
            ingredients = j.getStringList("ingredients"),
            weight = j.optString("weight", ""),
            variants = j.getStringList("variants"),
            launchInfo = j.optString("launchInfo", ""),
            author = j.optString("author", ""),
            scientificName = j.optString("scientificName", ""),
            characteristics = j.getStringList("characteristics"),
            habitat = j.optString("habitat", ""),
            careInfo = j.optString("careInfo", ""),
            calories = j.optString("calories", ""),
            nutritionalValues = nutMap,
            healthyAlternatives = j.getStringList("healthyAlternatives"),
            webResults = webList,
            similarImages = imgList,
            relatedProducts = prodList,
            aiSummary = j.optString("aiSummary", "")
        )
    }

    // ── Helper extensions ─────────────────────────────────────────────────
    private fun JSONObject.optStringOrNull(key: String): String? =
        if (has(key) && !isNull(key)) getString(key) else null

    private fun JSONObject.getStringList(key: String): List<String> {
        if (!has(key) || isNull(key)) return emptyList()
        val arr = getJSONArray(key)
        return (0 until arr.length()).map { arr.getString(it) }
    }

    private fun JSONObject.putStringList(key: String, list: List<String>): JSONObject {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        return put(key, arr)
    }

    // ── Translation ───────────────────────────────────────────────────────
    fun serializeTranslation(d: TranslationHistoryDetail): String = JSONObject().apply {
        put("originalText", d.originalText)
        put("translatedText", d.translatedText)
        put("sourceLang", d.sourceLang)
        put("targetLang", d.targetLang)
        put("meaningInSource", d.meaningInSource ?: JSONObject.NULL)
        put("meaningInTarget", d.meaningInTarget ?: JSONObject.NULL)
        put("meaningInEnglish", d.meaningInEnglish ?: JSONObject.NULL)
        put("context", d.context ?: JSONObject.NULL)
        putStringList("usageExamples", d.usageExamples)
        putStringList("alternativeMeanings", d.alternativeMeanings)
        put("pronunciation", d.pronunciation ?: JSONObject.NULL)
    }.toString()

    fun deserializeTranslation(json: String): TranslationHistoryDetail? = runCatching {
        val j = JSONObject(json)
        TranslationHistoryDetail(
            originalText = j.getString("originalText"),
            translatedText = j.getString("translatedText"),
            sourceLang = j.getString("sourceLang"),
            targetLang = j.getString("targetLang"),
            meaningInSource = j.optStringOrNull("meaningInSource"),
            meaningInTarget = j.optStringOrNull("meaningInTarget"),
            meaningInEnglish = j.optStringOrNull("meaningInEnglish"),
            context = j.optStringOrNull("context"),
            usageExamples = j.getStringList("usageExamples"),
            alternativeMeanings = j.getStringList("alternativeMeanings"),
            pronunciation = j.optStringOrNull("pronunciation")
        )
    }.getOrNull()

    // ── Voice ─────────────────────────────────────────────────────────────
    fun serializeVoice(d: VoiceHistoryDetail): String = JSONObject().apply {
        val arr = JSONArray()
        d.entries.forEach { e ->
            arr.put(JSONObject().apply {
                put("originalText", e.originalText)
                put("translatedText", e.translatedText)
                put("sourceLang", e.sourceLang)
                put("targetLang", e.targetLang)
                put("isFromUser", e.isFromUser)
                put("meaningInSource", e.meaningInSource ?: JSONObject.NULL)
                put("meaningInTarget", e.meaningInTarget ?: JSONObject.NULL)
                put("meaningInEnglish", e.meaningInEnglish ?: JSONObject.NULL)
                put("context", e.context ?: JSONObject.NULL)
                putStringList("usageExamples", e.usageExamples)
                putStringList("alternativeMeanings", e.alternativeMeanings)
                put("pronunciation", e.pronunciation ?: JSONObject.NULL)
            })
        }
        put("entries", arr)
    }.toString()

    fun deserializeVoice(json: String): VoiceHistoryDetail? = runCatching {
        val j = JSONObject(json)
        val arr = j.getJSONArray("entries")
        val entries = (0 until arr.length()).map { i ->
            val e = arr.getJSONObject(i)
            VoiceEntryDetail(
                originalText = e.getString("originalText"),
                translatedText = e.getString("translatedText"),
                sourceLang = e.getString("sourceLang"),
                targetLang = e.getString("targetLang"),
                isFromUser = e.optBoolean("isFromUser", true),
                meaningInSource = e.optStringOrNull("meaningInSource"),
                meaningInTarget = e.optStringOrNull("meaningInTarget"),
                meaningInEnglish = e.optStringOrNull("meaningInEnglish"),
                context = e.optStringOrNull("context"),
                usageExamples = e.getStringList("usageExamples"),
                alternativeMeanings = e.getStringList("alternativeMeanings"),
                pronunciation = e.optStringOrNull("pronunciation")
            )
        }
        VoiceHistoryDetail(entries)
    }.getOrNull()

    // ── OCR ───────────────────────────────────────────────────────────────
    fun serializeOcr(d: OcrHistoryDetail): String = JSONObject().apply {
        put("extractedText", d.extractedText)
        put("translatedText", d.translatedText ?: JSONObject.NULL)
        put("sourceLang", d.sourceLang)
        put("targetLang", d.targetLang ?: JSONObject.NULL)
        put("meaningInSource", d.meaningInSource ?: JSONObject.NULL)
        put("meaningInTarget", d.meaningInTarget ?: JSONObject.NULL)
        put("meaningInEnglish", d.meaningInEnglish ?: JSONObject.NULL)
        put("context", d.context ?: JSONObject.NULL)
        putStringList("usageExamples", d.usageExamples)
        putStringList("alternativeMeanings", d.alternativeMeanings)
        put("pronunciation", d.pronunciation ?: JSONObject.NULL)
        put("wordCount", d.wordCount)
        put("paragraphCount", d.paragraphCount)
    }.toString()

    fun deserializeOcr(json: String): OcrHistoryDetail? = runCatching {
        val j = JSONObject(json)
        OcrHistoryDetail(
            extractedText = j.getString("extractedText"),
            translatedText = j.optStringOrNull("translatedText"),
            sourceLang = j.getString("sourceLang"),
            targetLang = j.optStringOrNull("targetLang"),
            meaningInSource = j.optStringOrNull("meaningInSource"),
            meaningInTarget = j.optStringOrNull("meaningInTarget"),
            meaningInEnglish = j.optStringOrNull("meaningInEnglish"),
            context = j.optStringOrNull("context"),
            usageExamples = j.getStringList("usageExamples"),
            alternativeMeanings = j.getStringList("alternativeMeanings"),
            pronunciation = j.optStringOrNull("pronunciation"),
            wordCount = j.optInt("wordCount", 0),
            paragraphCount = j.optInt("paragraphCount", 0)
        )
    }.getOrNull()

    // ── QR ─────────────────────────────────────────────────────────────────
    fun serializeQr(d: QrHistoryDetail): String = JSONObject().apply {
        put("rawValue", d.rawValue)
        put("displayValue", d.displayValue)
        put("contentType", d.contentType)
        put("formatName", d.formatName)
        put("wifiSsid", d.wifiSsid ?: JSONObject.NULL)
        put("wifiPassword", d.wifiPassword ?: JSONObject.NULL)
        put("wifiSecurity", d.wifiSecurity ?: JSONObject.NULL)
        put("timestamp", d.timestamp)
    }.toString()

    fun deserializeQr(json: String): QrHistoryDetail? = runCatching {
        val j = JSONObject(json)
        QrHistoryDetail(
            rawValue = j.getString("rawValue"),
            displayValue = j.getString("displayValue"),
            contentType = j.getString("contentType"),
            formatName = j.getString("formatName"),
            wifiSsid = j.optStringOrNull("wifiSsid"),
            wifiPassword = j.optStringOrNull("wifiPassword"),
            wifiSecurity = j.optStringOrNull("wifiSecurity"),
            timestamp = j.optLong("timestamp", 0L)
        )
    }.getOrNull()

    // ── Object Detection ──────────────────────────────────────────────────
    fun serializeObject(d: ObjectHistoryDetail): String = JSONObject().apply {
        put("label", d.label)
        put("confidence", d.confidence.toDouble())
        put("category", d.category)
        put("description", d.description)
        putStringList("uses", d.uses)
        put("safetyInfo", d.safetyInfo)
        put("environmentalImpact", d.environmentalImpact)
        put("funFact", d.funFact)
        put("emoji", d.emoji)
        putStringList("maintenanceTips", d.maintenanceTips)
        putStringList("buyingSuggestions", d.buyingSuggestions)
        put("imagePath", d.imagePath ?: JSONObject.NULL)
        
        val sDataObj = serializeSearchData(d.searchData)
        put("searchData", sDataObj ?: JSONObject.NULL)
    }.toString()

    fun deserializeObject(json: String): ObjectHistoryDetail? = runCatching {
        val j = JSONObject(json)
        ObjectHistoryDetail(
            label = j.getString("label"),
            confidence = j.getDouble("confidence").toFloat(),
            category = j.getString("category"),
            description = j.getString("description"),
            uses = j.getStringList("uses"),
            safetyInfo = j.optString("safetyInfo", ""),
            environmentalImpact = j.optString("environmentalImpact", ""),
            funFact = j.optString("funFact", ""),
            emoji = j.optString("emoji", ""),
            maintenanceTips = j.getStringList("maintenanceTips"),
            buyingSuggestions = j.getStringList("buyingSuggestions"),
            imagePath = j.optStringOrNull("imagePath"),
            searchData = j.deserializeSearchData()
        )
    }.getOrNull()

    // ── Waste Classification ──────────────────────────────────────────────
    fun serializeWaste(d: WasteHistoryDetail): String = JSONObject().apply {
        put("wasteType", d.wasteType)
        put("exactItemName", d.exactItemName)
        put("confidence", d.confidence.toDouble())
        put("disposalMethod", d.disposalMethod)
        putStringList("disposalSteps", d.disposalSteps)
        put("reuseSuggestions", d.reuseSuggestions)
        put("environmentalImpact", d.environmentalImpact)
        put("ecoScore", d.ecoScore)
        put("carbonReduction", d.carbonReduction)
        put("carbonImpact", d.carbonImpact)
        put("isRecyclable", d.isRecyclable)
        put("isReusable", d.isReusable)
        put("sustainabilityTip", d.sustainabilityTip)
        put("imagePath", d.imagePath ?: JSONObject.NULL)
        put("safetyPrecautions", d.safetyPrecautions)
    }.toString()

    fun deserializeWaste(json: String): WasteHistoryDetail? = runCatching {
        val j = JSONObject(json)
        WasteHistoryDetail(
            wasteType = j.getString("wasteType"),
            exactItemName = j.optString("exactItemName", ""),
            confidence = j.getDouble("confidence").toFloat(),
            disposalMethod = j.optString("disposalMethod", ""),
            disposalSteps = j.getStringList("disposalSteps"),
            reuseSuggestions = j.optString("reuseSuggestions", ""),
            environmentalImpact = j.optString("environmentalImpact", ""),
            ecoScore = j.optInt("ecoScore", 50),
            carbonReduction = j.optString("carbonReduction", ""),
            carbonImpact = j.optString("carbonImpact", ""),
            isRecyclable = j.optBoolean("isRecyclable", true),
            isReusable = j.optBoolean("isReusable", true),
            sustainabilityTip = j.optString("sustainabilityTip", ""),
            imagePath = j.optStringOrNull("imagePath"),
            safetyPrecautions = j.optString("safetyPrecautions", "")
        )
    }.getOrNull()

    // ── Medicine ──────────────────────────────────────────────────────────
    fun serializeMedicine(d: MedicineHistoryDetail): String = JSONObject().apply {
        put("name", d.name)
        put("genericName", d.genericName)
        put("category", d.category)
        put("composition", d.composition)
        put("dosage", d.dosage)
        put("frequency", d.frequency)
        putStringList("uses", d.uses)
        putStringList("sideEffects", d.sideEffects)
        putStringList("warnings", d.warnings)
        putStringList("interactions", d.interactions)
        put("safetyScore", d.safetyScore)
        put("safetyLevel", d.safetyLevel)
        put("aiSummary", d.aiSummary)
        put("storageInfo", d.storageInfo)
        put("prescriptionOnly", d.prescriptionOnly)
        putStringList("whoShouldAvoid", d.whoShouldAvoid)
        put("emergencyWarning", d.emergencyWarning ?: JSONObject.NULL)
        put("imagePath", d.imagePath ?: JSONObject.NULL)
        put("manufacturer", d.manufacturer)
        putStringList("alternatives", d.alternatives)
    }.toString()

    fun deserializeMedicine(json: String): MedicineHistoryDetail? = runCatching {
        val j = JSONObject(json)
        MedicineHistoryDetail(
            name = j.getString("name"),
            genericName = j.getString("genericName"),
            category = j.getString("category"),
            composition = j.optString("composition", ""),
            dosage = j.optString("dosage", ""),
            frequency = j.optString("frequency", ""),
            uses = j.getStringList("uses"),
            sideEffects = j.getStringList("sideEffects"),
            warnings = j.getStringList("warnings"),
            interactions = j.getStringList("interactions"),
            safetyScore = j.optInt("safetyScore", 5),
            safetyLevel = j.optString("safetyLevel", "MODERATE"),
            aiSummary = j.optString("aiSummary", ""),
            storageInfo = j.optString("storageInfo", ""),
            prescriptionOnly = j.optBoolean("prescriptionOnly", false),
            whoShouldAvoid = j.getStringList("whoShouldAvoid"),
            emergencyWarning = j.optStringOrNull("emergencyWarning"),
            imagePath = j.optStringOrNull("imagePath"),
            manufacturer = j.optString("manufacturer", ""),
            alternatives = j.getStringList("alternatives")
        )
    }.getOrNull()

    // ── Student Helper ────────────────────────────────────────────────────
    fun serializeStudent(d: StudentHistoryDetail): String = JSONObject().apply {
        put("question", d.question)
        put("answer", d.answer)
        put("subject", d.subject)
    }.toString()

    fun deserializeStudent(json: String): StudentHistoryDetail? = runCatching {
        val j = JSONObject(json)
        StudentHistoryDetail(
            question = j.getString("question"),
            answer = j.getString("answer"),
            subject = j.optString("subject", "")
        )
    }.getOrNull()

    // ── Career Assistant ──────────────────────────────────────────────────
    fun serializeCareer(d: CareerHistoryDetail): String = JSONObject().apply {
        put("actionType", d.actionType)
        put("title", d.title)
        put("date", d.date)
        put("details", d.details)
        put("atsScore", d.atsScore)
        put("generatedContent", d.generatedContent)
        put("interviewQuestionsJson", d.interviewQuestionsJson)
    }.toString()

    fun deserializeCareer(json: String): CareerHistoryDetail? = runCatching {
        val j = JSONObject(json)
        CareerHistoryDetail(
            actionType = j.getString("actionType"),
            title = j.getString("title"),
            date = j.getString("date"),
            details = j.getString("details"),
            atsScore = j.optInt("atsScore", -1),
            generatedContent = j.optString("generatedContent", ""),
            interviewQuestionsJson = j.optString("interviewQuestionsJson", "")
        )
    }.getOrNull()
}
