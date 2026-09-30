package com.example.data

// ==========================================
// EDUCATION ARTICLE
// ==========================================

data class EducationArticle(
    val title: String,
    val slug: String,
    val category: String,
    val content: String,
    val safetyNote: String? = null
)

// ==========================================
// SELF-CARE ITEM
// ==========================================

data class SelfCareItem(
    val title: String,
    val description: String,
    val category: String,
    val steps: List<String>
)

// ==========================================
// CARE PRODUCT
// ==========================================

data class CareProduct(
    val name: String,
    val slug: String,
    val emoji: String,
    val whatItIs: String,
    val whenItHelps: String,
    val whatToCheck: String,
    val safetyNote: String,
    val nearbyQuery: String
)

// ==========================================
// HEALTH AWARENESS TOPIC
// ==========================================

data class HealthAwarenessTopic(
    val title: String,
    val slug: String,
    val emoji: String,
    val summary: String,
    val content: String,
    val symptoms: List<String>,
    val whenToSeekHelp: String,
    val disclaimer: String
)

// ==========================================
// LUNA CONTENT OBJECT
// ==========================================

object LunaContent {

    // Global disclaimer — shown on all health screens, AI, onboarding
    const val GLOBAL_DISCLAIMER =
        "LunaCare provides general educational support only. It is not medical advice, diagnosis, or treatment. " +
        "For severe pain, heavy bleeding, fever, infection symptoms, pregnancy concerns, fainting, " +
        "self-harm thoughts, or urgent symptoms, contact a qualified healthcare professional or emergency service immediately."

    const val REGIONAL_TERMINOLOGY_NOTE =
        "Health terms can vary by region. LunaCare uses common user-searched terms for education only. " +
        "A qualified healthcare professional can explain what applies to you."

    const val RELIGIOUS_CONTENT_NOTE =
        "Religious guidance may vary. For personalised faith-based advice, please consult a trusted " +
        "religious scholar and your healthcare professional."

    // ==========================================
    // MENSTRUAL CUP ARTICLES
    // ==========================================

