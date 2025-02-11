package com.joinforage.forage.android.pos.services.vault.requests

import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.requests.RosettaVaultRequest
import org.json.JSONObject

internal abstract class RosettaDeferRefundPaymentRequest(
    forageConfig: ForageConfig,
    traceId: String,
    idempotencyKey: String,
    paymentRef: String,
    body: JSONObject
) : RosettaVaultRequest(
    path = "proxy/api/payments/$paymentRef/refunds/collect_pin/",
    forageConfig = forageConfig,
    traceId = traceId,
    idempotencyKey = idempotencyKey,
    body = body
)
