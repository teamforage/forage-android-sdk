package com.joinforage.forage.android.pos.encryption.dukpt

import com.joinforage.forage.android.pos.encryption.AesBlock
import com.joinforage.forage.android.pos.encryption.PinBlock

internal interface WorkingKey {
    fun encryptPinBlock(pinBlock: PinBlock): AesBlock
}