    val articles = listOf(
        EducationArticle(
            title = "What is a Menstrual Cup?",
            slug = "what-is-menstrual-cup",
            category = "Menstrual Cup",
            content = """
A menstrual cup is a reusable hygiene product — a small, flexible funnel-shaped cup made of medical-grade silicone or latex that is inserted to collect menstrual fluid.

Unlike pads or tampons which absorb flow, cups collect it. This keeps the vaginal environment more balanced and reduces dryness.

Key facts:
• Can hold 20–30 ml (1–3 times more than a pad)
• Can be worn up to 8–12 hours depending on flow
• Reusable for up to 10 years with proper care
• Significantly reduces landfill waste
            """.trimIndent(),
            safetyNote = "Always choose medical-grade silicone cups. If you have an IUD, consult your gynecologist before use."
        ),
        EducationArticle(
            title = "Benefits and Limitations",
            slug = "benefits-limitations",
            category = "Menstrual Cup",
            content = """
Benefits:
• Budget-friendly: A single cup can last 10 years, saving significant money
• Eco-friendly: Reduces thousands of pads and tampons from landfills
• Longer wear: Up to 12 hours for lighter days
• No odour: Fluid is sealed away from air
• No toxic shock risk if used correctly and changed regularly

Limitations:
• Learning curve: Takes 2–3 cycles to feel comfortable
• Messier than pads in public restrooms
• Finding the right size/shape may take trial and error
• Not suitable for everyone (see medical caveats guide)
            """.trimIndent(),
            safetyNote = "If you have an IUD, consult your gynecologist before using a menstrual cup."
        ),
        EducationArticle(
            title = "Choosing Your Cup Size",
            slug = "choosing-cup-size",
            category = "Menstrual Cup",
            content = """
Most brands offer two main sizes:

Small (Size 1): Generally for those under 30 who have not given birth vaginally.

Large (Size 2): Generally for those over 30, or those who have given birth vaginally.

Also consider:
• Cervix height: A low cervix requires a shorter cup; a high cervix needs a longer cup
• Flow: Heavy flow may benefit from a higher-capacity cup
• Pelvic floor: Strong muscles may suit a firmer cup; weaker muscles may suit a softer one

When unsure, start with a smaller, softer cup and adjust over time.
            """.trimIndent(),
            safetyNote = "When in doubt, start small and soft. Your body will guide you over a few cycles."
        ),
        EducationArticle(
            title = "How to Fold a Cup",
            slug = "how-to-fold-cup",
            category = "Menstrual Cup",
            content = """
You must fold the cup for insertion. Three common folds:

1. C-Fold (U-Fold): Press the sides together and fold in half. Creates a 'C' shape. Good for beginners.

2. Punch-Down Fold: Press one point of the rim down into the cup. Creates a tight triangle. Easier to insert.

3. 7-Fold: Flatten the cup, fold one corner down diagonally. Creates a '7' shape.

Try each method and use what feels most comfortable for your body.
            """.trimIndent(),
            safetyNote = "Always wash hands thoroughly before folding."
        ),
        EducationArticle(
            title = "Insertion Guide",
            slug = "insertion-guide",
            category = "Menstrual Cup",
            content = """
Step-by-step safe insertion:

1. Wash hands with unscented soap and rinse well
2. Fold the cup using your preferred method
3. Find a comfortable position: squat, sit on toilet, or raise one leg
4. Take a deep breath and relax your pelvic floor muscles
5. Gently separate outer lips with one hand
6. Insert the cup tilted slightly backward (toward your spine), not straight up
7. Once inside, let the cup spring open by releasing your grip
8. Rotate gently and run a finger around the base to ensure a full seal

The base of the cup should sit low but fully inside — not protruding. The stem should not be visible but should be reachable.
            """.trimIndent(),
            safetyNote = "Never force insertion. If you feel severe pain, stop. A drop of water-based lubricant on the rim can help."
        ),
        EducationArticle(
            title = "Removal Guide",
            slug = "removal-guide",
            category = "Menstrual Cup",
            content = """
Safe removal is about breaking the vacuum seal — NEVER pull by the stem alone.

1. Wash hands thoroughly
2. Squat down to shorten the vaginal canal
3. Insert thumb and index finger until you reach the cup base
4. Pinch the base firmly to break the seal
5. Gently rock the cup side to side as you bring it down
6. Keep the cup upright to avoid spills
7. Empty into the toilet, rinse, and reinsert or store

If the cup feels stuck: stay calm, relax pelvic muscles, try different positions, and pinch — never pull the stem alone.
            """.trimIndent(),
            safetyNote = "Never yank the stem. Always pinch the base to release suction first. If truly stuck, see a healthcare provider."
        ),
        EducationArticle(
            title = "Cleaning & Sterilizing",
            slug = "cleaning-sterilizing",
            category = "Menstrual Cup",
            content = """
During your period:
• Rinse with cold water first (hot water can set stains)
• Wash with unscented, oil-free mild soap
• Rinse thoroughly before reinserting
• Clean holes around the rim with a soft toothbrush or syringe

Between cycles:
• Boil in water for 5–7 minutes
• Use a whisk or cloth to prevent cup touching the pot bottom
• Let air-dry completely
• Store in the breathable cotton pouch — NEVER in an airtight container

Do NOT use: dish soap, scented soaps, hand sanitizer, bleach, or vinegar.
            """.trimIndent(),
            safetyNote = "Proper sterilisation is critical. Improper cleaning increases risk of bacterial vaginosis or yeast infection."
        ),
        EducationArticle(
            title = "Who Should Consult a Doctor First",
            slug = "medical-caveats",
            category = "Menstrual Cup",
            content = """
Consult a healthcare professional BEFORE using a menstrual cup if you:
• Have an Intrauterine Device (IUD)
• Are less than 6 weeks post-childbirth or post-surgery
• Have been diagnosed with pelvic organ prolapse
• Experience chronic pelvic pain or vaginismus
• Have had Toxic Shock Syndrome (TSS) in the past
• Have active pelvic infection or skin irritation

If you use an IUD: check your strings regularly and notify your doctor that you use a cup.

If you experience persistent fever, severe cramping, or unusual discharge while using a cup — remove it and seek medical attention immediately.
            """.trimIndent(),
            safetyNote = "Stop using immediately if you notice persistent severe cramping, fever, or bad-smelling discharge."
        ),

        // ==========================================
        // PERIOD & PMS ARTICLES
        // ==========================================

        EducationArticle(
            title = "Understanding PMS",
            slug = "pms-guide",
            category = "Period & PMS",
            content = """
Premenstrual Syndrome (PMS) refers to physical and emotional changes that occur in the 1–2 weeks before a period starts, driven by hormonal shifts (drop in estrogen and progesterone).

Common physical signs:
• Pelvic cramps and lower back pain
• Breast tenderness and bloating
• Fatigue, headaches, and skin changes

Common emotional signs:
• Mood swings, irritability, anxiety
• Difficulty concentrating, tearfulness
• Food cravings, especially for sweets or salty foods
• Sleep disturbances

Tracking your cycle helps you anticipate these changes and plan extra self-care during this phase.

Note: LunaCare cannot diagnose PMS. If symptoms severely disrupt your daily life, consult a healthcare professional.
            """.trimIndent(),
            safetyNote = "If symptoms are debilitating or include severe depression, consult a doctor. It may indicate PMDD, which is treatable."
        ),
        EducationArticle(
            title = "PCOS Awareness",
            slug = "pcos-awareness",
            category = "Health Awareness",
            content = """
Polycystic Ovary Syndrome (PCOS) is a common hormonal condition that affects people who menstruate. It involves the ovaries producing higher-than-normal amounts of androgens (male hormones).

Common signs people report (self-reported awareness only — not a diagnosis):
• Irregular or infrequent periods
• Difficulty with weight
• Skin changes (acne, oiliness)
• Excess hair growth in some areas
• Hair thinning
• Difficulty conceiving

IMPORTANT: Only a healthcare professional can diagnose PCOS through clinical assessment and tests. LunaCare does not diagnose.

The term PCOD (Polycystic Ovarian Disease) is used in some regions — particularly South Asia — to refer to similar symptoms. Ask your doctor which term applies in your context.
            """.trimIndent(),
            safetyNote = GLOBAL_DISCLAIMER
        ),
        EducationArticle(
            title = "PMOS Awareness",
            slug = "pmos-awareness",
            category = "Health Awareness",
            content = """
PMOS (Premenstrual Ovulatory Syndrome or Pre-Menstrual Ovulation Syndrome) is a term sometimes used to describe symptom patterns around the ovulation phase, similar to PMS but occurring at a different cycle point.

Note: PMOS is not a universally standardised medical term. Regional and colloquial usage varies. LunaCare includes it here because many users search for this term.

If you experience mid-cycle symptoms such as:
• Pelvic discomfort around ovulation
• Mood shifts mid-cycle
• Breast tenderness not near your period

...discuss these with a qualified healthcare professional who can assess your individual cycle pattern.

$REGIONAL_TERMINOLOGY_NOTE
            """.trimIndent(),
            safetyNote = GLOBAL_DISCLAIMER
        ),
        EducationArticle(
            title = "Endometriosis Awareness",
            slug = "endometriosis-awareness",
            category = "Health Awareness",
            content = """
Endometriosis is a condition where tissue similar to the uterine lining grows outside the uterus. It is a leading cause of severe period pain and can affect fertility.

Signs some people report:
• Severe cramps during or between periods
• Pain during sex or bowel movements
• Heavy or irregular periods
• Bloating ('endo belly')
• Chronic pelvic pain

Endometriosis is often under-diagnosed and can take years to identify. If you experience severe, life-disrupting pain during your period, please seek medical assessment — you deserve proper care.

LunaCare cannot diagnose endometriosis. This is educational awareness only.
            """.trimIndent(),
            safetyNote = "If period pain is severe enough to stop your daily activities, please see a doctor. You deserve proper medical assessment."
        ),
        EducationArticle(
            title = "Heavy Bleeding Awareness",
            slug = "heavy-bleeding-awareness",
            category = "Health Awareness",
            content = """
Menorrhagia (heavy menstrual bleeding) means soaking through a pad or tampon every hour for several consecutive hours.

Signs of concern:
• Soaking through protection hourly for 2+ hours
• Passing large clots (larger than a 50-cent coin)
• Needing to double up on protection
• Period lasting longer than 7 days
• Feeling faint, extremely tired, or short of breath during your period

These signs may warrant medical assessment. Causes can include fibroids, hormonal imbalances, thyroid issues, or other conditions that are all manageable with proper care.

Do not dismiss severe bleeding as 'normal.' Seek medical assessment.
            """.trimIndent(),
            safetyNote = "If you are soaking through a pad/cup hourly for 2+ hours, this requires urgent medical attention."
        ),
        EducationArticle(
            title = "Infection Warning Signs",
            slug = "infection-warning-signs",
            category = "Health Awareness",
            content = """
Knowing when something may be an infection is important. Seek medical attention if you experience:

General warning signs:
• Fever above 38°C (100.4°F) during or after your period
• Unusual or foul-smelling vaginal discharge
• Severe pain that is sudden or getting worse
• Pelvic swelling or tenderness
• Burning or pain when urinating
• Rash or unusual skin changes in the vaginal area

If using a menstrual cup: remove immediately if you develop fever, severe cramping, or unusual discharge. These could be signs of Toxic Shock Syndrome (TSS) — seek emergency care.

TSS is rare but serious. Do not leave a cup in for more than 12 hours.
            """.trimIndent(),
            safetyNote = "Fever + severe pain + menstrual product in use = possible TSS. Remove product and seek emergency care immediately."
        ),
        EducationArticle(
            title = "Mental Health & Your Cycle",
            slug = "mental-health-cycle",
            category = "Emotional Wellbeing",
            content = """
Hormonal changes throughout the menstrual cycle can significantly affect mood, anxiety, energy, and sleep.

Phases and mental health:
• Menstrual phase: Some feel increased introspection and fatigue. Rest is important.
• Follicular phase: Rising estrogen often lifts mood and energy. Good time for learning and socialising.
• Ovulatory phase: Many feel most confident and social. Estrogen and testosterone peak.
• Luteal phase: Progesterone rises then drops. This is when PMS emotions often appear. Self-compassion is key.

Strategies that may help:
• Gentle movement (walking, yoga)
• Consistent sleep schedule
• Limiting caffeine and sugar
• Journaling and mindfulness
• Reaching out to trusted support

If mood changes are severe or interfere with daily life, please speak with a mental health professional or doctor.
            """.trimIndent(),
            safetyNote = "If you experience thoughts of self-harm, please contact emergency services or a crisis line immediately. You deserve support."
        ),
        EducationArticle(
            title = "When to Seek Medical Help",
            slug = "when-to-seek-help",
            category = "Health Awareness",
            content = """
Please seek medical attention promptly if you experience:

URGENT — Contact emergency services or go to a hospital now:
• Thoughts of self-harm or suicide
• Severe pelvic pain that comes on suddenly
• Soaking through a pad/cup hourly for 2+ hours
• Fever above 38°C with pelvic pain
• Fainting or difficulty breathing
• Signs of pregnancy with severe pain or bleeding

Soon — See a doctor this week:
• Period that hasn't started in 90+ days (and you're not pregnant)
• Period lasting more than 7–10 days
• Severe, life-disrupting cramps
• Sudden change in your normal cycle pattern
• Unusual discharge with odour or colour

General — Book a routine appointment:
• Questions about PCOS, PCOD, PMOS, or endometriosis
• Menstrual cup fitting or safety questions with IUD
• Mood or anxiety concerns lasting more than two weeks
• Any concern you have about your reproductive health

You always deserve to be listened to by a healthcare professional.
            """.trimIndent(),
            safetyNote = GLOBAL_DISCLAIMER
        ),
        EducationArticle(
            title = "Managing PMS Anxiety and Mood",
            slug = "pms-anxiety-mood",
            category = "Emotional Wellbeing",
            content = """
Hormonal changes in the luteal phase can disrupt neurotransmitters like serotonin, causing mood drops and anxiety.

Helpful strategies:
• Gentle movement: Yoga or light walking releases endorphins
• Mindfulness: 5-minute breathing exercises soothe the nervous system
• Warm compresses: A warm water bottle on the belly reduces physical tension
• Sleep hygiene: Consistent bedtime helps regulate mood
• Self-kindness: Acknowledge that hormones influence emotional sensitivity

Tracking your mood alongside your cycle helps you prepare and respond with compassion rather than confusion.
            """.trimIndent(),
            safetyNote = "Severe premenstrual mood episodes may indicate PMDD (Premenstrual Dysphoric Disorder). Please consult a healthcare provider."
        )
    )

