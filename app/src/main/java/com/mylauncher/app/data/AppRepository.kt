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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Reads launchable apps from PackageManager and keeps them fresh when packages are
 * installed, removed or updated. Also owns a small in-memory icon cache.
 */
class AppRepository(context: Context) {
    private val appContext = context.applicationContext
    private val pm: PackageManager = appContext.packageManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val iconCache = LruCache<String, ImageBitmap>(MAX_CACHED_ICONS)
    private var refreshJob: Job? = null

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            scheduleRefresh()
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
        scheduleRefresh(0L)
    }

    /** Debounced refresh: several package broadcasts usually arrive together. */
    fun scheduleRefresh(delayMs: Long = REFRESH_DEBOUNCE_MS) {
        refreshJob?.cancel()
        refreshJob = scope.launch {
            if (delayMs > 0) delay(delayMs)
            val fresh = queryApps()
            iconCache.evictAll()
            _apps.value = fresh
        }
    }

    private fun queryApps(): List<AppInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved: List<ResolveInfo> = try {
            if (Build.VERSION.SDK_INT >= 33) {
                pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(intent, 0)
            }
        } catch (e: Exception) {
            emptyList()
        }
        return resolved
            .mapNotNull { toAppInfo(it) }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    private fun toAppInfo(info: ResolveInfo): AppInfo? {
        return try {
            val activity = info.activityInfo ?: return null
            val label = info.loadLabel(pm)?.toString()?.trim().orEmpty()
            AppInfo(
                packageName = activity.packageName,
                activityName = activity.name,
                label = label.ifEmpty { activity.packageName },
            )
        } catch (e: Exception) {
            null
        }
    }

    /** Instant lookup, used to avoid icon flicker. */
    fun peekIcon(app: AppInfo): ImageBitmap? = iconCache.get(app.packageName)

    /** Loads (and caches) the app icon. Returns null if it cannot be loaded. */
    suspend fun loadIcon(app: AppInfo): ImageBitmap? {
        iconCache.get(app.packageName)?.let { return it }
        return withContext(Dispatchers.Default) {
            try {
                val drawable = pm.getActivityIcon(ComponentName(app.packageName, app.activityName))
                val bitmap = drawable.toBitmap(ICON_PX, ICON_PX).asImageBitmap()
                iconCache.put(app.packageName, bitmap)
                bitmap
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        }
    }

    private companion object {
        const val MAX_CACHED_ICONS = 300
        const val ICON_PX = 160
        const val REFRESH_DEBOUNCE_MS = 300L
    }
}
