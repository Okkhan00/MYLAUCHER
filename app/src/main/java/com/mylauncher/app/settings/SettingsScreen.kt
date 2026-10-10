package com.mylauncher.app.settings

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mylauncher.app.data.model.SecuritySettings
import com.mylauncher.app.security.pin.PinHasher
import com.mylauncher.app.ui.components.ClickableRow
import com.mylauncher.app.ui.components.PinField
import com.mylauncher.app.ui.components.ScreenHeader
import com.mylauncher.app.ui.components.SectionTitle
import com.mylauncher.app.ui.components.SwitchRow

/** Launcher lock settings. The PIN only protects launcher-controlled areas, never the whole phone. */
@Composable
fun SecurityScreen(
    security: SecuritySettings,
    biometricAvailable: Boolean,
    onSetPin: (String) -> Unit,
    onRemovePin: () -> Unit,
    onOptionsChange: (biometric: Boolean, protectSettings: Boolean, protectHidden: Boolean) -> Unit,
    onBack: () -> Unit,
) {
    var showPinDialog by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            ScreenHeader("Launcher lock", onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Text(
                    "The launcher lock protects launcher settings, customization and hidden apps. " +
                        "It does not lock your phone or other apps; use Android's screen lock for that.",
                    Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (!security.lockEnabled) {
                    Button(onClick = { showPinDialog = true }, modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text("Set a PIN")
                    }
                } else {
                    SectionTitle("PIN")
                    ClickableRow("Change PIN", onClick = { showPinDialog = true })
                    ClickableRow("Remove PIN and turn lock off", onClick = { showRemoveDialog = true })

                    SectionTitle("Unlock options")
                    SwitchRow(
                        title = "Use biometrics",
                        checked = security.biometricEnabled && biometricAvailable,
                        onCheckedChange = {
                            if (biometricAvailable) {
                                onOptionsChange(it, security.protectSettings, security.protectHidden)
                            }
                        },
                        subtitle = if (biometricAvailable) "Fingerprint or face, with PIN as backup"
                        else "Not available on this device",
                    )

                    SectionTitle("Protect")
                    SwitchRow(
                        "Launcher settings", security.protectSettings,
                        { onOptionsChange(security.biometricEnabled, it, security.protectHidden) },
                    )
                    SwitchRow(
                        "Hidden apps", security.protectHidden,
                        { onOptionsChange(security.biometricEnabled, security.protectSettings, it) },
                    )
                    Text(
                        "This screen is always protected while a PIN is set. The unlock lasts until you " +
                            "return to the home screen or leave the launcher.",
                        Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (showPinDialog) {
        PinSetupDialog(
            title = if (security.lockEnabled) "Change PIN" else "Set a PIN",
            onConfirm = onSetPin,
            onDismiss = { showPinDialog = false },
        )
    }
    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Remove PIN?") },
            text = { Text("Settings and hidden apps will no longer be protected.") },
            confirmButton = {
                TextButton(onClick = { onRemovePin(); showRemoveDialog = false }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { showRemoveDialog = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun PinSetupDialog(title: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    val valid = PinHasher.isValidPin(pin)
    val matches = pin == repeat
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Use ${PinHasher.MIN_LENGTH} to ${PinHasher.MAX_LENGTH} digits.")
                PinField(pin, { pin = it }, "New PIN")
                PinField(repeat, { repeat = it }, "Repeat PIN")
                if (repeat.isNotEmpty() && !matches) {
                    Text("PINs do not match", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid && matches, onClick = { onConfirm(pin); onDismiss() }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
