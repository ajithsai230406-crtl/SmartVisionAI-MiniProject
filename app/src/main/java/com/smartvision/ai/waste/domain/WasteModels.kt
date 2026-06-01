package com.smartvision.ai.waste.domain

// ─── Waste Category Definitions ───────────────────────────────────────────────

enum class WasteType(
    val displayName:   String,
    val emoji:         String,
    val colorArgb:     Long,
    val bin:           String,
    val recyclingCode: String
) {
    PLASTIC      ("Plastic",       "🧴", 0xFF00D4FF, "Blue Bin",    "#1–7"),
    METAL        ("Metal",         "🥫", 0xFF9E9E9E, "Blue Bin",    "Alu/Fe"),
    ORGANIC      ("Organic",       "🍃", 0xFF39FF14, "Green Bin",   "Bio"),
    PAPER        ("Paper",         "📄", 0xFFFFD700, "Yellow Bin",  "PAP"),
    GLASS        ("Glass",         "🍶", 0xFF00E5FF, "White Bin",   "GL"),
    EWASTE       ("E-Waste",       "💻", 0xFFFF6600, "WEEE Point",  "WEEE"),
    HAZARDOUS    ("Hazardous",     "☢️", 0xFFFF073A, "HHW Facility","HHW"),
    TEXTILE      ("Textile",       "👕", 0xFFBF5FFF, "Textile Box", "CLO"),
    NON_RECYCLABLE("Non-Recyclable","🗑️",0xFF555555, "Black Bin",  "—"),
    MIXED        ("Mixed Waste",   "♻️", 0xFF7B7BFF, "Check First", "MIX")
}

/** Rich per-type knowledge */
data class WasteKnowledge(
    val type:              WasteType,
    val description:       String,
    val disposalSteps:     List<String>,
    val ecoTip:            String,
    val reusabilityInfo:   String,
    val carbonFootprint:   String,
    val recyclingRate:     Float,          // 0–1
    val funFact:           String
)

/** A classified waste result */
data class WasteResult(
    val primaryType:   WasteType,
    val confidence:    Float,
    val allCategories: List<WasteCategoryScore>,
    val rawLabel:      String
)

data class WasteCategoryScore(val type: WasteType, val score: Float)

/** Scan state machine */
sealed class WasteScanState {
    object Idle      : WasteScanState()
    object Scanning  : WasteScanState()
    data class Classified(val result: WasteResult) : WasteScanState()
    data class Error(val message: String)          : WasteScanState()
}

// ─── Label → WasteType mapper ──────────────────────────────────────────────
fun String.toWasteType(): WasteType = when (this.lowercase().trim()) {
    "plastic_bottle", "plastic_bag", "styrofoam", "plastic" -> WasteType.PLASTIC
    "metal_can", "aluminium", "steel", "metal"              -> WasteType.METAL
    "food_waste", "organic_material", "organic"             -> WasteType.ORGANIC
    "cardboard", "paper", "newspaper"                       -> WasteType.PAPER
    "glass_bottle", "glass"                                 -> WasteType.GLASS
    "electronic_waste", "e-waste", "battery"                -> WasteType.EWASTE
    "hazardous", "chemical", "paint"                        -> WasteType.HAZARDOUS
    "clothing", "textile", "rubber"                         -> WasteType.TEXTILE
    "mixed_waste"                                           -> WasteType.MIXED
    else                                                    -> WasteType.NON_RECYCLABLE
}

// ─── Knowledge Base ────────────────────────────────────────────────────────────
object WasteKnowledgeBase {

