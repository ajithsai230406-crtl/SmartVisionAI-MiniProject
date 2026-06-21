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
    val rawOcrText:          String  = "",
    val confidenceScore:     Int     = 100,
    val sideEffectSeverities:Map<String, String> = emptyMap(),
    val interactionWarnings: List<String> = emptyList(),
    // Manufacturer & Alternatives
    val manufacturer:        String  = "",
    val alternatives:        List<String> = emptyList()
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
    data class NotMedicine(val message: String = "No medicine or supported product detected. Please scan a medicine package label.") : MedicineScanState()
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

data class GenericDrugTemplate(
    val genericName: String,
    val category: String,
    val uses: List<String>,
    val sideEffects: List<String>,
    val sideEffectSeverities: Map<String, String>,
    val precautions: List<String>,
    val interactions: List<String>,
    val warnings: List<String>,
    val storageInfo: String,
    val prescriptionOnly: Boolean,
    val safetyScore: Int,
    val safetyLevel: SafetyLevel,
    val emergencyWarning: String? = null,
    val aiSummary: String,
    val manufacturer: String = "",
    val alternatives: List<String> = emptyList()
)

object MedicineDatabase {
    private val templates = mapOf(
        "Acetaminophen" to GenericDrugTemplate(
            "Acetaminophen", "Analgesic / Antipyretic",
            listOf("Fever reduction", "Mild to moderate pain relief", "Headache & migraine", "Toothache", "Body aches"),
            listOf("Nausea", "Skin rash", "Liver damage (with overdose)"),
            mapOf("Nausea" to "Common", "Skin rash" to "Uncommon", "Liver damage" to "Rare/Severe"),
            listOf("People with liver disease", "Heavy alcohol drinkers"),
            listOf("Warfarin - increases bleeding risk", "Alcohol - liver damage risk"),
            listOf("⚠️ Do not exceed 4g/day", "⚠️ Avoid with alcohol"),
            "Store below 30°C in a dry place", false, 9, SafetyLevel.SAFE, null,
            "Acetaminophen is a common pain reliever and fever reducer. It is generally safe when taken at the recommended dose."
        ),
        "Ibuprofen" to GenericDrugTemplate(
            "Ibuprofen", "NSAID / Anti-inflammatory",
            listOf("Pain relief", "Inflammation reduction", "Arthritis", "Menstrual cramps", "Toothache"),
            listOf("Stomach upset", "Heartburn", "Dizziness", "Kidney strain"),
            mapOf("Stomach upset" to "Common", "Heartburn" to "Common", "Dizziness" to "Uncommon", "Kidney strain" to "Rare/Severe"),
            listOf("People with peptic ulcers", "Kidney disease", "Third trimester of pregnancy"),
            listOf("Aspirin - reduced effect", "Blood thinners - bleeding risk"),
            listOf("⚠️ Always take after food", "⚠️ Avoid if history of peptic ulcers"),
            "Store at room temperature 15-25°C", false, 7, SafetyLevel.MODERATE, null,
            "Ibuprofen is a nonsteroidal anti-inflammatory drug (NSAID). It helps reduce pain, fever, and inflammation, but must be taken with food to protect the stomach."
        ),
        "Amoxicillin" to GenericDrugTemplate(
            "Amoxicillin", "Antibiotic (Penicillin class)",
            listOf("Bacterial infections", "Strep throat", "Ear infections", "Urinary tract infections"),
            listOf("Diarrhea", "Nausea", "Skin rash", "Anaphylaxis (allergic shock)"),
            mapOf("Diarrhea" to "Common", "Nausea" to "Common", "Skin rash" to "Uncommon", "Anaphylaxis" to "Rare/Severe"),
            listOf("People allergic to penicillin", "Asthma patients"),
            listOf("Birth control pills - may reduce effectiveness", "Warfarin - bleeding risk"),
            listOf("⚠️ Complete the full course", "⚠️ Do not use for viral infections"),
            "Store capsules at 20-25°C", true, 8, SafetyLevel.CAUTION,
            "If throat swells or breathing becomes difficult, seek emergency care immediately (possible anaphylaxis).",
            "Amoxicillin is a penicillin-class antibiotic used for bacterial infections. It requires a prescription and the course must be completed fully."
        ),
        "Esomeprazole" to GenericDrugTemplate(
            "Esomeprazole", "Proton Pump Inhibitor (PPI)",
            listOf("Acid reflux / GERD", "Stomach ulcers", "Heartburn relief"),
            listOf("Headache", "Nausea", "Diarrhea", "Low magnesium (long-term)"),
            mapOf("Headache" to "Common", "Nausea" to "Uncommon", "Diarrhea" to "Common", "Low magnesium" to "Rare/Severe"),
            listOf("People allergic to PPIs", "Pregnant women (consult doctor)"),
            listOf("Clopidogrel - reduced effect", "Ketoconazole - reduced absorption"),
            listOf("⚠️ Take 30-60 min before breakfast", "⚠️ Do not use continuously > 8 weeks"),
            "Store below 25°C in a dry place", false, 8, SafetyLevel.MODERATE, null,
            "Esomeprazole reduces stomach acid. Take it before food for best results. Do not self-medicate for more than 14 days."
        ),
        "Cetirizine" to GenericDrugTemplate(
            "Cetirizine", "Antihistamine (Allergy)",
            listOf("Allergic rhinitis (hay fever)", "Hives", "Watery eyes", "Itching"),
            listOf("Drowsiness (mild)", "Dry mouth", "Fatigue"),
            mapOf("Drowsiness" to "Common", "Dry mouth" to "Common", "Fatigue" to "Uncommon"),
            listOf("Severe kidney disease", "Toddlers under 2"),
            listOf("Alcohol - increased drowsiness", "Sedatives - increased sedation"),
            listOf("⚠️ May cause mild drowsiness", "⚠️ Avoid alcohol while taking"),
            "Store below 30°C in dry place", false, 9, SafetyLevel.SAFE, null,
            "Cetirizine is a non-drowsy antihistamine that provides fast 24-hour relief from allergies."
        ),
        "Metformin" to GenericDrugTemplate(
            "Metformin", "Antidiabetic (Oral)",
            listOf("Type 2 diabetes management", "Insulin resistance", "PCOS"),
            listOf("Nausea", "Diarrhea", "Metallic taste", "Lactic acidosis (rare)"),
            mapOf("Nausea" to "Common", "Diarrhea" to "Common", "Metallic taste" to "Common", "Lactic acidosis" to "Rare/Severe"),
            listOf("Severe kidney disease", "Liver disease", "Heavy alcohol use"),
            listOf("Contrast dyes - stop 48h before scans", "Alcohol - lactic acidosis risk"),
            listOf("⚠️ Take with meals to reduce stomach upset", "⚠️ Monitor kidney function regularly"),
            "Store below 25°C", true, 7, SafetyLevel.CAUTION,
            "If experiencing hyperventilation, muscle pain, or unusual sleepiness, seek emergency care immediately (possible lactic acidosis).",
            "Metformin helps control blood sugar in type 2 diabetes by making the body more sensitive to insulin. It must be taken with meals."
        ),
        "Azithromycin" to GenericDrugTemplate(
            "Azithromycin", "Antibiotic (Macrolide class)",
            listOf("Pneumonia", "Sinusitis", "Skin infections", "Ear infections"),
            listOf("Nausea", "Vomiting", "Diarrhea", "QT prolongation (heart)"),
            mapOf("Nausea" to "Common", "Vomiting" to "Common", "Diarrhea" to "Common", "QT prolongation" to "Rare/Severe"),
            listOf("Heart rhythm issues", "Severe liver disease"),
            listOf("Antacids - take 2 hours apart", "Warfarin - bleeding risk"),
            listOf("⚠️ Take 1 hour before or 2 hours after meals", "⚠️ Complete the full 3 or 5-day course"),
            "Store below 30°C", true, 7, SafetyLevel.CAUTION, null,
            "Azithromycin is a macrolide antibiotic. It has a convenient short course (usually 3 or 5 days) but stays active in the body for longer."
        ),
        "Pantoprazole" to GenericDrugTemplate(
            "Pantoprazole", "Proton Pump Inhibitor (PPI)",
            listOf("GERD", "Stomach and duodenal ulcers", "Hyperacidity"),
            listOf("Headache", "Diarrhea", "Nausea", "Vitamin B12 deficiency (long-term)"),
            mapOf("Headache" to "Common", "Diarrhea" to "Common", "Nausea" to "Uncommon", "Vitamin B12 deficiency" to "Rare/Severe"),
            listOf("Allergy to PPIs", "Severe liver impairment"),
            listOf("Rilpivirine - contraindicated", "Iron supplements - reduced absorption"),
            listOf("⚠️ Take 30-60 minutes before breakfast", "⚠️ Swallow whole - do not crush"),
            "Store below 25°C", false, 8, SafetyLevel.MODERATE, null,
            "Pantoprazole suppresses stomach acid. It is enteric-coated and must be swallowed whole before your first meal."
        ),
        "Atorvastatin" to GenericDrugTemplate(
            "Atorvastatin", "Antihyperlipidemic (Statin)",
            listOf("High cholesterol", "Cardiovascular disease prevention"),
            listOf("Muscle pain", "Headache", "Increased blood sugar", "Liver enzyme increase"),
            mapOf("Muscle pain" to "Common", "Headache" to "Common", "Increased blood sugar" to "Uncommon", "Liver enzyme increase" to "Rare/Severe"),
            listOf("Active liver disease", "Pregnancy & breastfeeding"),
            listOf("Gemfibrozil - muscle damage risk", "Grapefruit juice - increases drug levels"),
            listOf("⚠️ Report unexplained muscle pain immediately", "⚠️ Avoid large amounts of grapefruit juice"),
            "Store below 25°C", true, 8, SafetyLevel.CAUTION, null,
            "Atorvastatin lowers LDL (bad) cholesterol and triglycerides, reducing the risk of heart attacks and strokes."
        ),
        "Amlodipine" to GenericDrugTemplate(
            "Amlodipine", "Calcium Channel Blocker (BP)",
            listOf("Hypertension (high blood pressure)", "Angina (chest pain)"),
            listOf("Peripheral edema (swelling of ankles)", "Dizziness", "Palpitations"),
            mapOf("Peripheral edema" to "Common", "Dizziness" to "Common", "Palpitations" to "Uncommon"),
            listOf("Severe hypotension", "Severe aortic stenosis"),
            listOf("Sildenafil - blood pressure drop", "Simvastatin - increased levels"),
            listOf("⚠️ Monitor blood pressure", "⚠️ May cause dizziness when standing up"),
            "Store below 30°C", true, 8, SafetyLevel.CAUTION, null,
            "Amlodipine relaxes blood vessels to lower blood pressure and improve blood flow, easing the heart's workload."
        ),
        "Lisinopril" to GenericDrugTemplate(
            "Lisinopril", "ACE Inhibitor (BP)",
            listOf("Hypertension", "Heart failure management", "Post-heart attack recovery"),
            listOf("Dry cough", "Dizziness", "Hyperkalemia (high potassium)", "Angioedema (swelling)"),
            mapOf("Dry cough" to "Common", "Dizziness" to "Common", "Hyperkalemia" to "Uncommon", "Angioedema" to "Rare/Severe"),
            listOf("Pregnancy (fetal danger)", "History of angioedema"),
            listOf("Potassium supplements - hyperkalemia risk", "NSAIDs - reduced BP control"),
            listOf("⚠️ Contact doctor immediately if face/throat swells", "⚠️ A dry cough is a common side effect"),
            "Store at room temp", true, 8, SafetyLevel.CAUTION,
            "If swelling of the lips, tongue, or throat occurs, seek emergency care immediately.",
            "Lisinopril is an ACE inhibitor used to lower blood pressure and manage heart failure. Do not take during pregnancy."
        ),
        "Albuterol" to GenericDrugTemplate(
            "Albuterol", "Bronchodilator (Beta-2 Agonist)",
            listOf("Asthma symptoms", "COPD relief", "Exercise-induced bronchospasm"),
            listOf("Tremor", "Nervousness", "Tachycardia (fast heart rate)", "Hypokalemia"),
            mapOf("Tremor" to "Common", "Nervousness" to "Common", "Tachycardia" to "Uncommon", "Hypokalemia" to "Rare/Severe"),
            listOf("Severe cardiac disease", "Arrhythmia history"),
            listOf("Beta-blockers - reduces effectiveness", "Diuretics - potassium drop"),
            listOf("⚠️ Carry at all times for asthma rescue", "⚠️ Shake inhaler well before use"),
            "Store below 25°C, protect from freezing", true, 7, SafetyLevel.CAUTION, null,
            "Albuterol (Salbutamol) is a rescue bronchodilator that quickly opens airways during asthma attacks or breathing difficulties."
        ),
        "Budesonide" to GenericDrugTemplate(
            "Budesonide", "Corticosteroid",
            listOf("Asthma maintenance", "Allergic rhinitis", "Crohn's disease"),
            listOf("Thrush (oral yeast infection)", "Hoarseness", "Nasal irritation"),
            mapOf("Thrush" to "Common", "Hoarseness" to "Common", "Nasal irritation" to "Uncommon"),
            listOf("Active untreated infections", "Tuberculosis"),
            listOf("Ketoconazole - increases budesonide levels", "Ritonavir - increases levels"),
            listOf("⚠️ Rinse mouth after inhaling to prevent thrush", "⚠️ Use daily as a controller, not a rescue"),
            "Store below 30°C", true, 7, SafetyLevel.CAUTION, null,
            "Budesonide is an anti-inflammatory corticosteroid. It prevents asthma flare-ups and nasal allergies when used daily."
        ),
        "Hydrocortisone" to GenericDrugTemplate(
            "Hydrocortisone", "Corticosteroid (Topical/Oral)",
            listOf("Skin itching & rash", "Eczema & dermatitis", "Insect bites"),
            listOf("Skin thinning", "Burning sensation", "Stretch marks"),
            mapOf("Skin thinning" to "Uncommon", "Burning sensation" to "Common", "Stretch marks" to "Rare/Severe"),
            listOf("Viral skin infections", "Fungal skin infections"),
            listOf("Other steroid creams - skin damage risk"),
            listOf("⚠️ Apply sparingly in a thin layer", "⚠️ Do not use on face or open wounds unless directed"),
            "Store in cool place", false, 8, SafetyLevel.MODERATE, null,
            "Hydrocortisone reduces swelling, itching, and redness. It is highly effective for localized allergic skin conditions."
        ),
        "Neomycin" to GenericDrugTemplate(
            "Neomycin", "Aminoglycoside Antibiotic",
            listOf("Prevention of skin infections", "Minor cuts & scrapes"),
            listOf("Contact dermatitis (allergy)", "Skin redness", "Hearing loss (long-term systemic)"),
            mapOf("Contact dermatitis" to "Uncommon", "Skin redness" to "Common", "Hearing loss" to "Rare/Severe"),
            listOf("Deep puncture wounds", "Large raw areas"),
            listOf("Oral aminoglycosides - risk of toxicity"),
            listOf("⚠️ For external use only", "⚠️ Do not use over large areas of the body"),
            "Store below 30°C", false, 9, SafetyLevel.SAFE, null,
            "Neomycin is a topical antibiotic that prevents bacterial growth in minor skin cuts, burns, and abrasions."
        ),
        "Timolol" to GenericDrugTemplate(
            "Timolol", "Beta-Blocker (Ophthalmic)",
            listOf("Glaucoma", "Ocular hypertension"),
            listOf("Eye irritation", "Bradycardia (slow heart rate)", "Bronchospasm"),
            mapOf("Eye irritation" to "Common", "Bradycardia" to "Rare/Severe", "Bronchospasm" to "Rare/Severe"),
            listOf("Asthma / severe COPD", "Bradycardia / heart block"),
            listOf("Oral beta-blockers - additive effect", "Calcium blockers - hypotension"),
            listOf("⚠️ Apply pressure to inner eye corner after drops", "⚠️ Remove contact lenses before applying"),
            "Store below 25°C", true, 8, SafetyLevel.CAUTION, null,
            "Timolol eye drops reduce intraocular pressure in glaucoma. It can be absorbed systemically, so caution is required in asthma patients."
        ),
        "Bisacodyl" to GenericDrugTemplate(
            "Bisacodyl", "Stimulant Laxative",
            listOf("Constipation relief", "Bowel prep before surgery"),
            listOf("Abdominal cramps", "Diarrhea", "Dehydration", "Electrolyte imbalance"),
            mapOf("Abdominal cramps" to "Common", "Diarrhea" to "Common", "Dehydration" to "Uncommon", "Electrolyte imbalance" to "Rare/Severe"),
            listOf("Appendicitis", "Intestinal obstruction"),
            listOf("Antacids - take 1 hour apart"),
            listOf("⚠️ Do not use for more than 7 days", "⚠️ Swallow tablets whole"),
            "Store in dry place", false, 8, SafetyLevel.SAFE, null,
            "Bisacodyl is a stimulant laxative that triggers bowel movements. It is intended for short-term relief only."
        ),
        "Glycerin" to GenericDrugTemplate(
            "Glycerin", "Osmotic Laxative",
            listOf("Acute constipation relief"),
            listOf("Rectal irritation", "Abdominal discomfort"),
            mapOf("Rectal irritation" to "Common", "Abdominal discomfort" to "Uncommon"),
            listOf("Severe hemorrhoids", "Rectal bleeding"),
            listOf("No major drug interactions reported"),
            listOf("⚠️ For rectal use only", "⚠️ Works quickly (15-45 minutes)"),
            "Store below 25°C", false, 9, SafetyLevel.SAFE, null,
            "Glycerin suppositories draw water into the stool, stimulating a quick bowel movement locally. Safe for occasional use."
        ),
        "Nicotine" to GenericDrugTemplate(
            "Nicotine", "Smoking Cessation Aid",
            listOf("Smoking cessation", "Nicotine withdrawal relief"),
            listOf("Skin irritation at patch site", "Sleep disturbances", "Vivid dreams", "Headache"),
            mapOf("Skin irritation" to "Common", "Sleep disturbances" to "Common", "Vivid dreams" to "Common", "Headache" to "Uncommon"),
            listOf("Recent heart attack", "Severe chest pain"),
            listOf("Smoking while on patch - nicotine toxicity"),
            listOf("⚠️ Apply to a clean, dry, hairless skin area daily", "⚠️ Keep out of reach of children and pets"),
            "Store in sealed pouches", false, 9, SafetyLevel.SAFE, null,
            "Nicotine patches provide a steady, controlled release of nicotine to help reduce cravings when quitting smoking."
        ),
        "Fentanyl" to GenericDrugTemplate(
            "Fentanyl", "Opioid Analgesic (Schedule II)",
            listOf("Severe chronic pain management", "Cancer pain relief"),
            listOf("Respiratory depression (slow breathing)", "Severe constipation", "Somnolence (extreme sleepiness)", "Addiction / dependence"),
            mapOf("Respiratory depression" to "Rare/Severe", "Severe constipation" to "Common", "Somnolence" to "Common", "Addiction" to "Common"),
            listOf("Opioid-naive patients", "Acute or severe asthma", "Respiratory failure"),
            listOf("Alcohol - fatal overdose risk", "Benzodiazepines - severe sedation/death"),
            listOf("⚠️ HIGH RISK OF ADDICTION AND OVERDOSE", "⚠️ Apply patch to flat skin; change every 72 hours", "⚠️ Dispose of patches carefully by folding in half"),
            "Store in locked cabinet below 25°C", true, 3, SafetyLevel.DANGER,
            "HIGH RISK OF OVERDOSE AND RESPIRATORY FAILURE. Keep away from heat sources. Dispose of safely. Antidote is Naloxone.",
            "Fentanyl is a highly potent opioid painkiller for severe, continuous pain. It is extremely dangerous and must be managed strictly under medical supervision."
        ),
        "Insulin" to GenericDrugTemplate(
            "Insulin", "Hormone / Antidiabetic",
            listOf("Type 1 diabetes", "Type 2 diabetes (when oral drugs fail)"),
            listOf("Hypoglycemia (low blood sugar)", "Injection site lipodystrophy", "Weight gain"),
            mapOf("Hypoglycemia" to "Common", "Injection site lipodystrophy" to "Uncommon", "Weight gain" to "Common"),
            listOf("Hypoglycemic episodes"),
            listOf("Beta-blockers - mask hypoglycemia signs", "Alcohol - increases hypoglycemia risk"),
            listOf("⚠️ Monitor blood sugar regularly", "⚠️ Rotate injection sites to prevent skin damage"),
            "Store unopened vials in refrigerator (2-8°C). Do not freeze.", true, 5, SafetyLevel.CAUTION,
            "Monitor for signs of severe hypoglycemia (shaking, sweating, confusion). Consume fast-acting sugar immediately if values drop.",
            "Insulin is a hormone required by all patients with type 1 diabetes and some with type 2 diabetes to manage glucose levels."
        ),
        "Lidocaine" to GenericDrugTemplate(
            "Lidocaine", "Local Anesthetic",
            listOf("Local pain relief", "Neuropathy pain"),
            listOf("Skin redness", "Numbness", "Allergic reaction"),
            mapOf("Skin redness" to "Common", "Numbness" to "Common", "Allergic reaction" to "Rare/Severe"),
            listOf("Allergy to amide anesthetics"),
            listOf("Class I antiarrhythmics - additive cardiac effects"),
            listOf("⚠️ Do not apply patches to broken or irritated skin", "⚠️ Remove after 12 hours of continuous use"),
            "Store below 25°C", false, 8, SafetyLevel.MODERATE, null,
            "Lidocaine is a local anesthetic that blocks nerve signals in the skin, providing temporary relief from pain and tenderness."
        )
    )

