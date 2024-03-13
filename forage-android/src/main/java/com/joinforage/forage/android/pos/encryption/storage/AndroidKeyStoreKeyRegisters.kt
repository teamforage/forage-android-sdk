package com.joinforage.forage.android.pos.encryption.storage

import android.os.Build
import androidx.annotation.RequiresApi
import com.joinforage.forage.android.pos.encryption.AesBlock
import com.joinforage.forage.android.pos.encryption.DerivationKeyAlias
import com.joinforage.forage.android.pos.encryption.dukpt.DukptCounter
import com.joinforage.forage.android.pos.encryption.dukpt.SecureKeyStorageRegisters
import com.joinforage.forage.android.pos.encryption.dukpt.StoredKey

@RequiresApi(Build.VERSION_CODES.M)
internal class AndroidKeyStoreKeyRegisters : SecureKeyStorageRegisters {
    private val androidKeyStore = AndroidKeyStoreService()

    override fun isKeySet(index: UInt): Boolean =
        androidKeyStore.isKeySet(DerivationKeyAlias(index).toString())

    fun isKeySet(counter: DukptCounter): Boolean =
        isKeySet(counter.currentKeyIndex)

    override fun reset() = androidKeyStore.clearAllKeys()

    override fun setInitialDerivationKey(initialKeyMaterial: AesBlock): StoredKey {
        val alias = DerivationKeyAlias.forInitialDerivationKey().toString()
        androidKeyStore.storeSecretAesKey(alias, initialKeyMaterial.data)
        return AndroidKeyStoreStoredKey(alias)
    }

    // the whole point of this method is so the callee
    // never needs to directly depend on
    // AndroidKeyStoreKey, avoiding coupling
    override fun getKey(index: UInt): StoredKey = AndroidKeyStoreStoredKey(
        DerivationKeyAlias(index).toString()
    )
}
