package com.joinforage.forage.android.pos.services.encryption.iso4

import com.joinforage.forage.android.pos.services.encryption.dukpt.WorkingKey
import com.joinforage.forage.android.pos.services.encryption.AesBlock



/**
 * Implementation taken from:
 * https://github.com/knovichikhin/psec/blob/a26de50bcc36f143a107e7df92a1321943547c39/psec/pinblock.py#L302-L306
 */
internal class PinBlockIso4(
    pan: PanFieldIso4,
    pin: PinFieldIso4,
    workingKey: WorkingKey
) {
    val contents: AesBlock
    init {
        val blockA = workingKey.aesEncryptEcb(pin.toAesBlock())
        val blockB = blockA.xor(pan.toAesBlock())
        contents = workingKey.aesEncryptEcb(blockB)
    }

    // this constructor is helpful for most use cases
    // the other (primary) constructor is helpful for testing
    constructor(
        rawPan: String,
        plainTextPin: String,
        workingKey: WorkingKey
    ) : this(PanFieldIso4(rawPan), PinFieldIso4(plainTextPin), workingKey)
}
