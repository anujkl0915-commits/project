package com.rekaro.app.model

/**
 * Represents the category of waste an item belongs to.
 */
enum class WasteCategory(val displayName: String, val bin: String, val binColor: Long) {
    RECYCLABLE("Recyclable", "Blue Bin — Recyclables", 0xFF1E88E5),
    NON_RECYCLABLE("Non-Recyclable", "Red Bin — Reject Waste", 0xFFE53935),
    COMPOSTABLE("Compostable / Wet", "Green Bin — Organic Waste", 0xFF43A047),
    HAZARDOUS("Hazardous", "Red Bin — Hazardous Waste", 0xFFE53935),
    E_WASTE("E-Waste", "E-Waste Collection Center", 0xFFFFB74D)
}

/**
 * A specific step the user should take before disposing the item.
 */
data class DisposalStep(
    val icon: String, // emoji icon
    val instruction: String,
    val isCritical: Boolean = false
)

/**
 * Represents a waste item the user has scanned and its analysis result.
 */
data class WasteAnalysisResult(
    val itemName: String,
    val category: WasteCategory,
    val confidence: Float, // 0.0 to 1.0
    val description: String,
    val disposalSteps: List<DisposalStep>,
    val tips: List<String> = emptyList(),
    val isRecyclable: Boolean = category == WasteCategory.RECYCLABLE ||
            category == WasteCategory.COMPOSTABLE,
    val alternativeDisposalNote: String? = null
)

/**
 * India-specific waste item database with disposal instructions.
 * In production, this would be an ML model; here it's a curated knowledge base
 * covering the most common household waste items in India.
 */
object IndiaWasteDatabase {

    /**
     * Returns analysis for a recognized item, or null if unknown.
     */
    fun analyze(itemName: String): WasteAnalysisResult? {
        val key = itemName.trim().lowercase()
        return database[key] ?: database.entries.firstOrNull { (name, _) ->
            key.contains(name) || name.contains(key)
        }?.value
    }

    /**
     * Get all known item names for ML classification labels.
     */
    fun allLabels(): List<String> = database.keys.toList()

    /**
     * Fallback analysis when item is not in the database.
     * Makes educated guess based on material keywords.
     */
    fun guessFromKeywords(text: String): WasteAnalysisResult? {
        val lower = text.lowercase()
        return when {
            lower.contains("plastic") || lower.contains("pet") || lower.contains("bottle") ->
                database["plastic bottle"]
            lower.contains("paper") || lower.contains("cardboard") || lower.contains("carton") ->
                database["newspaper"]
            lower.contains("glass") || lower.contains("kann") || lower.contains("bottle") ->
                database["glass bottle"]
            lower.contains("metal") || lower.contains("aluminium") || lower.contains("can") ->
                database["aluminium can"]
            lower.contains("food") || lower.contains("rotten") || lower.contains("leftover") ->
                database["food waste"]
            lower.contains("cloth") || lower.contains("fabric") || lower.contains("kapda") ->
                database["old clothes"]
            else -> null
        }
    }

