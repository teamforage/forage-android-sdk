package com.joinforage.forage.android.pos.encryption

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * I made this test while debugging the PinBlockIso4
 * module because I wasn't sure if the XOR logic was
 * failing.
 */
class AesBlockTest {
    @Test
    fun `test xor function`() {
        // Given two AesBlock instances with known data
        val block1 = AesBlock.fromHexString("a1b2c3d4e5f601234567890abcdef012")
        val block2 = AesBlock.fromHexString("1234567890abcdef0123456789abcdef")
        val expectedHexString = "b38695ac755dcccc4444cc6d35753dfd"
        val actualHexString = block1.xor(block2).toHexString()
        assertThat(expectedHexString).isEqualTo(actualHexString)
    }
}
