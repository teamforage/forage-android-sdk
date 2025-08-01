package com.joinforage.forage.android.core.services.vault.requests

import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.requests.RosettaVaultRequest
import org.json.JSONObject

internal abstract class RosettaCapturePaymentRequest(
    forageConfig: ForageConfig,
    traceId: String,
    idempotencyKey: String,
    paymentRef: String,
    body: JSONObject
) : RosettaVaultRequest(
    path = "proxy/api/payments/$paymentRef/capture/",
    forageConfig = forageConfig,
    traceId = traceId,
    idempotencyKey = idempotencyKey,
    body = body
)
