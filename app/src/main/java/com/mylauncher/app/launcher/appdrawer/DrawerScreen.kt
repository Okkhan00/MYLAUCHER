package com.mylauncher.app.launcher.appdrawer

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.mylauncher.app.data.model.AppInfo
import com.mylauncher.app.data.model.DrawerLayout
import com.mylauncher.app.data.model.LauncherSettings
import com.mylauncher.app.launcher.apps.AppActionsDialog
import com.mylauncher.app.launcher.apps.AppMenuActions
import com.mylauncher.app.launcher.search.AppSearch
import com.mylauncher.app.ui.components.AppRow
import com.mylauncher.app.ui.components.AppTile
import com.mylauncher.app.ui.components.EmptyState

/** App drawer with live search. Opening via "search" focuses the field and shows the keyboard. */
@Composable
fun DrawerScreen(
    apps: List<AppInfo>,
    settings: LauncherSettings,
    focusSearch: Boolean,
    recentApps: List<AppInfo>,
    mostUsedApps: List<AppInfo>,
    menu: AppMenuActions,
    onLaunch: (AppInfo) -> Unit,
    onOpenSettings: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var menuApp by remember { mutableStateOf<AppInfo?>(null) }
    val results = remember(apps, query) { AppSearch.filter(apps, query) }

    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(focusSearch) {
        if (focusSearch) {
            withFrameNanos { }
            try {
                focusRequester.requestFocus()
                keyboard?.show()
            } catch (e: IllegalStateException) {
                // Field not attached yet; the user can tap it.
            }
        }
    }

    val iconSize = settings.iconSize.sizeDp.dp
    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
    ) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val trailing: (@Composable () -> Unit)? = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                } else {
                    null
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                    singleLine = true,
                    shape = CircleShape,
                    placeholder = { Text("Search apps") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = trailing,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { results.firstOrNull()?.let(onLaunch) }),
                )
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Launcher settings")
                }
            }

            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val showIndex = query.isEmpty() && results.size >= 24 && maxHeight > 420.dp
                val letters = remember(results) { results.map { firstLetter(it.label) }.distinct() }
                val firstIndexByLetter = remember(results) {
                    val map = LinkedHashMap<Char, Int>()
                    results.forEachIndexed { i, app -> map.putIfAbsent(firstLetter(app.label), i) }
                    map
                }
                val endPadding = if (showIndex) 28.dp else 8.dp
                val showRecent = query.isEmpty() && recentApps.isNotEmpty() && maxHeight > 520.dp
                val showMostUsed = query.isEmpty() && mostUsedApps.isNotEmpty() && maxHeight > 720.dp

                Column(Modifier.fillMaxSize()) {
                if (showRecent) ShortcutRow("Recently used", recentApps, iconSize, settings.showLabels, onLaunch) { menuApp = it }
                if (showMostUsed) ShortcutRow("Most used", mostUsedApps, iconSize, settings.showLabels, onLaunch) { menuApp = it }
                Box(Modifier.weight(1f).fillMaxWidth()) {

                if (results.isEmpty()) {
                    EmptyState(
                        title = "No apps found",
                        subtitle = if (query.isNotEmpty()) "Try a different name." else null,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else if (settings.drawerLayout == DrawerLayout.GRID) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(settings.columns),
                        state = gridState,
                        contentPadding = PaddingValues(start = 8.dp, end = endPadding, bottom = 16.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(results, key = { it.packageName }) { app ->
                            AppTile(
                                app = app,
                                iconSize = iconSize,
                                showLabel = settings.showLabels,
                                onClick = { onLaunch(app) },
                                onLongClick = { menuApp = app },
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(start = 0.dp, end = endPadding - 8.dp, bottom = 16.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(results, key = { it.packageName }) { app ->
                            AppRow(
                                app = app,
                                iconSize = iconSize,
                                showLabel = settings.showLabels,
                                onClick = { onLaunch(app) },
                                onLongClick = { menuApp = app },
                            )
                        }
                    }
                }

                if (showIndex) {
                    AlphabetIndex(
                        letters = letters,
                        onLetter = { letter ->
                            val index = firstIndexByLetter[letter] ?: return@AlphabetIndex
                            if (settings.drawerLayout == DrawerLayout.GRID) {
                                gridState.requestScrollToItem(index)
                            } else {
                                listState.requestScrollToItem(index)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(24.dp)
                            .fillMaxHeight()
                            .padding(vertical = 8.dp),
                    )
                }
                }
                }
            }
        }
    }

    menuApp?.let { app ->
        AppActionsDialog(app, menu, showMove = false, onDismiss = { menuApp = null })
    }
}

@Composable
private fun ShortcutRow(
    title: String,
    apps: List<AppInfo>,
    iconSize: androidx.compose.ui.unit.Dp,
    showLabels: Boolean,
    onLaunch: (AppInfo) -> Unit,
    onLongPress: (AppInfo) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        LazyRow(contentPadding = PaddingValues(horizontal = 8.dp)) {
            items(apps, key = { it.packageName }) { app ->
                AppTile(
                    app = app,
                    iconSize = iconSize,
                    showLabel = showLabels,
                    onClick = { onLaunch(app) },
                    onLongClick = { onLongPress(app) },
                    modifier = Modifier.width(76.dp),
                )
            }
        }
    }
}
