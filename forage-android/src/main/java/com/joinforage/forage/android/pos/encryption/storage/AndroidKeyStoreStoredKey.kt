package com.joinforage.forage.android.pos.encryption.storage

import android.os.Build
import androidx.annotation.RequiresApi
import com.joinforage.forage.android.pos.encryption.AesBlock
import com.joinforage.forage.android.pos.encryption.DerivationKeyAlias
import com.joinforage.forage.android.pos.encryption.dukpt.AesEcbDerivationData
import com.joinforage.forage.android.pos.encryption.dukpt.DukptCounter
import com.joinforage.forage.android.pos.encryption.dukpt.KsnComponent
import com.joinforage.forage.android.pos.encryption.dukpt.StoredKey
import com.joinforage.forage.android.pos.encryption.dukpt.WorkingKey
import javax.crypto.Cipher

internal val WORKING_KEY_ALIAS = "dukpt_working_key"

@RequiresApi(Build.VERSION_CODES.M)
internal class AndroidKeyStoreStoredKey(private val alias: String) : StoredKey, WorkingKey {
    private val androidKeyStore = AndroidKeyStoreService()

    override fun aesEncryptEcb(aesBlock: AesBlock): AesBlock {
        val keyEntry = androidKeyStore.getKey(alias)
        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
            .apply { init(Cipher.ENCRYPT_MODE, keyEntry.secretKey) }
        val result = cipher.doFinal(aesBlock.data)
        return AesBlock(result)
    }

    override fun forceDeriveIntermediateDerivationKey(
        deviceDerivationId: KsnComponent,
        txCounter: DukptCounter,
        destinationKeyRegisterIndex: UInt
    ): AndroidKeyStoreStoredKey {
        val derivationData = AesEcbDerivationData.forIntermediateDerivationKey(
            deviceDerivationId,
            txCounter.toKsnComponent()
        )
        val derivationKeyMaterial = aesEncryptEcb(derivationData.toBytes())
        val derivationKeyAlias =
            DerivationKeyAlias(destinationKeyRegisterIndex).toString()
        androidKeyStore.storeSecretAesKey(
            derivationKeyAlias,
            derivationKeyMaterial.data
        )
        return AndroidKeyStoreStoredKey(derivationKeyAlias)
    }

    override fun safeDeriveIntermediateDerivationKey(
        deviceDerivationId: KsnComponent,
        txCounter: DukptCounter,
        destinationKeyRegisterIndex: UInt
    ): StoredKey {
        val derivationKeyAlias =
            DerivationKeyAlias(destinationKeyRegisterIndex).toString()
        if (derivationKeyAlias == alias) return this
        return forceDeriveIntermediateDerivationKey(
            deviceDerivationId,
            txCounter,
            destinationKeyRegisterIndex
        )
    }

    override fun derivePinEncryptionWorkingKey(
        deviceDerivationId: KsnComponent,
        txCounter: DukptCounter
    ): AndroidKeyStoreStoredKey {
        val derivationData = AesEcbDerivationData.forPinEncryptionWorkingKey(
            deviceDerivationId,
            txCounter.toKsnComponent()
        )
        val workingKeyMaterial = aesEncryptEcb(derivationData.toBytes())
        androidKeyStore.storeSecretAesKey(
            WORKING_KEY_ALIAS,
            workingKeyMaterial.data
        )
        return AndroidKeyStoreStoredKey(WORKING_KEY_ALIAS)
    }

    override fun clear() = androidKeyStore.clearKey(alias)

}
