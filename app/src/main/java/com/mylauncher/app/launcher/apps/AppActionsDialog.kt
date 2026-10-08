package com.mylauncher.app.launcher.apps

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mylauncher.app.categories.AppCategory
import com.mylauncher.app.categories.CategoryEntry
import com.mylauncher.app.data.model.AppInfo
import com.mylauncher.app.data.model.Folder

class AppMenuActions(
    /** Ordered home list: package names and "folder:<id>" tokens. */
    val favoriteIds: List<String>,
    val folders: List<Folder>,
    val toggleFavorite: (AppInfo) -> Unit,
    val move: (AppInfo, Int) -> Unit,
    val hide: (AppInfo) -> Unit,
    val addToFolder: (AppInfo, String) -> Unit,
    // ---- Phase 3 additions -------------------------------------------------------------
    /** Opens the app through the launcher's own launch path (history and App Lock apply). */
    val open: (AppInfo) -> Unit = {},
    val removeFromFolder: (AppInfo, String) -> Unit = { _, _ -> },
    /** Packages that are private (hidden inside the launcher). */
    val privateApps: Set<String> = emptySet(),
    val setPrivate: (AppInfo, Boolean) -> Unit = { _, _ -> },
    /** App Lock can only be offered when a launcher PIN exists. */
    val appLockAvailable: Boolean = false,
    val lockedApps: Set<String> = emptySet(),
    val setLocked: (AppInfo, Boolean) -> Unit = { _, _ -> },
    val categories: List<CategoryEntry> = emptyList(),
    val categoryOf: (AppInfo) -> AppCategory = { AppCategory.OTHER },
    val setCategory: (AppInfo, AppCategory?) -> Unit = { _, _ -> },
)

/** Long-press menu. Uses only official system screens and intents; nothing is silent or privileged. */
@Composable
fun AppActionsDialog(app: AppInfo, actions: AppMenuActions, showMove: Boolean, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val isFavorite = app.packageName in actions.favoriteIds
    val folder = actions.folders.firstOrNull { app.packageName in it.appIds }
    val isPrivate = app.packageName in actions.privateApps
    val isLocked = app.packageName in actions.lockedApps
    var pickFolder by remember { mutableStateOf(false) }
    var pickCategory by remember { mutableStateOf(false) }
    var confirmUninstall by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                MenuItem("Open") { actions.open(app); onDismiss() }
                MenuItem("App info") { AppActions.openAppInfo(context, app.packageName); onDismiss() }
                MenuItem(if (isFavorite) "Remove from home screen" else "Add to home screen") {
                    actions.toggleFavorite(app); onDismiss()
                }
                if (showMove && isFavorite) {
                    MenuItem("Move earlier") { actions.move(app, -1) }
                    MenuItem("Move later") { actions.move(app, 1) }
                }
                if (folder != null) {
                    MenuItem("Remove from folder \"${folder.name}\"") { actions.removeFromFolder(app, folder.id); onDismiss() }
                }
                if (actions.folders.isNotEmpty()) {
                    MenuItem(if (folder != null) "Move to another folder" else "Add to folder") { pickFolder = true }
                }
                if (actions.categories.isNotEmpty()) {
                    val current = actions.categories.firstOrNull { it.category == actions.categoryOf(app) }
                    MenuItem("Category: ${current?.displayName ?: "Other"}") { pickCategory = true }
                }
                MenuItem(if (isPrivate) "Remove from private apps" else "Hide (make private)") {
                    actions.setPrivate(app, !isPrivate); onDismiss()
                }
                if (actions.appLockAvailable) {
                    MenuItem(if (isLocked) "Turn off app lock" else "Turn on app lock") {
                        actions.setLocked(app, !isLocked); onDismiss()
                    }
                }
                MenuItem("Share app link") { AppActions.share(context, app); onDismiss() }
                MenuItem("Uninstall") { confirmUninstall = true }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )

    if (pickFolder) {
        AlertDialog(
            onDismissRequest = { pickFolder = false },
            title = { Text("Add to folder") },
            text = {
                Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                    actions.folders.forEach { f ->
                        MenuItem(f.name) {
                            actions.addToFolder(app, f.id)
                            pickFolder = false
                            onDismiss()
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { pickFolder = false }) { Text("Cancel") } },
        )
    }

    if (pickCategory) {
        AlertDialog(
            onDismissRequest = { pickCategory = false },
            title = { Text("Category for ${app.label}") },
            text = {
                Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                    MenuItem("Automatic (recommended)") { actions.setCategory(app, null); pickCategory = false; onDismiss() }
                    actions.categories.forEach { entry ->
                        MenuItem(entry.displayName) { actions.setCategory(app, entry.category); pickCategory = false; onDismiss() }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { pickCategory = false }) { Text("Cancel") } },
        )
    }

    if (confirmUninstall) {
        AlertDialog(
            onDismissRequest = { confirmUninstall = false },
            title = { Text("Uninstall ${app.label}?") },
            text = { Text("Android will ask you to confirm. Apps that came with your phone may not be removable.") },
            confirmButton = {
                TextButton(onClick = {
                    AppActions.uninstall(context, app.packageName)
                    confirmUninstall = false
                    onDismiss()
                }) { Text("Continue") }
            },
            dismissButton = { TextButton(onClick = { confirmUninstall = false }) { Text("Cancel") } },
        )
    }
}

@Composable
internal fun MenuItem(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text(label, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
    }
}
