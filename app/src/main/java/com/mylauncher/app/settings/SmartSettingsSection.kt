package com.mylauncher.app.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mylauncher.app.data.model.SmartSettings
import com.mylauncher.app.launcher.search.SearchEngine
import com.mylauncher.app.launcher.search.WebSearch
import com.mylauncher.app.ui.components.ChoiceRow
import com.mylauncher.app.ui.components.ClickableRow
import com.mylauncher.app.ui.components.SectionTitle
import com.mylauncher.app.ui.components.SwitchRow

/** "Smart" settings: search, suggestions, categories and insights. All of it stays on the device. */
@Composable
fun SmartSettingsSection(
    smart: SmartSettings,
    onChange: (SmartSettings) -> Unit,
    onOpenCategories: () -> Unit,
    onOpenInsights: () -> Unit,
) {
    var editCustomUrl by remember { mutableStateOf(false) }

    SectionTitle("Smart features")
    SwitchRow(
        "Smart suggestions", smart.smartSuggestions, { onChange(smart.copy(smartSuggestions = it)) },
        subtitle = "Suggest apps in the drawer based on the time of day. Uses only data on this device.",
    )
    SwitchRow(
        "Settings and actions in search", smart.searchSettingsShortcuts, { onChange(smart.copy(searchSettingsShortcuts = it)) },
        subtitle = "Find Wi-Fi, Bluetooth, battery and launcher screens by typing",
    )
    SwitchRow(
        "Web search", smart.webSearchEnabled, { onChange(smart.copy(webSearchEnabled = it)) },
        subtitle = "Offer \"Search the web\" when no app matches. Opens your browser.",
    )
    if (smart.webSearchEnabled) {
        ChoiceRow(
            "Search engine",
            SearchEngine.entries.map { it to it.label },
            smart.searchEngine,
        ) { onChange(smart.copy(searchEngine = it)) }
        if (smart.searchEngine == SearchEngine.CUSTOM) {
            val valid = WebSearch.isValidCustomTemplate(smart.customSearchUrl)
            ClickableRow(
                title = "Custom search address",
                subtitle = when {
                    smart.customSearchUrl.isBlank() -> "Not set. Web search is unavailable until you set one."
                    valid -> smart.customSearchUrl
                    else -> "Invalid address. It must start with https:// and contain %s"
                },
                onClick = { editCustomUrl = true },
            )
        }
    }
    ClickableRow(
        title = "App categories",
        subtitle = "Rename, hide, reorder and move apps between categories",
        onClick = onOpenCategories,
    )
    ClickableRow(
        title = "Usage insights",
        subtitle = "Most used and recently opened apps, today and this week",
        onClick = onOpenInsights,
    )

    if (editCustomUrl) {
        var text by remember { mutableStateOf(smart.customSearchUrl) }
        val valid = WebSearch.isValidCustomTemplate(text)
        AlertDialog(
            onDismissRequest = { editCustomUrl = false },
            title = { Text("Custom search address") },
            text = {
                Column {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it.take(300) },
                        singleLine = true,
                        isError = text.isNotBlank() && !valid,
                        label = { Text("https://example.com/search?q=%s") },
                    )
                    Text(
                        "Use %s where the search words go. Only http and https addresses are accepted.",
                        Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = valid,
                    onClick = { onChange(smart.copy(customSearchUrl = text.trim())); editCustomUrl = false },
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editCustomUrl = false }) { Text("Cancel") } },
        )
    }
}
