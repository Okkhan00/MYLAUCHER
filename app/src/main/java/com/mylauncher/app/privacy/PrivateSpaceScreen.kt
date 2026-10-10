package com.mylauncher.app.privacy

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mylauncher.app.data.model.AppInfo
import com.mylauncher.app.launcher.apps.AppActionsDialog
import com.mylauncher.app.launcher.apps.AppMenuActions
import com.mylauncher.app.launcher.search.AppSearch
import com.mylauncher.app.ui.components.AppRow
import com.mylauncher.app.ui.components.EmptyState
import com.mylauncher.app.ui.components.ScreenHeader

/**
 * Temporarily reveals private apps. It is only reachable through the launcher lock when a PIN is
 * set, and it locks again as soon as the user leaves it (see LauncherApp).
 */
@Composable
fun PrivateSpaceScreen(
    privateApps: List<AppInfo>,
    lockActive: Boolean,
    iconSizeDp: Int,
    menu: AppMenuActions,
    onLaunch: (AppInfo) -> Unit,
    onBack: () -> Unit,
) {
    var menuApp by remember { mutableStateOf<AppInfo?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(privateApps, query) { AppSearch.filter(privateApps, query) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            ScreenHeader("Private apps", onBack)
            Text(
                if (lockActive) AppLockPolicy.PRIVATE_TEXT
                else AppLockPolicy.PRIVATE_TEXT + " Set a launcher PIN in Settings > Security to require it before this screen opens.",
                Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (privateApps.isEmpty()) {
                EmptyState(
                    title = "No private apps",
                    subtitle = "Long-press an app and choose \"Make private\", or add apps in Settings > Private apps.",
                    modifier = Modifier.weight(1f),
                )
            } else {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text("Search private apps") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                )
                if (results.isEmpty()) {
                    EmptyState(title = "No apps found", modifier = Modifier.weight(1f))
                } else {
                    LazyColumn(Modifier.weight(1f)) {
                        items(results, key = { it.packageName }) { app ->
                            AppRow(
                                app = app,
                                iconSize = iconSizeDp.dp,
                                showLabel = true,
                                onClick = { onLaunch(app) },
                                onLongClick = { menuApp = app },
                            )
                        }
                    }
                }
            }
        }
    }

    menuApp?.let { app -> AppActionsDialog(app, menu, showMove = false, onDismiss = { menuApp = null }) }
}
