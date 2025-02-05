package com.joinforage.forage.android.pos.services.vault.requests

import com.joinforage.forage.android.core.services.vault.requests.IBaseBodyBuilder
import com.joinforage.forage.android.pos.services.emvchip.CardholderInteraction
import com.joinforage.forage.android.pos.services.emvchip.CardholderInteractionType
import com.joinforage.forage.android.pos.services.emvchip.TerminalCapabilities
import org.json.JSONObject

internal class PosBaseBodyBuilder(
    private val encryptedPinBlock: String,
    private val keySerialNumber: String,
    private val txnCounter: String,
    private val interaction: CardholderInteraction,
    private val capabilities: TerminalCapabilities,
    private val posTerminalId: String
) : IBaseBodyBuilder {
    override fun build(body: JSONObject): JSONObject = body.apply {
        put("pin", encryptedPinBlock)
        put("ksn", keySerialNumber)
        put("txn_counter", txnCounter)
        put(
            "pos_terminal",
            (body.optJSONObject("pos_terminal") ?: JSONObject()).apply {
                put("provider_terminal_id", posTerminalId)
                put(
                    "card_details",
                    JSONObject().apply {
                        put("terminal_capabilities", capabilities.value)
                        put("pos_entry_mode", interaction.type.value)
                        if (interaction.type == CardholderInteractionType.KeyEntry) {
                            put("manual_entry_pan", interaction.rawPan)
                            put("track_2_data", "")
                        } else {
                            put("track_2_data", interaction.track2Data)
                        }
                    }
                )
            }
        )
    }
}
