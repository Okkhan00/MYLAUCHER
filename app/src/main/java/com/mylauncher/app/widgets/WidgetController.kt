package com.mylauncher.app.widgets

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import com.mylauncher.app.data.preferences.LauncherPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first

/** Outcome of a widget configuration screen, forwarded from MainActivity.onActivityResult. */
data class ConfigureResult(val ok: Boolean, val appWidgetId: Int)

/**
 * Isolated widget hosting built on the official AppWidgetHost APIs. It owns the single host for
 * the process, persists placed widgets through [LauncherPreferences], and cleans up ids that were
 * allocated but never confirmed (for example after process death during configuration) or whose
 * provider was uninstalled, so host ids are not leaked.
 */
class WidgetController(context: Context, private val prefs: LauncherPreferences) {
    private val appContext = context.applicationContext
    val manager: AppWidgetManager = AppWidgetManager.getInstance(appContext)
    private val host = AppWidgetHost(appContext, HOST_ID)
    private var listening = false

    val widgets: Flow<List<HostedWidget>> = prefs.widgets

    private val _configureResults = MutableSharedFlow<ConfigureResult>(extraBufferCapacity = 1)
    val configureResults: SharedFlow<ConfigureResult> = _configureResults

    /** Called while the launcher is on screen so widgets update; stopped when it is hidden. */
    fun startListening() {
        if (listening) return
        try {
            host.startListening()
            listening = true
        } catch (e: Exception) {
            // Some devices throw if the widget service is unavailable; widgets just won't live-update.
        }
    }

    fun stopListening() {
        if (!listening) return
        try {
            host.stopListening()
        } catch (e: Exception) {
            // Nothing to release.
        }
        listening = false
    }

    /** Widgets that can be added, sorted by label. */
    fun providers(): List<AppWidgetProviderInfo> = try {
        manager.installedProviders.sortedBy { it.loadLabel(appContext.packageManager)?.lowercase().orEmpty() }
    } catch (e: Exception) {
        emptyList()
    }

    fun label(info: AppWidgetProviderInfo): String =
        try { info.loadLabel(appContext.packageManager) ?: "Widget" } catch (e: Exception) { "Widget" }

    fun infoFor(id: Int): AppWidgetProviderInfo? = try { manager.getAppWidgetInfo(id) } catch (e: Exception) { null }

    // ---- Adding a widget: allocate -> bind -> (configure) -> commit; any failure abandons the id ----

    /** Allocates an id and remembers it as pending so it can be cleaned up if the app dies mid-way. */
    suspend fun allocate(): Int {
        val id = host.allocateAppWidgetId()
        prefs.setPendingWidget(id)
        return id
    }

    /** True when the widget was bound without asking the user. */
    fun tryBind(id: Int, info: AppWidgetProviderInfo): Boolean = try {
        manager.bindAppWidgetIdIfAllowed(id, info.provider)
    } catch (e: Exception) {
        false
    }

    fun bindPermissionIntent(id: Int, info: AppWidgetProviderInfo): Intent =
        Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)

    fun needsConfigure(info: AppWidgetProviderInfo): Boolean = info.configure != null

    /** Starts the provider's configuration screen; the result arrives via [onConfigureResult]. */
    fun startConfigure(activity: Activity, id: Int): Boolean = try {
        host.startAppWidgetConfigureActivityForResult(activity, id, 0, REQUEST_CONFIGURE, null)
        true
    } catch (e: Exception) {
        false
    }

    fun onConfigureResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode != REQUEST_CONFIGURE) return false
        val id = data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
        _configureResults.tryEmit(ConfigureResult(resultCode == Activity.RESULT_OK, id))
        return true
    }

    suspend fun commit(id: Int, info: AppWidgetProviderInfo?, rowHeightDp: Int) {
        val rows = WidgetCodec.rowsFor(info?.minHeight?.let { pxToDp(it) } ?: 0, rowHeightDp)
        prefs.addWidget(HostedWidget(id, rows))
        prefs.setPendingWidget(null)
    }

    suspend fun abandon(id: Int) {
        try { host.deleteAppWidgetId(id) } catch (e: Exception) { /* already gone */ }
        prefs.setPendingWidget(null)
    }

    suspend fun remove(id: Int) {
        try { host.deleteAppWidgetId(id) } catch (e: Exception) { /* already gone */ }
        prefs.removeWidget(id)
    }

    suspend fun move(id: Int, delta: Int) = prefs.moveWidget(id, delta)

    suspend fun resize(id: Int, delta: Int) = prefs.resizeWidget(id, delta)

    /** Drops an unconfirmed pending id and widgets whose provider no longer exists. Safe to call at every start. */
    suspend fun cleanup() {
        val pending = prefs.pendingWidgetId()
        val placed = prefs.widgets.first()
        if (pending != null && placed.none { it.id == pending }) abandon(pending) else if (pending != null) prefs.setPendingWidget(null)
        placed.filter { infoFor(it.id) == null }.forEach { remove(it.id) }
    }

    fun createView(context: Context, id: Int): AppWidgetHostView? {
        val info = infoFor(id) ?: return null
        return try {
            host.createView(context, id, info)
        } catch (e: Exception) {
            null
        }
    }

    private fun pxToDp(px: Int): Int = (px / appContext.resources.displayMetrics.density).toInt()

    companion object {
        const val HOST_ID = 1024
        const val REQUEST_CONFIGURE = 0x4D57 // "MW"
    }
}
