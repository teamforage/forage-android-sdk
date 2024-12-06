package com.joinforage.forage.android.core.services.vault.requests

import org.json.JSONObject

internal class EcomBaseBodyBuilder(
    private val rawPin: String
) : IBaseBodyBuilder {
    override fun build(body: JSONObject): JSONObject = body.apply {
        put("pin", rawPin)
    }
}