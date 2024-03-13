package com.joinforage.forage.android.pos.encryption.dukpt

internal interface StoredKey {
    fun forceDeriveIntermediateDerivationKey(
        deviceDerivationId: KsnComponent,
        txCounter: DukptCounter,
        destinationKeyRegisterIndex: UInt
    ): StoredKey
    fun safeDeriveIntermediateDerivationKey(
        deviceDerivationId: KsnComponent,
        txCounter: DukptCounter,
        destinationKeyRegisterIndex: UInt
    ): StoredKey
    fun derivePinEncryptionWorkingKey(
        deviceDerivationId: KsnComponent,
        txCounter: DukptCounter
    ): WorkingKey
    fun clear()
}
