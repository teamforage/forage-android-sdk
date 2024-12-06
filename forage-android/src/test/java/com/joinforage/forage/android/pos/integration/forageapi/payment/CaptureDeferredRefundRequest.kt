package com.joinforage.forage.android.pos.integration.forageapi.payment

import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.requests.ClientApiRequest
import com.joinforage.forage.android.core.services.forageapi.requests.Headers
import com.joinforage.forage.android.core.services.forageapi.requests.makeApiUrl
import com.joinforage.forage.android.core.services.generateTraceId
import org.json.JSONObject

internal class CaptureDeferredRefundRequest(
    paymentRef: String,
    forageConfig: ForageConfig,
    traceId: String,
    posTerminalId: String,
    amount: Float,
    reason: String,
    metadata: Map<String, String>
) : ClientApiRequest.PostRequest(
    url = makeApiUrl(forageConfig, "api/payments/$paymentRef/refunds/"),
    forageConfig,
    traceId,
    apiVersion = Headers.ApiVersion.V_DEFAULT,
    headers = Headers(idempotencyKey = generateTraceId()),
    body = JSONObject().apply {
        put("amount", amount)
        put("reason", reason)
        put(
            "pos_terminal",
            JSONObject().apply {
                put("provider_terminal_id", posTerminalId)
            }
        )
        put("metadata", JSONObject(metadata))
    }
)
