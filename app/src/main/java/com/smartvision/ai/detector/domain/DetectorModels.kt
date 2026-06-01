package com.smartvision.ai.detector.domain

import androidx.compose.ui.graphics.Color

// ─── Domain Models ────────────────────────────────────────────────────────────

/** Normalized bounding box (0.0 – 1.0) relative to image dimensions */
data class DetectionBox(
    val left:   Float,
    val top:    Float,
    val right:  Float,
    val bottom: Float
) {
    val width  get() = right - left
    val height get() = bottom - top
    val centerX get() = (left + right) / 2f
    val centerY get() = (top  + bottom) / 2f
}

/** A single detected object in a frame */
data class DetectedItem(
    val id:          Int,
    val label:       String,
    val confidence:  Float,
    val box:         DetectionBox,
    val category:    ObjectCategory,
    val accentColor: Long               // ARGB for neon bounding box color
)

/** Rich AI knowledge about a detected object */
data class ObjectKnowledge(
    val label:             String,
    val category:          ObjectCategory,
    val description:       String,
    val uses:              List<String>,
    val safetyInfo:        String,
    val environmentalImpact: String,
    val funFact:           String,
    val emoji:             String
)

/** Scan state machine */
sealed class DetectionState {
    object Idle     : DetectionState()
    object Scanning : DetectionState()
    data class Active(val items: List<DetectedItem>, val frameMs: Long) : DetectionState()
    object NoObjects : DetectionState()
    data class Error(val message: String) : DetectionState()
}

enum class ObjectCategory(val displayName: String, val colorArgb: Long) {
    PERSON       ("Person",       0xFF00D4FF),   // Neon cyan
    ANIMAL       ("Animal",       0xFF39FF14),   // Neon green
    VEHICLE      ("Vehicle",      0xFFFF6600),   // Neon orange
    FOOD         ("Food",         0xFFFFD700),   // Neon yellow
    ELECTRONICS  ("Electronics",  0xFFBF5FFF),   // Neon purple
    FURNITURE    ("Furniture",    0xFF00FFFF),   // Cyan
    SPORTS       ("Sports",       0xFFFF073A),   // Neon red
    NATURE       ("Nature",       0xFF39FF14),   // Neon green
    HOUSEHOLD    ("Household",    0xFF4DEEEA),   // Light cyan
    OTHER        ("Other",        0xFF7B7BFF)    // Soft blue
}

// ─── Category mapping ─────────────────────────────────────────────────────────
fun String.toCategory(): ObjectCategory = when (this.lowercase()) {
    "person"                                                       -> ObjectCategory.PERSON
    "cat", "dog", "bird", "horse", "cow", "sheep", "bear",
    "elephant", "zebra", "giraffe"                                 -> ObjectCategory.ANIMAL
    "car", "truck", "bus", "train", "motorcycle", "bicycle",
    "airplane", "boat"                                             -> ObjectCategory.VEHICLE
    "pizza", "cake", "sandwich", "hot dog", "burger", "apple",
    "banana", "orange", "broccoli", "carrot", "donut"             -> ObjectCategory.FOOD
    "cell phone", "laptop", "tv", "keyboard", "mouse",
    "remote", "microwave", "oven", "toaster", "refrigerator"      -> ObjectCategory.ELECTRONICS
    "chair", "couch", "bed", "dining table", "toilet"             -> ObjectCategory.FURNITURE
    "tennis racket", "baseball bat", "skateboard", "sports ball",
    "frisbee", "skis", "snowboard", "surfboard"                   -> ObjectCategory.SPORTS
    "potted plant", "tree", "flower"                               -> ObjectCategory.NATURE
    "bottle", "cup", "fork", "knife", "spoon", "bowl",
    "vase", "scissors", "book", "clock", "umbrella", "handbag",
    "backpack", "suitcase", "tie"                                  -> ObjectCategory.HOUSEHOLD
    else                                                           -> ObjectCategory.OTHER
}

// ─── Accent color per category ────────────────────────────────────────────────
fun ObjectCategory.neonColor(): Color = Color(this.colorArgb)

// ─── Knowledge Base ───────────────────────────────────────────────────────────
object ObjectKnowledgeBase {

