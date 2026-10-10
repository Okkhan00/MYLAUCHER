package com.mylauncher.app.launcher.setup

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect

class DefaultLauncherController(
    val status: DefaultLauncher.Status,
    val request: () -> Unit,
    val openSettings: () -> Unit,
)

/**
 * Keeps the default-launcher status in sync. Status is re-read after the system dialog
 * returns and whenever the app resumes, so success is only shown once Android confirms it.
 */
@Composable
fun rememberDefaultLauncherController(): DefaultLauncherController {
    val context = LocalContext.current
    var status by remember { mutableStateOf(DefaultLauncher.status(context)) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        status = DefaultLauncher.status(context)
    }

    LifecycleResumeEffect(Unit) {
        status = DefaultLauncher.status(context)
        onPauseOrDispose { }
    }

    return DefaultLauncherController(
        status = status,
        request = {
            val intent = DefaultLauncher.createRoleRequestIntent(context)
            if (intent == null) {
                DefaultLauncher.openHomeSettings(context)
            } else {
                try {
                    launcher.launch(intent)
                } catch (e: ActivityNotFoundException) {
                    DefaultLauncher.openHomeSettings(context)
                }
            }
        },
        openSettings = { DefaultLauncher.openHomeSettings(context) },
    )
}
