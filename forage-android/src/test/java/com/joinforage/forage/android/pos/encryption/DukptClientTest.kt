package com.joinforage.forage.android.pos.encryption

import com.joinforage.forage.android.pos.encryption.dukpt.DukptService
import com.joinforage.forage.android.pos.encryption.storage.InMemoryWorkingKey
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class DukptClientTest {
    @Test
    fun `it correctly instantiates the all intermediate derivation keys on key load`() {
        val (dukpt, keyRegisters, firstKsn) = DukptFixtures.newDukpt()
        assertThat(firstKsn.dukptClientTxCount).isEqualTo(1u)
        assertThat(firstKsn.workingKeyTxCount).isEqualTo(0u)

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
        val (dukpt, keyRegisters, firstKsn) = DukptFixtures.newDukpt()
        assertThat(firstKsn.dukptClientTxCount).isEqualTo(1u)
        assertThat(firstKsn.workingKeyTxCount).isEqualTo(0u)

        // generate the second key, the one we care about
        val (key, ksn) = dukpt.generateWorkingKey() // second key

        // make sure ksn values are as we expect
        assertThat(ksn.dukptClientTxCount).isEqualTo(2u)
        assertThat(ksn.workingKeyTxCount).isEqualTo(1u)
        assertThat(ksn.baseDerivationKeyId).isEqualTo(DukptFixtures.Config.BaseDerivationKeyId.toHexString())
        assertThat(ksn.deviceDerivationId).isEqualTo(DukptFixtures.Config.DerivationDeviceId.toHexString())
        assertThat(ksn.apcKsn).isEqualTo(DukptFixtures.Config.InitialKeyId)
        assertThat(ksn.workingKeyTxCountAsBigEndian8CharHex).isEqualTo("00000001")

        // make sure the working key is as we expect
        val keyHex = (key as InMemoryWorkingKey).keyMaterial.toHexString()
        assertThat(keyHex).isEqualTo("af8cb133a78f8dc2d1359f18527593fb")

        // assert that all the intermediate derivation keys
        // are updated correctly
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
        val (dukpt, keyRegisters, firstKsn) = DukptFixtures.newDukpt()
        assertThat(firstKsn.dukptClientTxCount).isEqualTo(1u)
        assertThat(firstKsn.workingKeyTxCount).isEqualTo(0u)

        // generate the first key
        dukpt.generateWorkingKey()

        // generate the second key, the one we care about
        val (key, ksn) = dukpt.generateWorkingKey() // second key

        // make sure ksn values are as we expect
        assertThat(ksn.dukptClientTxCount).isEqualTo(3u)
        assertThat(ksn.workingKeyTxCount).isEqualTo(2u)
        assertThat(ksn.baseDerivationKeyId).isEqualTo(DukptFixtures.Config.BaseDerivationKeyId.toHexString())
        assertThat(ksn.deviceDerivationId).isEqualTo(DukptFixtures.Config.DerivationDeviceId.toHexString())
        assertThat(ksn.apcKsn).isEqualTo(DukptFixtures.Config.InitialKeyId)
        assertThat(ksn.workingKeyTxCountAsBigEndian8CharHex).isEqualTo("00000002")

        // make sure the working key is as we expect
        val keyHex = (key as InMemoryWorkingKey).keyMaterial.toHexString()
        assertThat(keyHex).isEqualTo("d30bdc73ec9714b000bec66bdb7b6d09")

        // assert that all the intermediate derivation keys
        // are updated correctly
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
        val (_, keyRegisters, firstKsn) = DukptFixtures.newDukpt()
        var ksn = firstKsn
        for ((i, expectedWorkingKey) in DukptFixtures.WorkingKeys.withIndex()) {
            val dukpt = DukptService(
                ksn = ksn,
                keyRegisters = keyRegisters
            )
            val (workingKey, nextKsn) = dukpt.generateWorkingKey()

            val expectedWorkingKeyTxCount = (i + 1).toUInt() // i is 0 indexed
            assertThat(nextKsn.workingKeyTxCount).isEqualTo(expectedWorkingKeyTxCount)

            val expectedDukptTxCount = (i + 2).toUInt() // i is 0 indexed
            assertThat(nextKsn.dukptClientTxCount).isEqualTo(expectedDukptTxCount)

            val actualWorkingKey = (workingKey as InMemoryWorkingKey).keyMaterial.toHexString()
            assertThat(actualWorkingKey).isEqualTo(expectedWorkingKey)

            // update ksn
            ksn = nextKsn
        }
    }
}
