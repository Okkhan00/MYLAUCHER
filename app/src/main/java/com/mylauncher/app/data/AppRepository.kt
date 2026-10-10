package com.mylauncher.app.data

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.mylauncher.app.data.model.AppInfo
import com.mylauncher.app.data.model.IconSize
import com.mylauncher.app.performance.PerformanceProfile
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Reads launchable apps from PackageManager and keeps them fresh, and owns the icon cache.
 *
 * Version 4.1 behaviour:
 *  - Startup: the last known app list is read from a small private file and shown immediately; the real
 *    PackageManager query then replaces it. Nothing here runs on the main thread.
 *  - Package changes (install, remove, update, enable/disable) re-query only the package that changed.
 *    If the list did not actually change (for example a non-launchable package), nothing is emitted, so
 *    the UI does no work. Large bursts fall back to one full query.
 *  - Icons are cached per package in a size-bounded LRU cache, loaded off the main thread, never loaded
 *    twice at the same time, pre-loaded once in the background so scrolling only reads memory, and
 *    dropped when the app changes or Android reports memory pressure.
 */
class AppRepository(context: Context) {
    private val appContext = context.applicationContext
    private val pm: PackageManager = appContext.packageManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // ---- icons -----------------------------------------------------------------------------

    /** Big enough for the largest icon size setting on this screen, small enough to bound memory. */
    private val iconPx: Int =
        (IconSize.entries.maxOf { it.sizeDp } * appContext.resources.displayMetrics.density)
            .roundToInt().coerceIn(MIN_ICON_PX, MAX_ICON_PX)
    private val bytesPerIcon = iconPx * iconPx * 4
    @Volatile private var maxIcons = DEFAULT_CACHED_ICONS

