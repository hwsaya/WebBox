package com.example.webbox

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {

    private fun generateKey(password: String): SecretKeySpec {
        val key = MessageDigest.getInstance("SHA-256").digest(password.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(key, "AES")
    }

    // Returns "Base64(IV):Base64(ciphertext)"
    fun encrypt(plainText: String, password: String): String = try {
        val iv = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding").also {
            it.init(Cipher.ENCRYPT_MODE, generateKey(password), IvParameterSpec(iv))
        }
        val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        val cipherB64 = Base64.encodeToString(
            cipher.doFinal(plainText.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP
        )
        "$ivB64:$cipherB64"
    } catch (e: Exception) { "" }

    fun decrypt(encryptedData: String, password: String): String = try {
        // Use indexOf instead of split to avoid ambiguity with multiple colons
        val sep = encryptedData.indexOf(':')
        if (sep < 0) return ""
        val iv = Base64.decode(encryptedData.substring(0, sep), Base64.NO_WRAP)
        val cipherBytes = Base64.decode(encryptedData.substring(sep + 1), Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding").also {
            it.init(Cipher.DECRYPT_MODE, generateKey(password), IvParameterSpec(iv))
        }
        String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
    } catch (e: Exception) { "" }
}
