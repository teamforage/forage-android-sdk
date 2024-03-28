package com.joinforage.forage.android.pos.encryption.iso4

import com.joinforage.forage.android.pos.encryption.AesBlock

/**
 * Implementation taken from
 * https://github.com/knovichikhin/psec/blob/a26de50bcc36f143a107e7df92a1321943547c39/psec/pinblock.py#L251-L256
 */
internal class PanFieldIso4(val rawPan: String) : Iso4Field {
    val lengthBeyond12 = rawPan.length - 12

    // the final output needs to be 32 hex chars to make
    // 16 bytes.
    // 32 = lengthBeyond12 is 1 char + pan.length + endPadding
    // so endPadding length = 32 - 1 - pan.length
    val endPadding = "0".repeat(32 - 1 - rawPan.length)

    init {
        require(rawPan.length in 16..19) {
            "PANs must be between 16 and 19 digits."
        }
    }

    override fun toString(): String = "$lengthBeyond12$rawPan$endPadding"

    override fun toAesBlock(): AesBlock = AesBlock(toString())
}
