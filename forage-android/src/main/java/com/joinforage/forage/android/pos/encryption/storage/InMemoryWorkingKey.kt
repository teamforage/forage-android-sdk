package com.joinforage.forage.android.pos.encryption.storage

import com.joinforage.forage.android.pos.encryption.AesBlock
import com.joinforage.forage.android.pos.encryption.PinBlock
import com.joinforage.forage.android.pos.encryption.dukpt.WorkingKey

internal class InMemoryWorkingKey(
    private val keyRegisters: InMemoryKeyRegisters
) : WorkingKey {
    override fun encryptPinBlock(pinBlock: PinBlock): AesBlock {
        TODO("Not yet implemented")
    }
    val keyMaterial: AesBlock
        get() = keyRegisters.getWorkingKeyMaterial()
}
