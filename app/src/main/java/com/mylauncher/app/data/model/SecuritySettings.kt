package com.mylauncher.app.data.model

/** Launcher lock configuration. The PIN itself is never stored, only a salted hash. */
data class SecuritySettings(
    val pinHash: String? = null,
    val pinSalt: String? = null,
    val biometricEnabled: Boolean = false,
    val protectSettings: Boolean = true,
    val protectHidden: Boolean = true,
) {
    val lockEnabled: Boolean get() = !pinHash.isNullOrEmpty() && !pinSalt.isNullOrEmpty()
}
