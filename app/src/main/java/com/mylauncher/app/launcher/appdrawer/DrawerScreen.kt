package com.mylauncher.app.launcher.appdrawer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.mylauncher.app.categories.CategoryGroup
import com.mylauncher.app.categories.CategorySwipe
import com.mylauncher.app.data.model.AppInfo
import com.mylauncher.app.data.model.DrawerLayout
import com.mylauncher.app.data.model.Folder
import com.mylauncher.app.data.model.LaunchStat
import com.mylauncher.app.data.model.LauncherSettings
import com.mylauncher.app.data.model.SmartSettings
import com.mylauncher.app.launcher.apps.AppActionsDialog
import com.mylauncher.app.launcher.apps.AppMenuActions
import com.mylauncher.app.launcher.search.AppSearch
import com.mylauncher.app.launcher.search.SearchResult
import com.mylauncher.app.launcher.search.SearchResultsList
import com.mylauncher.app.launcher.search.SettingShortcut
import com.mylauncher.app.launcher.search.SmartSearch
import com.mylauncher.app.launcher.search.SystemAction
import com.mylauncher.app.ui.components.AppRow
import com.mylauncher.app.ui.components.AppTile
import com.mylauncher.app.ui.components.EmptyState
import com.mylauncher.app.ui.theme.LocalSearchBarShape

