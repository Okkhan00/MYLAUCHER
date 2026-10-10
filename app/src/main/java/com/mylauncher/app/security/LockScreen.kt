package com.mylauncher.app.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.mylauncher.app.security.biometric.BiometricHelper
import com.mylauncher.app.security.biometric.findFragmentActivity
import com.mylauncher.app.security.pin.PinAttempt
import com.mylauncher.app.ui.components.PinField
import kotlinx.coroutines.launch

/** Shown instead of a protected screen while the launcher lock is active. */
@Composable
fun LockScreen(
    biometricEnabled: Boolean,
    biometricAvailable: Boolean,
    submitPin: suspend (String) -> PinAttempt,
    onBiometricSuccess: () -> Unit,
    onCancel: () -> Unit,
    title: String = "Launcher locked",
    message: String = "Enter your PIN to open launcher settings and hidden apps. This does not lock your phone.",
    cancelLabel: String = "Back to home",
    biometricTitle: String = "Unlock launcher",
    biometricSubtitle: String = "Confirm it's you to open protected launcher areas",
    pinLabel: String = "PIN",
    submitLabel: String = "Unlock",
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Deliberately not rememberSaveable: the PIN must not end up in saved instance state.
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val canUseBiometrics = biometricEnabled && biometricAvailable

    fun promptBiometric() {
        val activity = context.findFragmentActivity() ?: return
        BiometricHelper.prompt(
            activity = activity,
            title = biometricTitle,
            subtitle = biometricSubtitle,
            negativeText = "Use PIN",
            onSuccess = onBiometricSuccess,
        )
    }

    fun submit() {
        if (pin.isEmpty()) return
        val entered = pin
        scope.launch {
            val attempt = submitPin(entered)
            if (!attempt.success) {
                error = if (attempt.blockedSeconds > 0) {
                    "Too many attempts. Try again in ${attempt.blockedSeconds}s."
                } else {
                    "Wrong PIN"
                }
                pin = ""
            }
        }
    }

    LaunchedEffect(Unit) {
        if (canUseBiometrics) promptBiometric()
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, androidx.compose.ui.Alignment.CenterVertically),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PinField(
                value = pin,
                onValueChange = { pin = it; error = null },
                label = pinLabel,
                imeAction = ImeAction.Done,
                keyboardActions = KeyboardActions(onDone = { submit() }),
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = { submit() }, modifier = Modifier.fillMaxWidth()) { Text(submitLabel) }
            if (canUseBiometrics) {
                OutlinedButton(onClick = { promptBiometric() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Use biometrics")
                }
            }
            TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text(cancelLabel) }
        }
    }
}
