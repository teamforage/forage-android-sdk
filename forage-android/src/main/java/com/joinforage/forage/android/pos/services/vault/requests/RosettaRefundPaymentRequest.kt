package com.joinforage.forage.android.pos.services.vault.requests

import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.vault.VaultPaymentMethod
import org.json.JSONObject

internal abstract class RosettaRefundPaymentRequest(
    forageConfig: ForageConfig,
    traceId: String,
    idempotencyKey: String,
    paymentMethod: VaultPaymentMethod,
    paymentRef: String,
    amount: Float,
    reason: String,
    metadata: Map<String, String>?,
    body: JSONObject
) : RosettaVaultRequest(
    path = "proxy/api/payments/$paymentRef/refunds/",
    forageConfig = forageConfig,
    traceId = traceId,
    idempotencyKey = idempotencyKey,
    paymentMethod = paymentMethod,
    body = body.apply {
        put("amount", amount)
        put("reason", reason)
        put("metadata", JSONObject(metadata ?: HashMap<String, String>()))
    }
)