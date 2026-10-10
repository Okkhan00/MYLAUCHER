package com.mylauncher.app.device

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.mylauncher.app.launcher.apps.AppActions
import com.mylauncher.app.ui.components.ClickableRow
import com.mylauncher.app.ui.components.ScreenHeader
import com.mylauncher.app.ui.components.SectionTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Local device facts plus shortcuts to official Android settings. Read once per visit, never polled. */
@Composable
fun DeviceDashboardScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var snapshot by remember { mutableStateOf<DeviceSnapshot?>(null) }
    var refresh by remember { mutableStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }
    LaunchedEffect(refresh) { snapshot = withContext(Dispatchers.IO) { DeviceReader.read(context) } }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            ScreenHeader("Device dashboard", onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                val info = snapshot
                if (info == null) {
                    Text("Reading device information...", Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                } else {
                    SectionTitle("Device")
                    Fact("Device", info.deviceName)
                    Fact("Android version", "${info.androidVersion} (API ${info.sdkInt})")
                    Fact("My Launcher version", info.appVersion)

                    SectionTitle("Battery")
                    val battery = info.battery
                    if (battery == null) {
                        Fact("Battery", "Not available")
                    } else {
                        Fact("Level", "${battery.percent}%")
                        Fact("State", if (battery.charging) "Charging" else "Not charging")
                        LinearProgressIndicator(
                            progress = { battery.percent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .semantics { contentDescription = "Battery ${battery.percent} percent" },
                        )
                    }

                    SectionTitle("Storage")
                    val storage = info.storage
                    if (storage == null) {
                        Fact("Storage", "Not available")
                    } else {
                        Fact("Available", DeviceFormat.bytes(storage.availableBytes))
                        Fact("Total", DeviceFormat.bytes(storage.totalBytes))
                        val used = DeviceFormat.percent(storage.usedBytes, storage.totalBytes)
                        if (used != null) {
                            Fact("Used", "$used%")
                            LinearProgressIndicator(
                                progress = { used / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .semantics { contentDescription = "Storage $used percent used" },
                            )
                        }
                    }

                    SectionTitle("Shortcuts")
                    ClickableRow(title = "Battery settings", onClick = {
                        AppActions.start(context, Intent(Intent.ACTION_POWER_USAGE_SUMMARY), "Can't open battery settings")
                    })
                    ClickableRow(title = "Storage settings", onClick = {
                        AppActions.start(context, Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS), "Can't open storage settings")
                    })
                    ClickableRow(title = "Android settings", onClick = {
                        AppActions.start(context, Intent(Settings.ACTION_SETTINGS), "Can't open settings")
                    })
                    ClickableRow(title = "My Launcher app info", onClick = { AppActions.openAppInfo(context, context.packageName) })

                    Text(
                        "This information is read on your device and shown here only. Nothing is stored or uploaded.",
                        Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun Fact(label: String, value: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$label: $value" },
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