    // ==========================================
    // CARE PRODUCTS
    // ==========================================

    val careProducts = listOf(
        CareProduct(
            name = "Sanitary Pads",
            slug = "sanitary-pads",
            emoji = "🩹",
            whatItIs = "Absorbent pads worn inside underwear to absorb menstrual flow.",
            whenItHelps = "During periods of any flow level. Good for beginners or light activity.",
            whatToCheck = "Look for breathable, chemical-free options. Change every 4–6 hours or sooner if needed.",
            safetyNote = "Change frequently to prevent bacterial growth. Use unscented versions to avoid irritation.",
            nearbyQuery = "sanitary pads store near me"
        ),
        CareProduct(
            name = "Period Panties",
            slug = "period-panties",
            emoji = "🌸",
            whatItIs = "Reusable underwear with built-in absorbent layers for period protection.",
            whenItHelps = "As backup with a cup, on light days, or overnight.",
            whatToCheck = "Choose certified chemical-free options. Check absorbency rating for your flow.",
            safetyNote = "Rinse in cold water before washing. Machine wash on cold. Avoid fabric softeners.",
            nearbyQuery = "period panties near me"
        ),
        CareProduct(
            name = "Menstrual Cup",
            slug = "menstrual-cup",
            emoji = "🌙",
            whatItIs = "A reusable silicone cup that collects menstrual flow. Eco-friendly and long-lasting.",
            whenItHelps = "For any flow level. Up to 12 hours of wear. Great for sports, sleep, and travel.",
            whatToCheck = "Choose medical-grade silicone, FDA-cleared. Select correct size for your anatomy.",
            safetyNote = "Consult a doctor if you have an IUD. Boil between cycles. Change every 8–12 hours.",
            nearbyQuery = "menstrual cup store near me"
        ),
        CareProduct(
            name = "Hot Water Bag",
            slug = "hot-water-bag",
            emoji = "🔥",
            whatItIs = "A rubber or silicone bag filled with hot water, used as a warming pad.",
            whenItHelps = "Relieves period cramps, lower back pain, and general discomfort.",
            whatToCheck = "Check for leaks before use. Wrap in a cloth — never apply directly to bare skin.",
            safetyNote = "Use warm, not boiling water. Never fall asleep with it directly on skin.",
            nearbyQuery = "hot water bag pharmacy near me"
        ),
        CareProduct(
            name = "Heating Pad",
            slug = "heating-pad",
            emoji = "♨️",
            whatItIs = "An electric pad that provides consistent warmth for cramp and muscle relief.",
            whenItHelps = "Continuous warmth for period cramps, back pain, and muscle tension.",
            whatToCheck = "Look for auto-shutoff feature. Use lowest comfortable setting.",
            safetyNote = "Never use on broken skin. Do not sleep with it on. Follow manufacturer instructions.",
            nearbyQuery = "heating pad store near me"
        ),
        CareProduct(
            name = "Cotton Underwear",
            slug = "cotton-underwear",
            emoji = "🧺",
            whatItIs = "Breathable natural-fibre underwear that supports vaginal health.",
            whenItHelps = "Especially useful during and after your period. Reduces irritation and moisture.",
            whatToCheck = "100% organic cotton for most sensitive skin. Avoid synthetic fibres during your period.",
            safetyNote = "Change daily. Wash with gentle, unscented detergent.",
            nearbyQuery = "cotton underwear near me"
        ),
        CareProduct(
            name = "Dark Chocolate",
            slug = "dark-chocolate",
            emoji = "🍫",
            whatItIs = "A comfort food that many find enjoyable during their period.",
            whenItHelps = "As a gentle comfort treat. Not a medical treatment.",
            whatToCheck = "Choose 70%+ cacao. Low sugar options if suitable for your diet.",
            safetyNote = "This is comfort food, not treatment. It does not cure PMS or cramps. Enjoy in moderation.",
            nearbyQuery = "dark chocolate store near me"
        ),
        CareProduct(
            name = "Pharmacy / Dispensary",
            slug = "pharmacy",
            emoji = "💊",
            whatItIs = "A local pharmacy where you can find pads, cups, pain relief, and care products.",
            whenItHelps = "Any time you need supplies or over-the-counter comfort items.",
            whatToCheck = "Always take medication only as advised by a healthcare professional or product label.",
            safetyNote = "LunaCare does not recommend specific medications. Consult a pharmacist for guidance.",
            nearbyQuery = "pharmacy near me"
        ),
        CareProduct(
            name = "Clinic / Gynecologist",
            slug = "gynecologist",
            emoji = "🏥",
            whatItIs = "A medical facility where you can receive professional reproductive and menstrual health care.",
            whenItHelps = "For any concern about your cycle, pain, discharge, or reproductive health.",
            whatToCheck = "Look for certified gynecologists or OB-GYN specialists.",
            safetyNote = "LunaCare is educational only. Please see a qualified professional for medical assessment.",
            nearbyQuery = "gynecologist clinic near me"
        ),
        CareProduct(
            name = "Warm Tea / Café",
            slug = "warm-tea",
            emoji = "☕",
            whatItIs = "A warm drink for comfort during your period.",
            whenItHelps = "Ginger tea, chamomile, and warm drinks may ease discomfort for some people.",
            whatToCheck = "Choose caffeine-free options if you are sensitive to caffeine during your period.",
            safetyNote = "Not a medical treatment. Herbal teas are comfort beverages, not cures.",
            nearbyQuery = "cafe warm tea near me"
        )
    )

