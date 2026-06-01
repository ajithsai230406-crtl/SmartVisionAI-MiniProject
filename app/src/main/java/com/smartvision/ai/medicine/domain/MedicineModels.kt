package com.smartvision.ai.medicine.domain

import java.util.Date

// ══════════════════════════════════════════════════════════════════════════════
// FULL MEDICINE INFO MODEL — 20 fields
// ══════════════════════════════════════════════════════════════════════════════

data class MedicineInfo(
    val id:                  String  = java.util.UUID.randomUUID().toString(),
    // Identity
    val name:                String,
    val brandName:           String  = name,
    val genericName:         String,
    val composition:         String  = "",
    val category:            String,
    val medicineType:        MedicineType = MedicineType.TABLET,
    // Dosage
    val dosage:              String,
    val frequency:           String,
    val ageGroup:            String  = "Adults 18+",
    val beforeOrAfterFood:   String  = "After food",
    val howToUse:            String  = "Swallow with water",
    // Medical
    val uses:                List<String>,
    val symptomsItTreats:    List<String> = emptyList(),
    val whenToUse:           String  = "",
    val whoShouldAvoid:      List<String> = emptyList(),
    val sideEffects:         List<String>,
    val allergyWarnings:     List<String> = emptyList(),
    val interactions:        List<String>,
    val warnings:            List<String>,
    // Safety
    val prescriptionOnly:    Boolean,
    val safetyScore:         Int,      // 1-10
    val safetyLevel:         SafetyLevel = SafetyLevel.MODERATE,
    val emergencyWarning:    String? = null,
    // Storage
    val storageInfo:         String,
    val expiryNote:          String  = "Check expiry date on package",
    // AI
    val aiSummary:           String  = "",
    val disclaimer:          String  = DISCLAIMER,
    // Meta
    val isFavorite:          Boolean = false,
    val scannedAt:           Date    = Date(),
    val rawOcrText:          String  = ""
)

enum class MedicineType(val label: String, val emoji: String) {
    TABLET("Tablet", "💊"), CAPSULE("Capsule", "💊"), SYRUP("Syrup", "🍶"),
    INJECTION("Injection", "💉"), OINTMENT("Ointment", "🧴"),
    DROPS("Drops", "💧"), INHALER("Inhaler", "🌬️"), PATCH("Patch", "🩹"),
    POWDER("Powder", "⚗️"), SUPPOSITORY("Suppository", "💊")
}

enum class SafetyLevel(val label: String, val colorHex: Long) {
    SAFE("Safe OTC", 0xFF00E676), MODERATE("Use with Care", 0xFFFFD166),
    CAUTION("Prescription Only", 0xFFFF8C00), DANGER("High Risk", 0xFFFF4D6D)
}

// ── Scan state machine ─────────────────────────────────────────────────────────
sealed class MedicineScanState {
    object Idle           : MedicineScanState()
    object CameraOpen     : MedicineScanState()
    object Capturing      : MedicineScanState()
    data class OcrRunning(val progress: Float = 0.3f) : MedicineScanState()
    data class AiAnalyzing(val progress: Float = 0.7f) : MedicineScanState()
    data class Result(val info: MedicineInfo) : MedicineScanState()
    data class Error(val message: String, val canRetry: Boolean = true) : MedicineScanState()
}

// ── Saved record ───────────────────────────────────────────────────────────────
data class MedicineScanRecord(
    val id:         String = java.util.UUID.randomUUID().toString(),
    val info:       MedicineInfo,
    val isFavorite: Boolean = false,
    val scannedAt:  Date    = Date()
)

const val DISCLAIMER = "⚕️ Medical information is AI-generated for educational purposes only. Always consult a certified doctor or pharmacist before consuming any medicine. In case of emergency call 112."

// ══════════════════════════════════════════════════════════════════════════════
// OFFLINE MEDICINE DATABASE — 8 common medicines with all fields
// ══════════════════════════════════════════════════════════════════════════════

