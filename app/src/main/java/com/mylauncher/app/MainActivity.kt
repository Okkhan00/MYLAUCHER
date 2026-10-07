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

    // Pressing Home while the launcher is already open delivers a new HOME intent.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) {
            viewModel.onHomePressed()
        }
    }
}
