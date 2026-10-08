package com.mylauncher.app.categories

import com.mylauncher.app.data.model.AppInfo

enum class AppCategory(val id: String, val defaultName: String) {
    SOCIAL("social", "Social"),
    COMMUNICATION("communication", "Communication"),
    MEDIA("media", "Media"),
    GAMES("games", "Games"),
    WORK("work", "Work"),
    SHOPPING("shopping", "Shopping"),
    TRAVEL("travel", "Travel"),
    FINANCE("finance", "Finance"),
    TOOLS("tools", "Tools"),
    EDUCATION("education", "Education"),
    PHOTOGRAPHY("photography", "Photography"),
    OTHER("other", "Other");

    companion object {
        fun fromId(id: String?): AppCategory? = entries.firstOrNull { it.id == id }
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
