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
    val label:                  String,
    val category:               ObjectCategory,
    val description:            String,
    val uses:                   List<String>,
    val safetyInfo:             String,
    val environmentalImpact:    String,
    val funFact:                String,
    val emoji:                  String,
    val maintenanceTips:        List<String> = emptyList(),
    val buyingSuggestions:      List<String> = emptyList(),
    val relatedRecommendations: List<String> = emptyList(),
    val specializedActionType:  String? = null, // "medicine", "food", "textbook", "waste", "electronics"
    val specializedActionData:  String? = null  // custom data e.g. nutritional values or maintenance specs
)

/** Scan state machine */
sealed class DetectionState {
    object Idle     : DetectionState()
    object Scanning : DetectionState()
    data class Active(
        val items:       List<DetectedItem>,
        val frameMs:     Long,
        val frameWidth:  Int,
        val frameHeight: Int
    ) : DetectionState()
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
fun String.toCategory(): ObjectCategory = when (this.lowercase().trim()) {
    "person"                                                       -> ObjectCategory.PERSON
    "cat", "dog", "bird", "horse", "cow", "sheep", "bear",
    "elephant", "zebra", "giraffe"                                 -> ObjectCategory.ANIMAL
    "car", "truck", "bus", "train", "motorcycle", "bicycle",
    "airplane", "boat"                                             -> ObjectCategory.VEHICLE
    "pizza", "cake", "sandwich", "hot dog", "burger", "apple",
    "banana", "orange", "broccoli", "carrot", "donut", "food",
    "fresh red apple"                                              -> ObjectCategory.FOOD
    "cell phone", "laptop", "tv", "keyboard", "mouse",
    "remote", "microwave", "oven", "toaster", "refrigerator",
    "scientific calculator", "wireless earbuds charging case",
    "wireless earbuds case", "laptop keyboard"                     -> ObjectCategory.ELECTRONICS
    "chair", "couch", "bed", "dining table", "toilet"             -> ObjectCategory.FURNITURE
    "tennis racket", "baseball bat", "skateboard", "sports ball",
    "frisbee", "skis", "snowboard", "surfboard"                   -> ObjectCategory.SPORTS
    "potted plant", "tree", "flower"                               -> ObjectCategory.NATURE
    "bottle", "cup", "fork", "knife", "spoon", "bowl",
    "vase", "scissors", "book", "clock", "umbrella", "handbag",
    "backpack", "suitcase", "tie", "mathematics textbook",
    "plastic water bottle", "ceramic coffee mug", "coffee mug",
    "acetaminophen tablet bottle"                                  -> ObjectCategory.HOUSEHOLD
    else                                                           -> ObjectCategory.OTHER
}

// ─── Accent color per category ────────────────────────────────────────────────
fun ObjectCategory.neonColor(): Color = Color(this.colorArgb)

// ─── Knowledge Base ───────────────────────────────────────────────────────────
object ObjectKnowledgeBase {