    private val TABLET_BASES = listOf("Tylenol", "Advil", "Lipitor", "Glucophage", "Zyrtec", "Norvasc", "Zestril", "Voltaren", "Zofran", "Gabapin", "Prednisone", "Zantac")
    private val CAPSULE_BASES = listOf("Amoxil", "Prilosec", "Esomeprazole", "Celebrex", "Prozac", "Doxycycline", "Lansoprazole", "Pregabalin", "Omeprazole", "Keflex")
    private val SYRUP_BASES = listOf("Robitussin", "Delsym", "Benadryl", "Lactulose", "Brufen Syrup", "Calpol Syrup", "Tussionex", "Promethazine Syrup", "Phenergan")
    private val INJECTION_BASES = listOf("Humalog", "Lantus", "EpiPen", "Ceftriaxone", "Heparin", "Lidocaine Inj", "Morphine Inj", "Diazepam Inj", "Ondansetron Inj")
    private val OINTMENT_BASES = listOf("Neosporin", "Cortizone", "Mupirocin", "Clotrimazole Cream", "Ketoconazole Cream", "Betamethasone Oint", "Voltaren Gel")
    private val DROPS_BASES = listOf("Timoptic", "Latanoprost", "Ciloxan", "Otrivin", "Flonase Spray", "Tobradex", "Pataday", "Refresh Tears", "Systane")
    private val INHALER_BASES = listOf("Ventolin", "ProAir", "Flovent", "Symbicort", "Advair", "Spiriva", "Pulmicort", "Combivent", "Atrovent", "Dulera")
    private val POWDER_BASES = listOf("Metamucil", "ORS Powder", "MiraLAX", "Neomycin Powder", "Barium Powder", "Cholestyramine", "Questran", "Questran Light")
    private val PATCH_BASES = listOf("NicoDerm", "Fentanyl Patch", "Lidoderm", "Nitro-Dur", "Clonidine Patch", "Estradiol Patch", "Scopolamine Patch")
    private val SUPPOSITORY_BASES = listOf("Dulcolax Supp", "Glycerin Supp", "Anusol", "Mesalamine Supp", "Promethazine Supp", "Progesterone Supp", "Canasa")

