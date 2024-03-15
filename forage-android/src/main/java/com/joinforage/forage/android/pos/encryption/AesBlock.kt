package com.joinforage.forage.android.pos.encryption

internal val AES_BLOCK_BYTE_SIZE = 16

internal data class AesBlock(val data: ByteArray) {
    init {
        require(data.size == AES_BLOCK_BYTE_SIZE) { "AES data blocks must be precisely 16 bytes" }
    }

    constructor(hex: String) : this(ByteUtils.hex2ByteArray(hex))

    fun xor(other: AesBlock): AesBlock {
        val xorData =
                data.zip(other.data) { a, b -> (a.toInt() xor b.toInt()).toByte() }.toByteArray()
        return AesBlock(xorData)
    }

    // for unit tests
    fun toHexString(): String = ByteUtils.byteArray2Hex(data)
}
