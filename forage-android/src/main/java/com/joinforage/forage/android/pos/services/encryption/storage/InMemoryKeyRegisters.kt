package com.joinforage.forage.android.pos.services.encryption.storage

import com.joinforage.forage.android.pos.encryption.DerivationKeyAlias
import com.joinforage.forage.android.pos.services.encryption.AesBlock
import com.joinforage.forage.android.pos.services.encryption.dukpt.DukptCounter
import com.joinforage.forage.android.pos.services.encryption.dukpt.SecureKeyStorageRegisters
import com.joinforage.forage.android.pos.services.encryption.dukpt.StoredKey
import javax.crypto.spec.SecretKeySpec

internal class InMemoryKeyRegisters : SecureKeyStorageRegisters {
    var keyStore: HashMap<String, SecretKeySpec> = HashMap()

    private fun storeKey(alias: String, keyMaterial: AesBlock) {
        val secretKey = SecretKeySpec(keyMaterial.data, "AES")
        keyStore[alias] = secretKey
    }

    fun clearKey(alias: String) {
        keyStore.remove(alias)
    }

    override fun isKeySet(index: UInt): Boolean =
        keyStore.containsKey(DerivationKeyAlias(index).toString())

    fun isKeySet(counter: DukptCounter): Boolean =
        isKeySet(counter.currentKeyIndex)

    override fun reset() {
        keyStore = HashMap()
    }

    override fun setInitialDerivationKey(initialKeyMaterial: AesBlock): StoredKey {
        val alias = DerivationKeyAlias.forInitialDerivationKey().toString()
        storeKey(alias, initialKeyMaterial)
        return InMemoryStoredKey(alias, this)
    }

    fun setIntermediateDerivationKey(alias: String, keyMaterial: AesBlock) {
        storeKey(alias, keyMaterial)
    }

    fun setWorkingKey(keyMaterial: AesBlock) {
        storeKey(WORKING_KEY_ALIAS, keyMaterial)
    }

    fun getKey(alias: String): SecretKeySpec {
        return keyStore[alias]!!
    }

    fun getKeyOrNone(alias: String): SecretKeySpec? {
        return if (keyStore.containsKey(alias)) keyStore[alias] else null
    }

    // the whole point of this method is so the callee
    // never needs to directly depend on
    // AndroidKeyStoreKey, avoiding coupling
    override fun getKey(index: UInt): StoredKey = InMemoryStoredKey(
        DerivationKeyAlias(index).toString(),
        this
    )

    fun getIntermediateKeyMaterial(alias: String): AesBlock {
        val intermediateKey = getKey(alias)
        return AesBlock(intermediateKey.encoded)
    }

    fun getWorkingKeyMaterial(): AesBlock {
        val workingKey = getKey(WORKING_KEY_ALIAS)
        return AesBlock(workingKey.encoded)
    }
}
