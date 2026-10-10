package com.mylauncher.app.timer

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mylauncher.app.data.model.AppInfo
import com.mylauncher.app.security.LockScreen
import com.mylauncher.app.security.pin.PinAttempt
import com.mylauncher.app.ui.components.AppIcon

/**
 * Shown instead of opening an app whose allowed time is used up. Three calm steps: the notice, the
 * parent's timer PIN (or biometrics), then a choice between a fresh allowance and a few extra minutes.
 *
 * The step is kept with plain `remember`, not saved state: if the screen is recreated it starts again at
 * the notice, so verification never survives a restart.
 */
@Composable
fun TimerExpiredScreen(
    app: AppInfo,
    biometricEnabled: Boolean,
    biometricAvailable: Boolean,
    submitPin: suspend (String) -> PinAttempt,
    onReset: () -> Unit,
    onGrantExtra: (minutes: Int) -> Unit,
    onClose: () -> Unit,
) {
    var step by remember { mutableStateOf(Step.NOTICE) }
    when (step) {
        Step.NOTICE -> Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AppIcon(app, 72.dp)
                Text(app.label, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Text(
                    "Your allowed usage time for this app has ended.",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "Ask the authorized parent to verify to continue.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Button(onClick = { step = Step.VERIFY }, modifier = Modifier.fillMaxWidth()) { Text("Parent verification") }
                TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Back to home") }
            }
        }

        Step.VERIFY -> LockScreen(
            biometricEnabled = biometricEnabled,
            biometricAvailable = biometricAvailable,
            submitPin = { pin ->
                val attempt = submitPin(pin)
                if (attempt.success) step = Step.CHOOSE
                attempt
            },
            onBiometricSuccess = { step = Step.CHOOSE },
            onCancel = { step = Step.NOTICE },
            title = "Parent verification",
            message = "Enter the timer PIN set in My Launcher. This is not the phone's lock-screen PIN, and the " +
                "phone's own unlock screen does not count as verification.",
            cancelLabel = "Back",
            biometricTitle = "Parent verification",
            biometricSubtitle = "Fingerprints enrolled on this phone are accepted, so use the PIN if a child could pass.",
            pinLabel = "Timer PIN",
            submitLabel = "Verify",
        )

        Step.CHOOSE -> Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            ) {
                Text("Verified", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "How should ${app.label} continue?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onReset, modifier = Modifier.fillMaxWidth()) { Text("Reset timer (full allowance)") }
                Text(
                    "Starts the full allowance again. The timer stays on for next time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TimerRules.EXTRA_MINUTES.forEach { minutes ->
                    OutlinedButton(onClick = { onGrantExtra(minutes) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Add $minutes minutes only")
                    }
                }
                Text(
                    "Adds just a little time on top of today's allowance.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Not now") }
            }
        }
    }
}

private enum class Step { NOTICE, VERIFY, CHOOSE }
