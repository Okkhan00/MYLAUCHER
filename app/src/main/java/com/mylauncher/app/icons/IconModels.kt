package com.mylauncher.app.icons

import com.mylauncher.app.backup.MiniJson
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Where an app's icon comes from. Highest first: the user's own icon, the chosen icon pack, the app itself. */
enum class IconSourceKind { CUSTOM, PACK, ORIGINAL }

object IconPrecedence {
    fun resolve(hasCustom: Boolean, hasPackIcon: Boolean): IconSourceKind = when {
        hasCustom -> IconSourceKind.CUSTOM
        hasPackIcon -> IconSourceKind.PACK
        else -> IconSourceKind.ORIGINAL
    }
}

enum class CustomShape { CIRCLE, ROUNDED, SQUARE }

/**
 * Styling of one custom icon. The picture itself lives in a file; this is only how it is drawn.
 * [background] is ARGB, 0 meaning transparent. [version] changes whenever the picture is replaced.
 */
data class CustomIconDef(
    val shape: CustomShape = CustomShape.ROUNDED,
    val background: Int = 0,
    val paddingPercent: Int = 0,
    val version: Int = 1,
) {
    companion object {
        const val MAX_PADDING = 30
    }
}

/** One app per line: package TAB shape TAB background TAB padding TAB version. Bad lines are skipped. */
object CustomIconCodec {
    fun encode(defs: Map<String, CustomIconDef>): String =
        defs.entries.joinToString("\n") { (pkg, d) ->
            "$pkg\t${d.shape.name}\t${d.background}\t${d.paddingPercent}\t${d.version}"
        }

    fun decode(raw: String?): Map<String, CustomIconDef> {
        if (raw.isNullOrEmpty()) return emptyMap()
        val out = LinkedHashMap<String, CustomIconDef>()
        for (line in raw.lineSequence()) {
            val p = line.split("\t")
            if (p.size != 5 || p[0].isBlank()) continue
            val shape = CustomShape.entries.firstOrNull { it.name == p[1] } ?: continue
            val bg = p[2].toIntOrNull() ?: continue
            val pad = p[3].toIntOrNull() ?: continue
            val ver = p[4].toIntOrNull() ?: continue
            out[p[0]] = CustomIconDef(shape, bg, pad.coerceIn(0, CustomIconDef.MAX_PADDING), max(1, ver))
        }
        return out
    }
}

/** Reads the component names used by the widespread icon pack mapping file (appfilter.xml). */
object AppFilter {
    /**
     * "ComponentInfo{com.app/com.app.Main}" becomes "com.app/com.app.Main". A class that starts with a dot is
     * made absolute. Anything that is not a plain component (placeholders, empty values) returns null.
     */
    fun componentKey(raw: String?): String? {
        if (raw == null) return null
        val text = raw.trim()
        if (!text.startsWith("ComponentInfo{") || !text.endsWith("}")) return null
        val inner = text.removePrefix("ComponentInfo{").removeSuffix("}")
        val slash = inner.indexOf('/')
        if (slash <= 0 || slash == inner.length - 1) return null
        val pkg = inner.substring(0, slash)
        var cls = inner.substring(slash + 1)
        if (pkg.any { it.isWhitespace() } || cls.any { it.isWhitespace() }) return null
        if (cls.startsWith(".")) cls = pkg + cls
        return "$pkg/$cls"
    }

    fun packageOf(key: String): String = key.substringBefore('/')

    /** Drawable names in packs are plain resource names. Anything else is rejected. */
    fun isValidDrawableName(name: String?): Boolean =
        !name.isNullOrEmpty() && name.length <= 100 && name.all { it.isLetterOrDigit() || it == '_' }
}

/** A mapping loaded from an icon pack, with a package-only fallback for apps whose launch activity differs. */
class IconPackMapping(entries: Map<String, String>) {
    private val byComponent: Map<String, String> = entries
    private val byPackage: Map<String, String> = LinkedHashMap<String, String>().also { map ->
        for ((key, drawable) in entries) map.putIfAbsent(AppFilter.packageOf(key), drawable)
    }

