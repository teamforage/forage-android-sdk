package com.joinforage.forage.android.pos.encryption.iso4

import com.joinforage.forage.android.pos.encryption.AesBlock

/**
 * Implementation taken from
 * https://github.com/knovichikhin/psec/blob/a26de50bcc36f143a107e7df92a1321943547c39/psec/pinblock.py#L207-L216
 */
internal class PinFieldIso4(
    val rawPin: String,
    val endRandomPadding: String = makeRandomHexString(16)
) : Iso4Field {

    val startPadding = "4" // nothing to do with pin length
    val pinSize = 4 // hard-coded to only handle length 4 PINs
    val padAs = "A".repeat(14 - pinSize) // 10 for pin.length == 4

    init {
        require(rawPin.length == 4) {
            "PINs must be 4 digits long."
        }
    }

    override fun toString(): String =
        "$startPadding$pinSize$rawPin$padAs$endRandomPadding"

    override fun toAesBlock(): AesBlock = AesBlock(toString())
}
