package com.joinforage.forage.android.pos.encryption

internal val AES_BLOCK_BYTE_SIZE = 16
internal data class AesBlock(val data: ByteArray) {
    init {
        require(data.size == AES_BLOCK_BYTE_SIZE) {
            "AES data blocks must be precisely 16 bytes"
        }
    }

    fun xor(other: AesBlock) : AesBlock {
        val xorData = data.zip(other.data) { a, b ->
            (a.toInt() xor b.toInt()).toByte()
        }.toByteArray()
        return AesBlock(xorData)
    }

    // for unit tests
    fun toHexString(): String =
        data.joinToString(separator = "", transform = { "%02x".format(it) })

    companion object {

        // NOTE: this function assumes there is no leading
        // 0x<hex>... it assumes you just pass in <hex>...
        fun fromHexString(hex: String) : AesBlock {
            val bytearray = hex.chunked(2)
                .map { it.toInt(16).toByte() }
                .toByteArray()
            return AesBlock(bytearray)
        }
    }
}
