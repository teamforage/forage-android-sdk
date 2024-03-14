package com.joinforage.forage.android.pos.encryption.dukpt

import com.joinforage.forage.android.pos.encryption.AesBlock

internal interface WorkingKey {
    fun aesEncryptEcb(aesBlock: AesBlock): AesBlock
}