    // ==========================================
    // HEALTH AWARENESS TOPICS
    // ==========================================

    val healthAwarenessTopics = listOf(
        HealthAwarenessTopic(
            title = "PMS",
            slug = "pms",
            emoji = "🌊",
            summary = "Premenstrual Syndrome — emotional and physical changes before your period.",
            content = "PMS involves physical and emotional symptoms in the 1–2 weeks before menstruation. Common signs include cramps, bloating, mood swings, and food cravings. Tracking helps you prepare.",
            symptoms = listOf("Cramps", "Bloating", "Mood swings", "Irritability", "Food cravings", "Fatigue", "Headaches", "Breast tenderness"),
            whenToSeekHelp = "If PMS severely disrupts your daily life, consult a doctor. Severe premenstrual mood episodes may indicate PMDD.",
            disclaimer = GLOBAL_DISCLAIMER
        ),
        HealthAwarenessTopic(
            title = "PCOS",
            slug = "pcos",
            emoji = "🔬",
            summary = "Polycystic Ovary Syndrome — a hormonal condition affecting the ovaries.",
            content = "PCOS involves higher-than-normal androgens, irregular cycles, and possible cysts on the ovaries. Only a healthcare professional can diagnose PCOS. LunaCare does not diagnose.",
            symptoms = listOf("Irregular periods", "Excess hair growth", "Hair thinning", "Acne", "Weight changes", "Difficulty conceiving"),
            whenToSeekHelp = "If you experience irregular periods and other symptoms listed above, consult a gynecologist for proper assessment.",
            disclaimer = GLOBAL_DISCLAIMER
        ),
        HealthAwarenessTopic(
            title = "PCOD",
            slug = "pcod",
            emoji = "🔬",
            summary = "Polycystic Ovarian Disease — a term used widely in South Asia for similar symptoms.",
            content = "PCOD is a term common in South Asia referring to cysts in the ovaries with hormonal involvement. It overlaps with what is called PCOS in Western medicine. $REGIONAL_TERMINOLOGY_NOTE",
            symptoms = listOf("Irregular periods", "Cysts on ovaries", "Hormonal imbalance", "Weight changes"),
            whenToSeekHelp = "Consult a qualified gynecologist or endocrinologist for an accurate assessment.",
            disclaimer = GLOBAL_DISCLAIMER
        ),
        HealthAwarenessTopic(
            title = "PMOS Awareness",
            slug = "pmos",
            emoji = "📊",
            summary = "A term some use for symptoms around ovulation.",
            content = "PMOS is not a universally standardised medical term. Some use it to describe mid-cycle symptoms around ovulation. $REGIONAL_TERMINOLOGY_NOTE",
            symptoms = listOf("Mid-cycle pelvic discomfort", "Mood shifts", "Breast tenderness mid-cycle"),
            whenToSeekHelp = "If you experience persistent mid-cycle symptoms, consult a healthcare professional.",
            disclaimer = GLOBAL_DISCLAIMER
        ),
        HealthAwarenessTopic(
            title = "Endometriosis",
            slug = "endometriosis",
            emoji = "🌺",
            summary = "A condition where tissue similar to the uterine lining grows outside it.",
            content = "Endometriosis is a leading cause of severe period pain. It can take years to diagnose. If your period pain is severe enough to stop daily activities, please seek medical assessment.",
            symptoms = listOf("Severe cramps", "Pain during sex", "Chronic pelvic pain", "Heavy periods", "Infertility concerns", "Bloating"),
            whenToSeekHelp = "If period pain stops your daily activities, see a doctor. You deserve proper care and diagnosis.",
            disclaimer = GLOBAL_DISCLAIMER
        ),
        HealthAwarenessTopic(
            title = "Heavy Bleeding",
            slug = "heavy-bleeding",
            emoji = "⚠️",
            summary = "Soaking through protection hourly or unusually heavy flow.",
            content = "Heavy menstrual bleeding (menorrhagia) means soaking through a pad or tampon every hour for several consecutive hours. This warrants medical attention.",
            symptoms = listOf("Soaking hourly", "Large clots", "Period lasting 7+ days", "Fatigue and dizziness"),
            whenToSeekHelp = "If soaking through protection hourly for 2+ hours, this requires urgent medical attention.",
            disclaimer = GLOBAL_DISCLAIMER
        ),
        HealthAwarenessTopic(
            title = "Infection Warning Signs",
            slug = "infection-signs",
            emoji = "🚨",
            summary = "Signs that something may be an infection requiring medical care.",
            content = "Know when to seek help: fever, unusual discharge, worsening pain, or TSS signs if using a menstrual cup.",
            symptoms = listOf("Fever above 38°C", "Unusual discharge", "Foul odour", "Pelvic swelling", "Burning urination"),
            whenToSeekHelp = "If you have fever plus pelvic pain plus a menstrual product in use — remove it and seek emergency care immediately (possible TSS).",
            disclaimer = GLOBAL_DISCLAIMER
        )
    )