    val size: Int get() = byComponent.size

    fun drawableFor(packageName: String, activityName: String): String? =
        byComponent["$packageName/$activityName"] ?: byPackage[packageName]
}

/** Pure geometry for the Custom Icon Studio. */
object IconStudioMath {
    const val MAX_FILE_BYTES = 12L * 1024 * 1024
    const val MAX_SOURCE_SIDE = 2048
    const val MIN_SIDE = 48
    const val OUTPUT_SIDE = 512
    const val MIN_ZOOM = 1f
    const val MAX_ZOOM = 4f

    /** Largest power-of-two sample size that keeps the longest side at or under [maxSide]. */
    fun sampleSize(width: Int, height: Int, maxSide: Int = MAX_SOURCE_SIDE): Int {
        var sample = 1
        var longest = max(width, height)
        while (longest / 2 >= maxSide) {
            longest /= 2
            sample *= 2
        }
        return sample
    }

    /** null when the picture can be used, otherwise a short message for the user. */
    fun validate(fileBytes: Long, width: Int, height: Int): String? = when {
        width <= 0 || height <= 0 -> "This file is not a usable picture"
        fileBytes > MAX_FILE_BYTES -> "This picture is too large (over 12 MB)"
        min(width, height) < MIN_SIDE -> "This picture is too small (at least $MIN_SIDE px)"
        else -> null
    }

    /**
     * The square part of a [width] x [height] picture that is kept. [zoom] 1 keeps the largest square,
     * higher values zoom in. [panX] and [panY] go from -1 to 1 and move the square inside the picture.
     * Returns left, top and side length, always inside the picture.
     */
    fun cropRect(width: Int, height: Int, zoom: Float, panX: Float, panY: Float): IntArray {
        val z = zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
        val side = max(1, (min(width, height) / z).roundToInt())
        val slackX = (width - side).coerceAtLeast(0)
        val slackY = (height - side).coerceAtLeast(0)
        val left = ((slackX / 2f) + panX.coerceIn(-1f, 1f) * slackX / 2f).roundToInt().coerceIn(0, slackX)
        val top = ((slackY / 2f) + panY.coerceIn(-1f, 1f) * slackY / 2f).roundToInt().coerceIn(0, slackY)
        return intArrayOf(left, top, side)
    }

    /** Space left empty around the picture inside the icon, in pixels. */
    fun paddingPx(sizePx: Int, percent: Int): Int =
        (sizePx * percent.coerceIn(0, CustomIconDef.MAX_PADDING) / 100f).roundToInt()
}

/** The internal My Launcher custom icon export. It is NOT an installable Android icon pack. */
object IconExport {
    const val FORMAT = "mylauncher-custom-icons"
    const val VERSION = 1

    /** A file name made only of letters, digits, dots, dashes and underscores, so it is safe inside an archive. */
    fun safeFileName(packageName: String): String {
        val cleaned = packageName.map { if (it.isLetterOrDigit() || it == '.' || it == '_' || it == '-') it else '_' }
            .joinToString("").trim('.')
        return (if (cleaned.isEmpty()) "icon" else cleaned.take(120)) + ".png"
    }

    fun manifest(defs: Map<String, CustomIconDef>, exportedAtMillis: Long): String {
        val icons = defs.entries.sortedBy { it.key }.map { (pkg, d) ->
            mapOf(
                "package" to pkg,
                "file" to "icons/" + safeFileName(pkg),
                "shape" to d.shape.name,
                "background" to d.background.toDouble(),
                "paddingPercent" to d.paddingPercent.toDouble(),
            )
        }
        return MiniJson.write(
            mapOf(
                "format" to FORMAT,
                "version" to VERSION.toDouble(),
                "exportedAtMillis" to exportedAtMillis.toDouble(),
                "note" to "My Launcher custom icon backup. This is not an installable Android icon pack.",
                "icons" to icons,
            ),
        )
    }
}