object MedicineDatabase {
    val medicines = listOf(

        MedicineInfo(
            name             = "Paracetamol 500mg",
            brandName        = "Crocin / Dolo 650 / Tylenol",
            genericName      = "Acetaminophen",
            composition      = "Paracetamol 500mg",
            category         = "Analgesic / Antipyretic",
            medicineType     = MedicineType.TABLET,
            dosage           = "500mg – 1000mg per dose",
            frequency        = "Every 4–6 hours (max 4g/day)",
            ageGroup         = "Adults & children 12+",
            beforeOrAfterFood= "Can be taken with or without food",
            howToUse         = "Swallow whole with a glass of water",
            uses             = listOf("Fever reduction", "Mild to moderate pain relief", "Headache & migraine", "Toothache", "Body aches", "Cold & flu symptoms"),
            symptomsItTreats = listOf("Fever above 38°C", "Headache", "Body pain", "Dental pain"),
            whenToUse        = "When experiencing fever, pain, or discomfort",
            whoShouldAvoid   = listOf("People with liver disease", "Heavy alcohol drinkers", "People with kidney failure"),
            sideEffects      = listOf("Nausea (rare)", "Skin rash (rare)", "Liver damage (with overdose)", "Allergic reactions (rare)"),
            allergyWarnings  = listOf("Allergy to acetaminophen", "Allergy to paracetamol-containing products"),
            interactions     = listOf("Warfarin — increases bleeding risk", "Alcohol — liver damage risk", "Other paracetamol products — overdose risk", "Phenytoin — reduced effectiveness"),
            warnings         = listOf("⚠️ Do not exceed 4g/day (adults)", "⚠️ Avoid with alcohol", "⚠️ Check all medicines for paracetamol content", "⚠️ Consult doctor if symptoms persist > 3 days"),
            storageInfo      = "Store below 30°C, away from direct sunlight and moisture",
            expiryNote       = "Do not use after expiry date printed on pack",
            prescriptionOnly = false,
            safetyScore      = 9,
            safetyLevel      = SafetyLevel.SAFE,
            aiSummary        = "Paracetamol is one of the safest and most widely used pain relievers. It effectively reduces fever and relieves mild-to-moderate pain. It's generally safe when taken at the correct dose."
        ),

        MedicineInfo(
            name             = "Ibuprofen 400mg",
            brandName        = "Advil / Brufen / Nurofen",
            genericName      = "Ibuprofen",
            composition      = "Ibuprofen BP 400mg",
            category         = "NSAID / Anti-inflammatory",
            medicineType     = MedicineType.TABLET,
            dosage           = "200–400mg per dose",
            frequency        = "Every 6–8 hours (max 1200mg/day OTC)",
            ageGroup         = "Adults & children 12+ (lower doses for 6-12)",
            beforeOrAfterFood= "Always take after food or milk",
            howToUse         = "Take with food to protect stomach lining",
            uses             = listOf("Pain relief", "Inflammation reduction", "Arthritis", "Menstrual cramps", "Dental pain", "Sports injuries", "Headache"),
            symptomsItTreats = listOf("Joint pain", "Muscle pain", "Dental pain", "Fever", "Menstrual pain"),
            whenToUse        = "For pain and inflammation — always with food",
            whoShouldAvoid   = listOf("Pregnant women (3rd trimester)", "People with peptic ulcers", "People with kidney disease", "People with heart failure", "Children under 6"),
            sideEffects      = listOf("Stomach upset / indigestion", "Heartburn", "Nausea", "Dizziness", "Increased blood pressure", "Kidney strain with prolonged use"),
            allergyWarnings  = listOf("Allergy to NSAIDs or aspirin", "Asthma triggered by aspirin"),
            interactions     = listOf("Aspirin — reduced effectiveness", "Blood thinners — bleeding risk", "ACE inhibitors — reduced BP control", "Lithium — toxicity risk", "Diuretics — reduced effectiveness"),
            warnings         = listOf("⚠️ Must take with food or milk", "⚠️ Not for children under 6", "⚠️ Avoid if history of peptic ulcer", "⚠️ Long-term use may harm kidneys & heart"),
            storageInfo      = "Store at room temperature 15–25°C, away from moisture",
            prescriptionOnly = false,
            safetyScore      = 7,
            safetyLevel      = SafetyLevel.MODERATE,
            aiSummary        = "Ibuprofen is a powerful anti-inflammatory that relieves pain and reduces fever. Always take it with food to protect your stomach. Avoid if you have stomach ulcers, kidney problems, or heart disease."
        ),

        MedicineInfo(
            name             = "Amoxicillin 500mg",
            brandName        = "Amoxil / Trimox / Novamox",
            genericName      = "Amoxicillin Trihydrate",
            composition      = "Amoxicillin (as trihydrate) 500mg",
            category         = "Antibiotic (Penicillin class)",
            medicineType     = MedicineType.CAPSULE,
            dosage           = "250–500mg per dose",
            frequency        = "Every 8 hours for 5–10 days",
            ageGroup         = "Adults & children (weight-based for children)",
            beforeOrAfterFood= "Can be taken with or without food",
            howToUse         = "Complete the full course even if feeling better",
            uses             = listOf("Bacterial infections", "Strep throat", "Ear infections", "Urinary tract infections", "Pneumonia", "Skin infections", "Dental abscess"),
            symptomsItTreats = listOf("Sore throat (bacterial)", "Ear pain", "Burning urination (UTI)", "Chest infection"),
            whenToUse        = "Only when prescribed by a doctor for bacterial infection",
            whoShouldAvoid   = listOf("People allergic to penicillin", "People allergic to cephalosporins (possible cross-allergy)", "Patients with mononucleosis (causes rash)"),
            sideEffects      = listOf("Diarrhea (common)", "Nausea", "Skin rash", "Yeast infections", "Severe allergic reaction — anaphylaxis (rare but serious)"),
            allergyWarnings  = listOf("⚠️ CRITICAL: If allergic to penicillin — DO NOT USE", "Inform doctor of all allergies before starting"),
            interactions     = listOf("Warfarin — increased bleeding risk", "Methotrexate — toxicity risk", "Birth control pills — may reduce effectiveness", "Probenecid — increases amoxicillin levels"),
            warnings         = listOf("⚠️ PRESCRIPTION REQUIRED", "⚠️ Complete the full prescribed course", "⚠️ LIFE-THREATENING if penicillin allergy exists", "⚠️ May cause antibiotic-associated diarrhea"),
            storageInfo      = "Store capsules at 20–25°C. Refrigerate reconstituted suspension (2–8°C), use within 14 days",
            prescriptionOnly = true,
            safetyScore      = 8,
            safetyLevel      = SafetyLevel.CAUTION,
            emergencyWarning = "If throat swells or breathing becomes difficult after taking, call 112 immediately — possible anaphylaxis.",
            aiSummary        = "Amoxicillin is an antibiotic used to treat bacterial infections. It requires a prescription and must be taken for the complete course. NEVER use if you are allergic to penicillin — this can cause a life-threatening reaction."
        ),

        MedicineInfo(
            name             = "Omeprazole 20mg",
            brandName        = "Prilosec / Losec / Omez",
            genericName      = "Omeprazole",
            composition      = "Omeprazole 20mg",
            category         = "Proton Pump Inhibitor (PPI)",
            medicineType     = MedicineType.CAPSULE,
            dosage           = "20mg once daily",
            frequency        = "Once daily before breakfast",
            ageGroup         = "Adults 18+",
            beforeOrAfterFood= "30 minutes before breakfast (before food)",
            howToUse         = "Swallow whole — do not crush or chew the capsule",
            uses             = listOf("Acid reflux / GERD", "Peptic ulcers", "H. pylori eradication", "Zollinger-Ellison syndrome", "Prevention of NSAID-induced ulcers"),
            symptomsItTreats = listOf("Heartburn", "Acid reflux", "Stomach ulcer pain", "Excessive stomach acid"),
            whenToUse        = "For persistent heartburn or acid reflux — 30 min before meals",
            whoShouldAvoid   = listOf("People with allergy to PPIs", "People with low magnesium (long-term use)", "Pregnant women (consult doctor)"),
            sideEffects      = listOf("Headache", "Diarrhea or constipation", "Nausea", "Low magnesium (long-term use)", "Increased fracture risk (prolonged use)"),
            allergyWarnings  = listOf("Allergy to omeprazole or other PPIs"),
            interactions     = listOf("Clopidogrel — reduced effectiveness", "Methotrexate — increased toxicity", "HIV antivirals — reduced effectiveness", "Warfarin — increased effect"),
            warnings         = listOf("⚠️ Do not use long-term without doctor supervision", "⚠️ May mask symptoms of stomach cancer", "⚠️ Can cause low magnesium with prolonged use", "⚠️ Swallow capsule whole — never crush"),
            storageInfo      = "Store below 25°C in a dry place, away from light",
            prescriptionOnly = false,
            safetyScore      = 8,
            safetyLevel      = SafetyLevel.MODERATE,
            aiSummary        = "Omeprazole reduces stomach acid production. Take it 30 minutes before breakfast for best effect. Not suitable for long-term use without medical supervision."
        ),

        MedicineInfo(
            name             = "Cetirizine 10mg",
            brandName        = "Zyrtec / Alerid / Cetzine",
            genericName      = "Cetirizine Hydrochloride",
            composition      = "Cetirizine HCl 10mg",
            category         = "Antihistamine (2nd generation)",
            medicineType     = MedicineType.TABLET,
            dosage           = "10mg once daily",
            frequency        = "Once daily (evening preferred)",
            ageGroup         = "Adults & children 6+",
            beforeOrAfterFood= "With or without food",
            howToUse         = "Best taken at night as it may cause mild drowsiness",
            uses             = listOf("Allergic rhinitis (hay fever)", "Urticaria (hives)", "Allergic conjunctivitis", "Skin allergies", "Insect bite reactions"),
            symptomsItTreats = listOf("Runny nose", "Sneezing", "Watery eyes", "Skin itching", "Hives"),
            whenToUse        = "For allergy symptoms — can be taken daily during allergy season",
            whoShouldAvoid   = listOf("People with severe kidney disease", "People with allergy to cetirizine or hydroxyzine"),
            sideEffects      = listOf("Drowsiness (mild)", "Dry mouth", "Headache", "Dizziness", "Fatigue"),
            allergyWarnings  = listOf("Allergy to cetirizine or hydroxyzine"),
            interactions     = listOf("Alcohol — increased drowsiness", "CNS depressants — increased sedation", "Theophylline — reduced clearance of cetirizine"),
            warnings         = listOf("⚠️ May cause drowsiness — avoid driving or heavy machinery", "⚠️ Avoid alcohol while taking", "⚠️ Reduce dose in kidney disease"),
            storageInfo      = "Store below 30°C in a dry place",
            prescriptionOnly = false,
            safetyScore      = 9,
            safetyLevel      = SafetyLevel.SAFE,
            aiSummary        = "Cetirizine is a safe, non-drowsy antihistamine for allergy relief. It works within 1 hour and lasts 24 hours. Best taken at night as mild drowsiness is possible."
        ),

        MedicineInfo(
            name             = "Metformin 500mg",
            brandName        = "Glucophage / Glycomet / Fortamet",
            genericName      = "Metformin Hydrochloride",
            composition      = "Metformin HCl 500mg",
            category         = "Antidiabetic (Biguanide)",
            medicineType     = MedicineType.TABLET,
            dosage           = "500–2550mg per day (divided doses)",
            frequency        = "2–3 times daily with meals",
            ageGroup         = "Adults 18+ (and children 10+ for type 2 diabetes)",
            beforeOrAfterFood= "Always with meals to reduce stomach side effects",
            howToUse         = "Take with meals, do not crush extended-release tablets",
            uses             = listOf("Type 2 diabetes management", "PCOS (polycystic ovary syndrome)", "Prediabetes prevention", "Insulin resistance"),
            symptomsItTreats = listOf("High blood sugar", "Insulin resistance", "PCOS symptoms"),
            whenToUse        = "As prescribed for blood sugar control — always with food",
            whoShouldAvoid   = listOf("People with kidney disease (eGFR < 30)", "People with liver disease", "Alcoholics", "Before CT scan with contrast dye"),
            sideEffects      = listOf("Nausea and vomiting", "Diarrhea (common initially)", "Stomach upset", "Metallic taste in mouth", "Vitamin B12 deficiency (long-term)", "Lactic acidosis (rare, serious)"),
            allergyWarnings  = listOf("Allergy to metformin or biguanides"),
            interactions     = listOf("Alcohol — lactic acidosis risk", "Iodinated contrast dye — stop 48h before", "Diuretics — reduced effectiveness", "Corticosteroids — increased blood sugar"),
            warnings         = listOf("⚠️ PRESCRIPTION REQUIRED", "⚠️ Stop before surgery or CT with contrast", "⚠️ Monitor kidney function regularly", "⚠️ Never use in severe kidney or liver disease", "🚨 Lactic acidosis: Stop immediately if severe muscle pain, breathing difficulty"),
            storageInfo      = "Store at 20–25°C, away from heat and moisture",
            prescriptionOnly = true,
            safetyScore      = 7,
            safetyLevel      = SafetyLevel.CAUTION,
            emergencyWarning = "If experiencing severe muscle pain, difficulty breathing, or unusual sleepiness, seek emergency care — possible lactic acidosis.",
            aiSummary        = "Metformin is the first-line medication for type 2 diabetes. It lowers blood sugar by reducing liver glucose production. Always take with meals and never skip doses without consulting your doctor."
        ),

        MedicineInfo(
            name             = "Azithromycin 500mg",
            brandName        = "Zithromax / Azee / Z-Pack",
            genericName      = "Azithromycin Dihydrate",
            composition      = "Azithromycin 500mg",
            category         = "Antibiotic (Macrolide class)",
            medicineType     = MedicineType.TABLET,
            dosage           = "500mg day 1, then 250mg days 2–5",
            frequency        = "Once daily (Z-Pack: 5-day course)",
            ageGroup         = "Adults & children > 6 months (weight-based)",
            beforeOrAfterFood= "1 hour before or 2 hours after meals",
            howToUse         = "Complete the 5-day course even if feeling better",
            uses             = listOf("Community-acquired pneumonia", "Sinusitis", "Skin infections", "Sexually transmitted infections", "Ear infections (children)", "Typhoid fever"),
            symptomsItTreats = listOf("Chest infection", "Sinus congestion", "STI symptoms", "Ear pain"),
            whenToUse        = "Only for confirmed bacterial infections — as prescribed",
            whoShouldAvoid   = listOf("People with known QT prolongation", "People with severe liver disease", "People allergic to macrolide antibiotics"),
            sideEffects      = listOf("Nausea", "Diarrhea", "Stomach pain", "Vomiting", "Heart rhythm changes (prolonged QT — rare)", "Liver function changes"),
            allergyWarnings  = listOf("Allergy to azithromycin, erythromycin, or other macrolides"),
            interactions     = listOf("Antacids — reduce absorption (take 1h apart)", "Warfarin — increased bleeding", "Digoxin — increased levels", "QT-prolonging drugs — risk of arrhythmia"),
            warnings         = listOf("⚠️ PRESCRIPTION REQUIRED", "⚠️ Not to be used for viral infections (cold/flu)", "⚠️ Inform doctor of heart conditions", "⚠️ Take 1h before or 2h after antacids"),
            storageInfo      = "Store at room temperature 15–30°C, away from moisture and light",
            prescriptionOnly = true,
            safetyScore      = 7,
            safetyLevel      = SafetyLevel.CAUTION,
            aiSummary        = "Azithromycin is a short-course antibiotic effective for respiratory, skin, and certain other bacterial infections. The Z-Pack (5 days) stays in your body for up to 10 days after the last dose."
        ),

        MedicineInfo(
            name             = "Pantoprazole 40mg",
            brandName        = "Protonix / Pan 40 / Pantocid",
            genericName      = "Pantoprazole Sodium",
            composition      = "Pantoprazole Sodium 40mg (as sesquihydrate)",
            category         = "Proton Pump Inhibitor (PPI)",
            medicineType     = MedicineType.TABLET,
            dosage           = "40mg once daily",
            frequency        = "Once daily, 30–60 min before breakfast",
            ageGroup         = "Adults 18+",
            beforeOrAfterFood= "30–60 minutes before a meal (before food)",
            howToUse         = "Swallow whole with water — do not crush or split",
            uses             = listOf("GERD treatment", "Peptic and duodenal ulcers", "Zollinger-Ellison syndrome", "H. pylori eradication (combination therapy)", "Prevention of stress ulcers"),
            symptomsItTreats = listOf("Severe heartburn", "Acid reflux", "Stomach ulcer pain", "Sour belching"),
            whenToUse        = "For persistent or severe acid-related symptoms — before meals",
            whoShouldAvoid   = listOf("Allergy to pantoprazole or other PPIs", "Patients taking rilpivirine (HIV medicine)"),
            sideEffects      = listOf("Headache", "Diarrhea", "Nausea", "Abdominal pain", "Low magnesium (long-term)", "Bone fracture risk (long-term)"),
            allergyWarnings  = listOf("Allergy to pantoprazole or substituted benzimidazoles"),
            interactions     = listOf("Warfarin — increased INR", "Methotrexate — increased toxicity", "Rilpivirine — CONTRAINDICATED", "Iron supplements — reduced absorption"),
            warnings         = listOf("⚠️ Do not use continuously > 8 weeks without review", "⚠️ May reduce magnesium with long-term use", "⚠️ Do not crush enteric-coated tablet", "⚠️ Report persistent symptoms — may mask cancer"),
            storageInfo      = "Store below 25°C in original packaging to protect from light",
            prescriptionOnly = false,
            safetyScore      = 8,
            safetyLevel      = SafetyLevel.MODERATE,
            aiSummary        = "Pantoprazole powerfully suppresses stomach acid and is effective for acid reflux and ulcers. Take it 30–60 minutes before your first meal for best results. Don't use long-term without medical guidance."
        )
    )

    fun findByName(query: String): MedicineInfo? = medicines.firstOrNull {
        it.name.contains(query, true) || it.genericName.contains(query, true) ||
        it.brandName.contains(query, true) || it.composition.contains(query, true)
    }

    fun random(): MedicineInfo = medicines.random()

    fun searchAll(query: String): List<MedicineInfo> = medicines.filter {
        it.name.contains(query, true) || it.genericName.contains(query, true) ||
        it.brandName.contains(query, true) || it.category.contains(query, true)
    }
}
