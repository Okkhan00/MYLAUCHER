package com.mylauncher.app.backup

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Backups are a ZIP with one entry, backup.json. Nothing is ever extracted to disk. */
object BackupZip {
    const val ENTRY_NAME = "backup.json"
    const val FILE_NAME = "mylauncher-backup.zip"
    private const val MAX_BYTES = 2_000_000 // generous cap on the *uncompressed* JSON (zip-bomb guard)

    fun write(out: OutputStream, json: String) {
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry(ENTRY_NAME))
            zip.write(json.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
    }

    /** Returns the JSON text or null when the file is not a valid backup archive. Never throws. */
    fun readJson(input: InputStream): String? = try {
        ZipInputStream(input).use { zip ->
            var found: String? = null
            var entries = 0
            while (true) {
                val entry = zip.nextEntry ?: break
                if (++entries > 8) return null
                if (entry.name == ENTRY_NAME && !entry.isDirectory) {
                    val buffer = ByteArrayOutputStream()
                    val chunk = ByteArray(8192)
                    var total = 0
                    while (true) {
                        val n = zip.read(chunk)
                        if (n < 0) break
                        total += n
                        if (total > MAX_BYTES) return null
                        buffer.write(chunk, 0, n)
                    }
                    found = buffer.toString(Charsets.UTF_8.name())
                }
            }
            found
        }
    } catch (e: Exception) {
        null
    }
}
