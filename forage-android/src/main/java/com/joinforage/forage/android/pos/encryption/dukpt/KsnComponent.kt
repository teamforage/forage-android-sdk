package com.joinforage.forage.android.pos.encryption.dukpt

internal data class KsnComponent(val bytes: ByteArray) {
    init {
        // The Key Serial Number (KSN) consists of three words
        // that are each 4 bytes long resulting in the
        // KSN having a length of 12 bytes of 96 bits
        require(bytes.size == 4) {
            "A Key Serial Number component must be exactly 4 bytes."
        }
    }
    companion object {
        fun fromUnsignedInt(uint: UInt): KsnComponent {
            val singleByteMask = 0xffu
            return KsnComponent(
                // big-endian order byte representation of an kotlin UInt
                byteArrayOf(
                    (uint shr 24 and singleByteMask).toByte(), // byte 0 is bits 31 to 24
                    (uint shr 16 and singleByteMask).toByte(), // byte 1 is bits 23 to 16
                    (uint shr 8 and singleByteMask).toByte(), // byte 2 is bits 15 to 8
                    (uint and singleByteMask).toByte() // byte 3 is bits 7 to 0
                )
            )
        }
    }
}
