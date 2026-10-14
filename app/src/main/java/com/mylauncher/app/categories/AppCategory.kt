package com.mylauncher.app.categories

import com.mylauncher.app.data.model.AppInfo

enum class AppCategory(val id: String, val defaultName: String) {
    SOCIAL("social", "Social"),
    COMMUNICATION("communication", "Communication"),
    MEDIA("media", "Entertainment"),
    GAMES("games", "Games"),
    WORK("work", "Productivity"),
    SHOPPING("shopping", "Shopping"),
    TRAVEL("travel", "Travel"),
    FINANCE("finance", "Finance"),
    TOOLS("tools", "Tools"),
    EDUCATION("education", "Education"),
    PHOTOGRAPHY("photography", "Photography"),
    OTHER("other", "Other Apps"),

    // Slots for categories the user creates (V4.5). A slot is unused until it gets a name, so the 12 built-in
    // categories, their ids and all saved data are untouched. Unused slots never appear anywhere.
    CUSTOM_1("custom_1", "My category 1"),
    CUSTOM_2("custom_2", "My category 2"),
    CUSTOM_3("custom_3", "My category 3"),
    CUSTOM_4("custom_4", "My category 4"),
    CUSTOM_5("custom_5", "My category 5"),
    CUSTOM_6("custom_6", "My category 6"),
    CUSTOM_7("custom_7", "My category 7"),
    CUSTOM_8("custom_8", "My category 8"),
    CUSTOM_9("custom_9", "My category 9"),
    CUSTOM_10("custom_10", "My category 10"),
    CUSTOM_11("custom_11", "My category 11"),
    CUSTOM_12("custom_12", "My category 12");

    /** True for the slots used by user-created categories. */
    val isCustom: Boolean get() = id.startsWith("custom_")

    companion object {
        fun fromId(id: String?): AppCategory? = entries.firstOrNull { it.id == id }
    }
}

/**
 * One emoji per category, shown in the category cards and settings. Emoji need no icon library and
 * are drawn by the system font; they are purely decorative (screen readers get the category name).
 */
object CategoryVisuals {
    fun emoji(category: AppCategory): String = when (category) {
        AppCategory.COMMUNICATION -> "\uD83D\uDCAC"      // speech bubble
        AppCategory.SOCIAL -> "\uD83D\uDC65"             // people
        AppCategory.MEDIA -> "\uD83C\uDFAC"              // clapper board
        AppCategory.GAMES -> "\uD83C\uDFAE"              // game controller
        AppCategory.WORK -> "\uD83D\uDCBC"               // briefcase
        AppCategory.SHOPPING -> "\uD83D\uDED2"           // shopping cart
        AppCategory.TRAVEL -> "\u2708\uFE0F"             // airplane
        AppCategory.FINANCE -> "\uD83D\uDCB3"            // credit card
        AppCategory.TOOLS -> "\uD83D\uDEE0\uFE0F"       // hammer and wrench
        AppCategory.EDUCATION -> "\uD83C\uDF93"          // graduation cap
        AppCategory.PHOTOGRAPHY -> "\uD83D\uDCF7"        // camera
        AppCategory.OTHER -> "\uD83D\uDCE6"              // package
        AppCategory.CUSTOM_1 -> "\u2B50"                // star
        AppCategory.CUSTOM_2 -> "\u2764\uFE0F"        // heart
        AppCategory.CUSTOM_3 -> "\uD83D\uDD25"         // fire
        AppCategory.CUSTOM_4 -> "\uD83C\uDF1F"         // glowing star
        AppCategory.CUSTOM_5 -> "\uD83C\uDFAF"         // bullseye
        AppCategory.CUSTOM_6 -> "\uD83C\uDF08"         // rainbow
        AppCategory.CUSTOM_7 -> "\uD83D\uDCA1"         // light bulb
        AppCategory.CUSTOM_8 -> "\uD83C\uDFB5"         // musical note
        AppCategory.CUSTOM_9 -> "\uD83D\uDCDA"         // books
        AppCategory.CUSTOM_10 -> "\uD83C\uDF55"        // pizza
        AppCategory.CUSTOM_11 -> "\uD83D\uDE80"        // rocket
        AppCategory.CUSTOM_12 -> "\uD83C\uDF40"        // four-leaf clover
    }

    /** Icons offered when creating a category. */
    val choices: List<String> = listOf(
        "\u2B50", "\u2764\uFE0F", "\uD83D\uDD25", "\uD83C\uDF1F", "\uD83C\uDFAF", "\uD83C\uDF08",
        "\uD83D\uDCA1", "\uD83C\uDFB5", "\uD83D\uDCDA", "\uD83C\uDF55", "\uD83D\uDE80", "\uD83C\uDF40",
    )

    /** The icon shown for an entry: the one the user picked, otherwise the category's own. */
    fun emoji(entry: CategoryEntry): String = entry.emoji?.takeIf { it in choices } ?: emoji(entry.category)
}

/** How many preview icons fit on a category card. Pure math so it is unit tested. */
object CategoryPreview {
    const val MAX = 4

