package com.joinforage.forage.android.pos.emvchip

import com.joinforage.forage.android.pos.services.emvchip.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class CardInteractionTest {

    // Failures to cover

    @Test
    fun `MagSwipeInteraction does not throw exception when track2Data is missing semicolon`() {
        val track2Data = "1234567890123456=23011201201234567890"
        val interaction = MagSwipeInteraction(track2Data)
        assertThat(interaction.rawPan).isEqualTo("1234567890123456")
    }

    @Test
    fun `MagSwipeInteraction does not throw exception when track2Data is missing equals sign`() {
        val track2Data = ";1234567890123456"
        val interaction = MagSwipeInteraction(track2Data)
        assertThat(interaction.rawPan).isEqualTo(";1234567890123456")
    }

    @Test
    fun `MagSwipeInteraction does not throw exception when track2Data is empty string`() {
        val track2Data = ""
        val interaction = MagSwipeInteraction(track2Data)
        assertThat(interaction.rawPan).isEqualTo("")
    }

    @Test
    fun `MagSwipeInteraction sets interaction type to Unknown for unrecognized service code`() {
        val track2Data = ";1234567890123456=23019991201234567890"
        val interaction = MagSwipeInteraction(track2Data)
        assertThat(interaction.type).isEqualTo(CardholderInteractionType.Unknown)
    }

    // Successes to cover

    @Test
    fun `MagSwipeInteraction correctly parses 16-digit PAN as rawPan`() {
        val track2Data = ";1234567890123456=23011201201234567890"
        val interaction = MagSwipeInteraction(track2Data)
        assertThat(interaction.rawPan).isEqualTo("1234567890123456")
    }

    @Test
    fun `MagSwipeInteraction identifies interaction type for service code 120`() {
        val track2Data = ";1234567890123456=23011201201234567890"
        val interaction = MagSwipeInteraction(track2Data)
        assertThat(interaction.type).isEqualTo(CardholderInteractionType.MagSwipeLegacy)
    }

    @Test
    fun `MagSwipeInteraction identifies interaction type for service code 220`() {
        val track2Data = ";1234567890123456=23012201201234567890"
        val interaction = MagSwipeInteraction(track2Data)
        assertThat(interaction.type).isEqualTo(CardholderInteractionType.MagSwipeFallback)
        assertThat(interaction.emvField55Data).isNull()
    }

    @Test
    fun `TapEMVInteraction properties are as expected`() {
        val track2Data = ";1234567890123456=23011201201234567890"
        val emvField55Data = "someEmvData"
        val interaction = TapEMVInteraction(track2Data, emvField55Data)

        assertThat(interaction.rawPan).isEqualTo("1234567890123456")
        assertThat(interaction.type).isEqualTo(CardholderInteractionType.Tap)
        assertThat(interaction.track2Data).isEqualTo(track2Data)
        assertThat(interaction.emvField55Data).isEqualTo(emvField55Data)
    }

    @Test
    fun `InsertEMVInteraction properties are as expected`() {
        val track2Data = ";1234567890123456=23011201201234567890"
        val emvField55Data = "someEmvData"
        val interaction = InsertEMVInteraction(track2Data, emvField55Data)

        assertThat(interaction.rawPan).isEqualTo("1234567890123456")
        assertThat(interaction.type).isEqualTo(CardholderInteractionType.Insert)
        assertThat(interaction.track2Data).isEqualTo(track2Data)
        assertThat(interaction.emvField55Data).isEqualTo(emvField55Data)
    }

    @Test
    fun `ManualEntryInteraction properties are as expected`() {
        val rawPan = "1234567890123456"
        val interaction = ManualEntryInteraction(rawPan)

        assertThat(interaction.rawPan).isEqualTo(rawPan)
        assertThat(interaction.type).isEqualTo(CardholderInteractionType.KeyEntry)
        assertThat(interaction.track2Data).isNull()
        assertThat(interaction.emvField55Data).isNull()
    }
}