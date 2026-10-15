package com.mylauncher.app.icons

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.drawable.Drawable
import androidx.core.content.res.ResourcesCompat
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

/** An installed app that offers icons through one of the common icon pack conventions. */
data class IconPackInfo(val packageName: String, val label: String)

/** What the launcher is really showing, so the UI never claims a pack is applied when it is not. */
data class IconPackState(
    /** The pack the user chose (kept even while it is missing, so it applies again when reinstalled). */
    val desired: String? = null,
    /** True only when the icon pipeline is really using the pack. */
    val applied: Boolean = false,
    val mappedApps: Int = 0,
    val message: String? = null,
    /** The pack app's name, when known. */
    val label: String? = null,
)

class LoadedIconPack(
    val packageName: String,
    val mapping: IconPackMapping,
    private val resources: Resources,
) {
    /** The icon drawable, or null when the resource is missing or cannot be read. */
    fun drawable(name: String): Drawable? {
        if (!AppFilter.isValidDrawableName(name)) return null
        return try {
            val id = resources.getIdentifier(name, "drawable", packageName)
            if (id == 0) null else ResourcesCompat.getDrawable(resources, id, null)
        } catch (e: Exception) {
            null
        }
    }
}

sealed interface PackLoadResult {
    data class Loaded(val pack: LoadedIconPack) : PackLoadResult
    data class Failed(val message: String) : PackLoadResult
}

/**
 * Finds and reads icon packs that follow the widespread "appfilter" convention: the pack app declares one of
 * the well-known theme actions and ships a mapping (assets/appfilter.xml or res/xml/appfilter.xml) from app
 * components to drawable names. Packs using other formats are reported as unsupported, never guessed at.
 */
class IconPackLoader(context: Context) {
    private val appContext = context.applicationContext
    private val pm = appContext.packageManager

    fun discover(): List<IconPackInfo> {
        val found = LinkedHashMap<String, IconPackInfo>()
        for (action in THEME_ACTIONS) {
            val matches = try {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(Intent(action), 0)
            } catch (e: Exception) {
                emptyList()
            }
            for (match in matches) {
                val pkg = match.activityInfo?.packageName ?: continue
                if (pkg == appContext.packageName || pkg in found) continue
                val label = try { match.loadLabel(pm)?.toString() } catch (e: Exception) { null }
                found[pkg] = IconPackInfo(pkg, if (label.isNullOrBlank()) pkg else label)
            }
        }
        return found.values.sortedBy { it.label.lowercase() }
    }

    fun load(packageName: String): PackLoadResult {
        val resources = try {
            pm.getResourcesForApplication(packageName)
        } catch (e: Exception) {
            return PackLoadResult.Failed("This icon pack is not installed")
        }
        val entries = try {
            readAssetMapping(resources) ?: readXmlMapping(resources, packageName)
        } catch (e: Exception) {
            null
        }
        if (entries == null) return PackLoadResult.Failed("This icon pack uses a format My Launcher can't read")
        if (entries.isEmpty()) return PackLoadResult.Failed("This icon pack has no icons for apps")
        return PackLoadResult.Loaded(LoadedIconPack(packageName, IconPackMapping(entries), resources))
    }

    private fun readAssetMapping(resources: Resources): Map<String, String>? {
        val stream = try { resources.assets.open("appfilter.xml") } catch (e: Exception) { return null }
        return stream.use { input ->
            val parser = XmlPullParserFactory.newInstance().newPullParser()
            parser.setInput(input, null)
            parseItems(parser)
        }
    }

    private fun readXmlMapping(resources: Resources, packageName: String): Map<String, String>? {
        val id = resources.getIdentifier("appfilter", "xml", packageName)
        if (id == 0) return null
        val parser = resources.getXml(id)
        return try {
            parseItems(parser)
        } finally {
            parser.close()
        }
    }

    private fun parseItems(parser: XmlPullParser): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT && out.size < MAX_ENTRIES) {
            if (event == XmlPullParser.START_TAG && parser.name == "item") {
                val key = AppFilter.componentKey(parser.getAttributeValue(null, "component"))
                val drawable = parser.getAttributeValue(null, "drawable")
                if (key != null && AppFilter.isValidDrawableName(drawable)) out.putIfAbsent(key, drawable)
            }
            event = parser.next()
        }
        return out
    }

    companion object {
        /** Actions that icon pack apps declare (used by the most common launchers). */
        val THEME_ACTIONS = listOf(
            "org.adw.launcher.THEMES",
            "com.novalauncher.THEME",
            "com.anddoes.launcher.THEME",
            "org.adw.ActivityStarter.THEMES",
        )
        const val MAX_ENTRIES = 20_000
    }
}
