package com.joinforage.forage.android.pos.encryption

import com.joinforage.forage.android.pos.services.encryption.iso4.PanFieldIso4
import com.joinforage.forage.android.pos.services.encryption.iso4.PinBlockIso4
import com.joinforage.forage.android.pos.services.encryption.iso4.PinFieldIso4
import com.joinforage.forage.android.pos.services.encryption.storage.InMemoryWorkingKey
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal val randomPadding = "2F69ADDE2E9E7ACE"
internal val pin = PinFieldIso4("1234", randomPadding)
internal val pan = PanFieldIso4("4111111111111111")

class PinBlockIso4Test {
    @Test
    fun `it should correctly encrypt with wk1`() {
        val workingKey = InMemoryWorkingKey.fromHex("AF8CB133A78F8DC2D1359F18527593FB")
        val expectedIso4PinBlock = "A912150391AB65A67E52883D81CE2D15"
        val actualIso4PinBlock =
            PinBlockIso4(pan, pin, workingKey).contents.toHexString().uppercase()
        assertThat(actualIso4PinBlock).isEqualTo(expectedIso4PinBlock)
    }

    @Test
    fun `it should correctly encrypt with wk2`() {
        val workingKey = InMemoryWorkingKey.fromHex("D30BDC73EC9714B000BEC66BDB7B6D09")
        val expectedIso4PinBlock = "52A00503BD34BA1383F6A7EE9FE2547F"
        val actualIso4PinBlock =
            PinBlockIso4(pan, pin, workingKey).contents.toHexString().uppercase()
        assertThat(actualIso4PinBlock).isEqualTo(expectedIso4PinBlock)
    }

    @Test
    fun `it should correctly encrypt with wk3`() {
        val workingKey = InMemoryWorkingKey.fromHex("7D69F01F3B45449F62C7816ECE723268")
        val expectedIso4PinBlock = "A5A27E82B43A9A866A93D7ABE89CEF93"
        val actualIso4PinBlock =
            PinBlockIso4(pan, pin, workingKey).contents.toHexString().uppercase()
        assertThat(actualIso4PinBlock).isEqualTo(expectedIso4PinBlock)
    }
}
