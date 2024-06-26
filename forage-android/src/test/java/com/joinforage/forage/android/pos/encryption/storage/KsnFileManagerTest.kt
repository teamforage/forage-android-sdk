package com.joinforage.forage.android.pos.encryption.storage

import com.joinforage.forage.android.pos.services.encryption.storage.KeySerialNumber
import com.joinforage.forage.android.pos.services.encryption.storage.KsnFileManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

val initialKeyId1 = "0123456789abcedf" // 16 chars
val bdkId1 = initialKeyId1.substring(0, 8)
val deviceId1 = initialKeyId1.substring(8, 16)
val initialKeyId2 = initialKeyId1.reversed() // 16 chars

class KsnFileManagerInitTest {
    @Test
    fun `init should exit early - matching bdkId and deviceId`() {
        val ksn = KsnFileManager.byString()
        ksn.init(initialKeyId1) // initialKeyId = bdkId + deviceId
        val result = ksn.init(initialKeyId1)
        assertThat(result).isFalse
    }

    @Test
    fun `init should run - file does not exist`() {
        val ksn = KsnFileManager.byString()
        val result = ksn.init(initialKeyId1)
        assertThat(result).isTrue
    }

    @Test
    fun `init should run - different bdkId`() {
        val ksn = KsnFileManager.byString()
        ksn.init(initialKeyId1) // initialKeyId = bdkId + deviceId
        val differentBdkId = "ffffffff"
        val differentInitialKeyId = differentBdkId + initialKeyId1.drop(8)
        val result = ksn.init(differentInitialKeyId)
        assertThat(result).isTrue
    }

    @Test
    fun `init should run - different deviceId`() {
        val ksn = KsnFileManager.byString()
        ksn.init(initialKeyId1) // initialKeyId = bdkId + deviceId
        val differentDeviceId = "00000000"
        val differentInitialKeyId = initialKeyId1.take(8) + differentDeviceId
        val result = ksn.init(differentInitialKeyId)
        assertThat(result).isTrue
    }

    // TODO: figure out how to test cases where contents of file are null
}

class KsnFileManagerAccessorsTest {
    @Test
    fun `accessors should be null before init`() {
        val ksn = KsnFileManager.byString()
        assertThat(ksn.readDukptClientTxCount()).isNull()
        assertThat(ksn.readBaseDerivationKeyId()).isNull()
        assertThat(ksn.readDeviceDerivationId()).isNull()
        assertThat(ksn.readAll()).isNull()
    }

    @Test
    fun `accessors should return accurate values`() {
        val ksn = KsnFileManager.byString()
        ksn.init(initialKeyId1)
        assertThat(ksn.readDukptClientTxCount()!!.toUInt()).isEqualTo(0u)
        assertThat(ksn.readBaseDerivationKeyId()!!.toHexString()).isEqualTo(bdkId1)
        assertThat(ksn.readDeviceDerivationId()!!.toHexString()).isEqualTo(deviceId1)
        val ksnObj = ksn.readAll()
        assertThat(ksnObj).isNotNull

        // TODO: should probably move these tests to their own file
        //  since they test KeySerialNumber
        assertThat(ksnObj!!.apcKsn).isEqualTo(initialKeyId1)
        assertThat(ksnObj.fileContent).isEqualTo(
            "" +
                "${bdkId1}\n" +
                "${deviceId1}\n" +
                "0\n"
        )
        assertThat(ksnObj.baseDerivationKeyId).isEqualTo(bdkId1)
        assertThat(ksnObj.deviceDerivationId).isEqualTo(deviceId1)

        // NOTE: we don't test ksnObj.workingKeyTxCount here
        // because it's value has no meaning without calling
        // .loadKey nor .generateWorkingKey. Only
        // the value of dukptClientTxCount has meaning in this
        // test
        assertThat(ksnObj.dukptClientTxCount).isEqualTo(0u)
    }

    @Test
    fun `working key dutkpClient = 0u right after init`() {
        val ksn = KsnFileManager.byString()
        ksn.init(initialKeyId1)
        ksn.init(initialKeyId2)
        val ksnObj = ksn.readAll()

        // NOTE: we don't test ksnObj.workingKeyTxCount here
        // because it's value has no meaning without calling
        // .loadKey nor .generateWorkingKey. Only
        // the value of dukptClientTxCount has meaning in this
        // test
        assertThat(ksnObj!!.dukptClientTxCount).isEqualTo(0u)
    }

    @Test
    fun `update txCount should succeed for same bdkId and deviceId`() {
        val ksn = KsnFileManager.byString()
        ksn.init(initialKeyId1)
        val expectedTxCount = 17u
        ksn.updateKsn(
            KeySerialNumber(
                baseDerivationKeyId = bdkId1,
                deviceDerivationId = deviceId1,
                dukptClientTxCount = expectedTxCount
            )
        )
        assertThat(ksn.readDukptClientTxCount()!!.toUInt()).isEqualTo(expectedTxCount)

        // TODO: should probably move this to it's own test file since
        //  KeySerialNumber is different from KsnFileManager
        val ksnObj = ksn.readAll()!!
        assertThat(ksnObj.dukptClientTxCount).isEqualTo(expectedTxCount)
        assertThat(ksnObj.workingKeyTxCount).isEqualTo(16u)
        assertThat(ksnObj.workingKeyTxCountAsBigEndian8CharHex).isEqualTo("00000010")
    }

    @Test
    fun `update should fail for mismatching bdkId`() {
        val ksn = KsnFileManager.byString()
        ksn.init(initialKeyId1)
        val expectedTxCount = 17u
        assertThrows(IllegalArgumentException::class.java) {
            ksn.updateKsn(
                KeySerialNumber(
                    baseDerivationKeyId = "ffffffff",
                    deviceDerivationId = deviceId1,
                    dukptClientTxCount = expectedTxCount
                )
            )
        }
    }

    @Test
    fun `update should fail for mismatching deviceId`() {
        val ksn = KsnFileManager.byString()
        ksn.init(initialKeyId1)
        val expectedTxCount = 17u
        assertThrows(IllegalArgumentException::class.java) {
            ksn.updateKsn(
                KeySerialNumber(
                    baseDerivationKeyId = bdkId1,
                    deviceDerivationId = "ffffffff",
                    dukptClientTxCount = expectedTxCount
                )
            )
        }
    }
}
