package com.joinforage.forage.android.pos.encryption.dukpt

import com.joinforage.forage.android.pos.encryption.AesBlock

internal enum class KeyUsage(val derivationDataBytes: ByteArray) {
    PinEncryption(byteArrayOf(0x10, 0x00)), // for the WorkingKey since it encrypt PINs :P
    KeyDerivation(byteArrayOf(0x80.toByte(), 0x00)) // for all the intermediate derivation keys
}

internal data class AesEcbDerivationData(
    val keyUsage: KeyUsage,
    val deviceDerivationId: KsnComponent,
    val txCounter: KsnComponent
) {
    val version: Byte = 0x01

    // we use AES 128 exclusively so never need more than one block
    val keyBlockCounter: Byte = 0x01
    val derivedKeyType: ByteArray = byteArrayOf(0x0002)
    val algo: ByteArray = byteArrayOf(0x00, 0x02)
    val length: ByteArray = byteArrayOf(0x00, 0x80.toByte())

    fun toBytes(): AesBlock = AesBlock(
        byteArrayOf(
            version, // byte 0
            keyBlockCounter, // byte 1
            *keyUsage.derivationDataBytes, // byte 2-3
            *algo, // byte 4-5
            *length, // byte 6-7
            *deviceDerivationId.bytes, // byte 8-11
            *txCounter.bytes // byte 12-15
        )
    )
    companion object {
        fun forIntermediateDerivationKey(
            deviceDerivationId: KsnComponent,
            txCounter: KsnComponent
        ): AesEcbDerivationData =
            AesEcbDerivationData(KeyUsage.KeyDerivation, deviceDerivationId, txCounter)

        fun forPinEncryptionWorkingKey(
            deviceDerivationId: KsnComponent,
            txCounter: KsnComponent
        ): AesEcbDerivationData =
            AesEcbDerivationData(KeyUsage.PinEncryption, deviceDerivationId, txCounter)
    }
}
