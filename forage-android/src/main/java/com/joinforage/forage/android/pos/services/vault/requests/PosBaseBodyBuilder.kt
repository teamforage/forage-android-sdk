package com.joinforage.forage.android.pos.services.vault.requests

import com.joinforage.forage.android.core.services.vault.requests.IBaseBodyBuilder
import com.joinforage.forage.android.pos.services.CardholderInteraction
import com.joinforage.forage.android.pos.services.CardholderInteractionType
import com.joinforage.forage.android.pos.services.TerminalCapabilities
import org.json.JSONObject

internal class PosBaseBodyBuilder(
    private val encryptedPinBlock: String,
    private val keySerialNumber: String,
    private val txnCounter: String,
    private val interaction: CardholderInteraction,
    private val capabilities: TerminalCapabilities
) : IBaseBodyBuilder {
    override fun build(body: JSONObject): JSONObject = body.apply {
        put("pin", encryptedPinBlock)
        put("ksn", keySerialNumber)
        put("txn_counter", txnCounter)
        put(
            "pos_terminal",
            (body.optJSONObject("pos_terminal") ?: JSONObject()).apply {
                put(
                    "ecl_fallback",
                    JSONObject().apply {
                        put("terminal_capabilities", capabilities)
                        put("cardholder_interaction_type", interaction.type.name)
                        if (interaction.type == CardholderInteractionType.KeyEntry) {
                            put("manual_entry_pan", interaction.rawPan)
                        } else {
                            put("track_2_data", interaction.track2Data)
                        }
                    }
                )
            }
        )
    }
}
