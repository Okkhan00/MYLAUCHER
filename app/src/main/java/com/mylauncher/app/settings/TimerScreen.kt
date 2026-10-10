package com.mylauncher.app.settings

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.foundation.text.KeyboardOptions
import com.mylauncher.app.data.model.AppInfo
import com.mylauncher.app.security.pin.PinHasher
import com.mylauncher.app.timer.AppTimer
import com.mylauncher.app.timer.TimerEngine
import com.mylauncher.app.timer.TimerRules
import com.mylauncher.app.timer.TimerState
import com.mylauncher.app.timer.UsageAccess
import com.mylauncher.app.ui.components.AppIcon
import com.mylauncher.app.ui.components.PinField
import com.mylauncher.app.ui.components.ScreenHeader
import com.mylauncher.app.ui.components.SettingDivider
import com.mylauncher.app.ui.components.SettingNote
import com.mylauncher.app.ui.components.SettingRow
import com.mylauncher.app.ui.components.SettingSwitch
import com.mylauncher.app.ui.components.SettingsGroup

private fun formatMinutes(m: Int): String = when {
    m < 60 -> "$m min"
    m % 60 == 0 -> "${m / 60} h"
    else -> "${m / 60} h ${m % 60} min"
}

private fun formatRemaining(ms: Long): String {
    val minutes = ((ms + 59_999L) / 60_000L).toInt()
    return if (minutes <= 0) "less than a minute" else formatMinutes(minutes)
}

