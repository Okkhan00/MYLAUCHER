package com.mylauncher.app.launcher.apps

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
)

/** Long-press menu. Uses only official system screens for App info and Uninstall. */
@Composable
fun AppActionsDialog(app: AppInfo, actions: AppMenuActions, showMove: Boolean, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val isFavorite = app.packageName in actions.favoriteIds
    var pickFolder by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column {
                MenuItem("Open") { AppActions.launch(context, app); onDismiss() }
                MenuItem("App info") { AppActions.openAppInfo(context, app.packageName); onDismiss() }
                MenuItem(if (isFavorite) "Remove from home screen" else "Add to home screen") {
                    actions.toggleFavorite(app); onDismiss()
                }
                if (showMove && isFavorite) {
                    MenuItem("Move earlier") { actions.move(app, -1) }
                    MenuItem("Move later") { actions.move(app, 1) }
                }
                if (actions.folders.isNotEmpty()) {
                    MenuItem("Add to folder") { pickFolder = true }
                }
                MenuItem("Hide from launcher") { actions.hide(app); onDismiss() }
                MenuItem("Uninstall") { AppActions.uninstall(context, app.packageName); onDismiss() }
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
                Column {
                    actions.folders.forEach { folder ->
                        MenuItem(folder.name) {
                            actions.addToFolder(app, folder.id)
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
}

@Composable
internal fun MenuItem(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text(label, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
    }
}
