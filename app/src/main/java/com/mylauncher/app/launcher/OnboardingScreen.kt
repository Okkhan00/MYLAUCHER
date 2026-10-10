package com.mylauncher.app.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mylauncher.app.data.model.LauncherSettings
import com.mylauncher.app.data.model.SmartSettings
import com.mylauncher.app.data.model.ThemeMode
import com.mylauncher.app.launcher.setup.DefaultLauncherController
import com.mylauncher.app.ui.components.IconBadge
import com.mylauncher.app.ui.components.SettingChoice

private const val LAST_STEP = 3

/**
 * First-launch setup in four short steps: welcome, default launcher, two basic choices, finish.
 * Nothing is blocking (every step can be skipped), no permission is requested, and completion is
 * saved with the existing `onboardingDone` setting so it is shown only once.
 */
@Composable
fun OnboardingScreen(
    settings: LauncherSettings,
    smart: SmartSettings,
    controller: DefaultLauncherController,
    onThemeChange: (ThemeMode) -> Unit,
    onSmartChange: (SmartSettings) -> Unit,
    onFinish: () -> Unit,
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val isDefault = controller.status.isDefault

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 24.dp, vertical = 16.dp)) {
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
            ) {
                when (step) {
                    0 -> Step(
                        emoji = "🏠",
                        title = "Welcome to My Launcher",
                        body = "A fast, private home screen from Azi Creation. My Launcher replaces your phone's " +
                            "home screen and app drawer. No ads, no tracking and no special permissions.",
                    )
                    1 -> Step(
                        emoji = "📲",
                        title = "Make it your home app",
                        body = if (isDefault) {
                            "My Launcher is now your home app. Pressing the Home button opens it."
                        } else {
                            "Choose My Launcher as your home app so the Home button opens it. Android shows its own " +
                                "confirmation and you can switch back at any time. You can also skip this and do it " +
                                "later in Settings > General > Default launcher."
                        },
                    ) {
                        if (isDefault) {
                            Text(
                                "✓ Confirmed by Android",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            Button(
                                onClick = controller.request,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                            ) { Text("Set as default launcher") }
                            OutlinedButton(
                                onClick = controller.openSettings,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                            ) { Text("Open Android home app settings") }
                        }
                    }
                    2 -> Step(
                        emoji = "✨",
                        title = "Choose how it starts",
                        body = "Two simple choices. Everything can be changed later in Settings.",
                    ) {
                        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
                            Column {
                                SettingChoice(
                                    "Appearance",
                                    listOf(ThemeMode.SYSTEM to "System", ThemeMode.LIGHT to "Light", ThemeMode.DARK to "Dark"),
                                    settings.theme,
                                    onThemeChange,
                                )
                                SettingChoice(
                                    "Open the app drawer in",
                                    listOf(true to "Categories", false to "All Apps"),
                                    smart.groupDrawerByCategory,
                                    { onSmartChange(smart.copy(groupDrawerByCategory = it)) },
                                )
                            }
                        }
                    }
                    else -> Step(
                        emoji = "🎉",
                        title = "You're all set",
                        body = "Swipe up for your apps, swipe down to search, and press and hold the home screen to " +
                            "add favorites or open settings.",
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (i in 0..LAST_STEP) {
                    Box(
                        Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (i == step) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (i == step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                    )
                }
            }
            Text(
                "Step ${step + 1} of ${LAST_STEP + 1}",
                Modifier.fillMaxWidth().padding(top = 4.dp).semantics { contentDescription = "Step ${step + 1} of ${LAST_STEP + 1}" },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.size(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (step == 0) {
                    TextButton(onClick = onFinish, modifier = Modifier.heightIn(min = 48.dp)) { Text("Skip") }
                } else if (step < LAST_STEP) {
                    TextButton(onClick = { step-- }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Back") }
                } else {
                    Spacer(Modifier.size(1.dp))
                }
                Button(
                    onClick = { if (step >= LAST_STEP) onFinish() else step++ },
                    modifier = Modifier.heightIn(min = 52.dp),
                ) {
                    Text(
                        when {
                            step >= LAST_STEP -> "Open my home screen"
                            step == 1 && !isDefault -> "Not now"
                            else -> "Next"
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun Step(emoji: String, title: String, body: String, extra: (@Composable () -> Unit)? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        IconBadge(emoji, size = 72.dp)
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (extra != null) extra()
    }
}