/** Settings for the per-app usage timer. Only reachable after the timer PIN (once one exists). */
@Composable
fun TimerScreen(
    state: TimerState,
    apps: List<AppInfo>,
    biometricAvailable: Boolean,
    loadStatuses: suspend () -> Map<String, TimerEngine.Evaluation>,
    onSetEnabled: (Boolean) -> Unit,
    onSetPin: (String) -> Unit,
    onRemovePin: () -> Unit,
    onSetBiometric: (Boolean) -> Unit,
    onSaveTimer: (packageName: String, minutes: Int, renewDaily: Boolean) -> Unit,
    onRemoveTimer: (String) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var usageGranted by remember { mutableStateOf(UsageAccess.isGranted(context)) }
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        usageGranted = UsageAccess.isGranted(context)
        tick++
    }

    var statuses by remember { mutableStateOf<Map<String, TimerEngine.Evaluation>>(emptyMap()) }
    LaunchedEffect(state.timers, usageGranted, tick) {
        statuses = if (usageGranted && state.timers.isNotEmpty()) loadStatuses() else emptyMap()
    }

    val byPackage = remember(apps) { apps.associateBy { it.packageName } }

    var showUsageExplainer by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var confirmRemovePin by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Pair<AppInfo, AppTimer?>?>(null) }
    var notificationsAllowed by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notificationsAllowed = it
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            ScreenHeader("App usage timer", onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                Text(
                    "Set a daily time limit for chosen apps, for example 60 minutes for a video app. " +
                        "Everything stays on this phone.",
                    Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                SettingsGroup("Timer PIN", "🔑") {
                    SettingRow(
                        if (state.pinSet) "Change timer PIN" else "Set a timer PIN",
                        onClick = { showPinDialog = true },
                        subtitle = if (state.pinSet) {
                            "Only whoever knows this PIN can change timers or give more time"
                        } else {
                            "Required first. Choose a PIN your child does not know. It is separate from your phone's PIN."
                        },
                        value = if (state.pinSet) "Set" else "Not set",
                    )
                    if (state.pinSet) {
                        SettingDivider()
                        SettingRow(
                            "Remove timer PIN",
                            onClick = { confirmRemovePin = true },
                            subtitle = "This also turns all timers off",
                        )
                        if (biometricAvailable) {
                            SettingDivider()
                            SettingSwitch(
                                "Allow fingerprint for parent verification",
                                state.biometric,
                                onSetBiometric,
                                subtitle = "Any fingerprint saved on this phone will work, including a child's. Leave off if unsure.",
                            )
                        }
                    }
                }

                SettingsGroup("Timers", "⏱️") {
                    SettingSwitch(
                        "Turn timers on",
                        state.enabled && state.pinSet,
                        { if (state.pinSet) onSetEnabled(it) },
                        subtitle = if (state.pinSet) "Limits only apply to the apps you list below" else "Set a timer PIN first",
                    )
                    SettingDivider()
                    SettingRow(
                        "Usage Access",
                        onClick = { if (usageGranted) openUsageSettings(context) else showUsageExplainer = true },
                        subtitle = if (usageGranted) {
                            "Allowed. My Launcher can measure how long each limited app is on screen."
                        } else {
                            "Not allowed. Without it, time cannot be measured and no app is blocked."
                        },
                        value = if (usageGranted) "On" else "Off",
                    )
                    if (Build.VERSION.SDK_INT >= 33) {
                        SettingDivider()
                        SettingRow(
                            "Time-up reminder",
                            onClick = {
                                if (!notificationsAllowed) notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            },
                            subtitle = "A quiet notification when an app's time ends while it is open",
                            value = if (notificationsAllowed) "On" else "Allow",
                        )
                    }
                }
                if (state.enabled && state.pinSet && !usageGranted && state.timers.isNotEmpty()) {
                    SettingNote("Warning: timers are listed but Usage Access is off, so nothing is being limited right now.")
                }

                SettingsGroup("Limited apps", "📋") {
                    if (state.timers.isEmpty()) {
                        Text(
                            "No apps yet. Tap \"Add an app\" to choose one.",
                            Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    state.timers.forEach { timer ->
                        val app = byPackage[timer.packageName]
                        val e = statuses[timer.packageName]
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 64.dp)
                                .clickable(enabled = app != null) { editing = app?.let { it to timer } }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            if (app != null) AppIcon(app, 40.dp)
                            Column(Modifier.weight(1f)) {
                                Text(app?.label ?: timer.packageName, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    buildString {
                                        append("Limit ${formatMinutes(timer.limitMinutes)}")
                                        append(if (timer.renewDaily) " per day" else " until a parent resets it")
                                        when {
                                            app == null -> append(" · app not installed")
                                            e == null -> {}
                                            e.status.exhausted -> append(" · time is up")
                                            else -> append(" · ${formatRemaining(e.status.remainingMs)} left")
                                        }
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (app == null) {
                                TextButton(onClick = { onRemoveTimer(timer.packageName) }) { Text("Remove") }
                            }
                        }
                        SettingDivider()
                    }
                    SettingRow(
                        "Add an app",
                        onClick = { showPicker = true },
                        subtitle = "Only the apps you choose are limited. My Launcher itself cannot be limited.",
                    )
                }

                SettingNote(
                    "What this can and cannot do: the limit is enforced when an app is opened from My Launcher " +
                        "(home, drawer, search). Android does not let a launcher close another app, so if the app is " +
                        "already open, or is opened from recent apps, a notification or another launcher, My Launcher can " +
                        "only send a reminder. Turning the phone's default launcher to another one, force-stopping or " +
                        "uninstalling My Launcher also ends the limits. A forgotten timer PIN cannot be recovered; " +
                        "clearing My Launcher's data removes it.",
                )
            }
        }
    }

    if (showUsageExplainer) {
        AlertDialog(
            onDismissRequest = { showUsageExplainer = false },
            title = { Text("Allow Usage Access?") },
            text = {
                Text(
                    "To count how long an app is open, Android requires you to allow \"Usage Access\" for My Launcher " +
                        "on the next screen. My Launcher only reads this on the phone, only for the apps you limit, " +
                        "and never sends it anywhere. You can turn it off there at any time.",
                )
            },
            confirmButton = {
                TextButton(onClick = { showUsageExplainer = false; openUsageSettings(context) }) { Text("Continue") }
            },
            dismissButton = { TextButton(onClick = { showUsageExplainer = false }) { Text("Not now") } },
        )
    }

    if (confirmRemovePin) {
        AlertDialog(
            onDismissRequest = { confirmRemovePin = false },
            title = { Text("Remove the timer PIN?") },
            text = { Text("All timers will be turned off. Your list of apps and limits is kept.") },
            confirmButton = { TextButton(onClick = { onRemovePin(); confirmRemovePin = false }) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { confirmRemovePin = false }) { Text("Cancel") } },
        )
    }

    if (showPinDialog) {
        TimerPinDialog(
            changing = state.pinSet,
            onSave = { onSetPin(it); showPinDialog = false },
            onDismiss = { showPinDialog = false },
        )
    }

    if (showPicker) {
        val taken = remember(state.timers) { state.timers.map { it.packageName }.toSet() }
        AppPickerDialog(
            apps = apps.filter { it.packageName !in taken },
            ownPackageName = context.packageName,
            onPick = { showPicker = false; editing = it to null },
            onDismiss = { showPicker = false },
        )
    }

    editing?.let { (app, existing) ->
        TimerEditDialog(
            app = app,
            existing = existing,
            onSave = { minutes, renew -> onSaveTimer(app.packageName, minutes, renew); editing = null },
            onRemove = { onRemoveTimer(app.packageName); editing = null },
            onDismiss = { editing = null },
        )
    }
}

