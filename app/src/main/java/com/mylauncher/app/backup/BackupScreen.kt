package com.mylauncher.app.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mylauncher.app.ui.components.ScreenHeader

/**
 * Export and import use Android's file pickers (Storage Access Framework), so no storage
 * permission is needed. Importing asks for confirmation because it replaces current settings.
 */
@Composable
fun BackupScreen(
    status: BackupStatus,
    onExport: (Uri) -> Unit,
    onImport: (Uri) -> Unit,
    onClearStatus: () -> Unit,
    onBack: () -> Unit,
) {
    var confirmImport by remember { mutableStateOf<Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) onExport(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) confirmImport = uri
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            ScreenHeader("Backup and restore", onBack)
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "A backup is a small ZIP file with your launcher layout and settings: favorites, folders, " +
                        "private and locked app lists, categories, theme, wallpaper options, gestures, search, grid, " +
                        "clock, drawer, suggestions and performance settings.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Not included: your PIN or any password, usage history, and widgets (they belong to this " +
                        "device). After restoring on a new phone, set a launcher PIN again for App lock to work.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = { onClearStatus(); exportLauncher.launch(BackupZip.FILE_NAME) },
                    enabled = status !is BackupStatus.Working,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Export backup") }
                OutlinedButton(
                    onClick = {
                        onClearStatus()
                        importLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
                    },
                    enabled = status !is BackupStatus.Working,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Import backup") }

                when (status) {
                    BackupStatus.Idle -> Unit
                    BackupStatus.Working -> CircularProgressIndicator()
                    is BackupStatus.Done -> Text(
                        status.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (status.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }

    confirmImport?.let { uri ->
        AlertDialog(
            onDismissRequest = { confirmImport = null },
            title = { Text("Restore this backup?") },
            text = { Text("Your current launcher layout and settings will be replaced. Your PIN is not changed.") },
            confirmButton = { TextButton(onClick = { onImport(uri); confirmImport = null }) { Text("Restore") } },
            dismissButton = { TextButton(onClick = { confirmImport = null }) { Text("Cancel") } },
        )
    }
}
