package com.joinforage.forage.android.pos.integration

import com.joinforage.forage.android.pos.services.emvchip.MagSwipeInteraction
import com.joinforage.forage.android.pos.services.emvchip.ManualEntryInteraction
import com.joinforage.forage.android.pos.services.emvchip.TerminalCapabilities
import com.joinforage.forage.android.pos.services.vault.requests.PosBaseBodyBuilder
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test

class PosBaseBodyBuilderTest {
    private val testTerminalId = "test-terminal-id"

    @Test
    fun `pos_terminal existing keys are not overwritten`() {
        val existingBody = JSONObject().apply {
            put(
                "pos_terminal",
                JSONObject().apply {
                    put("existing_key", "existing_value")
                }
            )
        }
        val builder = PosBaseBodyBuilder(
            encryptedPinBlock = "encryptedPin",
            keySerialNumber = "ksn",
            txnCounter = "txnCounter",
            interaction = ManualEntryInteraction("rawPan"),
            capabilities = TerminalCapabilities.TapAndInsert,
            posTerminalId = testTerminalId
        )

        val result = builder.build(existingBody)

        val posTerminal = result.getJSONObject("pos_terminal")
        assertThat(posTerminal.getString("existing_key")).isEqualTo("existing_value")
        assertThat(posTerminal.has("card_details")).isTrue
        assertThat(posTerminal.getString("provider_terminal_id")).isEqualTo(testTerminalId)
    }

    @Test
    fun `manual_entry_pan is included for KeyEntry type`() {
        val body = JSONObject()
        val builder = PosBaseBodyBuilder(
            encryptedPinBlock = "encryptedPin",
            keySerialNumber = "ksn",
            txnCounter = "txnCounter",
            interaction = ManualEntryInteraction("rawPan"),
            capabilities = TerminalCapabilities.TapAndInsert,
            posTerminalId = testTerminalId
        )

        val result = builder.build(body)

        val posTerminal = result.getJSONObject("pos_terminal")
        val cardDetails = posTerminal.getJSONObject("card_details")
        assertThat(cardDetails.getString("manual_entry_pan")).isEqualTo("rawPan")
        assertThat(posTerminal.getString("provider_terminal_id")).isEqualTo(testTerminalId)
    }

    @Test
    fun `track_2_data is included for non-KeyEntry types`() {
        val body = JSONObject()
        val builder = PosBaseBodyBuilder(
            encryptedPinBlock = "encryptedPin",
            keySerialNumber = "ksn",
            txnCounter = "txnCounter",
            interaction = MagSwipeInteraction("track2Data"),
            capabilities = TerminalCapabilities.TapAndInsert,
            posTerminalId = testTerminalId
        )

        val result = builder.build(body)

        val posTerminal = result.getJSONObject("pos_terminal")
        val cardDetails = posTerminal.getJSONObject("card_details")
        assertThat(cardDetails.getString("track_2_data")).isEqualTo("track2Data")
        assertThat(cardDetails.has("manual_entry_pan")).isFalse
        assertThat(posTerminal.getString("provider_terminal_id")).isEqualTo(testTerminalId)
    }

    @Test
    fun `all JSON keys are present with correct values`() {
        val body = JSONObject()
        val encryptedPinBlock = "encryptedPin"
        val keySerialNumber = "ksn"
        val txnCounter = "txnCounter"
        val track2Data = "track2Data"
        val interaction = MagSwipeInteraction(track2Data)
        val capabilities = TerminalCapabilities.TapAndInsert

        val builder = PosBaseBodyBuilder(
            encryptedPinBlock = encryptedPinBlock,
            keySerialNumber = keySerialNumber,
            txnCounter = txnCounter,
            interaction = interaction,
            capabilities = capabilities,
            posTerminalId = testTerminalId
        )

        val result = builder.build(body)

        assertThat(result.getString("pin")).isEqualTo(encryptedPinBlock)
        assertThat(result.getString("ksn")).isEqualTo(keySerialNumber)
        assertThat(result.getString("txn_counter")).isEqualTo(txnCounter)

        val posTerminal = result.getJSONObject("pos_terminal")
        val cardDetails = posTerminal.getJSONObject("card_details")

        assertThat(cardDetails.getString("terminal_capabilities")).isEqualTo(capabilities.value)
        assertThat(cardDetails.getString("pos_entry_mode")).isEqualTo(interaction.type.value)
        assertThat(cardDetails.getString("track_2_data")).isEqualTo(track2Data)
        assertThat(cardDetails.has("manual_entry_pan")).isFalse
        assertThat(posTerminal.getString("provider_terminal_id")).isEqualTo(testTerminalId)
    }
}
