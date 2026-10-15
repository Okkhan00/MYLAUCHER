package com.mylauncher.app.settings

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.mylauncher.app.data.model.AppInfo
import com.mylauncher.app.icons.CustomIconDef
import com.mylauncher.app.icons.CustomIconRenderer
import com.mylauncher.app.icons.CustomIconStore
import com.mylauncher.app.icons.CustomShape
import com.mylauncher.app.icons.IconPackInfo
import com.mylauncher.app.icons.IconPackState
import com.mylauncher.app.icons.IconSourceKind
import com.mylauncher.app.icons.IconStudioMath
import com.mylauncher.app.icons.PictureImport
import com.mylauncher.app.icons.PictureResult
import com.mylauncher.app.ui.components.AppIcon
import com.mylauncher.app.ui.components.ScreenHeader
import com.mylauncher.app.ui.components.SettingDivider
import com.mylauncher.app.ui.components.SettingsGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ---------------------------------------------------------------------------------------------
// Icon packs
// ---------------------------------------------------------------------------------------------

/**
 * Pick an installed icon pack. The status card only says "applied" when the icon pipeline has really
 * switched to the pack ([IconPackState.applied]); otherwise it says why not.
 */
@Composable
fun IconPacksScreen(
    state: IconPackState,
    discover: () -> List<IconPackInfo>,
    onSelect: (String?) -> Unit,
    onBack: () -> Unit,
) {
    var rescan by remember { mutableIntStateOf(0) }
    var packs by remember { mutableStateOf<List<IconPackInfo>?>(null) }
    LaunchedEffect(rescan) { packs = withContext(Dispatchers.IO) { discover() } }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            ScreenHeader("Icon packs", onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                val status = when {
                    state.desired == null -> "Original app icons are shown."
                    state.applied -> "Applied: ${state.label ?: state.desired}. ${state.mappedApps} icons in the pack. " +
                        "Apps the pack does not cover keep their original icon."
                    else -> "Not applied: ${state.message ?: "this icon pack could not be used"}. Original icons are shown."
                }
                Text(
                    status,
                    Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (state.desired != null && !state.applied) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface,
                )
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onSelect(null) },
                        enabled = state.desired != null,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) { Text("Use original icons") }
                    OutlinedButton(onClick = { rescan++ }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Rescan") }
                }

                val list = packs
                SettingsGroup("Installed icon packs", "🎭") {
                    when {
                        list == null -> Text("Looking for icon packs…", Modifier.padding(16.dp))
                        list.isEmpty() -> Text(
                            "No compatible icon packs found. Install an icon pack app that supports the common " +
                                "launcher icon pack format, then tap Rescan.",
                            Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        else -> list.forEachIndexed { index, pack ->
                            if (index > 0) SettingDivider()
                            PackRow(pack, selected = pack.packageName == state.desired, onApply = { onSelect(pack.packageName) })
                        }
                    }
                }
                Text(
                    "Supported: icon packs that use the widespread appfilter mapping (the format used by Nova, ADW and " +
                        "similar launchers). Packs in other formats are not supported and are reported here. " +
                        "Your own custom icons always come first, then the pack, then the original icon.",
                    Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PackRow(pack: IconPackInfo, selected: Boolean, onApply: () -> Unit) {
    val context = LocalContext.current
    val icon: ImageBitmap? = remember(pack.packageName) {
        try {
            context.packageManager.getApplicationIcon(pack.packageName).toBitmap(96, 96).asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (icon != null) Image(icon, contentDescription = null, modifier = Modifier.size(44.dp))
        else Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer))
        Column(Modifier.weight(1f)) {
            Text(pack.label, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            Text(
                if (selected) "Selected" else pack.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        if (selected) Text("✓", style = MaterialTheme.typography.titleMedium)
        else Button(onClick = onApply, modifier = Modifier.heightIn(min = 40.dp)) { Text("Apply") }
    }
}

// ---------------------------------------------------------------------------------------------
// Custom icons
// ---------------------------------------------------------------------------------------------

private val backgroundChoices = listOf(
    0 to "None",
    0xFFFFFFFF.toInt() to "White",
    0xFF1B1B1F.toInt() to "Black",
    0xFF3F6BFF.toInt() to "Blue",
    0xFF2E9E6B.toInt() to "Green",
    0xFFE5484D.toInt() to "Red",
    0xFFF5A524.toInt() to "Amber",
)

/**
 * Custom Icon Studio: choose an app, pick a picture, crop it to a square, style it, save it.
 * Reset returns the app to the icon pack's icon (if it has one) or its original icon.
 */
@Composable
fun CustomIconsScreen(
    apps: List<AppInfo>,
    defs: Map<String, CustomIconDef>,
    store: CustomIconStore,
    sourceOf: (AppInfo) -> IconSourceKind,
    packLabel: String?,
    onSave: (AppInfo, Bitmap, CustomIconDef, (Boolean) -> Unit) -> Unit,
    onReset: (AppInfo) -> Unit,
    onResetAll: () -> Unit,
    onExport: (Uri, (Int) -> Unit) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var editing by remember { mutableStateOf<AppInfo?>(null) }
    var query by remember { mutableStateOf("") }
    var confirmResetAll by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) {
            onExport(uri) { count ->
                val message = if (count >= 0) "Exported $count custom icons" else "Export failed"
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    BackHandler(enabled = editing != null) { editing = null }

    val app = editing
    if (app != null) {
        IconEditor(
            app = app,
            existing = defs[app.packageName],
            store = store,
            onSave = onSave,
            onClose = { editing = null },
        )
        return
    }

    val shown = remember(apps, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) apps else apps.filter { it.label.lowercase().contains(q) }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            ScreenHeader("Custom icons", onBack)
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search apps") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { exportLauncher.launch("my-launcher-custom-icons.zip") },
                    enabled = defs.isNotEmpty(),
                    modifier = Modifier.heightIn(min = 44.dp),
                ) { Text("Export") }
                OutlinedButton(
                    onClick = { confirmResetAll = true },
                    enabled = defs.isNotEmpty(),
                    modifier = Modifier.heightIn(min = 44.dp),
                ) { Text("Reset all") }
            }
            Text(
                "Export saves your own icons and their styles as a My Launcher backup file. It is not an installable " +
                    "icon pack for other launchers.",
                Modifier.padding(horizontal = 20.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (shown.isEmpty()) {
                Text(
                    if (apps.isEmpty()) "No apps to show yet." else "No app matches \"$query\".",
                    Modifier.padding(20.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LazyColumn(Modifier.weight(1f)) {
                items(shown, key = { it.packageName + "/" + it.activityName }) { item ->
                    val source = sourceOf(item)
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        AppIcon(item, 44.dp)
                        Column(Modifier.weight(1f)) {
                            Text(item.label, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                            Text(
                                when (source) {
                                    IconSourceKind.CUSTOM -> "Custom icon"
                                    IconSourceKind.PACK -> "From icon pack" + (packLabel?.let { ": $it" } ?: "")
                                    IconSourceKind.ORIGINAL -> "Original app icon"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (source == IconSourceKind.CUSTOM) {
                            TextButton(onClick = { onReset(item) }) { Text("Reset") }
                        }
                        TextButton(onClick = { editing = item }) { Text("Edit") }
                    }
                }
            }
        }
    }

    if (confirmResetAll) {
        AlertDialog(
            onDismissRequest = { confirmResetAll = false },
            title = { Text("Reset all custom icons?") },
            text = { Text("Your custom icons are deleted. Apps go back to the icon pack icon or the original icon.") },
            confirmButton = { TextButton(onClick = { confirmResetAll = false; onResetAll() }) { Text("Reset all") } },
            dismissButton = { TextButton(onClick = { confirmResetAll = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun IconEditor(
    app: AppInfo,
    existing: CustomIconDef?,
    store: CustomIconStore,
    onSave: (AppInfo, Bitmap, CustomIconDef, (Boolean) -> Unit) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var source by remember { mutableStateOf<Bitmap?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }
    var shape by remember { mutableStateOf(existing?.shape ?: CustomShape.ROUNDED) }
    var background by remember { mutableIntStateOf(existing?.background ?: 0) }
    var padding by remember { mutableFloatStateOf((existing?.paddingPercent ?: 0).toFloat()) }
    var saving by remember { mutableStateOf(false) }

    // An icon that was saved before is opened again with its stored picture.
    LaunchedEffect(app.packageName) {
        if (existing != null && source == null) source = withContext(Dispatchers.IO) { store.load(app.packageName) }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                when (val result = withContext(Dispatchers.IO) { PictureImport.load(context, uri) }) {
                    is PictureResult.Ok -> {
                        source = result.bitmap
                        zoom = 1f; panX = 0f; panY = 0f
                        message = null
                    }
                    is PictureResult.Error -> message = result.message
                }
            }
        }
    }

    val def = CustomIconDef(
        shape = shape,
        background = background,
        paddingPercent = padding.toInt(),
        version = (existing?.version ?: 0) + 1,
    )
    val preview: ImageBitmap? = remember(source, zoom, panX, panY, shape, background, padding) {
        val src = source
        if (src == null) null else try {
            val square = PictureImport.cropSquare(src, zoom, panX, panY, outputSide = 256)
            CustomIconRenderer.render(square, def, 192).asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            ScreenHeader("Icon for ${app.label}", onClose)
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(160.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    if (preview != null) {
                        Image(preview, contentDescription = "Icon preview", modifier = Modifier.size(144.dp))
                    } else {
                        Text(
                            "Choose a picture",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
                message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { Text(if (source == null) "Choose picture" else "Choose another picture") }

                if (source != null) {
                    Text("Crop to a square", style = MaterialTheme.typography.titleSmall)
                    LabeledSlider("Zoom", zoom, IconStudioMath.MIN_ZOOM..IconStudioMath.MAX_ZOOM) { zoom = it }
                    LabeledSlider("Move left / right", panX, -1f..1f) { panX = it }
                    LabeledSlider("Move up / down", panY, -1f..1f) { panY = it }
                }

                Text("Shape", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(CustomShape.CIRCLE to "Circle", CustomShape.ROUNDED to "Rounded", CustomShape.SQUARE to "Square")
                        .forEach { (value, label) ->
                            FilterChip(selected = shape == value, onClick = { shape = value }, label = { Text(label) })
                        }
                }
                Text("Background", style = MaterialTheme.typography.titleSmall)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    backgroundChoices.chunked(4).forEach { rowChoices ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowChoices.forEach { (value, label) ->
                                FilterChip(selected = background == value, onClick = { background = value }, label = { Text(label) })
                            }
                        }
                    }
                }
                LabeledSlider("Space around the picture (${padding.toInt()}%)", padding, 0f..CustomIconDef.MAX_PADDING.toFloat()) {
                    padding = it
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedButton(onClick = onClose, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cancel") }
                    Button(
                        enabled = source != null && !saving,
                        onClick = {
                            val src = source ?: return@Button
                            saving = true
                            scope.launch {
                                val cropped = withContext(Dispatchers.Default) {
                                    try { PictureImport.cropSquare(src, zoom, panX, panY) } catch (e: Exception) { null }
                                }
                                if (cropped == null) {
                                    saving = false
                                    message = "Can't use this picture"
                                } else {
                                    onSave(app, cropped, def) { ok ->
                                        saving = false
                                        if (ok) onClose() else message = "Could not save the icon"
                                    }
                                }
                            }
                        },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) { Text("Save icon") }
                }
            }
        }
    }
}

@Composable
private fun LabeledSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}