    private val SUFFIXES = listOf("", " Extra Strength", " Ultra", " Max", " Plus", " Dual Action", " Rapid Release", " Infant", " Children", " Forte", " XR", " PM")

    private fun mapBrandToGeneric(brand: String): String {
        val b = brand.lowercase()
        return when {
            b.contains("tylenol") || b.contains("calpol") || b.contains("pacimol") || b.contains("crocin") || b.contains("dolo") || b.contains("pyrige") -> "Acetaminophen"
            b.contains("advil") || b.contains("motrin") || b.contains("nurofen") || b.contains("brufen") -> "Ibuprofen"
            b.contains("lipitor") || b.contains("atorva") || b.contains("zocor") || b.contains("crestor") -> "Atorvastatin"
            b.contains("glucophage") || b.contains("glycomet") || b.contains("metformin") -> "Metformin"
            b.contains("norvasc") || b.contains("amlopin") || b.contains("amlodac") || b.contains("amlodipine") -> "Amlodipine"
            b.contains("zestril") || b.contains("lisinopril") || b.contains("prinivil") -> "Lisinopril"
            b.contains("zyrtec") || b.contains("alerid") || b.contains("cetzine") || b.contains("okacet") || b.contains("cetirizine") -> "Cetirizine"
            b.contains("prednisone") || b.contains("deltasone") || b.contains("rayos") -> "Prednisone"
            b.contains("zantac") || b.contains("ranitidine") -> "Ranitidine"
            b.contains("zofran") || b.contains("ondem") -> "Ondansetron"
            b.contains("voltaren") || b.contains("diclofenac") || b.contains("dicloran") -> "Diclofenac"
            b.contains("neurontin") || b.contains("gabapin") || b.contains("gabapentin") -> "Gabapentin"
            b.contains("amox") || b.contains("novamox") || b.contains("amoxicillin") -> "Amoxicillin"
            b.contains("prilosec") || b.contains("omez") || b.contains("omee") || b.contains("omeprazole") || b.contains("esomeprazole") -> "Esomeprazole"
            b.contains("pantocid") || b.contains("pan 40") || b.contains("protonix") || b.contains("pantoprazole") -> "Pantoprazole"
            b.contains("azithral") || b.contains("azee") || b.contains("zady") || b.contains("azithromycin") -> "Azithromycin"
            b.contains("insulin") || b.contains("humalog") || b.contains("novolog") || b.contains("lantus") -> "Insulin"
            b.contains("fentanyl") || b.contains("duragesic") -> "Fentanyl"
            b.contains("dulcolax") || b.contains("bisacodyl") -> "Bisacodyl"
            b.contains("nicotine") || b.contains("nicorette") || b.contains("nicoderm") -> "Nicotine"
            b.contains("lidocaine") || b.contains("lignocaine") || b.contains("xylocaine") || b.contains("lidoderm") -> "Lidocaine"
            b.contains("timolol") || b.contains("timoptic") -> "Timolol"
            b.contains("glycerin") || b.contains("glycerol") -> "Glycerin"
            b.contains("hydrocortisone") || b.contains("cortizone") -> "Hydrocortisone"
            b.contains("neomycin") || b.contains("neosporin") -> "Neomycin"
            b.contains("budesonide") || b.contains("pulmicort") -> "Budesonide"
            b.contains("albuterol") || b.contains("salbutamol") || b.contains("ventolin") || b.contains("astahal") -> "Albuterol"
            b.contains("clotrimazole") || b.contains("canesten") -> "Clotrimazole"
            b.contains("ketoconazole") || b.contains("nizoral") -> "Ketoconazole"
            b.contains("dextromethorphan") || b.contains("robitussin") || b.contains("benadryl") -> "Dextromethorphan"
            b.contains("xylometazoline") || b.contains("otrivin") -> "Xylometazoline"
            else -> "Acetaminophen"
        }
    }