    val all: Map<WasteType, WasteKnowledge> = mapOf(

        WasteType.PLASTIC to WasteKnowledge(
            type             = WasteType.PLASTIC,
            description      = "Plastic is a synthetic polymer. Most plastic packaging is recyclable if clean and dry. Check the resin code (1–7) for recycling guidelines.",
            disposalSteps    = listOf("Rinse and remove food residue", "Check resin code on bottom", "Remove lids separately (often different plastic)", "Place in Blue Recycling Bin", "Never mix with food waste"),
            ecoTip           = "Switch to reusable containers. One reusable bottle replaces 156 single-use bottles per year.",
            reusabilityInfo  = "Plastic #1 (PET) and #2 (HDPE) are most widely accepted. #5 (PP) is increasingly recyclable. Avoid #3, #6, #7.",
            carbonFootprint  = "Producing 1 kg of plastic emits ~6 kg CO₂. Recycling saves ~1.5 kg CO₂ per kg.",
            recyclingRate    = 0.91f,
            funFact          = "Only 9% of all plastic ever produced has been recycled. The rest is in landfills or oceans."
        ),

        WasteType.METAL to WasteKnowledge(
            type             = WasteType.METAL,
            description      = "Metal is one of the most recyclable materials — aluminium can be recycled indefinitely without quality loss.",
            disposalSteps    = listOf("Empty and rinse cans", "Do not crush aluminium (some plants need them whole)", "Remove paper labels if possible", "Place in Blue/Metal recycling bin", "Scrap metal → take to a metal recycling centre"),
            ecoTip           = "Recycling one aluminium can saves enough energy to power a TV for 3 hours.",
            reusabilityInfo  = "Steel and aluminium are 100% infinitely recyclable. Tin-coated steel cans are also widely accepted.",
            carbonFootprint  = "Recycling aluminium uses 95% less energy than producing it from raw bauxite ore.",
            recyclingRate    = 0.97f,
            funFact          = "Aluminium is the most abundant metal on Earth, yet it takes 1,500 years to degrade in landfill."
        ),

        WasteType.ORGANIC to WasteKnowledge(
            type             = WasteType.ORGANIC,
            description      = "Organic waste includes food scraps, garden waste, and biodegradable materials. Composting converts them into nutrient-rich soil.",
            disposalSteps    = listOf("Separate from dry waste", "Place in Green Wet Waste Bin", "Alternatively, start home composting", "Avoid mixing with plastics or metals", "Do not pour cooking oil down drains"),
            ecoTip           = "Composting your food waste reduces methane emissions from landfills by up to 50%.",
            reusabilityInfo  = "Excellent for composting, vermicomposting, and biogas generation. Coffee grounds can be reused as fertilizer directly.",
            carbonFootprint  = "When food rots in landfill, it produces methane — 80× more potent than CO₂ over 20 years.",
            recyclingRate    = 0.85f,
            funFact          = "India generates ~62 million tonnes of municipal solid waste annually, of which 50–60% is organic."
        ),

        WasteType.PAPER to WasteKnowledge(
            type             = WasteType.PAPER,
            description      = "Paper is one of the most recycled materials globally. Keep it dry — wet paper loses its fibres and cannot be recycled.",
            disposalSteps    = listOf("Keep paper dry at all times", "Remove staples, tape and plastic windows from envelopes", "Flatten cardboard boxes", "Bundle newspapers and magazines", "Place in Yellow/Paper recycling bin"),
            ecoTip           = "Go paperless for bills and statements. One tonne of recycled paper saves 17 trees.",
            reusabilityInfo  = "Paper can be recycled 5–7 times before fibres degrade. Use both sides before discarding.",
            carbonFootprint  = "Recycling 1 tonne of paper saves 3.3 cubic metres of landfill space and prevents 1 tonne of CO₂.",
            recyclingRate    = 0.88f,
            funFact          = "The world uses 300 million tonnes of paper per year — roughly equal to the weight of all humans on Earth."
        ),

        WasteType.GLASS to WasteKnowledge(
            type             = WasteType.GLASS,
            description      = "Glass is 100% recyclable and can be recycled endlessly without loss of quality or purity.",
            disposalSteps    = listOf("Rinse bottles and jars", "Remove metal lids (recycle separately)", "Do NOT mix with: window glass, Pyrex, mirrors, or ceramics", "Sort by colour if required in your area", "Wrap broken glass in newspaper before placing in bin"),
            ecoTip           = "Reuse glass jars for storage instead of buying new containers.",
            reusabilityInfo  = "Glass jars and bottles are excellent for storage. Sterilise in boiling water for food use.",
            carbonFootprint  = "Using recycled glass reduces CO₂ emissions by up to 20% vs raw production.",
            recyclingRate    = 0.80f,
            funFact          = "Glass takes over 1 million years to decompose in landfill — but can be recycled in as little as 30 days."
        ),

        WasteType.EWASTE to WasteKnowledge(
            type             = WasteType.EWASTE,
            description      = "Electronic waste (e-waste) contains valuable metals like gold, silver and copper, but also toxic substances like lead, mercury and cadmium.",
            disposalSteps    = listOf("NEVER dispose in general bins", "Wipe data from phones/laptops before recycling", "Find nearest WEEE collection point", "Many manufacturers offer take-back programmes", "Donate functional devices to NGOs or schools"),
            ecoTip           = "Extend device life through repair. Buying secondhand electronics is the greenest choice.",
            reusabilityInfo  = "Phones, tablets and computers should be donated or sold before recycling. Batteries must be removed and recycled separately.",
            carbonFootprint  = "E-waste releases 50 million tonnes of toxic waste per year globally. 80% ends up in developing nations.",
            recyclingRate    = 0.20f,
            funFact          = "One million recycled mobile phones yield ~15 kg gold, 150 kg silver and 3 kg palladium."
        ),

        WasteType.HAZARDOUS to WasteKnowledge(
            type             = WasteType.HAZARDOUS,
            description      = "Hazardous household waste (HHW) includes paints, solvents, pesticides, batteries and medicines. These require special disposal.",
            disposalSteps    = listOf("NEVER pour down drains or toilets", "Keep in original containers with labels", "Take to Household Hazardous Waste (HHW) collection facility", "Medicines: return to pharmacy for safe disposal", "Batteries: use dedicated battery recycling bins"),
            ecoTip           = "Switch to eco-friendly cleaning products and water-based paints to eliminate hazardous waste at source.",
            reusabilityInfo  = "Leftover paint can be donated to community groups. Check local 'paint exchange' programmes.",
            carbonFootprint  = "Improper hazardous waste disposal can contaminate groundwater for decades.",
            recyclingRate    = 0.30f,
            funFact          = "The average household generates 4–9 kg of hazardous waste per year without realising it."
        ),

        WasteType.TEXTILE to WasteKnowledge(
            type             = WasteType.TEXTILE,
            description      = "Textile waste includes clothing, shoes, bedding, and fabric. Only 15% of clothing is currently donated or recycled globally.",
            disposalSteps    = listOf("Donate wearable items to NGOs or thrift stores", "Use clothing collection bins for worn-out items", "Cut into rags for household cleaning", "Check brand take-back schemes (H&M, Zara, etc.)", "Avoid throwing in general waste"),
            ecoTip           = "Swap, rent, or buy secondhand clothing to dramatically reduce textile waste.",
            reusabilityInfo  = "Old T-shirts → cleaning rags. Old jeans → patchwork. Old bedsheets → reusable bags.",
            carbonFootprint  = "The fashion industry produces 10% of global carbon emissions — more than aviation and shipping combined.",
            recyclingRate    = 0.15f,
            funFact          = "Every second, the equivalent of a garbage truck of textiles is landfilled or burned somewhere in the world."
        ),

        WasteType.NON_RECYCLABLE to WasteKnowledge(
            type             = WasteType.NON_RECYCLABLE,
            description      = "Some items cannot currently be recycled due to material composition, contamination, or lack of recycling infrastructure.",
            disposalSteps    = listOf("Place in Black/General Waste Bin", "Try to reduce usage of these items", "Look for product-specific take-back programmes", "Consider refuse → reduce before recycling"),
            ecoTip           = "The best waste is no waste. Refuse single-use items where possible.",
            reusabilityInfo  = "Some 'non-recyclable' items may be recyclable in specialised facilities. Check your local council website.",
            carbonFootprint  = "Non-recyclables typically end up in landfill, releasing CO₂ and methane over decades.",
            recyclingRate    = 0.05f,
            funFact          = "Styrofoam can take up to 500 years to decompose but takes only minutes to manufacture."
        )
    )

    fun getKnowledge(type: WasteType): WasteKnowledge = all[type] ?: all[WasteType.NON_RECYCLABLE]!!
}
