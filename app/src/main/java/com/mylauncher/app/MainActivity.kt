package com.mylauncher.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import com.mylauncher.app.launcher.LauncherApp
import com.mylauncher.app.launcher.LauncherViewModel

// FragmentActivity (a ComponentActivity subclass) is required by AndroidX BiometricPrompt.
class MainActivity : FragmentActivity() {
    private val viewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { LauncherApp(viewModel) }
    }

    // Widget configuration screens report back here (AppWidgetHost uses the classic result API).
    @Deprecated("Required by AppWidgetHost.startAppWidgetConfigureActivityForResult")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val container = (application as MyLauncherApplication).container
        if (!container.widgetController.onConfigureResult(requestCode, resultCode, data)) {
            @Suppress("DEPRECATION")
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    // Pressing Home while the launcher is already open delivers a new HOME intent.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) {
            viewModel.onHomePressed()
        }
    }
}