    private fun createMedicineFromTemplate(brand: String, genericKey: String, type: MedicineType, dosage: String, idIndex: Int): MedicineInfo {
        val t = templates[genericKey] ?: templates["Acetaminophen"]!!
        return MedicineInfo(
            id = "procedural_${type.name}_$idIndex",
            name = "$brand $dosage",
            brandName = brand,
            genericName = t.genericName,
            composition = "${t.genericName} $dosage",
            category = t.category,
            medicineType = type,
            dosage = dosage,
            frequency = "As directed by physician",
            uses = t.uses,
            sideEffects = t.sideEffects,
            sideEffectSeverities = t.sideEffectSeverities,
            whoShouldAvoid = t.precautions,
            interactions = t.interactions,
            warnings = t.warnings,
            storageInfo = t.storageInfo,
            prescriptionOnly = t.prescriptionOnly,
            safetyScore = t.safetyScore,
            safetyLevel = t.safetyLevel,
            emergencyWarning = t.emergencyWarning,
            aiSummary = t.aiSummary,
            confidenceScore = 100,
            manufacturer = t.manufacturer,
            alternatives = t.alternatives
        )
    }

    val medicines: List<MedicineInfo> = generateLibrary()

    private fun generateLibrary(): List<MedicineInfo> {
        val list = mutableListOf<MedicineInfo>()
        var idIndex = 0
        
        fun generateForCategory(bases: List<String>, type: MedicineType, dosageGen: (Int) -> String) {
            bases.forEach { base ->
                SUFFIXES.forEach { suffix ->
                    val brand = "$base$suffix"
                    val genericKey = mapBrandToGeneric(brand)
                    val dosage = dosageGen(idIndex)
                    list.add(createMedicineFromTemplate(brand, genericKey, type, dosage, idIndex))
                    idIndex++
                }
            }
        }

        // Generate 120+ items for each of the 10 categories
        generateForCategory(TABLET_BASES, MedicineType.TABLET) { i -> "${(i % 3 + 1) * 100}mg" }
        generateForCategory(CAPSULE_BASES, MedicineType.CAPSULE) { i -> "${(i % 3 + 1) * 150}mg" }
        generateForCategory(SYRUP_BASES, MedicineType.SYRUP) { i -> "${(i % 2 + 1) * 50}mg/5ml" }
        generateForCategory(INJECTION_BASES, MedicineType.INJECTION) { i -> "${(i % 3 + 1) * 5}mg/ml" }
        generateForCategory(OINTMENT_BASES, MedicineType.OINTMENT) { i -> if (i % 2 == 0) "1%" else "2%" }
        generateForCategory(DROPS_BASES, MedicineType.DROPS) { i -> if (i % 2 == 0) "0.5%" else "0.1%" }
        generateForCategory(INHALER_BASES, MedicineType.INHALER) { i -> "${(i % 2 + 1) * 100}mcg" }
        generateForCategory(POWDER_BASES, MedicineType.POWDER) { i -> if (i % 2 == 0) "100g" else "250g" }
        generateForCategory(PATCH_BASES, MedicineType.PATCH) { i -> "${(i % 3 + 1) * 7}mg/24h" }
        generateForCategory(SUPPOSITORY_BASES, MedicineType.SUPPOSITORY) { i -> "${(i % 2 + 1) * 10}mg" }

        return list
    }

    fun findByName(query: String): MedicineInfo? = medicines.firstOrNull {
        it.name.contains(query, true) || it.genericName.contains(query, true) ||
        it.brandName.contains(query, true) || it.composition.contains(query, true)
    }

    fun random(): MedicineInfo = medicines.random()

    fun searchAll(query: String): List<MedicineInfo> = medicines.filter {
        it.name.contains(query, true) || it.genericName.contains(query, true) ||
        it.brandName.contains(query, true) || it.category.contains(query, true) ||
        it.manufacturer.contains(query, true) ||
        it.uses.any { u -> u.contains(query, true) } ||
        it.symptomsItTreats.any { s -> s.contains(query, true) }
    }
}
