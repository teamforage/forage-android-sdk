package com.joinforage.forage.android.ecom.forageapi.refund

import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.requests.ClientApiRequest
import com.joinforage.forage.android.core.services.forageapi.requests.Headers
import com.joinforage.forage.android.core.services.forageapi.requests.makeApiUrl
import com.joinforage.forage.android.core.services.generateTraceId
import org.json.JSONObject

internal class RefundPaymentRequest(
    amount: String,
    paymentRef: String,
    reason: String,
    metadata: Map<String, String>,
    forageConfig: ForageConfig,
    traceId: String
) : ClientApiRequest.PostRequest(
    url = makeApiUrl(forageConfig, "api/payments/$paymentRef/refunds/"),
    forageConfig,
    traceId,
    apiVersion = Headers.ApiVersion.V_DEFAULT,
    headers = Headers(idempotencyKey = generateTraceId()),
    body = JSONObject().apply {
        put("amount", amount)
        put("reason", reason)
        put("metadata", JSONObject(metadata))
    }
)
