package com.surwi.compass360

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Private local birth-profile storage. AES-GCM key material stays in Android Keystore. */
class BirthProfileStore(context: Context) {
    private val prefs = context.getSharedPreferences("private_birth_profile", Context.MODE_PRIVATE)

    fun save(name: String, dob: String, time: String, place: String, zone: String) {
        val plain = listOf(name, dob, time, place, zone).joinToString("\n")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val ciphertext = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        val payload = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        check(prefs.edit().putString(KEY_PROFILE, "$iv:$payload").commit()) { "Could not save encrypted profile." }
    }

    fun load(): List<String>? {
        val saved = prefs.getString(KEY_PROFILE, null) ?: return null
        return try {
            val parts = saved.split(":", limit = 2)
            require(parts.size == 2)
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val ciphertext = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            cipher.doFinal(ciphertext).toString(Charsets.UTF_8).split("\n")
        } catch (_: Exception) { null }
    }

    fun delete() {
        prefs.edit().remove(KEY_PROFILE).commit()
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (store.containsAlias(KEY_ALIAS)) store.deleteEntry(KEY_ALIAS)
    }

    private fun getOrCreateKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = store.getKey(KEY_ALIAS, null)
        if (existing is SecretKey) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(
            KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true).build())
        return generator.generateKey()
    }

    companion object {
        private const val KEY_ALIAS = "vedic_compass_birth_profile_key"
        private const val KEY_PROFILE = "encrypted_profile"
    }
}
