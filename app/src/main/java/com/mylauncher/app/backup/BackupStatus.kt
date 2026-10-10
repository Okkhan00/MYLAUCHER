package com.mylauncher.app.backup

sealed interface BackupStatus {
    data object Idle : BackupStatus
    data object Working : BackupStatus
    data class Done(val message: String, val success: Boolean) : BackupStatus
}
