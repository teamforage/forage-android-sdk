package com.joinforage.forage.android.pos.encryption.storage

import android.os.Build
import android.security.keystore.KeyProperties
import android.security.keystore.KeyProtection
import androidx.annotation.RequiresApi
import java.security.KeyStore
import javax.crypto.spec.SecretKeySpec

@RequiresApi(Build.VERSION_CODES.M)
class AndroidKeyStoreService {
    private val androidKeyStore
        get() = KeyStore.getInstance("AndroidKeyStore")
            .apply { load(null) }

    private val protectionParam = KeyProtection.Builder(
        KeyProperties.PURPOSE_ENCRYPT
    )
        .setBlockModes(KeyProperties.BLOCK_MODE_ECB)
        .setRandomizedEncryptionRequired(false)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
        .build()

    fun getKey(keyAlias: String) = androidKeyStore.getEntry(
        keyAlias,
        protectionParam
    ) as KeyStore.SecretKeyEntry
    fun isKeySet(keyAlias: String): Boolean = androidKeyStore.containsAlias(keyAlias)
    fun clearKey(keyAlias: String) = androidKeyStore.deleteEntry(keyAlias)
    fun clearAllKeys() = androidKeyStore.aliases().toList().forEach {
        androidKeyStore.deleteEntry(it)
    }
    fun storeSecretAesKey(keyAlias: String, secretKeyMaterial: ByteArray) {
        val key = KeyStore.SecretKeyEntry(SecretKeySpec(secretKeyMaterial, "AES"))

        androidKeyStore.setEntry(keyAlias, key, protectionParam)
    }
}
