package com.joinforage.forage.android.ecom.services.vault

import com.joinforage.forage.android.core.services.vault.requests.IBaseBodyBuilder
import org.json.JSONObject

internal class EcomBaseBodyBuilder(
    private val rawPin: String,
    private val cardNumberToken: String
) : IBaseBodyBuilder {
    override fun build(body: JSONObject): JSONObject = body.apply {
        put("pin", rawPin)
        put("card_number_token", cardNumberToken)
    }
}
