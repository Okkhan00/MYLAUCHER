package com.mylauncher.app

import com.mylauncher.app.backup.BackupCodec
import com.mylauncher.app.backup.BackupResult
import com.mylauncher.app.backup.BackupSchema
import com.mylauncher.app.backup.BackupZip
import com.mylauncher.app.backup.MiniJson
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupTest {
    private val sample: Map<String, Any> = mapOf(
        "theme" to "DARK",
        "columns" to 5,
        "show_labels" to false,
        "favorites" to "com.a\ncom.b\nfolder:f1",
        "folders" to "f1\tWork \"quoted\"\tcom.a,com.b",
        "theme_accent_color" to -13342775,
        "locked_apps" to "com.bank",
    )

    private fun ok(r: BackupResult) = r as BackupResult.Ok

    @Test fun roundTripPreservesEveryValueAndType() {
        val json = BackupCodec.encode(sample, 1234L, "0.2.0")
        val r = ok(BackupCodec.decode(json))
        assertEquals(1, r.version)
        assertEquals(1234L, r.createdAtMs)
        assertEquals("0.2.0", r.appVersion)
        assertEquals(sample, r.values)
    }

    @Test fun backupContainsAVersionForFutureMigrations() {
        assertTrue(BackupCodec.encode(sample, 1L, "x").contains("\"backupVersion\":1"))
    }

    @Test fun secretsAndUsageDataAreNeverExported() {
        val withSecrets = sample + mapOf(
            "pin_hash" to "abc", "pin_salt" to "def", "biometric_enabled" to true,
            "launch_stats" to "a\t1\t2", "launch_log" to "a\t5", "unknown_key" to "x",
        )
        val json = BackupCodec.encode(withSecrets, 1L, "x")
        listOf("pin_hash", "pin_salt", "biometric_enabled", "launch_stats", "launch_log", "unknown_key").forEach {
            assertFalse("$it leaked", json.contains("\"$it\""))
        }
        BackupSchema.neverExported.forEach { assertFalse(BackupSchema.keys.containsKey(it)) }
    }

    @Test fun handEditedSecretsAreIgnoredOnImport() {
        val json = """{"backupVersion":1,"prefs":{"pin_hash":{"t":"s","v":"evil"},"theme":{"t":"s","v":"LIGHT"}}}"""
        val r = ok(BackupCodec.decode(json))
        assertEquals(mapOf<String, Any>("theme" to "LIGHT"), r.values)
    }

    @Test fun unknownKeysAreIgnoredForForwardCompatibility() {
        val json = """{"backupVersion":1,"prefs":{"future_feature":{"t":"s","v":"x"},"columns":{"t":"i","v":6}}}"""
        assertEquals(mapOf<String, Any>("columns" to 6), ok(BackupCodec.decode(json)).values)
    }

    @Test fun malformedFilesAreRejectedNotCrashed() {
        val bad = listOf(
            "", "   ", "not json", "{", "[]", "null", "{\"backupVersion\":1}", "{\"prefs\":{}}",
            "{\"backupVersion\":\"1\",\"prefs\":{}}", "{\"backupVersion\":0,\"prefs\":{}}",
            "{\"backupVersion\":1.5,\"prefs\":{}}", "{\"backupVersion\":1,\"prefs\":[]}",
            "{\"backupVersion\":1,\"prefs\":{\"theme\":\"DARK\"}}",
            "{\"backupVersion\":1,\"prefs\":{\"theme\":{\"t\":\"b\",\"v\":true}}}",
            "{\"backupVersion\":1,\"prefs\":{\"columns\":{\"t\":\"i\",\"v\":\"6\"}}}",
            "{\"backupVersion\":1,\"prefs\":{\"columns\":{\"t\":\"i\",\"v\":1.5}}}",
            "{\"backupVersion\":1,\"prefs\":{\"columns\":{\"t\":\"i\",\"v\":99999999999}}}",
            "{\"backupVersion\":1,\"prefs\":{}} trailing",
            "\u0000\u0001garbage",
        )
        bad.forEach { assertTrue("accepted: $it", BackupCodec.decode(it) is BackupResult.Invalid) }
    }

    @Test fun newerBackupVersionIsRejectedSafely() {
        val r = BackupCodec.decode("""{"backupVersion":99,"prefs":{}}""")
        assertTrue(r is BackupResult.Invalid)
    }

    @Test fun deepNestingAndHugeStringsAreRejected() {
        assertTrue(BackupCodec.decode("[".repeat(5000)) is BackupResult.Invalid)
        val big = "x".repeat(BackupCodec.MAX_STRING + 1)
        assertTrue(BackupCodec.decode("""{"backupVersion":1,"prefs":{"favorites":{"t":"s","v":"$big"}}}""") is BackupResult.Invalid)
    }

    @Test fun userMessageIsExact() {
        assertEquals("Invalid or corrupted backup file.", BackupCodec.USER_MESSAGE)
    }

    @Test fun zipRoundTrip() {
        val json = BackupCodec.encode(sample, 1L, "x")
        val out = ByteArrayOutputStream()
        BackupZip.write(out, json)
        assertEquals(json, BackupZip.readJson(ByteArrayInputStream(out.toByteArray())))
    }

    @Test fun invalidZipsReturnNull() {
        assertNull(BackupZip.readJson(ByteArrayInputStream(ByteArray(0))))
        assertNull(BackupZip.readJson(ByteArrayInputStream("hello".toByteArray())))
        val wrongName = ByteArrayOutputStream()
        ZipOutputStream(wrongName).use { it.putNextEntry(ZipEntry("other.txt")); it.write(1); it.closeEntry() }
        assertNull(BackupZip.readJson(ByteArrayInputStream(wrongName.toByteArray())))
    }

    @Test fun zipBombIsRejected() {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z ->
            z.putNextEntry(ZipEntry(BackupZip.ENTRY_NAME))
            val chunk = ByteArray(100_000) { 'a'.code.toByte() }
            repeat(40) { z.write(chunk) } // 4 MB uncompressed, tiny compressed
            z.closeEntry()
        }
        assertNull(BackupZip.readJson(ByteArrayInputStream(out.toByteArray())))
    }

    @Test fun jsonEscapesRoundTrip() {
        val tricky = "tab\t newline\n quote\" backslash\\ unicode é 😀 control\u0001"
        assertEquals(tricky, MiniJson.parse(MiniJson.write(tricky)))
        assertEquals(listOf(1.0, "a", true, null), MiniJson.parse("[1, \"a\", true, null]"))
    }
}