    // ==========================================
    // SELF-CARE TIPS
    // ==========================================

    val selfCareTips = listOf(
        SelfCareItem(
            title = "4-7-8 Breathing Exercise",
            description = "A breathing technique to soothe anxiety and calm the nervous system.",
            category = "Breathing",
            steps = listOf(
                "Exhale completely through your mouth.",
                "Close your mouth and inhale through your nose for 4 counts.",
                "Hold your breath for 7 counts.",
                "Exhale completely through your mouth for 8 counts.",
                "Repeat 4 times."
            )
        ),
        SelfCareItem(
            title = "5-4-3-2-1 Grounding Method",
            description = "Ground yourself during moments of panic by connecting with your senses.",
            category = "Grounding",
            steps = listOf(
                "Name 5 things you can see.",
                "Name 4 things you can touch or feel.",
                "Name 3 things you can hear.",
                "Name 2 things you can smell.",
                "Name 1 thing you can taste."
            )
        ),
        SelfCareItem(
            title = "Gentle Period Stretch",
            description = "Relieve back tension and pelvic cramps with child's pose.",
            category = "Stretch",
            steps = listOf(
                "Kneel with knees wide and big toes touching.",
                "Sit back on your heels and lower torso forward.",
                "Extend arms forward, rest forehead on floor or mat.",
                "Breathe deeply into your lower back.",
                "Hold for 2–3 minutes, relaxing pelvic muscles."
            )
        ),
        SelfCareItem(
            title = "Warm Comfort Ritual",
            description = "Ease pelvic cramping and body chills during your period.",
            category = "Comfort",
            steps = listOf(
                "Prepare a warm heating pad or hot water bottle wrapped in a cloth.",
                "Place gently on your lower abdomen or lower back.",
                "Lie comfortably, knees tucked towards chest.",
                "Sip chamomile or ginger tea.",
                "Rest for 20–30 minutes."
            )
        ),
        SelfCareItem(
            title = "Hydration Reminder",
            description = "Staying hydrated reduces bloating, water retention, and fatigue.",
            category = "Comfort",
            steps = listOf(
                "Drink a full glass of water now.",
                "Keep a refillable bottle nearby throughout the day.",
                "Try warm herbal teas or water with lemon.",
                "Limit caffeine and high-sodium foods."
            )
        ),
        SelfCareItem(
            title = "Mindful Body Scan",
            description = "A gentle check-in with your body from head to toe.",
            category = "Mindfulness",
            steps = listOf(
                "Lie down or sit comfortably.",
                "Close your eyes and take 3 slow deep breaths.",
                "Bring attention to your head and scalp — notice any tension.",
                "Move slowly down: neck, shoulders, chest, abdomen, legs, feet.",
                "At each area, breathe in and consciously release any tension.",
                "End with 3 slow breaths and open your eyes."
            )
        )
    )

