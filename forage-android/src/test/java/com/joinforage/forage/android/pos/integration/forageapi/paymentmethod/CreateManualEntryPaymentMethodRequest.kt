package com.joinforage.forage.android.pos.integration.forageapi.paymentmethod

import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.requests.ClientApiRequest
import com.joinforage.forage.android.core.services.forageapi.requests.Headers
import com.joinforage.forage.android.core.services.forageapi.requests.makeApiUrl
import org.json.JSONObject

internal class CreateManualEntryPaymentMethodRequest(
    pan: String,
    forageConfig: ForageConfig,
    traceId: String
) : ClientApiRequest.PostRequest(
    url = makeApiUrl(forageConfig, "api/payment_methods/"),
    forageConfig,
    traceId,
    apiVersion = Headers.ApiVersion.V_2023_05_15,
    headers = Headers(),
    body = JSONObject().apply {
        put("type", "ebt")
        put("reusable", true)
        put("customer_id", "Postman balance inquiry for Toast")
        put(
            "card",
            JSONObject().apply {
                put("number", pan)
            }
        )
    }
)
