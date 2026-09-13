package ca.gapwise.android.core.persistence

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Small encrypted app-private store backed by Android Keystore.
 *
 * Raw calendar source bytes are never persisted. Gapwise stores only the normalized
 * timetable/session JSON needed by the native app, encrypted at rest with a
 * non-exportable AES key held by Android Keystore.
 */
class SecureLocalStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "gapwise_secure_v1",
        Context.MODE_PRIVATE,
    )

    fun put(name: String, plaintext: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        cipher.updateAAD(aad(name))
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        preferences.edit()
            .putString("$name.iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString("$name.data", Base64.encodeToString(ciphertext, Base64.NO_WRAP))
            .apply()
    }

    fun get(name: String): String? {
        val ivEncoded = preferences.getString("$name.iv", null) ?: return null
        val dataEncoded = preferences.getString("$name.data", null) ?: return null
        return runCatching {
            val iv = Base64.decode(ivEncoded, Base64.NO_WRAP)
            val ciphertext = Base64.decode(dataEncoded, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
            cipher.updateAAD(aad(name))
            cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
        }.getOrNull()
    }

    fun remove(name: String) {
        preferences.edit().remove("$name.iv").remove("$name.data").apply()
    }

    private fun aad(name: String) = "gapwise:android:$name:v1".toByteArray(Charsets.UTF_8)

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val KEY_ALIAS = "gapwise.android.local.v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
