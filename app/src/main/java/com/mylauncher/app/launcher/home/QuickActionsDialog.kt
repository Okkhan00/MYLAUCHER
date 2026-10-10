package com.mylauncher.app.launcher.home

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.mylauncher.app.launcher.apps.AppActions
import com.mylauncher.app.launcher.apps.MenuItem

/** Shortcuts to official Android settings screens. No special permissions needed. */
object QuickActions {
    val items: List<Pair<String, String>> = listOf(
        "Android settings" to Settings.ACTION_SETTINGS,
        "Wi-Fi" to Settings.ACTION_WIFI_SETTINGS,
        "Bluetooth" to Settings.ACTION_BLUETOOTH_SETTINGS,
        "Battery" to Settings.ACTION_BATTERY_SAVER_SETTINGS,
        "Display" to Settings.ACTION_DISPLAY_SETTINGS,
        "Sound" to Settings.ACTION_SOUND_SETTINGS,
        "App settings" to Settings.ACTION_APPLICATION_SETTINGS,
    )
}

@Composable
fun QuickActionsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick actions") },
        text = {
            Column {
                QuickActions.items.forEach { (label, action) ->
                    MenuItem(label) {
                        AppActions.start(context, Intent(action), "Can't open $label")
                        onDismiss()
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