    private val db: Map<String, ObjectKnowledge> = mapOf(

        "person" to ObjectKnowledge(
            label       = "Person",
            category    = ObjectCategory.PERSON,
            description = "A human being detected in the scene. The detector identifies posture, position and approximate location.",
            uses        = listOf("Identity detection", "Crowd analysis", "Safety monitoring", "Accessibility assistance"),
            safetyInfo  = "Always respect privacy. Do not use in surveillance without consent.",
            environmentalImpact = "Neutral — detection only, no environmental footprint.",
            funFact     = "ML Kit can detect up to 100 people per frame at 30 FPS.",
            emoji       = "🧑"
        ),
        "cat" to ObjectKnowledge(
            label       = "Cat",
            category    = ObjectCategory.ANIMAL,
            description = "Domestic feline. Cats have been companions to humans for over 10,000 years and are among the world's most popular pets.",
            uses        = listOf("Companionship", "Pest control", "Therapy animals", "Social media content 😄"),
            safetyInfo  = "Cats can scratch. Approach calmly and let the cat initiate contact.",
            environmentalImpact = "Outdoor cats can impact local bird populations. Keep cats indoors when possible.",
            funFact     = "Cats spend 70% of their lives sleeping!",
            emoji       = "🐱"
        ),
        "dog" to ObjectKnowledge(
            label       = "Dog",
            category    = ObjectCategory.ANIMAL,
            description = "Canis lupus familiaris — humanity's oldest domesticated animal, with over 340 recognized breeds worldwide.",
            uses        = listOf("Companionship", "Service animals", "Search and rescue", "Herding", "Police work"),
            safetyInfo  = "Ask the owner before approaching a dog. Avoid sudden movements.",
            environmentalImpact = "Pet food has a significant carbon footprint. Consider eco-friendly options.",
            funFact     = "A dog's nose print is as unique as a human fingerprint.",
            emoji       = "🐶"
        ),
        "car" to ObjectKnowledge(
            label       = "Car",
            category    = ObjectCategory.VEHICLE,
            description = "A four-wheeled motor vehicle for personal transportation, one of the defining inventions of the 20th century.",
            uses        = listOf("Personal transport", "Delivery", "Emergency services", "Racing"),
            safetyInfo  = "Always maintain safe distances. Electric vehicles are silent — be alert near driveways.",
            environmentalImpact = "ICE vehicles contribute ~15% of global CO₂. EVs significantly reduce this footprint.",
            funFact     = "There are over 1.4 billion cars on Earth — more than one per 6 people.",
            emoji       = "🚗"
        ),
        "cell phone" to ObjectKnowledge(
            label       = "Mobile Phone",
            category    = ObjectCategory.ELECTRONICS,
            description = "A portable electronic device combining communication, computing, photography, and entertainment in one unit.",
            uses        = listOf("Communication", "Photography", "Navigation", "Banking", "Entertainment"),
            safetyInfo  = "Avoid phone use while driving. Use blue-light filters at night.",
            environmentalImpact = "E-waste is a major issue. Recycle old phones at certified centers.",
            funFact     = "The average person checks their phone 96 times per day.",
            emoji       = "📱"
        ),
        "laptop" to ObjectKnowledge(
            label       = "Laptop",
            category    = ObjectCategory.ELECTRONICS,
            description = "A portable personal computer integrating a display, keyboard and battery into a compact form factor.",
            uses        = listOf("Work", "Education", "Programming", "Creative work", "Gaming"),
            safetyInfo  = "Keep on flat surfaces for ventilation. Use surge protectors.",
            environmentalImpact = "Manufacturing a laptop generates ~300–400 kg of CO₂. Extend device life to reduce footprint.",
            funFact     = "The first laptop, the Osborne 1 (1981), weighed 10.7 kg and had a 5-inch screen.",
            emoji       = "💻"
        ),
        "chair" to ObjectKnowledge(
            label       = "Chair",
            category    = ObjectCategory.FURNITURE,
            description = "A piece of furniture designed for single-person seating, with a back support and typically four legs.",
            uses        = listOf("Seating", "Office use", "Dining", "Accessibility support"),
            safetyInfo  = "Check weight capacity. Adjust height to maintain ergonomic posture.",
            environmentalImpact = "Wood chairs from FSC-certified forests are the most sustainable option.",
            funFact     = "The earliest known chairs date back to ancient Egypt around 2680 BCE.",
            emoji       = "🪑"
        ),
        "bottle" to ObjectKnowledge(
            label       = "Bottle",
            category    = ObjectCategory.HOUSEHOLD,
            description = "A rigid container with a narrow neck, used to store liquids ranging from water to medicines.",
            uses        = listOf("Liquid storage", "Packaging", "Decoration", "Recycling"),
            safetyInfo  = "Check expiry dates on food/medicine bottles. Don't reuse single-use plastic bottles.",
            environmentalImpact = "1 million plastic bottles are purchased every minute globally. Choose reusable options.",
            funFact     = "Glass bottles can be recycled indefinitely without loss of quality.",
            emoji       = "🍶"
        ),
        "book" to ObjectKnowledge(
            label       = "Book",
            category    = ObjectCategory.HOUSEHOLD,
            description = "A portable collection of knowledge, stories or information preserved through written or printed text.",
            uses        = listOf("Education", "Entertainment", "Reference", "Cultural preservation"),
            safetyInfo  = "No safety concerns. Reading in poor light can strain eyes.",
            environmentalImpact = "One book requires roughly 2 kg of CO₂ to produce. E-books have lower per-unit impact.",
            funFact     = "The oldest dated printed book, the Diamond Sutra, was printed in 868 CE.",
            emoji       = "📚"
        ),
        "pizza" to ObjectKnowledge(
            label       = "Pizza",
            category    = ObjectCategory.FOOD,
            description = "A flatbread dish originating from Italy, topped with tomato sauce, cheese and various toppings.",
            uses        = listOf("Meal", "Social gathering food", "Street food"),
            safetyInfo  = "Allergen alert: contains gluten, dairy. Check for nut toppings.",
            environmentalImpact = "A single pizza generates ~2 kg of CO₂. Plant-based toppings reduce this significantly.",
            funFact     = "Americans eat approximately 3 billion pizzas per year — about 23 pounds per person.",
            emoji       = "🍕"
        ),
        "bicycle" to ObjectKnowledge(
            label       = "Bicycle",
            category    = ObjectCategory.VEHICLE,
            description = "A human-powered, two-wheeled vehicle that is one of the most efficient machines ever invented.",
            uses        = listOf("Transportation", "Exercise", "Sport", "Delivery"),
            safetyInfo  = "Always wear a helmet. Use lights at night. Follow traffic rules.",
            environmentalImpact = "Bicycles produce zero direct emissions — the greenest form of motorized transport.",
            funFact     = "More bicycles exist on Earth than any other vehicle — over 1 billion and counting.",
            emoji       = "🚲"
        ),
        "bird" to ObjectKnowledge(
            label       = "Bird",
            category    = ObjectCategory.ANIMAL,
            description = "A warm-blooded vertebrate characterized by feathers, beaks and typically the ability to fly.",
            uses        = listOf("Ecological balance", "Pest control", "Seed dispersal", "Inspiration for aviation"),
            safetyInfo  = "Do not feed wild birds processed foods. Keep cats away from bird feeders.",
            environmentalImpact = "Birds are critical indicators of ecosystem health. Declining populations signal environmental stress.",
            funFact     = "The Arctic Tern migrates 90,000 km per year — the longest migration of any animal.",
            emoji       = "🐦"
        ),
        "tv" to ObjectKnowledge(
            label       = "Television",
            category    = ObjectCategory.ELECTRONICS,
            description = "An electronic display system that receives and presents audio-visual broadcast content.",
            uses        = listOf("Entertainment", "News", "Education", "Gaming display"),
            safetyInfo  = "Maintain 1.5–2.5m viewing distance. Limit screen time, especially for children.",
            environmentalImpact = "Modern LED TVs use 60% less energy than CRT models. Enable eco mode to save power.",
            funFact     = "The first color TV broadcast in the US aired on June 25, 1951.",
            emoji       = "📺"
        ),
        "couch" to ObjectKnowledge(
            label       = "Sofa/Couch",
            category    = ObjectCategory.FURNITURE,
            description = "An upholstered seat designed to comfortably accommodate multiple people in a lounge or living area.",
            uses        = listOf("Seating", "Relaxation", "Social space"),
            safetyInfo  = "Check fabric fire resistance ratings. Regularly clean to prevent dust mite buildup.",
            environmentalImpact = "Choose natural fabrics (cotton, wool) over synthetic for lower environmental impact.",
            funFact     = "The average person spends about 4 years of their life sitting on a sofa.",
            emoji       = "🛋️"
        )
    )

