package com.joinforage.forage.android.core.forageapi.payment

import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.requests.ClientApiRequest
import com.joinforage.forage.android.core.services.forageapi.requests.Headers
import com.joinforage.forage.android.core.services.forageapi.requests.makeApiUrl
import com.joinforage.forage.android.core.services.generateTraceId
import org.json.JSONObject

internal class CreatePaymentRequest(
    amount: String,
    fundingType: String,
    description: String,
    metadata: Map<String, String>,
    paymentMethodRef: String,
    posTerminalId: String,
    forageConfig: ForageConfig,
    traceId: String
) : ClientApiRequest.PostRequest(
    url = makeApiUrl(forageConfig, "api/payments/"),
    forageConfig,
    traceId,
    apiVersion = Headers.ApiVersion.V_DEFAULT,
    headers = Headers(idempotencyKey = generateTraceId()),
    body = JSONObject().apply {
        put("amount", amount)
        put("funding_type", fundingType)
        put("description", description)
        put("metadata", JSONObject(metadata))
        put("payment_method", paymentMethodRef)
        put(
            "pos_terminal",
            JSONObject().apply {
                put("provider_terminal_id", posTerminalId)
            }
        )
    }
)
