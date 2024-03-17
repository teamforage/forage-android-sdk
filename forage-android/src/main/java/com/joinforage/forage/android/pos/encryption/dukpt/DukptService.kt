package com.joinforage.forage.android.pos.encryption.dukpt

import com.joinforage.forage.android.pos.encryption.AesBlock
import com.joinforage.forage.android.pos.encryption.storage.KeySerialNumber

internal class DukptService(
    private val ksn: KeySerialNumber,
    private val keyRegisters: SecureKeyStorageRegisters
) {
    private val deviceDerivationId: KsnComponent = KsnComponent(ksn.deviceDerivationId)
    private var txCounter: DukptCounter = DukptCounter(ksn.dukptClientTxCount)

    private val currentKey: StoredKey
        get() = keyRegisters.getKey(txCounter.currentKeyIndex)

    val count
        get() = txCounter.count

    private fun forceUpdateDerivationKeys(shiftRegister: ShiftRegister, baseKey: StoredKey) {
        // iterate through all of the child counter values of the current
        // transaction count. Recall that the child counter values are
        // obtained by keeping the least significant 1 bit of the counter
        // fixed and then one-at-a-time flipping the remaining 0 bits to
        // 1s
        ShiftRegister.forEachRightShift(shiftRegister) { rightShifted ->
            // write the derived key back to key register that
            // corresponds to the offspring counter value of
            // txCount bitOR rightShifted
            baseKey.forceDeriveIntermediateDerivationKey(
                deviceDerivationId,
                txCounter.bitOr(rightShifted.contents),
                destinationKeyRegisterIndex = rightShifted.lsbOffset
            )
        }
    }

    // same as forced except won't overwrite the key entry
    // if it's contents are not empty
    private fun safeUpdateDerivationKeys(shiftRegister: ShiftRegister, baseKey: StoredKey) {
        ShiftRegister.forEachRightShift(shiftRegister) { rightShifted ->
            baseKey.safeDeriveIntermediateDerivationKey(
                deviceDerivationId,
                txCounter.bitOr(rightShifted.contents),
                destinationKeyRegisterIndex = rightShifted.lsbOffset
            )
        }
    }

    private fun updateStateForNextTx() {
        txCounter =
            if (txCounter.isLessThanMaxWork) {
                safeUpdateDerivationKeys(txCounter.shiftRegister, currentKey)
                currentKey.clear()
                // TODO: can anything bad happen if the app crashes right here?
                //  like can DUKPT recover from this?
                txCounter.inc()
            } else {
                currentKey.clear()
                // TODO: can anything bad happen if the app crashes right here?
                //  like can DUKPT recover from this?
                txCounter.incByLsb()
            }
    }

    fun generateWorkingKey(): Pair<WorkingKey, KeySerialNumber> {
        // go to the next txCounter value that has computed key
        // NOTE: this won't change the txCounter if the current
        //  txCounter value already has a key associated with it
        txCounter = DukptCounter.incToNextExistingKey(txCounter, keyRegisters)

        // create the working key from the intermediate derivation key
        // associated with the current tx value. The only difference
        // between the working key and the intermediate derivation key
        // is that we use AES one last time to create a key of type
        // PIN encryption key. The intermediate keys were all created
        // using AES in derivative key more, which is no bueno for actual
        // working keys; they need to come from AES in PIN encryption mode
        val workingKey = currentKey.derivePinEncryptionWorkingKey(deviceDerivationId, txCounter)

        // we need to delete the intermediate key that we just used
        // to create the working key to satisfy the DUKPT constraint
        // that no keys linger around on the device that can be used
        // to rederive a working key
        updateStateForNextTx()

        // return the working key and the KsnNext state.
        // NOTE: there are two things happening here with
        // the txCounter:
        //  1) we technically returning the txCounter for
        //  the dukpt's client next state (i.e. after running
        //  updateStateForNextTx). This txCounter value is
        //  +1 ahead of the txCounter value associated with
        //  the current working key returned.
        //  2) we return via
        //  nextKsnState.txCountAsBigEndian8CharHex the value
        //  of the count associated with the current working
        //  key. We return this value so the host machine may
        //  derive the current working key as well (it's a
        //  symmetric key after all)
        val counterForNextState = KsnComponent(txCounter.count) // one ahead of current tx
        val nextKsnState = ksn.newKsnWithCount(counterForNextState)
        return Pair(workingKey, nextKsnState)
    }

    fun loadKey(initialDerivationKeyMaterial: AesBlock) : KeySerialNumber {
        keyRegisters.reset()
        val initialDerivationKey =
            keyRegisters.setInitialDerivationKey(initialDerivationKeyMaterial)
        forceUpdateDerivationKeys(ShiftRegister.fromHighestValue(), initialDerivationKey)
        txCounter = txCounter.inc()
        return ksn.newKsnWithCount(KsnComponent(txCounter.count))
    }
}