    private val db: Map<String, ObjectKnowledge> = mapOf(

        "wireless earbuds charging case" to ObjectKnowledge(
            label       = "Wireless Earbuds Charging Case",
            category    = ObjectCategory.ELECTRONICS,
            description = "A compact, rechargeable docking case designed for storing and charging true wireless Bluetooth earbud headphones.",
            uses        = listOf("Earbud protection", "On-the-go charging", "Bluetooth pairing sync"),
            safetyInfo  = "Keep away from high humidity and liquids. Inspect charging pins for metal debris to prevent short circuits.",
            environmentalImpact = "Contains a lithium-ion battery. Dispose strictly at certified e-waste or battery recycling points.",
            funFact     = "Most modern earbud cases can charge earbuds fully 3 to 4 times on a single internal battery cycle.",
            emoji       = "🔋",
            maintenanceTips = listOf("Clean the interior contacts with a dry cotton swab weekly", "Avoid leaving in temperatures above 45°C", "Use certified USB-C cables to prolong battery health"),
            buyingSuggestions = listOf("Look for wireless Qi-certified cases", "Verify battery capacity (mAh) before purchasing"),
            relatedRecommendations = listOf("Silicone Protective Sleeve", "Lanyard Attachment", "Anti-dust Plugs"),
            specializedActionType = "electronics",
            specializedActionData = "USB-C Fast Charge · 450mAh Li-Polymer · Status Indicator LED"
        ),

        "wireless earbuds case" to ObjectKnowledge(
            label       = "Wireless Earbuds Charging Case",
            category    = ObjectCategory.ELECTRONICS,
            description = "A compact, rechargeable docking case designed for storing and charging true wireless Bluetooth earbud headphones.",
            uses        = listOf("Earbud protection", "On-the-go charging", "Bluetooth pairing sync"),
            safetyInfo  = "Keep away from high humidity and liquids. Inspect charging pins for metal debris to prevent short circuits.",
            environmentalImpact = "Contains a lithium-ion battery. Dispose strictly at certified e-waste or battery recycling points.",
            funFact     = "Most modern earbud cases can charge earbuds fully 3 to 4 times on a single internal battery cycle.",
            emoji       = "🔋",
            maintenanceTips = listOf("Clean the interior contacts with a dry cotton swab weekly", "Avoid leaving in temperatures above 45°C", "Use certified USB-C cables to prolong battery health"),
            buyingSuggestions = listOf("Look for wireless Qi-certified cases", "Verify battery capacity (mAh) before purchasing"),
            relatedRecommendations = listOf("Silicone Protective Sleeve", "Lanyard Attachment", "Anti-dust Plugs"),
            specializedActionType = "electronics",
            specializedActionData = "USB-C Fast Charge · 450mAh Li-Polymer · Status Indicator LED"
        ),

        "scientific calculator" to ObjectKnowledge(
            label       = "Scientific Calculator",
            category    = ObjectCategory.ELECTRONICS,
            description = "An advanced electronic calculator designed to compute mathematical, scientific, engineering, and trigonometric equations.",
            uses        = listOf("Academic algebra", "Trigonometric modeling", "Statistical variance computations"),
            safetyInfo  = "Do not bend or apply heavy pressure to the LCD screen. Remove batteries if storing unused for over 6 months.",
            environmentalImpact = "Extremely long operational life. Solar panels reduce standard alkaline battery consumption by 90%.",
            funFact     = "Modern scientific calculators are millions of times faster and have more memory than the Apollo Guidance Computer.",
            emoji       = "🧮",
            maintenanceTips = listOf("Clean keyboard gaps with compressed air", "Avoid direct screen exposure to intense sunlight", "Wipe casing with a microfiber cloth"),
            buyingSuggestions = listOf("Casio fx-991EX ClassWiz for algebraic work", "TI-30XS MultiView for basic high-school science"),
            relatedRecommendations = listOf("Graphing Notebook", "Engineering Ruler Set", "Mechanical Pencil 0.5mm"),
            specializedActionType = "textbook", // triggers Student Helper Homework Solver!
            specializedActionData = "Math & Engineering Companion Mode"
        ),

        "plastic water bottle" to ObjectKnowledge(
            label       = "Plastic Water Bottle",
            category    = ObjectCategory.HOUSEHOLD,
            description = "A portable cylindrical container designed for holding water or beverages, typically molded from PET or BPA-free polymers.",
            uses        = listOf("Hydration utility", "Liquid storage", "Workout accessory"),
            safetyInfo  = "Do not reuse single-use PET bottles to avoid bacterial buildup and microplastics. Never fill with boiling liquids.",
            environmentalImpact = "Standard PET bottles take over 450 years to decompose. Upgrading to a reusable container prevents ~150 single-use bottles/year.",
            funFact     = "It takes three times the amount of water to manufacture a single plastic bottle than it does to actually fill it.",
            emoji       = "💧",
            maintenanceTips = listOf("Hand wash daily with a soft bottle brush", "Let air dry completely with the cap off to prevent mold", "Keep away from high heat sources"),
            buyingSuggestions = listOf("Select high-grade BPA-Free Eastman Tritan bottles", "Consider insulated stainless steel vacuum flasks"),
            relatedRecommendations = listOf("Neoprene Insulating Sleeve", "Screw-on Sport Cap", "Fruit Infuser insert"),
            specializedActionType = "waste", // triggers Waste Classifier
            specializedActionData = "PET Recycle Category 1: Blue Bin Recyclable"
        ),

        "mathematics textbook" to ObjectKnowledge(
            label       = "Mathematics Textbook",
            category    = ObjectCategory.HOUSEHOLD,
            description = "An academic book containing mathematical theory, equations, exercises, and instructional material for learning.",
            uses        = listOf("Guided math studies", "Reference formulas", "Problem-solving exercises"),
            safetyInfo  = "Heavy weight — lift properly to avoid muscle strain. Avoid storage in damp areas to prevent mold.",
            environmentalImpact = "Printed on recyclable wood pulp paper. Made using sustainable FSC-certified forestry schemes.",
            funFact     = "The oldest known mathematics textbook is the 'Rhind Papyrus', dating back to 1650 BCE in ancient Egypt.",
            emoji       = "📚",
            maintenanceTips = listOf("Wipe plastic dust cover with a slightly damp cloth", "Store vertically to prevent binding degradation", "Use sticky flags instead of folding page corners"),
            buyingSuggestions = listOf("Look for digital rentals or previous editions to save up to 80%", "Buy softcovers for lighter carrying"),
            relatedRecommendations = listOf("AI Homework Solver Screen", "Grid Scrapbook", "Highlighter Set"),
            specializedActionType = "textbook", // triggers Student Helper
            specializedActionData = "Curriculum level mathematical textbook"
        ),

        "laptop keyboard" to ObjectKnowledge(
            label       = "Laptop Keyboard",
            category    = ObjectCategory.ELECTRONICS,
            description = "An integrated alphanumeric QWERTY input panel using low-profile scissor switches or membrane keys.",
            uses        = listOf("Text input", "System controls", "Coding and gaming"),
            safetyInfo  = "Do not eat or drink near the keyboard. Liquid spills can fry internal motherboard circuits instantly.",
            environmentalImpact = "Contains minor e-waste copper circuitry. Recycle at e-waste centers when disposing of the laptop.",
            funFact     = "Keyboards are statistically dirtier than average toilet seats if not regularly sanitized.",
            emoji       = "⌨️",
            maintenanceTips = listOf("Blow out dust with compressed air regularly", "Wipe keys with 70% isopropyl alcohol wipes", "Avoid heavy key hammering"),
            buyingSuggestions = listOf("Look for models with anti-spill key trays", "Select keyboards with backlight controls"),
            relatedRecommendations = listOf("Microfiber Keyboard Towel", "Keycap Cleaning Gel", "External Wireless Keyboard"),
            specializedActionType = "electronics",
            specializedActionData = "Scissor key switches with RGB backlight support"
        ),

        "ceramic coffee mug" to ObjectKnowledge(
            label       = "Ceramic Coffee Mug",
            category    = ObjectCategory.HOUSEHOLD,
            description = "A heavy-duty earthenware cup with a loop handle, designed for drinking hot beverages such as coffee or tea.",
            uses        = listOf("Coffee/Tea consumption", "Desktop pen holder", "Microwave mug cakes"),
            safetyInfo  = "Prone to cracking when dropped. Inspect for fine hairline fractures before filling with boiling liquids.",
            environmentalImpact = "Extremely long operational life. Reusable ceramic mugs help eliminate paper cup waste.",
            funFact     = "Earthenware mugs were made by hand in China as early as 10,000 BCE.",
            emoji       = "☕",
            maintenanceTips = listOf("Scrub with baking soda paste to remove dark coffee stains", "Avoid sudden extreme temperature drops (e.g. ice water immediately after boiling)", "Dishwasher safe on top rack"),
            buyingSuggestions = listOf("Select double-walled mugs for heat retention", "Stoneware mugs are sturdier than fine china"),
            relatedRecommendations = listOf("Silicone Coaster", "Mug Warming Plate", "Long Stirring Spoon"),
            specializedActionType = null,
            specializedActionData = null
        ),

        "coffee mug" to ObjectKnowledge(
            label       = "Ceramic Coffee Mug",
            category    = ObjectCategory.HOUSEHOLD,
            description = "A heavy-duty earthenware cup with a loop handle, designed for drinking hot beverages such as coffee or tea.",
            uses        = listOf("Coffee/Tea consumption", "Desktop pen holder", "Microwave mug cakes"),
            safetyInfo  = "Prone to cracking when dropped. Inspect for fine hairline fractures before filling with boiling liquids.",
            environmentalImpact = "Extremely long operational life. Reusable ceramic mugs help eliminate paper cup waste.",
            funFact     = "Earthenware mugs were made by hand in China as early as 10,000 BCE.",
            emoji       = "☕",
            maintenanceTips = listOf("Scrub with baking soda paste to remove dark coffee stains", "Avoid sudden extreme temperature drops (e.g. ice water immediately after boiling)", "Dishwasher safe on top rack"),
            buyingSuggestions = listOf("Select double-walled mugs for heat retention", "Stoneware mugs are sturdier than fine china"),
            relatedRecommendations = listOf("Silicone Coaster", "Mug Warming Plate", "Long Stirring Spoon"),
            specializedActionType = null,
            specializedActionData = null
        ),

        "acetaminophen tablet bottle" to ObjectKnowledge(
            label       = "Acetaminophen Tablet Bottle",
            category    = ObjectCategory.HOUSEHOLD,
            description = "A prescription or over-the-counter medicine container housing paracetamol pain-relief tablets, secured with a child-resistant safety cap.",
            uses        = listOf("Pain management", "Fever reduction", "First-aid cabinet stock"),
            safetyInfo  = "Keep strictly out of reach of children. Never exceed the maximum daily allowance to prevent severe liver damage.",
            environmentalImpact = "Medical plastic bottles are made of high-density HDPE. Clean and recycle at dedicated medicine dropoff containers.",
            funFact     = "Acetaminophen was first synthesized in 1878 by chemist Harmon Northrop Morse.",
            emoji       = "💊",
            maintenanceTips = listOf("Keep cap tightly sealed when not in use", "Store in a cool, dry place away from direct light (not the bathroom cabinet due to humidity)"),
            buyingSuggestions = listOf("Choose child-safety cap formats", "Buy generic brand paracetamol to save cost on identical active ingredients"),
            relatedRecommendations = listOf("Medicine Organizer box", "First-Aid Kit Bag", "Weekly Pill Splitter"),
            specializedActionType = "medicine", // triggers Medicine Scanner
            specializedActionData = "Active Ingredient: Acetaminophen 500mg"
        ),

        "fresh red apple" to ObjectKnowledge(
            label       = "Fresh Red Apple",
            category    = ObjectCategory.FOOD,
            description = "A crisp, sweet round fruit cultivated from Malus domestica apple trees, rich in dietary fiber and vitamin C.",
            uses        = listOf("Healthy raw snack", "Baking ingredient", "Fruit salads and juices"),
            safetyInfo  = "Wash thoroughly before eating to remove pesticide residues and wax. Apple seeds contain trace cyanide; do not consume in large quantities.",
            environmentalImpact = "Very low carbon footprint. Locally sourced apples have minimal transportation emission impacts.",
            funFact     = "Apples float in water because 25% of their total volume is actually empty air.",
            emoji       = "🍎",
            maintenanceTips = listOf("Keep in the refrigerator crisper drawer to keep fresh up to 4 weeks", "Store away from bananas, as ethylene gas causes them to ripen faster"),
            buyingSuggestions = listOf("Select firm apples with smooth skin and no bruises", "Buy organic to avoid conventional agricultural wax coatings"),
            relatedRecommendations = listOf("Apple Corer Slicer", "Fruit Washing Liquid", "Wooden Fruit Basket"),
            specializedActionType = "food", // triggers Nutrition estimate
            specializedActionData = "Calories: 95 kcal · Carbs: 25g · Dietary Fiber: 4.4g · Protein: 0.5g · Vitamin C: 14%"
        ),

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
            emoji       = "📱",
            specializedActionType = "electronics",
            specializedActionData = "Rechargeable Lithium-Ion battery · Screen time safety triggers active"
        ),
        "laptop" to ObjectKnowledge(
            label       = "Laptop",
            category    = ObjectCategory.ELECTRONICS,
            description = "A portable personal computer integrating a display, keyboard and battery into a compact form factor.",
            uses        = listOf("Work", "Education", "Programming", "Creative work", "Gaming"),
            safetyInfo  = "Keep on flat surfaces for ventilation. Use surge protectors.",
            environmentalImpact = "Manufacturing a laptop generates ~300–400 kg of CO₂. Extend device life to reduce footprint.",
            funFact     = "The first laptop, the Osborne 1 (1981), weighed 10.7 kg and had a 5-inch screen.",
            emoji       = "💻",
            specializedActionType = "electronics",
            specializedActionData = "Dual channel copper heat pipes active · Lithium battery protection active"
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
            emoji       = "🍶",
            specializedActionType = "waste",
            specializedActionData = "Standard container packaging. Dispose in recyclable bin."
        ),
        "book" to ObjectKnowledge(
            label       = "Book",
            category    = ObjectCategory.HOUSEHOLD,
            description = "A portable collection of knowledge, stories or information preserved through written or printed text.",
            uses        = listOf("Education", "Entertainment", "Reference", "Cultural preservation"),
            safetyInfo  = "No safety concerns. Reading in poor light can strain eyes.",
            environmentalImpact = "One book requires roughly 2 kg of CO₂ to produce. E-books have lower per-unit impact.",
            funFact     = "The oldest dated printed book, the Diamond Sutra, was printed in 868 CE.",
            emoji       = "📚",
            specializedActionType = "textbook",
            specializedActionData = "Curriculum manual. Open Study Solver."
        ),
        "pizza" to ObjectKnowledge(
            label       = "Pizza",
            category    = ObjectCategory.FOOD,
            description = "A flatbread dish originating from Italy, topped with tomato sauce, cheese and various toppings.",
            uses        = listOf("Meal", "Social gathering food", "Street food"),
            safetyInfo  = "Allergen alert: contains gluten, dairy. Check for nut toppings.",
            environmentalImpact = "A single pizza generates ~2 kg of CO₂. Plant-based toppings reduce this significantly.",
            funFact     = "Americans eat approximately 3 billion pizzas per year — about 23 pounds per person.",
            emoji       = "🍕",
            specializedActionType = "food",
            specializedActionData = "Estimated Calories: 285 kcal per slice · Carbs: 32g · Protein: 12g · Fat: 10g"
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

    fun getKnowledge(label: String): ObjectKnowledge = db[label.lowercase().trim()]
        ?: db.entries.firstOrNull { label.lowercase().trim().contains(it.key) }?.value
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
            },
            maintenanceTips = listOf("Inspect periodically for signs of wear and tear", "Keep clean and dry when storing", "Refer to standard owner manuals for detailed service guides"),
            buyingSuggestions = listOf("Compare energy ratings or eco certifications", "Buy from local verified sustainable vendors"),
            relatedRecommendations = listOf("Protective storage organizer", "Cleaning micro-cloth"),
            specializedActionType = when (cat) {
                ObjectCategory.FOOD -> "food"
                ObjectCategory.ELECTRONICS -> "electronics"
                else -> null
            },
            specializedActionData = when (cat) {
                ObjectCategory.FOOD -> "Calories: 120 kcal · Dynamic nutritional estimation active"
                ObjectCategory.ELECTRONICS -> "Operating current: 5V · Standard consumer electronics details"
                else -> null
            }
        )
    }
}
