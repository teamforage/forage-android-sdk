package com.joinforage.forage.android.core.services.vault.requests

import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.requests.RosettaVaultRequest
import org.json.JSONObject

internal abstract class RosettaBalanceInquiryRequest(
    forageConfig: ForageConfig,
    traceId: String,
    idempotencyKey: String,
    body: JSONObject
) : RosettaVaultRequest(
    path = "proxy/api/payment_methods/balance/",
    forageConfig = forageConfig,
    traceId = traceId,
    idempotencyKey = idempotencyKey,
    body = body
)
