package com.joinforage.forage.android.pos.encryption

internal val AES_BLOCK_BYTE_SIZE = 16
internal data class AesBlock(val data: ByteArray) {
    init {
        require(data.size == AES_BLOCK_BYTE_SIZE) {
            "AES data blocks must be precisely 16 bytes"
        }
    }

    // for unit tests
    fun toHexString(): String =
        data.joinToString(separator = "", transform = { "%02x".format(it) })
}
