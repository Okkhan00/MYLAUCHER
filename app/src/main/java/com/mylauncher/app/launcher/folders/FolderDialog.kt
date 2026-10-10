package com.mylauncher.app.launcher.folders

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mylauncher.app.data.model.AppInfo
import com.mylauncher.app.data.model.LauncherSettings
import com.mylauncher.app.launcher.apps.AppActions
import com.mylauncher.app.launcher.apps.HomeEntry
import com.mylauncher.app.launcher.apps.MenuItem
import com.mylauncher.app.launcher.home.AddAppsDialog
import com.mylauncher.app.ui.components.AppTile
import com.mylauncher.app.ui.components.EmptyState
import com.mylauncher.app.ui.components.ReorderableGrid

/** Opened folder: launch, reorder by dragging, add or remove apps, rename, delete. */
@Composable
fun FolderDialog(
    entry: HomeEntry.FolderEntry,
    settings: LauncherSettings,
    selectableApps: List<AppInfo>,
    actions: FolderActions,
    onLaunch: (AppInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    val folder = entry.folder
    val context = LocalContext.current
    var reorder by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var menuApp by remember { mutableStateOf<AppInfo?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(folder.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column {
                if (entry.apps.isEmpty()) {
                    EmptyState(title = "Folder is empty", subtitle = "Add apps to this folder.")
                } else {
                    ReorderableGrid(
                        items = entry.apps,
                        key = { it.packageName },
                        columns = minOf(settings.columns, 4),
                        reorderEnabled = reorder,
                        onReorder = { actions.reorder(folder.id, it) },
                        modifier = Modifier.heightIn(max = 320.dp),
                    ) { app, _ ->
                        AppTile(
                            app = app,
                            iconSize = settings.iconSize.sizeDp.dp,
                            showLabel = settings.showLabels,
                            interactive = !reorder,
                            onClick = { onLaunch(app); onDismiss() },
                            onLongClick = { menuApp = app },
                        )
                    }
                }
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 8.dp)) {
                    TextButton(onClick = { showAdd = true }) { Text("Add apps") }
                    if (entry.apps.size > 1) {
                        TextButton(onClick = { reorder = !reorder }) { Text(if (reorder) "Done" else "Reorder") }
                    }
                    TextButton(onClick = { showRename = true }) { Text("Rename") }
                    TextButton(onClick = { showDelete = true }) { Text("Delete") }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )

    if (showAdd) {
        AddAppsDialog(
            apps = selectableApps,
            favoriteIds = folder.appIds,
            onToggle = { app ->
                if (app.packageName in folder.appIds) actions.removeApp(folder.id, app.packageName)
                else actions.addApp(folder.id, app.packageName)
            },
            onDismiss = { showAdd = false },
        )
    }
    if (showRename) {
        FolderNameDialog("Rename folder", folder.name, { actions.rename(folder.id, it) }, { showRename = false })
    }
    if (showDelete) {
        DeleteFolderDialog(folder.name, { actions.delete(folder.id); onDismiss() }, { showDelete = false })
    }
    menuApp?.let { app ->
        AlertDialog(
            onDismissRequest = { menuApp = null },
            title = { Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            text = {
                Column {
                    MenuItem("Open") { onLaunch(app); menuApp = null; onDismiss() }
                    MenuItem("App info") { AppActions.openAppInfo(context, app.packageName); menuApp = null }
                    MenuItem("Remove from folder") { actions.removeApp(folder.id, app.packageName); menuApp = null }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { menuApp = null }) { Text("Close") } },
        )
    }
}
