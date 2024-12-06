package com.joinforage.forage.android.core.services.vault.requests

import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.vault.VaultPaymentMethod
import com.joinforage.forage.android.pos.services.vault.requests.RosettaVaultRequest
import org.json.JSONObject

internal abstract class RosettaBalanceInquiryRequest(
    forageConfig: ForageConfig,
    traceId: String,
    idempotencyKey: String,
    paymentMethod: VaultPaymentMethod,
    body: JSONObject
) : RosettaVaultRequest(
    path = "proxy/api/payment_methods/${paymentMethod.ref}/balance/",
    forageConfig = forageConfig,
    traceId = traceId,
    idempotencyKey = idempotencyKey,
    paymentMethod = paymentMethod,
    body = body
)