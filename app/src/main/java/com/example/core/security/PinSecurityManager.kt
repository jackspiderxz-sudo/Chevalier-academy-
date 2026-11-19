package com.example.core.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec

object PinSecurityManager {
    private const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val PBKDF2_ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16
    private const val KEYSTORE_ALIAS = "chevalier_local_auth_seal_key"
    private val secureRandom = SecureRandom()

    fun isValidPinFormat(pin: String): Boolean =
        pin.length >= 6 && pin.all { it.isDigit() }

    fun generateSaltBase64(): String {
        val salt = ByteArray(SALT_BYTES)
        secureRandom.nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    fun deriveVerifierBase64(secret: String, saltBase64: String): String {
        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
        val spec = PBEKeySpec(secret.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        return try {
            val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
            val derived = factory.generateSecret(spec).encoded
            Base64.encodeToString(derived, Base64.NO_WRAP)
        } finally {
            spec.clearPassword()
        }
    }

    fun verifySecretConstantTime(
        inputSecret: String,
        saltBase64: String,
        expectedVerifierBase64: String
    ): Boolean {
        if (inputSecret.isEmpty() || saltBase64.isBlank() || expectedVerifierBase64.isBlank()) {
            return false
        }
        val actualBase64 = deriveVerifierBase64(inputSecret, saltBase64)
        val expectedBytes = Base64.decode(expectedVerifierBase64, Base64.NO_WRAP)
        val actualBytes = Base64.decode(actualBase64, Base64.NO_WRAP)
        return MessageDigest.isEqual(expectedBytes, actualBytes)
    }

    fun generateRecoveryCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val groups = (0 until 3).map {
            buildString {
                repeat(4) {
                    append(alphabet[secureRandom.nextInt(alphabet.length)])
                }
            }
        }
        return groups.joinToString("-")
    }

    fun computeLockoutDurationMillis(failedAttempts: Int): Long = when {
        failedAttempts < 3 -> 0L
        failedAttempts == 3 -> 15_000L
        failedAttempts == 4 -> 30_000L
        failedAttempts == 5 -> 60_000L
        else -> 300_000L
    }

    /**
     * Optional AndroidKeyStore-backed sealing for local non-PIN session metadata when supported by device Keystore.
     */
    fun sealWithAndroidKeyStore(plaintext: String): String? {
        return try {
            val key = getOrCreateKeyStoreKey() ?: return null
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv
            val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(1 + iv.size + encrypted.size)
            combined[0] = iv.size.toByte()
            System.arraycopy(iv, 0, combined, 1, iv.size)
            System.arraycopy(encrypted, 0, combined, 1 + iv.size, encrypted.size)
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (_: Throwable) {
            null
        }
    }

    fun unsealWithAndroidKeyStore(sealedBase64: String): String? {
        return try {
            val key = getOrCreateKeyStoreKey() ?: return null
            val combined = Base64.decode(sealedBase64, Base64.NO_WRAP)
            val ivLen = combined[0].toInt() and 0xFF
            val iv = combined.copyOfRange(1, 1 + ivLen)
            val ciphertext = combined.copyOfRange(1 + ivLen, combined.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (_: Throwable) {
            null
        }
    }

    private fun getOrCreateKeyStoreKey(): SecretKey? {
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        val existing = keyStore.getKey(KEYSTORE_ALIAS, null) as? SecretKey
        if (existing != null) return existing
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }
}
