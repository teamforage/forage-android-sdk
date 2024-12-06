package com.joinforage.forage.android.pos.services.vault.requests

import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.ClientApiRequest
import com.joinforage.forage.android.core.services.forageapi.Headers
import com.joinforage.forage.android.core.services.forageapi.makeVaultUrl
import com.joinforage.forage.android.core.services.vault.VaultPaymentMethod
import org.json.JSONObject

internal abstract class RosettaVaultRequest(
    path: String,
    forageConfig: ForageConfig,
    traceId: String,
    idempotencyKey: String,
    paymentMethod: VaultPaymentMethod,
    body: JSONObject
) : ClientApiRequest.PostRequest(
    url = makeVaultUrl(forageConfig, path),
    forageConfig,
    traceId,
    apiVersion = Headers.ApiVersion.V_2024_01_08,
    headers = Headers(),
    body = body.apply {
        put("card_number_token", paymentMethod.token)
    }
) {
    init {
        headers = headers.setIdempotencyKey(idempotencyKey).setXKey("")
    }
}
