package com.joinforage.forage.android.pos.services.encryption.dukpt

internal val NUM_KEY_REGISTERS = 32u

internal class ShiftRegister(val lsbOffset: UInt) {
    val contents: UInt = 1u shl lsbOffset.toInt()
    init {
        require(lsbOffset >= 0u) {
            "ShiftRegister value cannot be less than 1"
        }
        require(lsbOffset < 32u) {
            "ShiftRegister cannot represent values larger than 32 bits"
        }
    }
    fun shiftRight(): ShiftRegister = ShiftRegister(lsbOffset - 1u)
    fun shiftLeft(): ShiftRegister = ShiftRegister(lsbOffset + 1u)

    companion object {
        fun forCounter(count: UInt): ShiftRegister {
            var shiftRegister = fromLowestValue()

            if (count == 0u) return shiftRegister

            while ((shiftRegister.contents and count) == 0u)
                shiftRegister = shiftRegister.shiftLeft()

            return shiftRegister
        }
        fun fromLowestValue(): ShiftRegister = ShiftRegister(0u)
        fun fromHighestValue(): ShiftRegister = ShiftRegister(NUM_KEY_REGISTERS - 1u)
        tailrec fun forEachRightShift(
            shiftRegister: ShiftRegister,
            callback: (s: ShiftRegister) -> Unit
        ) {
            // execute callback no matter what because it's impossible
            // to instantiate a ShiftRegister with value 0 or less
            callback(shiftRegister)

            // then check whether to exit or continue
            if (shiftRegister.contents == 1u) return
            return forEachRightShift(shiftRegister.shiftRight(), callback)
        }
    }

    // for debugging
    override fun toString(): String = contents.toString(16)
}
