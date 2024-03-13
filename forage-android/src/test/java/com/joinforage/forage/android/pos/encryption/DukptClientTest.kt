package com.joinforage.forage.android.pos.encryption

import com.joinforage.forage.android.pos.encryption.storage.InMemoryWorkingKey
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class DukptClientTest {
    @Test
    fun `it correctly instantiates the all intermediate derivation keys on key load`() {
        val (dukpt, keyRegisters) = DukptFixtures.newDukpt()
        assertThat(dukpt.count).isEqualTo(1u)

        // assert that all the initial keys were generated correctly
        for (keyIndex in 0u..31u) {
            val alias = DerivationKeyAlias(keyIndex).toString()
            val intermediateDerivationKey = keyRegisters.getKeyOrNone(alias)
            val actualHexValue = AesBlock(intermediateDerivationKey!!.encoded).toHexString()
            val expectedHexValue = DukptFixtures.IntermediateKeys.AfterLoadKey[keyIndex.toInt()]
            assertThat(actualHexValue).isEqualTo(expectedHexValue)
        }
    }

    @Test
    fun `it correctly updates the intermediate derivation keys after first working key`() {
        val (dukpt, keyRegisters) = DukptFixtures.newDukpt()
        assertThat(dukpt.count).isEqualTo(1u)
        val (key, txCount) = dukpt.generateWorkingKey()
        assertThat(txCount).isEqualTo(1u)
        val keyHex = (key as InMemoryWorkingKey).keyMaterial.toHexString()
        assertThat(keyHex).isEqualTo("af8cb133a78f8dc2d1359f18527593fb")

        // assert that all the initial keys were generated correctly
        for (keyIndex in 0u..31u) {
            val alias = DerivationKeyAlias(keyIndex).toString()
            val intermediateDerivationKey = keyRegisters.getKeyOrNone(alias)
            val actualHexValue =
                if (intermediateDerivationKey == null) {
                    null
                } else {
                    AesBlock(intermediateDerivationKey.encoded).toHexString()
                }
            val expectedHexValue = DukptFixtures.IntermediateKeys.AfterTx1[keyIndex.toInt()]
            assertThat(actualHexValue).isEqualTo(expectedHexValue)
        }
    }

    @Test
    fun `it correctly updates the intermediate derivation keys after second working key`() {
        val (dukpt, keyRegisters) = DukptFixtures.newDukpt()
        assertThat(dukpt.count).isEqualTo(1u)

        // generate the first key
        dukpt.generateWorkingKey()

        // generate the second key, the one we care about
        val (key, txCount) = dukpt.generateWorkingKey() // second key
        assertThat(txCount).isEqualTo(2u)
        val keyHex = (key as InMemoryWorkingKey).keyMaterial.toHexString()
        assertThat(keyHex).isEqualTo("d30bdc73ec9714b000bec66bdb7b6d09")

        // assert that all the initial keys were generated correctly
        for (keyIndex in 0u..31u) {
            val alias = DerivationKeyAlias(keyIndex).toString()
            val intermediateDerivationKey = keyRegisters.getKeyOrNone(alias)
            val actualHexValue =
                if (intermediateDerivationKey == null) {
                    null
                } else {
                    AesBlock(intermediateDerivationKey.encoded).toHexString()
                }
            val expectedHexValue = DukptFixtures.IntermediateKeys.AfterTx2[keyIndex.toInt()]
            if (actualHexValue != expectedHexValue) {
                println(keyIndex)
            }
            assertThat(actualHexValue).isEqualTo(expectedHexValue)
        }
    }

    @Test
    fun `it generates first 100 working keys correctly`() {
        val (dukpt) = DukptFixtures.newDukpt()

        for ((i, expectedWorkingKey) in DukptFixtures.WorkingKeys.withIndex()) {
            val (workingKey, actualTxCount) = dukpt.generateWorkingKey()

            val actualWorkingKey = (workingKey as InMemoryWorkingKey).keyMaterial.toHexString()
            assertThat(actualWorkingKey).isEqualTo(expectedWorkingKey)

            val expectedTxCount = (i + 1).toUInt()
            assertThat(actualTxCount).isEqualTo(expectedTxCount)
        }
    }
}
