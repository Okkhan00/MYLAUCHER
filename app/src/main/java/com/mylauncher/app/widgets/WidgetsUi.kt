package com.mylauncher.app.widgets

import android.app.Activity
import android.appwidget.AppWidgetProviderInfo
import android.view.View
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.mylauncher.app.launcher.apps.MenuItem
import com.mylauncher.app.security.biometric.findFragmentActivity
import kotlinx.coroutines.launch

/** Height of one home-grid row for widget sizing; matches an icon tile with its label. */
const val WIDGET_ROW_DP = 88

/** Keeps the widget host listening only while the launcher is visible. */
@Composable
fun WidgetHostLifecycle(controller: WidgetController) {
    LifecycleEventEffect(Lifecycle.Event.ON_START) { controller.startListening() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { controller.stopListening() }
    LaunchedEffect(Unit) { controller.cleanup() }
}

/**
 * Returns a function that runs the full "add widget" flow for a chosen provider:
 * allocate id, bind (asking the user if Android requires it), configure if needed, then save.
 * Any failure or cancel releases the id again.
 */
@Composable
fun rememberAddWidget(controller: WidgetController): (AppWidgetProviderInfo) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<Pair<Int, AppWidgetProviderInfo>?>(null) }

    fun fail(message: String) {
        val p = pending
        pending = null
        if (p != null) scope.launch { controller.abandon(p.first) }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun afterBind(id: Int, info: AppWidgetProviderInfo) {
        if (!controller.needsConfigure(info)) {
            pending = null
            scope.launch { controller.commit(id, info, WIDGET_ROW_DP) }
            return
        }
        val activity = context.findFragmentActivity()
        if (activity == null || !controller.startConfigure(activity, id)) fail("Can't set up this widget")
    }

    val bindLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val p = pending
        if (p == null) return@rememberLauncherForActivityResult
        if (result.resultCode == Activity.RESULT_OK) afterBind(p.first, p.second) else fail("Widget not added")
    }

    LaunchedEffect(controller) {
        controller.configureResults.collect { r ->
            val p = pending ?: return@collect
            if (r.ok) {
                pending = null
                controller.commit(p.first, p.second, WIDGET_ROW_DP)
            } else {
                fail("Widget setup cancelled")
            }
        }
    }

    return { info ->
        scope.launch {
            val id = try { controller.allocate() } catch (e: Exception) { -1 }
            if (id < 0) {
                Toast.makeText(context, "Can't add widgets on this device", Toast.LENGTH_SHORT).show()
            } else {
                pending = id to info
                if (controller.tryBind(id, info)) afterBind(id, info)
                else {
                    try {
                        bindLauncher.launch(controller.bindPermissionIntent(id, info))
                    } catch (e: Exception) {
                        fail("Can't add this widget")
                    }
                }
            }
        }
    }
}

@Composable
fun WidgetPickerDialog(controller: WidgetController, onPick: (AppWidgetProviderInfo) -> Unit, onDismiss: () -> Unit) {
    val providers = remember { controller.providers() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add widget") },
        text = {
            if (providers.isEmpty()) {
                Text("No widgets are available on this device.")
            } else {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(providers, key = { it.provider.flattenToString() }) { info ->
                        MenuItem(controller.label(info)) { onPick(info); onDismiss() }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** The widgets area shown on the home screen. Each widget has a small menu for move, resize and remove. */
@Composable
fun HomeWidgets(
    controller: WidgetController,
    widgets: List<HostedWidget>,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var menuFor by remember { mutableStateOf<HostedWidget?>(null) }
    var confirmRemove by remember { mutableStateOf<HostedWidget?>(null) }

    Column(modifier.fillMaxWidth()) {
        widgets.forEach { widget ->
            key(widget.id) {
                BoxWithConstraints(Modifier.fillMaxWidth().height((widget.rows * WIDGET_ROW_DP).dp).padding(vertical = 4.dp)) {
                    val widthDp = maxWidth.value.toInt()
                    val heightDp = maxHeight.value.toInt()
                    AndroidView(
                        factory = { ctx -> controller.createView(ctx, widget.id) ?: View(ctx) },
                        update = { view ->
                            @Suppress("DEPRECATION")
                            (view as? android.appwidget.AppWidgetHostView)
                                ?.updateAppWidgetSize(null, widthDp, heightDp, widthDp, heightDp)
                        },
                        modifier = Modifier.fillMaxWidth().height(maxHeight),
                    )
                    IconButton(
                        onClick = { menuFor = widget },
                        modifier = Modifier.align(Alignment.TopEnd),
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Widget options", tint = Color.White)
                    }
                }
            }
        }
    }

    menuFor?.let { widget ->
        val index = widgets.indexOfFirst { it.id == widget.id }
        AlertDialog(
            onDismissRequest = { menuFor = null },
            title = { Text(controller.infoFor(widget.id)?.let { controller.label(it) } ?: "Widget") },
            text = {
                Column {
                    if (index > 0) MenuItem("Move up") { scope.launch { controller.move(widget.id, -1) } }
                    if (index in 0 until widgets.lastIndex) MenuItem("Move down") { scope.launch { controller.move(widget.id, 1) } }
                    if (widget.rows < HostedWidget.MAX_ROWS) MenuItem("Taller") { scope.launch { controller.resize(widget.id, 1) } }
                    if (widget.rows > HostedWidget.MIN_ROWS) MenuItem("Shorter") { scope.launch { controller.resize(widget.id, -1) } }
                    MenuItem("Remove widget") { confirmRemove = widget; menuFor = null }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { menuFor = null }) { Text("Close") } },
        )
    }

    confirmRemove?.let { widget ->
        AlertDialog(
            onDismissRequest = { confirmRemove = null },
            title = { Text("Remove widget?") },
            text = { Text("The widget is removed from your home screen. Its app is not uninstalled.") },
            confirmButton = {
                TextButton(onClick = { scope.launch { controller.remove(widget.id) }; confirmRemove = null }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { confirmRemove = null }) { Text("Cancel") } },
        )
    }
}
