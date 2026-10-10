package com.mylauncher.app.security.pin

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Salted PBKDF2 hashing. The PIN is never stored or logged. */
object PinHasher {
    const val MIN_LENGTH = 4
    const val MAX_LENGTH = 8
    const val ITERATIONS = 120_000
    private const val SALT_BYTES = 16
    private const val KEY_BITS = 256
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"

    data class Hashed(val hash: String, val salt: String)

    fun isValidPin(pin: String): Boolean =
        pin.length in MIN_LENGTH..MAX_LENGTH && pin.all { it in '0'..'9' }

    fun newSalt(): ByteArray = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }

    fun hash(pin: String, salt: ByteArray = newSalt(), iterations: Int = ITERATIONS): Hashed =
        Hashed(
            hash = Base64.getEncoder().encodeToString(derive(pin, salt, iterations)),
            salt = Base64.getEncoder().encodeToString(salt),
        )

    fun verify(pin: String, hashB64: String, saltB64: String, iterations: Int = ITERATIONS): Boolean {
        return try {
            val expected = Base64.getDecoder().decode(hashB64)
            val actual = derive(pin, Base64.getDecoder().decode(saltB64), iterations)
            MessageDigest.isEqual(expected, actual) // constant-time comparison
        } catch (e: Exception) {
            false
        }
    }

    private fun derive(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_BITS)
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