    // Comprehensive India-specific waste database
    private val database = mapOf(
        // ====== PLASTICS ======
        "plastic bottle" to WasteAnalysisResult(
            itemName = "Plastic Bottle",
            category = WasteCategory.RECYCLABLE,
            confidence = 0.95f,
            description = "PET plastic bottles are highly recyclable. Most Indian cities have PET recycling facilities.",
            disposalSteps = listOf(
                DisposalStep("💧", "Empty the bottle completely"),
                DisposalStep("🧴", "Rinse with water — leftover liquids contaminate the batch", isCritical = true),
                DisposalStep("🏷️", "Remove the plastic label and cap"),
                DisposalStep("🫙", "Crush the bottle to save space"),
                DisposalStep("🔵", "Drop in the BLUE recycling bin")
            ),
            tips = listOf(
                "Separate caps are made of PP plastic — check if your local facility accepts them",
                "Bottles with green tint fetch lower recycling value",
                "Clean bottles are worth ₹5-15/kg at scrap shops (kabadiwala)"
            )
        ),
        "milk pouch" to WasteAnalysisResult(
            itemName = "Milk Pouch",
            category = WasteCategory.RECYCLABLE,
            confidence = 0.90f,
            description = "Milk pouches are made of LDPE plastic. They are recyclable BUT must be clean and dry.",
            disposalSteps = listOf(
                DisposalStep("💧", "Rinse thoroughly with water — milk residue spoils the batch", isCritical = true),
                DisposalStep("✂️", "Cut open the pouch and rinse the inside"),
                DisposalStep("🌬️", "Dry it completely"),
                DisposalStep("📦", "Collect multiple pouches, bundle them together"),
                DisposalStep("🔵", "Drop in the BLUE recycling bin or give to kabadiwala")
            ),
            tips = listOf(
                "Wet/moldy pouches cannot be recycled — always dry before storing",
                "Some brands now use recyclable mono-material — check for the recycling symbol",
                "Store at least 10-15 clean pouches before taking to scrap shop"
            )
        ),
        "chips packet" to WasteAnalysisResult(
            itemName = "Chips Packet",
            category = WasteCategory.NON_RECYCLABLE,
            confidence = 0.85f,
            description = "Multi-layered plastic (metallized plastic) used for chips packets is NOT recyclable in most Indian facilities.",
            disposalSteps = listOf(
                DisposalStep("🍽️", "Scrape out any leftover food residue"),
                DisposalStep("🚮", "Dispose in the RED bin (reject waste)"),
                DisposalStep("♻️", "Check if your city has a multi-layer plastic recycling program")
            ),
            tips = listOf(
                "Many snack companies are moving to recyclable mono-material — look for 'Recyclable' label",
                "Some cities like Pune have separate MLP (Multi-Layer Plastic) collection",
                "Alternative: collect MLP waste separately and send to brands that accept it via their take-back programs"
            ),
            alternativeDisposalNote = "Some NGOs collect multi-layer plastic for upcycling into furniture/boards."
        ),
        "shampoo sachet" to WasteAnalysisResult(
            itemName = "Shampoo Sachet",
            category = WasteCategory.NON_RECYCLABLE,
            confidence = 0.88f,
            description = "Small plastic sachets are multi-layered and NOT recyclable through regular channels.",
            disposalSteps = listOf(
                DisposalStep("🧴", "Squeeze out all remaining product"),
                DisposalStep("💧", "Quick rinse if possible"),
                DisposalStep("🚮", "Dispose in the RED bin")
            ),
            tips = listOf(
                "Switch to refill packs or shampoo bars to reduce sachet waste",
                "Some brands like Dove have sachet recycling programs — check their website",
                "Over 1.2 billion sachets are sold in India every month"
            )
        ),
        "plastic bag" to WasteAnalysisResult(
            itemName = "Plastic Carry Bag",
            category = WasteCategory.RECYCLABLE,
            confidence = 0.92f,
            description = "Plastic carry bags (LDPE/HDPE) are recyclable but must be clean and dry.",
            disposalSteps = listOf(
                DisposalStep("💧", "Empty and rinse the bag"),
                DisposalStep("🧽", "Wipe clean if it had food contact"),
                DisposalStep("🌬️", "Dry it thoroughly"),
                DisposalStep("📦", "Fold and store with other plastic bags"),
                DisposalStep("🔵", "Drop in the BLUE recycling bin")
            ),
            tips = listOf(
                "Thin plastic bags (<50 microns) are banned in many Indian states",
                "Use reusable cloth bags when shopping",
                "Plastic bags can jam recycling machinery — bundle them together instead of loose"
            ),
            alternativeDisposalNote = "Clean plastic bags can also be given to kabadiwala."
        ),
        "plastic container" to WasteAnalysisResult(
            itemName = "Plastic Container (Tupperware)",
            category = WasteCategory.RECYCLABLE,
            confidence = 0.87f,
            description = "Hard plastic containers (PP #5) are recyclable. Check the recycling number at the bottom.",
            disposalSteps = listOf(
                DisposalStep("🍽️", "Clean thoroughly — remove all food residue", isCritical = true),
                DisposalStep("🏷️", "Remove any non-plastic labels"),
                DisposalStep("🔢", "Check the recycling number (♳-♷) at the bottom"),
                DisposalStep("🔵", "Drop in the BLUE recycling bin")
            ),
            tips = listOf(
                "PP (#5) containers are widely recyclable in India",
                "PS (#6) containers are NOT recyclable in most cities",
                "Dented or cracked containers can be recycled but fetch lower value"
            )
        ),

        // ====== PAPER ======
        "newspaper" to WasteAnalysisResult(
            itemName = "Newspaper",
            category = WasteCategory.RECYCLABLE,
            confidence = 0.97f,
            description = "Newspaper is highly recyclable and one of the most commonly recycled items in India.",
            disposalSteps = listOf(
                DisposalStep("📰", "Remove any plastic wrapping or inserts"),
                DisposalStep("📎", "Remove staples, bindings, or tape"),
                DisposalStep("📦", "Stack neatly and tie with string"),
                DisposalStep("♻️", "Give to kabadiwala or drop in BLUE bin")
            ),
            tips = listOf(
                "Old newspapers sell for ₹10-18/kg via kabadiwala",
                "Keep newspapers dry — wet paper cannot be recycled",
                "Shredded newspaper can be used for composting or packaging"
            )
        ),
        "cardboard box" to WasteAnalysisResult(
            itemName = "Cardboard / Corrugated Box",
            category = WasteCategory.RECYCLABLE,
            confidence = 0.96f,
            description = "Cardboard is widely recyclable and has good scrap value in India.",
            disposalSteps = listOf(
                DisposalStep("📦", "Flatten the box to save space"),
                DisposalStep("📎", "Remove tape, staples, and plastic inserts", isCritical = true),
                DisposalStep("💧", "Ensure it's dry and clean"),
                DisposalStep("♻️", "Give to kabadiwala or BLUE bin")
            ),
            tips = listOf(
                "Pizza boxes with grease stains: tear off the clean part for recycling, compost the greasy part",
                "Corrugated cardboard sells for ₹8-15/kg",
                "Amazon/Delivery boxes can be reused for shipping"
            )
        ),
        "pizza box" to WasteAnalysisResult(
            itemName = "Pizza Box",
            category = WasteCategory.RECYCLABLE,
            confidence = 0.88f,
            description = "Pizza boxes are tricky! The clean cardboard top can be recycled, but the greasy/oily bottom should go to compost or waste.",
            disposalSteps = listOf(
                DisposalStep("✂️", "TEAR OFF the clean (unsoiled) top lid", isCritical = true),
                DisposalStep("♻️", "Recycle the clean part in BLUE bin"),
                DisposalStep("🌱", "Compost the greasy bottom part in GREEN bin"),
                DisposalStep("🚮", "If no compost available, put greasy part in RED bin")
            ),
            tips = listOf(
                "Oil/grease contaminates the paper recycling process — never put the oily part in recycling!",
                "If the box has plastic/soy sauce packets, separate and dispose them properly",
                "Pro tip: Tear the box along the grease line — visible oil mark = too greasy to recycle"
            )
        ),
        "notebook" to WasteAnalysisResult(
            itemName = "Old Notebook / Paper",
            category = WasteCategory.RECYCLABLE,
            confidence = 0.93f,
            description = "Paper notebooks and documents are recyclable. Remove non-paper elements first.",
            disposalSteps = listOf(
                DisposalStep("📎", "Remove spiral binding, staples, or tape"),
                DisposalStep("🛡️", "Remove plastic covers/transparent sheets"),
                DisposalStep("♻️", "Drop in BLUE bin or give to kabadiwala"),
                DisposalStep("🔐", "Shred if it contains personal information")
            ),
            tips = listOf(
                "Shredded paper: check with your recycler — some accept, some don't",
                "Glossy magazine paper is recyclable but lower quality",
                "Wax-coated paper (like some food wrappers) is NOT recyclable"
            )
        ),

        // ====== GLASS ======
        "glass bottle" to WasteAnalysisResult(
            itemName = "Glass Bottle",
            category = WasteCategory.RECYCLABLE,
            confidence = 0.95f,
            description = "Glass is 100% recyclable and can be recycled endlessly without quality loss.",
            disposalSteps = listOf(
                DisposalStep("💧", "Empty and rinse the bottle"),
                DisposalStep("🥤", "Remove the cap/lid (metal goes in metal recycling)"),
                DisposalStep("🏷️", "Remove label if possible"),
                DisposalStep("🛡️", "Wrap in newspaper — broken glass is dangerous for waste workers", isCritical = true),
                DisposalStep("♻️", "Drop in BLUE bin or return to store")
            ),
            tips = listOf(
                "Broken glass: wrap in multiple newspaper layers, mark 'GLASS' on it",
                "Glass bottles (beer, soft drinks) can be returned to some stores for deposit",
                "Glass does NOT go in the green bin — it's not biodegradable",
                "Colored glass (green, amber) is recycled separately from clear glass"
            )
        ),
        "glass jar" to WasteAnalysisResult(
            itemName = "Glass Jar (Pickle/Jam)",
            category = WasteCategory.RECYCLABLE,
            confidence = 0.93f,
            description = "Glass jars are recyclable. The metal lid may be a separate material.",
            disposalSteps = listOf(
                DisposalStep("💧", "Empty contents and rinse thoroughly"),
                DisposalStep("🧽", "Remove all food residue — pickle smell is tough!", isCritical = true),
                DisposalStep("🔄", "Separate metal lid — recycle with metals"),
                DisposalStep("♻️", "Drop glass jar in BLUE recycling bin")
            ),
            tips = listOf(
                "Soak in warm soapy water to remove stubborn pickle/oil residue",
                "Glass jars make excellent storage containers — reuse before recycling!",
                "The rubber gasket on pickle jar lids goes in RED bin"
            )
        ),

        // ====== METALS ======
        "aluminium can" to WasteAnalysisResult(
            itemName = "Aluminium Can",
            category = WasteCategory.RECYCLABLE,
            confidence = 0.96f,
            description = "Aluminium cans are highly recyclable and the most valuable recyclable material per kg.",
            disposalSteps = listOf(
                DisposalStep("🥤", "Empty all liquid"),
                DisposalStep("💧", "Rinse with water"),
                DisposalStep("🫙", "Crush the can to save space"),
                DisposalStep("♻️", "Drop in BLUE bin or give to kabadiwala")
            ),
            tips = listOf(
                "Aluminium sells for ₹80-120/kg — highest scrap value!",
                "Recycling 1 can saves enough energy to run a TV for 3 hours",
                "Keep steel cans (beans, soup) separate from aluminium cans"
            )
        ),
        "steel can" to WasteAnalysisResult(
            itemName = "Steel / Tin Can",
            category = WasteCategory.RECYCLABLE,
            confidence = 0.90f,
            description = "Steel/tin cans are recyclable but heavier than aluminium.",
            disposalSteps = listOf(
                DisposalStep("🥫", "Empty contents completely"),
                DisposalStep("💧", "Rinse with water"),
                DisposalStep("🏷️", "Remove paper label if possible"),
                DisposalStep("♻️", "Drop in BLUE bin or give to kabadiwala")
            ),
            tips = listOf(
                "A magnet sticks to steel, not aluminium — easy way to tell them apart",
                "Steel scrap sells for ₹20-35/kg",
                "Sharps: fold the lid inward so it doesn't cut anyone"
            )
        ),
        "utensils" to WasteAnalysisResult(
            itemName = "Old Metal Utensils",
            category = WasteCategory.RECYCLABLE,
            confidence = 0.85f,
            description = "Metal utensils (steel, iron, brass) are fully recyclable as metal scrap.",
            disposalSteps = listOf(
                DisposalStep("🧼", "Clean off food residue"),
                DisposalStep("🪵", "Remove wooden/plastic handles if possible"),
                DisposalStep("♻️", "Sell to kabadiwala or give to scrap dealer")
            ),
            tips = listOf(
                "Metal scrap rates (approximate): Steel ₹20-40/kg, Brass ₹250-350/kg, Copper ₹400-500/kg",
                "Non-stick pans: the coating makes them NOT recyclable — put in RED bin",
                "Donate usable utensils to NGOs instead of recycling them"
            )
        ),

        // ====== ORGANIC / KITCHEN ======
        "food waste" to WasteAnalysisResult(
            itemName = "Food Waste (Leftovers)",
            category = WasteCategory.COMPOSTABLE,
            confidence = 0.94f,
            description = "Food waste is compostable. It belongs in the green bin for organic waste processing.",
            disposalSteps = listOf(
                DisposalStep("🍽️", "Separate from non-organic items (plastic, metal)"),
                DisposalStep("💧", "Drain excess water/oil"),
                DisposalStep("🟢", "Put in GREEN composting bin"),
                DisposalStep("🚮", "Do NOT put in plastic bags — use newspaper wrap")
            ),
            tips = listOf(
                "Start home composting! Food waste + dry leaves = great fertilizer",
                "Never mix food waste with recyclables — it contaminates the batch",
                "Meat/bones may need separate processing — check local facility",
                "In many Indian cities, wet waste is collected daily — time your disposal"
            )
        ),
        "fruit peel" to WasteAnalysisResult(
            itemName = "Fruit & Vegetable Peels",
            category = WasteCategory.COMPOSTABLE,
            confidence = 0.96f,
            description = "Fruit and vegetable peels are excellent for composting.",
            disposalSteps = listOf(
                DisposalStep("🥬", "Collect peels in a separate container"),
                DisposalStep("🟢", "Drop in GREEN bin for composting"),
                DisposalStep("🌱", "Or start a home compost bin!")
            ),
            tips = listOf(
                "Citrus peels (orange, lemon) are great for compost but use in moderation",
                "Banana peels make excellent fertilizer — bury in garden soil",
                "Some peels (potato, carrot) can be used to make vegetable broth"
            )
        ),
        "tea leaves" to WasteAnalysisResult(
            itemName = "Used Tea Leaves / Coffee Grounds",
            category = WasteCategory.COMPOSTABLE,
            confidence = 0.95f,
            description = "Tea leaves and coffee grounds are great for composting and gardening.",
            disposalSteps = listOf(
                DisposalStep("🧴", "Remove any plastic tea bag material", isCritical = true),
                DisposalStep("🌬️", "Dry the leaves/grounds"),
                DisposalStep("🌱", "Add to compost bin or use directly as plant fertilizer"),
                DisposalStep("🟢", "Or put in GREEN bin")
            ),
            tips = listOf(
                "Used tea leaves are excellent rose plant fertilizer",
                "Coffee grounds repel ants and snails",
                "Plastic tea bags: cut open and compost the leaves, discard the bag in RED bin"
            )
        ),
        "coconut shell" to WasteAnalysisResult(
            itemName = "Coconut Shell",
            category = WasteCategory.COMPOSTABLE,
            confidence = 0.85f,
            description = "Coconut shells are biodegradable but take very long to decompose. Best used as fuel or garden material.",
            disposalSteps = listOf(
                DisposalStep("🌞", "Dry the shell in sunlight"),
                DisposalStep("🪓", "Break into smaller pieces"),
                DisposalStep("🌱", "Use as plant mulch or in garden beds"),
                DisposalStep("🟢", "OR put in GREEN bin for municipal composting")
            ),
            tips = listOf(
                "Coconut shells make excellent natural bowls and planters",
                "Dried shells are used as fuel in some regions",
                "Coir (husk) is used for making ropes, mats, and potting soil"
            )
        ),

        // ====== NON-RECYCLABLES ======
        "thermocol" to WasteAnalysisResult(
            itemName = "Thermocol (Styrofoam)",
            category = WasteCategory.NON_RECYCLABLE,
            confidence = 0.92f,
            description = "Thermocol (expanded polystyrene) is NOT recyclable in most Indian cities. It's a major environmental hazard.",
            disposalSteps = listOf(
                DisposalStep("🏠", "Break into smaller pieces to reduce volume"),
                DisposalStep("🚮", "Dispose in RED bin (reject waste)"),
                DisposalStep("❌", "NEVER burn thermocol — releases toxic fumes!", isCritical = true)
            ),
            tips = listOf(
                "Burning thermocol releases styrene gas — highly toxic!",
                "Some cities have specialized thermocol recycling plants",
                "Alternatives: use biodegradable packaging materials"
            ),
            alternativeDisposalNote = "Some courier companies accept thermocol for reuse."
        ),
        "sanitary pad" to WasteAnalysisResult(
            itemName = "Sanitary Pad / Diaper",
            category = WasteCategory.HAZARDOUS,
            confidence = 0.90f,
            description = "Sanitary waste is hazardous and should NEVER be flushed or put in regular recycling.",
            disposalSteps = listOf(
                DisposalStep("📦", "Wrap securely in newspaper or the original wrapper"),
                DisposalStep("🚮", "Dispose in RED bin — marked as sanitary waste"),
                DisposalStep("🧼", "Wash hands thoroughly after handling"),
                DisposalStep("❌", "NEVER flush — blocks sewage systems!", isCritical = true)
            ),
            tips = listOf(
                "Use biodegradable/eco-friendly sanitary products when possible",
                "Many apartment complexes have separate sanitary waste incinerators",
                "Menstrual cups and cloth pads are zero-waste alternatives"
            )
        ),
        "broken glass" to WasteAnalysisResult(
            itemName = "Broken Glass (CRITICAL - Handle with Care)",
            category = WasteCategory.HAZARDOUS,
            confidence = 0.95f,
            description = "Broken glass is hazardous waste. It can injure waste workers and damage recycling equipment.",
            disposalSteps = listOf(
                DisposalStep("🧹", "Sweep carefully — use a broom, NOT bare hands", isCritical = true),
                DisposalStep("📰", "Wrap in MULTIPLE layers of newspaper", isCritical = true),
                DisposalStep("✏️", "Mark '⚠️ BROKEN GLASS' clearly on the package"),
                DisposalStep("🚮", "Place in RED bin — NOT in recycling bin")
            ),
            tips = listOf(
                "Wet newspaper before wrapping — helps contain tiny glass shards",
                "Never put loose broken glass in any bin — it's a safety hazard for everyone",
                "Consider wearing gloves while cleaning broken glass"
            )
        ),
        "e_waste" to WasteAnalysisResult(
            itemName = "E-Waste (Old Electronics)",
            category = WasteCategory.E_WASTE,
            confidence = 0.88f,
            description = "Electronic waste contains valuable metals AND toxic materials. Must be disposed through certified e-waste recyclers.",
            disposalSteps = listOf(
                DisposalStep("🔋", "Remove batteries if possible — recycle separately", isCritical = true),
                DisposalStep("🗑️", "Delete personal data from devices", isCritical = true),
                DisposalStep("🏭", "Drop at authorized e-waste collection center"),
                DisposalStep("♻️", "Or give to brand's take-back program")
            ),
            tips = listOf(
                "Dell, HP, Samsung, and Apple have free e-waste take-back in India",
                "E-waste rules in India require producers to provide collection points",
                "Working old electronics can be donated or sold on OLX/Cashify",
                "Data: factory reset all devices before disposal"
            )
        ),
        "battery" to WasteAnalysisResult(
            itemName = "Used Battery",
            category = WasteCategory.HAZARDOUS,
            confidence = 0.94f,
            description = "Batteries contain toxic heavy metals (lead, mercury, cadmium) and MUST be disposed as hazardous waste.",
            disposalSteps = listOf(
                DisposalStep("🪫", "Wrap the terminals with tape (prevents short-circuit)", isCritical = true),
                DisposalStep("📦", "Store in a dry container away from heat"),
                DisposalStep("🏭", "Drop at battery collection centre or e-waste facility"),
                DisposalStep("❌", "NEVER throw in regular bins or burn!", isCritical = true)
            ),
            tips = listOf(
                "Most mobile stores and electronics shops accept used batteries",
                "Car batteries have a deposit system — exchange at the shop",
                "Switch to rechargeable batteries — they last longer and reduce waste"
            )
        ),

        // ====== CLOTHING ======
        "old clothes" to WasteAnalysisResult(
            itemName = "Old Clothes / Textile",
            category = WasteCategory.RECYCLABLE,
            confidence = 0.85f,
            description = "Old clothes can be donated, upcycled, or recycled. Synthetic fabrics can take 200+ years to decompose.",
            disposalSteps = listOf(
                DisposalStep("🧺", "Wash and dry the clothes"),
                DisposalStep("✅", "Sort wearable vs unwearable"),
                DisposalStep("🎁", "Donate wearable clothes to NGOs/clothing banks"),
                DisposalStep("🧵", "Unwearable: cut into rags or give for textile recycling")
            ),
            tips = listOf(
                "Goonj, Red Cross, and local temples accept clothing donations",
                "Old t-shirts make great cleaning rags",
                "Synthetic fabrics (polyester, nylon) are NOT biodegradable",
                "Some brands (H&M, Zara) have textile recycling bins in their stores"
            )
        ),

        // ====== MEDICAL ======
        "medicine strip" to WasteAnalysisResult(
            itemName = "Medicine / Tablet Strip",
            category = WasteCategory.HAZARDOUS,
            confidence = 0.82f,
            description = "Medicine strips (aluminium + plastic blister packs) are NOT recyclable and should be treated as hazardous waste.",
            disposalSteps = listOf(
                DisposalStep("💊", "Remove any remaining tablets"),
                DisposalStep("✂️", "Cut the strip to prevent misuse"),
                DisposalStep("🚮", "Dispose in RED bin — labeled as pharmaceutical waste")
            ),
            tips = listOf(
                "NEVER flush medicines down the toilet — they contaminate water sources",
                "Expired medicines can be returned to some pharmacies",
                "Blister packs are multi-layer and very hard to recycle"
            )
        ),

        // ====== MISC ======
        "mask" to WasteAnalysisResult(
            itemName = "Used Mask / PPE",
            category = WasteCategory.HAZARDOUS,
            confidence = 0.91f,
            description = "Used masks and PPE are bio-hazardous waste. They must be disposed safely.",
            disposalSteps = listOf(
                DisposalStep("✂️", "Cut the straps (prevents wildlife entanglement)", isCritical = true),
                DisposalStep("📦", "Wrap in a paper bag or newspaper"),
                DisposalStep("🚮", "Dispose in RED bin"),
                DisposalStep("🧼", "Wash hands immediately after")
            ),
            tips = listOf(
                "Switch to reusable cloth masks — they generate far less waste",
                "Never litter masks — they end up in oceans and rivers",
                "PPE kits should go through proper medical waste channels"
            )
        ),
        "cigarette butt" to WasteAnalysisResult(
            itemName = "Cigarette Butt",
            category = WasteCategory.NON_RECYCLABLE,
            confidence = 0.80f,
            description = "Cigarette butts contain plastic filters and toxic chemicals. They are NOT biodegradable despite looking like cotton.",
            disposalSteps = listOf(
                DisposalStep("💧", "Ensure it's fully extinguished", isCritical = true),
                DisposalStep("📦", "Wrap in paper/tissue"),
                DisposalStep("🚮", "Dispose in RED bin — do NOT litter!", isCritical = true)
            ),
            tips = listOf(
                "One cigarette butt can contaminate 1000 litres of water",
                "Cigarette filters are made of cellulose acetate — a type of plastic",
                "Carry a small portable ashtray if you smoke"
            )
        ),
        "light bulb" to WasteAnalysisResult(
            itemName = "Broken Light Bulb (CFL/LED)",
            category = WasteCategory.HAZARDOUS,
            confidence = 0.86f,
            description = "CFL bulbs contain mercury — toxic! LED bulbs contain electronic components. Both need special disposal.",
            disposalSteps = listOf(
                DisposalStep("🪟", "Open windows to ventilate (CFL breakage)", isCritical = true),
                DisposalStep("🧹", "Sweep carefully — don't vacuum (spreads mercury)"),
                DisposalStep("📦", "Seal in a plastic bag/container"),
                DisposalStep("🏭", "Drop at e-waste facility or bulb collection point")
            ),
            tips = listOf(
                "Havells, Philips, and Syska have bulb take-back programs",
                "LED bulbs are less toxic than CFLs but still count as e-waste",
                "Working old bulbs can be donated"
            )
        )
    )
}