/** App drawer with live search. Opening via "search" focuses the field and shows the keyboard. */
@Composable
fun DrawerScreen(
    apps: List<AppInfo>,
    settings: LauncherSettings,
    focusSearch: Boolean,
    recentApps: List<AppInfo>,
    mostUsedApps: List<AppInfo>,
    suggestedApps: List<AppInfo>,
    folders: List<Folder>,
    launchStats: Map<String, LaunchStat>,
    smart: SmartSettings,
    categoryGroups: List<CategoryGroup>,
    menu: AppMenuActions,
    onLaunch: (AppInfo) -> Unit,
    onOpenSettings: () -> Unit,
    onGroupByCategoryChange: (Boolean) -> Unit,
    onSettingShortcut: (SettingShortcut) -> Unit,
    onSystemAction: (SystemAction) -> Unit,
    onWebSearch: (String) -> Unit,
    privateCount: Int,
    onOpenPrivate: () -> Unit,
    drawerOpacity: Float,
    wallpaperScrim: Color,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var menuApp by remember { mutableStateOf<AppInfo?>(null) }
    val searching = query.isNotBlank()
    // Categories mode shows category cards; tapping one opens that category's apps (id survives rotation).
    var openCategoryId by rememberSaveable { mutableStateOf<String?>(null) }
    val grouped = smart.groupDrawerByCategory && !searching
    val openGroup = if (grouped) categoryGroups.firstOrNull { it.entry.category.id == openCategoryId } else null
    // Back from an opened category returns to the cards instead of leaving the drawer.
    BackHandler(enabled = openGroup != null) { openCategoryId = null }
    // The category emptied (apps hidden/uninstalled): fall back to the cards.
    LaunchedEffect(openGroup, grouped) {
        if (grouped && openCategoryId != null && openGroup == null) openCategoryId = null
    }
    // Plain A-Z list used by the alphabet index and the non-search views (search has its own results).
    val results = apps
    // Smart Search 2.0 results; only computed while the user is typing.
    // Search data (lower-cased labels, words) is built once per app list, only when first needed,
    // so typing never re-processes every label and never queries PackageManager.
    val searchIndex = remember(apps) { lazy(LazyThreadSafetyMode.NONE) { AppSearch.Index(apps) } }
    val searchResults = remember(apps, query, folders, launchStats, smart.searchSettingsShortcuts, smart.webSearchEnabled) {
        if (searching) {
            SmartSearch.search(
                query = query,
                apps = apps,
                folders = folders,
                stats = launchStats,
                includeSettings = smart.searchSettingsShortcuts,
                includeActions = smart.searchSettingsShortcuts,
                includeWebFallback = smart.webSearchEnabled,
                index = searchIndex.value,
            )
        } else {
            emptyList()
        }
    }
    val activate: (SearchResult) -> Unit = { result ->
        when (result) {
            is SearchResult.AppResult -> onLaunch(result.app)
            is SearchResult.SettingResult -> onSettingShortcut(result.shortcut)
            is SearchResult.ActionResult -> onSystemAction(result.action)
            is SearchResult.WebResult -> onWebSearch(result.query)
        }
    }

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

    // The wallpaper overlay only shows through when the drawer is translucent and the user enabled it.
    Box(Modifier.fillMaxSize().background(wallpaperScrim)) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = drawerOpacity),
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
                    shape = LocalSearchBarShape.current,
                    placeholder = { Text(if (smart.webSearchEnabled || smart.searchSettingsShortcuts) "Search apps, settings and more" else "Search apps") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = trailing,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { searchResults.firstOrNull()?.let(activate) }),
                )
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Launcher settings")
                }
            }

            if (!searching) {
                DrawerModeTabs(
                    categoriesMode = smart.groupDrawerByCategory,
                    onSelect = { categories ->
                        openCategoryId = null
                        onGroupByCategoryChange(categories)
                    },
                )
                if (privateCount > 0) {
                    TextButton(onClick = onOpenPrivate, modifier = Modifier.padding(horizontal = 8.dp)) {
                        Text("Private apps ($privateCount)")
                    }
                }
            }

            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val showIndex = !grouped && query.isEmpty() && results.size >= 24 && maxHeight > 420.dp
                val letters = remember(results) { results.map { firstLetter(it.label) }.distinct() }
                val firstIndexByLetter = remember(results) {
                    val map = LinkedHashMap<Char, Int>()
                    results.forEachIndexed { i, app -> map.putIfAbsent(firstLetter(app.label), i) }
                    map
                }
                val endPadding = if (showIndex) 28.dp else 8.dp
                val showSuggested = smart.smartSuggestions && query.isEmpty() && openGroup == null && suggestedApps.isNotEmpty() && maxHeight > 520.dp
                // When suggestions are shown the other shortcut rows need a little more room.
                val showRecent = smart.showRecentApps && query.isEmpty() && openGroup == null && recentApps.isNotEmpty() && maxHeight > (if (showSuggested) 680.dp else 520.dp)
                val showMostUsed = query.isEmpty() && openGroup == null && mostUsedApps.isNotEmpty() && maxHeight > (if (showSuggested) 880.dp else 720.dp)

                Column(Modifier.fillMaxSize()) {
                if (showSuggested) ShortcutRow("Suggested", suggestedApps, iconSize, settings.showLabels, onLaunch) { menuApp = it }
                if (showRecent) ShortcutRow("Recently used", recentApps, iconSize, settings.showLabels, onLaunch) { menuApp = it }
                if (showMostUsed) ShortcutRow("Most used", mostUsedApps, iconSize, settings.showLabels, onLaunch) { menuApp = it }
                Box(Modifier.weight(1f).fillMaxWidth()) {

                if (searching) {
                    SearchResultsList(
                        results = searchResults,
                        iconSize = iconSize,
                        showLabels = settings.showLabels,
                        onActivate = activate,
                        onAppLongClick = { menuApp = it },
                    )
                } else if (grouped) {
                    if (categoryGroups.isEmpty()) {
                        EmptyState(
                            title = "No categories to show",
                            subtitle = "All categories are hidden or empty. Switch to All Apps, or change this in Settings > App categories.",
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else if (openGroup == null) {
                        CategoryCardGrid(
                            groups = categoryGroups,
                            animate = settings.animations,
                            onOpen = { openCategoryId = it.entry.category.id },
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        // Swiping sideways inside a category moves to the next/previous one. Vertical scrolling and
                        // taps are untouched: only a mostly-horizontal drag past the threshold counts.
                        val orderedIds = remember(categoryGroups) { categoryGroups.map { it.entry.category.id } }
                        val thresholdPx = with(LocalDensity.current) { 72.dp.toPx() }
                        val animate = settings.animations
                        val currentIds by rememberUpdatedState(orderedIds)
                        val currentOpen by rememberUpdatedState(openGroup.entry.category.id)
                        Box(
                            Modifier
                                .fillMaxSize()
                                .pointerInput(thresholdPx) {
                                    var total = 0f
                                    detectHorizontalDragGestures(
                                        onDragStart = { total = 0f },
                                        onDragCancel = { total = 0f },
                                        onDragEnd = {
                                            CategorySwipe.target(currentIds, currentOpen, total, thresholdPx)
                                                ?.let { openCategoryId = it }
                                            total = 0f
                                        },
                                        onHorizontalDrag = { _, dragAmount -> total += dragAmount },
                                    )
                                },
                        ) {
                            AnimatedContent(
                                targetState = openGroup.entry.category.id,
                                transitionSpec = {
                                    if (!animate) {
                                        EnterTransition.None togetherWith ExitTransition.None
                                    } else {
                                        val forward = orderedIds.indexOf(targetState) > orderedIds.indexOf(initialState)
                                        val dir = if (forward) 1 else -1
                                        (slideInHorizontally(tween(180)) { it / 4 * dir } + fadeIn(tween(180))) togetherWith
                                            (slideOutHorizontally(tween(140)) { -it / 4 * dir } + fadeOut(tween(100)))
                                    }
                                },
                                label = "category",
                                modifier = Modifier.fillMaxSize(),
                            ) { categoryId ->
                                val group = categoryGroups.firstOrNull { it.entry.category.id == categoryId }
                                if (group != null) {
                                    Column(Modifier.fillMaxSize()) {
                                        CategoryAppsHeader(group, onBack = { openCategoryId = null })
                                        if (group.apps.isEmpty()) {
                                            EmptyState(title = "No apps in this category", modifier = Modifier.fillMaxSize())
                                        } else if (settings.drawerLayout == DrawerLayout.GRID) {
                                            LazyVerticalGrid(
                                                columns = GridCells.Fixed(settings.columns),
                                                contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 16.dp),
                                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                            ) {
                                                items(group.apps, key = { it.packageName }, contentType = { "app" }) { app ->
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
                                            LazyColumn(contentPadding = PaddingValues(bottom = 16.dp), modifier = Modifier.weight(1f).fillMaxWidth()) {
                                                items(group.apps, key = { it.packageName }, contentType = { "app" }) { app ->
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
                                    }
                                }
                            }
                        }
                    }
                } else if (results.isEmpty()) {
                    EmptyState(
                        title = "No apps found",
                        modifier = Modifier.fillMaxSize(),
                    )
                } else if (settings.drawerLayout == DrawerLayout.GRID) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(settings.columns),
                        state = gridState,
                        contentPadding = PaddingValues(start = 8.dp, end = endPadding, bottom = 16.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(results, key = { it.packageName }, contentType = { "app" }) { app ->
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
                        items(results, key = { it.packageName }, contentType = { "app" }) { app ->
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
