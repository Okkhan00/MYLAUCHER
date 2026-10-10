package com.mylauncher.app.backup

/** What a stored launcher value is. */
enum class BackupType(val tag: String) { STRING("s"), BOOLEAN("b"), INT("i") }

/**
 * The only preference keys a backup may contain. This is an allow-list on purpose:
 *  - the PIN hash/salt and the biometric flag are never exported (secrets stay on the device);
 *  - launch history is private usage data and is not exported;
 *  - widget ids belong to this device's widget host and cannot be restored elsewhere.
 * Unknown keys found in a file are ignored, which keeps older apps able to read newer backups.
 */
object BackupSchema {
    val keys: Map<String, BackupType> = mapOf(
        "theme" to BackupType.STRING, "icon_size" to BackupType.STRING, "columns" to BackupType.INT,
        "show_labels" to BackupType.BOOLEAN, "animations" to BackupType.BOOLEAN,
        "clock_style" to BackupType.STRING, "show_date" to BackupType.BOOLEAN,
        "show_search_bar" to BackupType.BOOLEAN, "drawer_layout" to BackupType.STRING,
        "onboarding_done" to BackupType.BOOLEAN,
        "gesture_swipe_up" to BackupType.STRING, "gesture_swipe_down" to BackupType.STRING,
        "gesture_double_tap" to BackupType.STRING,
        "show_battery" to BackupType.BOOLEAN, "track_launches" to BackupType.BOOLEAN,
        "favorites" to BackupType.STRING, "hidden" to BackupType.STRING, "folders" to BackupType.STRING,
        "protect_settings" to BackupType.BOOLEAN, "protect_hidden" to BackupType.BOOLEAN,
        "smart_suggestions" to BackupType.BOOLEAN, "search_web_enabled" to BackupType.BOOLEAN,
        "search_engine" to BackupType.STRING, "search_custom_url" to BackupType.STRING,
        "search_settings_shortcuts" to BackupType.BOOLEAN, "drawer_group_by_category" to BackupType.BOOLEAN,
        "category_overrides" to BackupType.STRING, "category_config" to BackupType.STRING,
        "theme_dynamic_color" to BackupType.BOOLEAN, "theme_accent_color" to BackupType.INT,
        "theme_surface_style" to BackupType.STRING, "theme_text_scale" to BackupType.STRING,
        "theme_search_bar_style" to BackupType.STRING, "theme_corner_radius" to BackupType.INT,
        "theme_drawer_opacity" to BackupType.INT, "theme_custom" to BackupType.STRING,
        "wallpaper_overlay" to BackupType.STRING, "wallpaper_overlay_percent" to BackupType.INT,
        "wallpaper_blur" to BackupType.STRING, "wallpaper_drawer_overlay" to BackupType.BOOLEAN,
        "locked_apps" to BackupType.STRING,
        "performance_mode" to BackupType.STRING,
    )

    /** Keys that must never appear in a backup, even if someone edits a file by hand. */
    val neverExported: Set<String> = setOf(
        "pin_hash", "pin_salt", "biometric_enabled", "launch_stats", "launch_log",
        // App usage timers: configuration, usage state and the timer PIN stay on this device.
        "timers_enabled", "app_timers", "timer_pin_hash", "timer_pin_salt", "timer_biometric", "timer_pin_guard",
    )
}

sealed interface BackupResult {
    data class Ok(val version: Int, val createdAtMs: Long, val appVersion: String, val values: Map<String, Any>) : BackupResult
    /** [reason] is for developers/tests; users only ever see [USER_MESSAGE]. */
    data class Invalid(val reason: String) : BackupResult
}

object BackupCodec {
    const val CURRENT_VERSION = 1
    const val USER_MESSAGE = "Invalid or corrupted backup file."
    const val MAX_STRING = 100_000
    const val MAX_JSON_CHARS = 1_000_000

    /** Builds the backup JSON. Entries outside [BackupSchema] are dropped, never written. */
    fun encode(values: Map<String, Any>, createdAtMs: Long, appVersion: String): String {
        val prefs = LinkedHashMap<String, Any?>()
        for ((key, value) in values.toSortedMap()) {
            val type = BackupSchema.keys[key] ?: continue
            if (!matches(type, value)) continue
            prefs[key] = mapOf("t" to type.tag, "v" to when (type) {
                BackupType.INT -> (value as Number).toLong()
                else -> value
            })
        }
        return MiniJson.write(
            linkedMapOf(
                "backupVersion" to CURRENT_VERSION,
                "createdAt" to createdAtMs,
                "appVersion" to appVersion,
                "prefs" to prefs,
            ),
        )
    }

    /** Never throws: anything unexpected becomes [BackupResult.Invalid]. */
    fun decode(json: String): BackupResult {
        if (json.length > MAX_JSON_CHARS) return BackupResult.Invalid("too large")
        val root = try {
            MiniJson.parse(json)
        } catch (e: Exception) {
            return BackupResult.Invalid("not valid JSON")
        }
        val obj = root as? Map<*, *> ?: return BackupResult.Invalid("not an object")
        val version = (obj["backupVersion"] as? Double)?.takeIf { it == Math.floor(it) }?.toInt()
            ?: return BackupResult.Invalid("missing backupVersion")
        if (version < 1) return BackupResult.Invalid("bad backupVersion")
        if (version > CURRENT_VERSION) return BackupResult.Invalid("made by a newer version (v$version)")
        val prefs = obj["prefs"] as? Map<*, *> ?: return BackupResult.Invalid("missing prefs")

        val out = LinkedHashMap<String, Any>()
        for ((rawKey, rawEntry) in prefs) {
            val key = rawKey as? String ?: return BackupResult.Invalid("bad key")
            if (key in BackupSchema.neverExported) continue
            val type = BackupSchema.keys[key] ?: continue // unknown key: ignore for forward compatibility
            val entry = rawEntry as? Map<*, *> ?: return BackupResult.Invalid("bad entry for $key")
            if (entry["t"] != type.tag) return BackupResult.Invalid("wrong type for $key")
            val value: Any = when (type) {
                BackupType.STRING -> (entry["v"] as? String)?.takeIf { it.length <= MAX_STRING }
                BackupType.BOOLEAN -> entry["v"] as? Boolean
                BackupType.INT -> (entry["v"] as? Double)
                    ?.takeIf { it == Math.floor(it) && it >= Int.MIN_VALUE && it <= Int.MAX_VALUE }?.toInt()
            } ?: return BackupResult.Invalid("bad value for $key")
            out[key] = value
        }
        return BackupResult.Ok(
            version = version,
            createdAtMs = (obj["createdAt"] as? Double)?.toLong() ?: 0L,
            appVersion = obj["appVersion"] as? String ?: "",
            values = out,
        )
    }

    private fun matches(type: BackupType, value: Any): Boolean = when (type) {
        BackupType.STRING -> value is String && value.length <= MAX_STRING
        BackupType.BOOLEAN -> value is Boolean
        BackupType.INT -> value is Int || value is Long
    }
}
