package com.mylauncher.app.launcher.home

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.mylauncher.app.data.model.AppInfo
import com.mylauncher.app.data.model.GestureAction
import com.mylauncher.app.data.model.LauncherSettings
import com.mylauncher.app.launcher.apps.AppActions
import com.mylauncher.app.launcher.apps.AppActionsDialog
import com.mylauncher.app.launcher.apps.AppMenuActions
import com.mylauncher.app.launcher.apps.HomeEntry
import com.mylauncher.app.launcher.apps.MenuItem
import com.mylauncher.app.launcher.folders.DeleteFolderDialog
import com.mylauncher.app.launcher.folders.FolderActions
import com.mylauncher.app.launcher.folders.FolderDialog
import com.mylauncher.app.launcher.folders.FolderNameDialog
import com.mylauncher.app.launcher.gestures.SwipeDirection
import com.mylauncher.app.launcher.gestures.SwipeResolver
import com.mylauncher.app.ui.components.AppTile
import com.mylauncher.app.ui.components.EmptyState
import com.mylauncher.app.ui.components.FolderTile
import com.mylauncher.app.ui.components.ReorderableGrid

@Composable
fun HomeScreen(
    settings: LauncherSettings,
    entries: List<HomeEntry>,
    selectableApps: List<AppInfo>,
    menu: AppMenuActions,
    folderActions: FolderActions,
    onOpenDrawer: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onLaunch: (AppInfo) -> Unit,
    onReorderHome: (List<String>) -> Unit,
) {
    val context = LocalContext.current
    var menuApp by remember { mutableStateOf<AppInfo?>(null) }
    var showEdit by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    var showNewFolder by remember { mutableStateOf(false) }
    var showQuickActions by remember { mutableStateOf(false) }
    var folderMenuId by remember { mutableStateOf<String?>(null) }
    var renameFolderId by remember { mutableStateOf<String?>(null) }
    var deleteFolderId by remember { mutableStateOf<String?>(null) }
    var openFolderId by rememberSaveable { mutableStateOf<String?>(null) }
    var reorderMode by rememberSaveable { mutableStateOf(false) }

    fun folderEntry(id: String?): HomeEntry.FolderEntry? =
        if (id == null) null else entries.filterIsInstance<HomeEntry.FolderEntry>().firstOrNull { it.folder.id == id }

    val perform: (GestureAction) -> Unit = { action ->
        when (action) {
            GestureAction.OPEN_DRAWER -> onOpenDrawer()
            GestureAction.OPEN_SEARCH -> onOpenSearch()
            GestureAction.QUICK_ACTIONS -> showQuickActions = true
            GestureAction.OPEN_SETTINGS -> onOpenSettings()
            GestureAction.NONE -> Unit
        }
    }
    val currentPerform by rememberUpdatedState(perform)
    val currentSettings by rememberUpdatedState(settings)
    val thresholdPx = with(LocalDensity.current) { 72.dp.toPx() }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.22f))
            // Double tap runs the configured action; long press opens the edit menu.
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { currentPerform(currentSettings.doubleTap) },
                    onLongPress = { showEdit = true },
                )
            }
            // Swipe up / down run the configured actions.
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragCancel = { total = 0f },
                    onDragEnd = {
                        when (SwipeResolver.resolve(total, thresholdPx)) {
                            SwipeDirection.UP -> currentPerform(currentSettings.swipeUp)
                            SwipeDirection.DOWN -> currentPerform(currentSettings.swipeDown)
                            null -> Unit
                        }
                        total = 0f
                    },
                ) { _, dy -> total += dy }
            }
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        val landscape = maxWidth > maxHeight

        val favoritesArea: @Composable (Modifier) -> Unit = { modifier ->
            if (entries.isEmpty()) {
                EmptyState(
                    title = "Add your favorite apps here",
                    subtitle = "They stay on your home screen.",
                    actionLabel = "Add app",
                    onAction = { showAdd = true },
                    onWallpaper = true,
                    modifier = modifier,
                )
            } else {
                Column(modifier) {
                    if (reorderMode) {
                        Text(
                            "Drag to reorder",
                            style = MaterialTheme.typography.labelLarge.copy(color = Color.White),
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                    ReorderableGrid(
                        items = entries,
                        key = { it.key },
                        columns = settings.columns,
                        reorderEnabled = reorderMode,
                        onReorder = onReorderHome,
                        modifier = Modifier.heightIn(max = 340.dp),
                    ) { entry, _ ->
                        when (entry) {
                            is HomeEntry.AppEntry -> AppTile(
                                app = entry.app,
                                iconSize = settings.iconSize.sizeDp.dp,
                                showLabel = settings.showLabels,
                                interactive = !reorderMode,
                                onClick = { onLaunch(entry.app) },
                                onLongClick = { menuApp = entry.app },
                                onWallpaper = true,
                            )
                            is HomeEntry.FolderEntry -> FolderTile(
                                name = entry.folder.name,
                                previewApps = entry.apps,
                                appCount = entry.apps.size,
                                iconSize = settings.iconSize.sizeDp.dp,
                                showLabel = settings.showLabels,
                                interactive = !reorderMode,
                                onClick = { openFolderId = entry.folder.id },
                                onLongClick = { folderMenuId = entry.folder.id },
                                onWallpaper = true,
                            )
                        }
                    }
                }
            }
        }

        val bottomButton: @Composable (Modifier) -> Unit = { modifier ->
            if (reorderMode) {
                Button(onClick = { reorderMode = false }, modifier = modifier.heightIn(min = 48.dp)) { Text("Done") }
            } else {
                FilledTonalButton(onClick = onOpenDrawer, modifier = modifier.heightIn(min = 48.dp)) { Text("All apps") }
            }
        }

        if (landscape) {
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.weight(0.45f).fillMaxHeight()) {
                    ClockBlock(settings)
                    if (settings.showSearchBar) {
                        Spacer(Modifier.height(12.dp))
                        SearchPill(onOpenSearch)
                    }
                    Spacer(Modifier.weight(1f))
                    bottomButton(Modifier)
                }
                favoritesArea(Modifier.weight(0.55f).fillMaxHeight().verticalScroll(rememberScrollState()))
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                ClockBlock(settings)
                if (settings.showSearchBar) {
                    Spacer(Modifier.height(16.dp))
                    SearchPill(onOpenSearch)
                }
                Spacer(Modifier.weight(1f))
                favoritesArea(Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                bottomButton(Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }

    menuApp?.let { app ->
        AppActionsDialog(app, menu, showMove = true, onDismiss = { menuApp = null })
    }

    folderEntry(openFolderId)?.let { entry ->
        FolderDialog(
            entry = entry,
            settings = settings,
            selectableApps = selectableApps,
            actions = folderActions,
            onLaunch = onLaunch,
            onDismiss = { openFolderId = null },
        )
    }

    folderEntry(folderMenuId)?.let { entry ->
        AlertDialog(
            onDismissRequest = { folderMenuId = null },
            title = { Text(entry.folder.name) },
            text = {
                Column {
                    MenuItem("Open") { openFolderId = entry.folder.id; folderMenuId = null }
                    MenuItem("Rename") { renameFolderId = entry.folder.id; folderMenuId = null }
                    MenuItem("Delete folder") { deleteFolderId = entry.folder.id; folderMenuId = null }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { folderMenuId = null }) { Text("Close") } },
        )
    }

    folderEntry(renameFolderId)?.let { entry ->
        FolderNameDialog(
            title = "Rename folder",
            initialName = entry.folder.name,
            onConfirm = { folderActions.rename(entry.folder.id, it) },
            onDismiss = { renameFolderId = null },
        )
    }

    folderEntry(deleteFolderId)?.let { entry ->
        DeleteFolderDialog(
            folderName = entry.folder.name,
            onConfirm = { folderActions.delete(entry.folder.id) },
            onDismiss = { deleteFolderId = null },
        )
    }

    if (showNewFolder) {
        FolderNameDialog(
            title = "New folder",
            initialName = "",
            onConfirm = { folderActions.create(it) },
            onDismiss = { showNewFolder = false },
        )
    }

    if (showQuickActions) {
        QuickActionsDialog(onDismiss = { showQuickActions = false })
    }

    if (showEdit) {
        AlertDialog(
            onDismissRequest = { showEdit = false },
            title = { Text("Edit home screen") },
            text = {
                Column {
                    MenuItem("Add app") { showEdit = false; showAdd = true }
                    MenuItem("Add folder") { showEdit = false; showNewFolder = true }
                    if (entries.size > 1) {
                        MenuItem("Reorder home screen") { showEdit = false; reorderMode = true }
                    }
                    MenuItem("Quick actions") { showEdit = false; showQuickActions = true }
                    MenuItem("Change wallpaper") { showEdit = false; AppActions.openWallpaperPicker(context) }
                    MenuItem("Launcher settings") { showEdit = false; onOpenSettings() }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showEdit = false }) { Text("Close") } },
        )
    }

    if (showAdd) {
        AddAppsDialog(
            apps = selectableApps,
            favoriteIds = menu.favoriteIds,
            onToggle = menu.toggleFavorite,
            onDismiss = { showAdd = false },
        )
    }
}

@Composable
private fun SearchPill(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.2f),
        contentColor = Color.White,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Search, contentDescription = null)
            Text("Search apps", Modifier.padding(start = 12.dp))
        }
    }
}
