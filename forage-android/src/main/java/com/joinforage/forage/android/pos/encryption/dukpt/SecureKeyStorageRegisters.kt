package com.joinforage.forage.android.pos.encryption.dukpt

import com.joinforage.forage.android.pos.encryption.AesBlock

internal interface SecureKeyStorageRegisters {
    fun reset()
    fun setInitialDerivationKey(initialKeyMaterial: AesBlock): StoredKey
    fun isKeySet(index: UInt): Boolean

    // we add this method to the interface so that consumers
    // don't need to be aware of the specific KeyStorage
    // mechanism backing the key registers. If we didn't have
    // this method, consumers we need to depend on this
    // interface as well as a specific KeyStorage implementation
    // which would introduce the very coupling we're trying to avoid
    fun getKey(index: UInt): StoredKey
}