private fun openUsageSettings(context: android.content.Context) {
    try {
        context.startActivity(UsageAccess.settingsIntent().addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        android.widget.Toast.makeText(context, "This phone has no Usage Access screen.", android.widget.Toast.LENGTH_LONG).show()
    }
}

@Composable
private fun TimerPinDialog(changing: Boolean, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    // Plain remember on purpose: PINs must never be written to saved instance state.
    var pin by remember { mutableStateOf("") }
    var again by remember { mutableStateOf("") }
    val valid = PinHasher.isValidPin(pin)
    val matches = pin == again
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (changing) "Change timer PIN" else "Set a timer PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Use 4 to 8 digits. Do not use your phone's PIN, and do not share it with your child.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                PinField(pin, { pin = it }, "New timer PIN")
                PinField(again, { again = it }, "Repeat the PIN")
                if (again.isNotEmpty() && !matches) Text("The PINs do not match.", color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(pin) }, enabled = valid && matches) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun AppPickerDialog(apps: List<AppInfo>, ownPackageName: String, onPick: (AppInfo) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val shown = remember(apps, query) {
        apps.filter { it.packageName != ownPackageName && it.label.contains(query.trim(), ignoreCase = true) }
            .sortedBy { it.label.lowercase() }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose an app") },
        text = {
            Column {
                OutlinedTextField(query, { query = it }, label = { Text("Search") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(shown, key = { it.packageName }) { app ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable { onPick(app) }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            AppIcon(app, 36.dp)
                            Text(app.label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun TimerEditDialog(
    app: AppInfo,
    existing: AppTimer?,
    onSave: (minutes: Int, renewDaily: Boolean) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    var minutes by remember { mutableIntStateOf(existing?.limitMinutes ?: 60) }
    var custom by remember { mutableStateOf(if (existing != null && existing.limitMinutes !in TimerRules.PRESET_MINUTES) "${existing.limitMinutes}" else "") }
    var renew by remember { mutableStateOf(existing?.renewDaily ?: true) }
    val effective = custom.toIntOrNull() ?: minutes
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(app.label) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Allowed time", style = MaterialTheme.typography.labelLarge)
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimerRules.PRESET_MINUTES.forEach { m ->
                        FilterChip(
                            selected = custom.isEmpty() && minutes == m,
                            onClick = { minutes = m; custom = "" },
                            label = { Text(formatMinutes(m)) },
                        )
                    }
                }
                OutlinedTextField(
                    value = custom,
                    onValueChange = { custom = it.filter(Char::isDigit).take(4) },
                    label = { Text("Or custom minutes (1–1440)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    Modifier.fillMaxWidth().clickable { renew = !renew },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Start again every morning", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Switch(renew, onCheckedChange = null)
                }
                if (existing != null) TextButton(onClick = onRemove) { Text("Remove this limit") }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(effective, renew) }, enabled = TimerRules.isValidLimit(effective)) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