    fun getKnowledge(label: String): ObjectKnowledge = db[label.lowercase()]
        ?: db.entries.firstOrNull { label.lowercase().contains(it.key) }?.value
        ?: generateGeneric(label)

    private fun generateGeneric(label: String): ObjectKnowledge {
        val cat = label.toCategory()
        return ObjectKnowledge(
            label       = label.replaceFirstChar { it.uppercaseChar() },
            category    = cat,
            description = "A ${cat.displayName.lowercase()} detected by the AI vision system with high confidence.",
            uses        = listOf("General purpose", "Daily use", "${cat.displayName} application"),
            safetyInfo  = "Follow standard safety practices when interacting with this ${cat.displayName.lowercase()}.",
            environmentalImpact = "Consider the lifecycle impact when purchasing or disposing of this item.",
            funFact     = "The AI model can identify over 80 COCO object categories in real time.",
            emoji       = when (cat) {
                ObjectCategory.PERSON      -> "🧑"
                ObjectCategory.ANIMAL      -> "🐾"
                ObjectCategory.VEHICLE     -> "🚗"
                ObjectCategory.FOOD        -> "🍽️"
                ObjectCategory.ELECTRONICS -> "⚡"
                ObjectCategory.FURNITURE   -> "🪑"
                ObjectCategory.SPORTS      -> "⚽"
                ObjectCategory.NATURE      -> "🌿"
                ObjectCategory.HOUSEHOLD   -> "🏠"
                ObjectCategory.OTHER       -> "🔍"
            }
        )
    }
}
