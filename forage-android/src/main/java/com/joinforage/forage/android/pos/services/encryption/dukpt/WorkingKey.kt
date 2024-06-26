package com.joinforage.forage.android.pos.services.encryption.dukpt

import com.joinforage.forage.android.pos.services.encryption.AesBlock

internal interface WorkingKey {
    fun aesEncryptEcb(aesBlock: AesBlock): AesBlock
}
