package com.joinforage.forage.android.pos.services.encryption.storage

import com.joinforage.forage.android.pos.encryption.DerivationKeyAlias
import com.joinforage.forage.android.pos.services.encryption.AesBlock
import com.joinforage.forage.android.pos.services.encryption.dukpt.AesEcbDerivationData
import com.joinforage.forage.android.pos.services.encryption.dukpt.DukptCounter
import com.joinforage.forage.android.pos.services.encryption.dukpt.KsnComponent
import com.joinforage.forage.android.pos.services.encryption.dukpt.StoredKey
import javax.crypto.Cipher

internal class InMemoryStoredKey(
    private val alias: String,
    private val keyRegisters: InMemoryKeyRegisters
) : StoredKey {

    private fun aesEncryptEcb(dataToEncrypt: AesBlock): AesBlock {
        val secretKey = keyRegisters.getKey(alias)
        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
            .apply { init(Cipher.ENCRYPT_MODE, secretKey) }
        val result = cipher.doFinal(dataToEncrypt.data)
        return AesBlock(result)
    }

    override fun forceDeriveIntermediateDerivationKey(
        deviceDerivationId: KsnComponent,
        txCounter: DukptCounter,
        destinationKeyRegisterIndex: UInt
    ): StoredKey {
        val derivationData = AesEcbDerivationData.forIntermediateDerivationKey(
            deviceDerivationId,
            txCounter.toKsnComponent()
        )
        val derivationKeyMaterial = aesEncryptEcb(derivationData.toBytes())
        val derivationKeyAlias =
            DerivationKeyAlias(destinationKeyRegisterIndex).toString()
        keyRegisters.setIntermediateDerivationKey(
            derivationKeyAlias,
            derivationKeyMaterial
        )
        return InMemoryStoredKey(derivationKeyAlias, keyRegisters)
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
    ): InMemoryWorkingKey {
        val derivationData = AesEcbDerivationData.forPinEncryptionWorkingKey(
            deviceDerivationId,
            txCounter.toKsnComponent()
        )
        val workingKeyMaterial = aesEncryptEcb(derivationData.toBytes())
        keyRegisters.setWorkingKey(workingKeyMaterial)
        return InMemoryWorkingKey(keyRegisters)
    }

    override fun clear() = keyRegisters.clearKey(alias)

    val keyMaterial: AesBlock
        get() = keyRegisters.getIntermediateKeyMaterial(alias)
}
