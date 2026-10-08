package com.mylauncher.app.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mylauncher.app.data.model.LauncherSettings
import com.mylauncher.app.data.model.ThemeMode
import com.mylauncher.app.launcher.setup.DefaultLauncherController

private const val LAST_STEP = 3

/** Four skippable steps. No permissions are requested. */
@Composable
fun OnboardingScreen(
    settings: LauncherSettings,
    controller: DefaultLauncherController,
    onThemeChange: (ThemeMode) -> Unit,
    onFinish: () -> Unit,
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(24.dp),
        ) {
            Spacer(Modifier.weight(1f))
            when (step) {
                0 -> StepText("Welcome to My Launcher", "A fast, private home screen. No ads, no tracking, no special permissions.")
                1 -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    StepText(
                        "Set as default launcher",
                        if (controller.status.isDefault) "My Launcher is already your home app."
                        else "Make My Launcher your home app. Android will ask you to confirm, and you can switch back any time.",
                    )
                    Button(onClick = controller.request, enabled = !controller.status.isDefault) {
                        Text(if (controller.status.isDefault) "Done" else "Set as default")
                    }
                }
                2 -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    StepText("Choose a theme", "You can change this later in Settings.")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(ThemeMode.SYSTEM to "System", ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark")
                            .forEach { (mode, label) ->
                                FilterChip(
                                    selected = settings.theme == mode,
                                    onClick = { onThemeChange(mode) },
                                    label = { Text(label) },
                                )
                            }
                    }
                }
                else -> StepText(
                    "Make it yours",
                    "Press and hold the home screen to add favorite apps or open settings. " +
                        "Swipe up for all apps, swipe down to search.",
                )
            }
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onFinish) { Text("Skip") }
                Text("${step + 1} / ${LAST_STEP + 1}", style = MaterialTheme.typography.labelMedium)
                Button(onClick = { if (step >= LAST_STEP) onFinish() else step++ }) {
                    Text(if (step >= LAST_STEP) "Get started" else "Next")
                }
            }
        }
    }
}

@Composable
private fun StepText(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