    fun count(availableDp: Float, appCount: Int, iconDp: Float = 28f, gapDp: Float = 4f): Int {
        if (appCount <= 0) return 0
        val fit = ((availableDp + gapDp) / (iconDp + gapDp)).toInt().coerceIn(1, MAX)
        return minOf(fit, appCount)
    }
}

/**
 * Offline, heuristic categorizer. It never touches the network: it looks at the package name, the
 * label and the category the app declared to Android (ApplicationInfo.category).
 */
object AppCategorizer {
    // Values of android.content.pm.ApplicationInfo.CATEGORY_* (kept literal so this stays pure Kotlin).
    private const val SYS_GAME = 0
    private const val SYS_AUDIO = 1
    private const val SYS_VIDEO = 2
    private const val SYS_IMAGE = 3
    private const val SYS_SOCIAL = 4
    private const val SYS_MAPS = 6
    private const val SYS_PRODUCTIVITY = 7

    /** Checked in this order; the first category with a keyword hit wins. */
    private val keywords: List<Pair<AppCategory, List<String>>> = listOf(
        AppCategory.COMMUNICATION to listOf(
            "whatsapp", "telegram", "signal", "messenger", "orca", "messaging", "messages", "mms", "sms",
            "dialer", "phone", "contacts", "gm", "gmail", "mail", "email", "outlook", "skype", "viber",
            "line", "wechat", "imo", "meet", "duo", "chat",
        ),
        AppCategory.SOCIAL to listOf(
            "facebook", "katana", "instagram", "twitter", "snapchat", "tiktok", "musically", "reddit",
            "linkedin", "pinterest", "discord", "tumblr", "mastodon", "threads", "likee", "social",
        ),
        AppCategory.GAMES to listOf("game", "games", "supercell", "roblox", "minecraft", "pubg", "playgames"),
        AppCategory.MEDIA to listOf(
            "youtube", "spotify", "netflix", "music", "video", "videos", "player", "vlc", "soundcloud",
            "podcast", "podcasts", "twitch", "primevideo", "hotstar", "radio", "audio", "tv",
        ),
        AppCategory.PHOTOGRAPHY to listOf(
            "camera", "gallery", "photos", "photo", "snapseed", "lightroom", "vsco", "picsart", "photoshop",
        ),
        AppCategory.TRAVEL to listOf(
            "maps", "uber", "careem", "airbnb", "booking", "expedia", "trip", "tripadvisor", "flight",
            "flights", "travel", "transit", "waze", "indrive", "navigation", "navigate",
        ),
        AppCategory.FINANCE to listOf(
            "bank", "banking", "pay", "wallet", "paypal", "easypaisa", "jazzcash", "finance", "crypto",
            "binance", "coinbase", "revolut", "wise", "money", "sadapay", "nayapay",
        ),
        AppCategory.SHOPPING to listOf(
            "amazon", "ebay", "aliexpress", "daraz", "flipkart", "shop", "shopping", "temu", "shein",
            "walmart", "olx", "foodpanda",
        ),
        AppCategory.EDUCATION to listOf(
            "duolingo", "khanacademy", "coursera", "udemy", "classroom", "education", "learn", "learning",
            "dictionary", "quran", "study", "school",
        ),
        AppCategory.WORK to listOf(
            "docs", "sheets", "slides", "drive", "slack", "teams", "zoom", "notion", "trello", "jira",
            "calendar", "office", "word", "excel", "powerpoint", "onenote", "keep", "notes", "work",
            "tasks", "todo",
        ),
        AppCategory.TOOLS to listOf(
            "calculator", "clock", "deskclock", "settings", "files", "filemanager", "documentsui",
            "flashlight", "torch", "vpn", "chrome", "browser", "firefox", "brave", "vending", "weather",
            "compass", "recorder", "scanner", "translate", "launcher", "setup", "installer",
        ),
    )

    fun categorize(app: AppInfo): AppCategory = categorize(app.packageName, app.label, app.systemCategory)

    fun categorize(packageName: String, label: String, systemCategory: Int = -1): AppCategory {
        if (systemCategory == SYS_GAME) return AppCategory.GAMES
        val tokens = tokenize(packageName) + tokenize(label)
        for ((category, words) in keywords) {
            if (words.any { w -> tokens.any { t -> matches(t, w) } }) return category
        }
        return when (systemCategory) {
            SYS_AUDIO, SYS_VIDEO -> AppCategory.MEDIA
            SYS_IMAGE -> AppCategory.PHOTOGRAPHY
            SYS_SOCIAL -> AppCategory.SOCIAL
            SYS_MAPS -> AppCategory.TRAVEL
            SYS_PRODUCTIVITY -> AppCategory.WORK
            else -> AppCategory.OTHER
        }
    }

    private fun tokenize(text: String): List<String> =
        text.lowercase().split('.', '_', '-', ' ', '/', '&', ':').filter { it.isNotEmpty() }

    /** Short keywords must match a whole token; longer ones may match inside a token ("camera2"). */
    private fun matches(token: String, keyword: String): Boolean =
        token == keyword || (keyword.length >= 5 && token.contains(keyword))
}
