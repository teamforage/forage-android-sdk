package com.joinforage.forage.android.pos.integration.forageapi.payment

import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.requests.ClientApiRequest
import com.joinforage.forage.android.core.services.forageapi.requests.Headers
import com.joinforage.forage.android.core.services.forageapi.requests.makeApiUrl
import com.joinforage.forage.android.core.services.generateTraceId
import org.json.JSONObject

internal class CaptureDeferredPaymentRequest(
    paymentRef: String,
    forageConfig: ForageConfig,
    traceId: String
) : ClientApiRequest.PostRequest(
    url = makeApiUrl(forageConfig, "api/payments/$paymentRef/capture_payment/"),
    forageConfig,
    traceId,
    apiVersion = Headers.ApiVersion.V_DEFAULT,
    headers = Headers(idempotencyKey = generateTraceId()),
    body = JSONObject()
)