    /** Sized in bytes (not item count) so memory stays bounded whatever the screen density is. */
    private val iconCache = object : LruCache<String, ImageBitmap>(DEFAULT_CACHED_ICONS * bytesPerIcon) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
    }
    private val inFlight = ConcurrentHashMap<String, Deferred<ImageBitmap?>>()
    private var prefetchJob: Job? = null

    // ---- app list --------------------------------------------------------------------------

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    /** True once PackageManager has answered at least once (the snapshot alone does not count). */
    @Volatile private var freshLoaded = false
    @Volatile private var debounceMs = REFRESH_DEBOUNCE_MS
    @Volatile private var fullRequested = false
    private val pending = ConcurrentHashMap.newKeySet<String>()
    private var refreshJob: Job? = null
    private val snapshotFile = File(appContext.cacheDir, SNAPSHOT_FILE)
    private val snapshotLock = Any()

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val pkg = intent?.data?.schemeSpecificPart
            if (pkg.isNullOrEmpty()) scheduleRefresh() else packageMayHaveChanged(pkg)
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        try {
            ContextCompat.registerReceiver(appContext, packageReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        } catch (e: Exception) {
            // The list still loads once; it just won't live-refresh.
        }
        loadSnapshot()
        scheduleRefresh(0L)
    }

    /** Applies a performance mode: icon cache size and how long package broadcasts are debounced. */
    fun applyProfile(profile: PerformanceProfile) {
        debounceMs = profile.refreshDebounceMs
        maxIcons = profile.iconCacheSize
        iconCache.resize(profile.iconCacheSize * bytesPerIcon)
    }

    /** Re-reads the whole app list (debounced). Used at startup and for very large bursts of changes. */
    fun scheduleRefresh(delayMs: Long = debounceMs) {
        fullRequested = true
        launchRefresh(delayMs)
    }

    /** Re-reads just [packageName] (debounced). Several broadcasts for one install arrive together. */
    fun packageMayHaveChanged(packageName: String, immediate: Boolean = false) {
        pending.add(packageName)
        launchRefresh(if (immediate) 0L else debounceMs)
    }

    private fun launchRefresh(delayMs: Long) {
        refreshJob?.cancel()
        refreshJob = scope.launch {
            if (delayMs > 0) delay(delayMs)
            val wantFull = fullRequested || !freshLoaded
            val batch = pending.toList()
            fullRequested = false
            var published = false
            try {
                val next: List<AppInfo> =
                    if (wantFull || batch.size > MAX_INCREMENTAL_PACKAGES) {
                        AppListOps.normalize(queryAll())
                    } else {
                        var list = _apps.value
                        for (pkg in batch) list = AppListOps.replacePackage(list, pkg, queryPackage(pkg))
                        list
                    }
                ensureActive()
                publish(next, batch, fullQuery = wantFull || batch.size > MAX_INCREMENTAL_PACKAGES)
                pending.removeAll(batch.toSet())
                published = true
            } finally {
                // Cancelled by a newer refresh before it finished: make sure the newer one redoes the work.
                if (!published && wantFull) fullRequested = true
            }
        }
    }

    private fun publish(next: List<AppInfo>, changedPackages: List<String>, fullQuery: Boolean) {
        // Icons of changed packages may have changed (app update); everything else stays cached.
        changedPackages.forEach { iconCache.remove(it) }
        if (fullQuery) {
            val present = next.mapTo(HashSet()) { it.packageName }
            iconCache.snapshot().keys.filterNot { it in present }.forEach { iconCache.remove(it) }
        }
        if (fullQuery) freshLoaded = true // before the value is set, so a late snapshot cannot overwrite it
        val changed = _apps.value != next
        _apps.value = next
        if (changed || fullQuery) {
            if (changed) saveSnapshot(next)
            prefetchIcons(next)
        }
    }

    private fun queryAll(): List<AppInfo> = resolve(null).mapNotNull { toAppInfo(it) }

    private fun queryPackage(packageName: String): List<AppInfo> =
        resolve(packageName).mapNotNull { toAppInfo(it) }

    private fun resolve(packageName: String?): List<ResolveInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        if (packageName != null) intent.setPackage(packageName)
        return try {
            if (Build.VERSION.SDK_INT >= 33) {
                pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(intent, 0)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun toAppInfo(info: ResolveInfo): AppInfo? {
        return try {
            val activity = info.activityInfo ?: return null
            val label = info.loadLabel(pm)?.toString()?.trim().orEmpty()
            AppInfo(
                packageName = activity.packageName,
                activityName = activity.name,
                label = label.ifEmpty { activity.packageName },
                systemCategory = activity.applicationInfo?.category ?: -1,
            )
        } catch (e: Exception) {
            null
        }
    }

    // ---- startup snapshot -------------------------------------------------------------------

    private fun loadSnapshot() {
        scope.launch(Dispatchers.IO) {
            val cached = try {
                synchronized(snapshotLock) { if (snapshotFile.exists()) snapshotFile.readText() else null }
            } catch (e: Exception) {
                null
            }.let { AppListCodec.decode(it) }
            if (cached.isEmpty()) return@launch
            var used = false
            _apps.update { current -> if (freshLoaded || current.isNotEmpty()) current else cached.also { used = true } }
            if (used) prefetchIcons(cached)
        }
    }

    private fun saveSnapshot(list: List<AppInfo>) {
        scope.launch(Dispatchers.IO) {
            try {
                synchronized(snapshotLock) {
                    val tmp = File(snapshotFile.parentFile, "$SNAPSHOT_FILE.tmp")
                    tmp.writeText(AppListCodec.encode(list))
                    if (!tmp.renameTo(snapshotFile)) {
                        snapshotFile.delete()
                        tmp.renameTo(snapshotFile)
                    }
                }
            } catch (e: Exception) {
                // Only a startup speed-up; the next run just queries PackageManager as before.
            }
        }
    }

    // ---- icons: lookup, loading, prefetch, trimming ----------------------------------------

    /** Instant lookup, used to avoid icon flicker. */
    fun peekIcon(app: AppInfo): ImageBitmap? = iconCache.get(app.packageName)

    /** Loads (and caches) the app icon off the main thread. Returns null if it cannot be loaded. */
    suspend fun loadIcon(app: AppInfo): ImageBitmap? {
        iconCache.get(app.packageName)?.let { return it }
        return inFlightLoad(app).await()
    }

    /** One decode per package at a time, shared by the screen and the background prefetch. */
    private fun inFlightLoad(app: AppInfo): Deferred<ImageBitmap?> {
        val key = app.packageName
        inFlight[key]?.let { return it }
        // Runs in the repository scope, so a tile scrolling off screen does not cancel a load others wait for.
        val job = scope.async(start = CoroutineStart.LAZY) {
            val bitmap = decodeIcon(app)
            if (bitmap != null) iconCache.put(key, bitmap)
            bitmap
        }
        val existing = inFlight.putIfAbsent(key, job)
        if (existing != null) {
            job.cancel()
            return existing
        }
        job.invokeOnCompletion { inFlight.remove(key, job) }
        job.start()
        return job
    }

    private fun decodeIcon(app: AppInfo): ImageBitmap? = try {
        // Adaptive icons are drawn into a bitmap of the size actually needed on this screen.
        pm.getActivityIcon(ComponentName(app.packageName, app.activityName)).toBitmap(iconPx, iconPx).asImageBitmap()
    } catch (e: OutOfMemoryError) {
        iconCache.evictAll()
        null
    } catch (e: Exception) {
        null // uninstalled or broken icon: the UI shows its letter placeholder
    }

    /** Warms the cache once, one icon at a time, so scrolling the drawer reads memory instead of PackageManager. */
    private fun prefetchIcons(list: List<AppInfo>) {
        prefetchJob?.cancel()
        prefetchJob = scope.launch {
            delay(PREFETCH_DELAY_MS) // let the first frames and visible icons go first
            for (app in list.take(maxIcons)) {
                ensureActive()
                if (iconCache.get(app.packageName) == null) loadIcon(app)
            }
        }
    }

    /** Called with Android's onTrimMemory level. Hiding the launcher's UI is normal and does not trim anything. */
    fun trimMemory(level: Int) {
        when {
            level == TRIM_UI_HIDDEN -> Unit
            level >= TRIM_BACKGROUND -> {
                prefetchJob?.cancel()
                iconCache.evictAll()
            }
            level >= TRIM_RUNNING_LOW -> iconCache.trimToSize(iconCache.size() / 2)
        }
    }

    private companion object {
        const val DEFAULT_CACHED_ICONS = 300
        const val MIN_ICON_PX = 96
        const val MAX_ICON_PX = 192
        const val REFRESH_DEBOUNCE_MS = 300L
        const val PREFETCH_DELAY_MS = 200L
        const val MAX_INCREMENTAL_PACKAGES = 12
        const val SNAPSHOT_FILE = "app-list-v1.txt"

        // ComponentCallbacks2 levels (the named constants are deprecated on recent Android versions).
        const val TRIM_RUNNING_LOW = 10
        const val TRIM_UI_HIDDEN = 20
        const val TRIM_BACKGROUND = 40
    }
}
