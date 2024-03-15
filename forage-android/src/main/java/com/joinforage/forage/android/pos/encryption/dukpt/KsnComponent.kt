package com.joinforage.forage.android.pos.encryption.dukpt

import com.joinforage.forage.android.pos.encryption.ByteUtils

internal data class KsnComponent(val bytes: ByteArray) {
    init {
        // The Key Serial Number (KSN) consists of three words
        // that are each 4 bytes long resulting in the
        // KSN having a length of 12 bytes of 96 bits
        require(bytes.size == 4) {
            "A Key Serial Number component must be exactly 4 bytes."
        }
    }

    constructor(uint: UInt) : this(ByteUtils.uintToByteArray(uint))
    constructor(hex: String) : this(ByteUtils.hex2ByteArray(hex))

    fun toHexString(): String = ByteUtils.byteArray2Hex(bytes)
    fun toUInt(): UInt = ByteUtils.byteArrayToUInt(bytes)
}