    // ==========================================
    // RELIGIOUS WELLNESS CONTENT
    // ==========================================

    fun getWellnessContentForReligion(religion: String?): List<String> {
        return when (religion) {
            "ISLAM" -> listOf(
                "During menstruation (hayd), many Islamic scholars note that rest and self-care are permitted and encouraged.",
                "Privacy and dignity in managing your period are valued. LunaCare helps you track with discretion.",
                "If fasting during Ramadan intersects with your period, consult a trusted religious scholar for guidance on religious obligations. Medical and religious advice may differ.",
                "Your health and wellbeing matter. Seeking medical help for period concerns is always appropriate.",
                "$RELIGIOUS_CONTENT_NOTE"
            )
            "HINDU" -> listOf(
                "Many Hindu families have traditional beliefs around menstruation. LunaCare respects all practices and focuses on your health and comfort.",
                "Gentle movement, warm food, and rest are traditional self-care practices that align with medical guidance.",
                "You deserve to feel dignified and supported during your period, in ways that feel right for you.",
                "$RELIGIOUS_CONTENT_NOTE"
            )
            "CHRISTIAN" -> listOf(
                "Your body deserves care and respect. LunaCare supports your wellness journey with compassion.",
                "Gentle movement, journaling, and community support can all complement your faith-based wellbeing.",
                "$RELIGIOUS_CONTENT_NOTE"
            )
            "BUDDHIST" -> listOf(
                "Mindfulness practices such as breathing and body scanning can be deeply supportive during your cycle.",
                "The 4-7-8 breathing and 5-4-3-2-1 grounding exercises in LunaCare align with mindfulness principles.",
                "Compassion toward yourself is a central practice. Be kind to your body during difficult days.",
                "$RELIGIOUS_CONTENT_NOTE"
            )
            else -> listOf(
                "LunaCare provides neutral, respectful wellness content that honours your personal values.",
                "Your wellbeing matters regardless of your background or beliefs."
            )
        }
    }

    // ==========================================
    // EMERGENCY RESOURCES
    // ==========================================

    val emergencyResources = listOf(
        Triple("🌐 International", "Emergency", "112 (EU/many countries) or your local number"),
        Triple("🇺🇸 USA", "Crisis Line", "Call/text 988 — Suicide & Crisis Lifeline"),
        Triple("🇺🇸 USA", "Emergency", "911"),
        Triple("🇬🇧 UK", "Crisis", "116 123 — Samaritans"),
        Triple("🇬🇧 UK", "Emergency", "999"),
        Triple("🇦🇺 Australia", "Crisis", "13 11 14 — Lifeline"),
        Triple("🇲🇾 Malaysia", "Crisis", "15999 — Befrienders"),
        Triple("🇮🇳 India", "Crisis", "iCall: 9152987821"),
        Triple("🇵🇰 Pakistan", "Crisis", "Umang: 0317-4288665"),
        Triple("🇧🇩 Bangladesh", "Emergency", "999"),
        Triple("🇸🇬 Singapore", "Crisis", "1800-221-4444 — SOS")
    )
}
