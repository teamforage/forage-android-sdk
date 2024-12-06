package com.joinforage.forage.android.pos.services.encryption.dukpt

internal val MAX_COUNTER: UInt = UInt.MAX_VALUE
internal val MAX_SERVER_DERIVATION_WORK = 16u

internal class DukptCounter(val count: UInt) {
    val shiftRegister: ShiftRegister
    val currentKeyIndex: UInt // the key register index of the current key
    val isLessThanMaxWork: Boolean

    init {
        require(count <= MAX_COUNTER) {
            "Counter exceeds possible key combinations."
        }
        shiftRegister = ShiftRegister.forCounter(count)
        currentKeyIndex = shiftRegister.lsbOffset
        isLessThanMaxWork = count.countOneBits().toUInt() <= MAX_SERVER_DERIVATION_WORK
    }

    fun inc(): DukptCounter = DukptCounter(count + 1u)
    fun incByLsb(): DukptCounter = DukptCounter(count + shiftRegister.lsbOffset)
    fun bitOr(other: UInt): DukptCounter = DukptCounter(count or other)
    fun toKsnComponent(): KsnComponent = KsnComponent(count)

    companion object {
        private const val MAX_EXECUTION_TIME_MS = 5000 // 5 seconds


        tailrec fun incToNextExistingKey(
            counter: DukptCounter,
            keyRegisters: SecureKeyStorageRegisters,
            startTime: Long = System.currentTimeMillis()
        ): DukptCounter {
            if (System.currentTimeMillis() - startTime > MAX_EXECUTION_TIME_MS) {
                throw InfiniteLoopException()
            }

            if (keyRegisters.isKeySet(counter.currentKeyIndex)) {
                return counter
            }
            return incToNextExistingKey(counter.incByLsb(), keyRegisters, startTime)
        }

        fun fromZero() = DukptCounter(0u)
    }

    class InfiniteLoopException : Exception("incToNextExistingKey has been running for more than 5 seconds")

    // for helpful debugging
    override fun toString(): String = count.toString(16)
}
