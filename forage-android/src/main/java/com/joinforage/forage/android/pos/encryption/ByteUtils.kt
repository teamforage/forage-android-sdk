package com.joinforage.forage.android.pos.encryption

val singleByteMask = 0xffu
object ByteUtils {
    // NOTE: this function assumes there is no leading
    // 0x<hex>... it assumes you just pass in <hex>...
    fun hex2ByteArray(hex: String): ByteArray = hex.chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
    fun uintToByteArray(uint: UInt): ByteArray = byteArrayOf(
        (uint shr 24 and singleByteMask).toByte(), // byte 0 is bits 31 to 24
        (uint shr 16 and singleByteMask).toByte(), // byte 1 is bits 23 to 16
        (uint shr 8 and singleByteMask).toByte(), // byte 2 is bits 15 to 8
        (uint and singleByteMask).toByte() // byte 3 is bits 7 to 0
    )
    fun byteArrayToUInt(bytes: ByteArray): UInt {
        require(bytes.size == 4) { "UInt is exactly 4 bytes." }
        return ((bytes[0].toUInt() and singleByteMask) shl 24) or
            ((bytes[1].toUInt() and singleByteMask) shl 16) or
            ((bytes[2].toUInt() and singleByteMask) shl 8) or
            (bytes[3].toUInt() and singleByteMask)
    }
    fun byteArray2Hex(bytes: ByteArray): String =
        bytes.joinToString(separator = "", transform = { "%02x".format(it) })
}
