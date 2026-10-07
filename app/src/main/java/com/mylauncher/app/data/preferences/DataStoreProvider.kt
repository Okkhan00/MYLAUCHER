package com.mylauncher.app.data.preferences

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore

/** Single DataStore for the whole launcher. A corrupted file is replaced by an empty one. */
val Context.launcherDataStore by preferencesDataStore(
    name = "launcher_prefs",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)
