package com.mylauncher.app.launcher.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mylauncher.app.ui.components.ScreenHeader

/** Shows the real default-launcher status and the safest way to change it. */
@Composable
fun SetupScreen(controller: DefaultLauncherController, onBack: () -> Unit) {
    val status = controller.status
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            ScreenHeader("Launcher setup", onBack)
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Current default launcher", style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = when {
                                status.isDefault -> "My Launcher"
                                status.currentLabel != null -> status.currentLabel
                                else -> "None chosen"
                            },
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            text = when {
                                status.isDefault -> "My Launcher is your home app. Pressing Home opens it."
                                status.currentLabel != null ->
                                    "${status.currentLabel} is currently your home app. Your phone keeps using it until you change it."
                                else -> "Android will ask which launcher to use when you press Home."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Button(
                    onClick = controller.request,
                    enabled = !status.isDefault,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (status.isDefault) "Already your default launcher" else "Set My Launcher as default") }

                Text(
                    "Android decides whether the change happens. This screen only shows success " +
                        "once Android reports My Launcher as the home app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Text("Changing launcher safely", style = MaterialTheme.typography.titleMedium)
                Text(
                    "1. Open Android Settings, then Apps, then Default apps, then Home app.\n" +
                        "2. Pick My Launcher, or pick your previous launcher to go back.\n" +
                        "3. Press Home to check it worked.\n\n" +
                        "Menu names differ a little between phone brands. You never lose your apps " +
                        "or data by switching launchers.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(onClick = controller.openSettings, modifier = Modifier.fillMaxWidth()) {
                    Text("Open Android home app settings")
                }
            }
        }
    }
}
