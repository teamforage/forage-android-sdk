package com.joinforage.forage.android.core.services.forageapi.requests

import com.joinforage.forage.android.core.services.ForageConfig
import org.json.JSONObject

internal abstract class RosettaVaultRequest(
    path: String,
    forageConfig: ForageConfig,
    traceId: String,
    idempotencyKey: String,
    body: JSONObject
) : ClientApiRequest.PostRequest(
    url = makeVaultUrl(forageConfig, path),
    forageConfig,
    traceId,
    apiVersion = Headers.ApiVersion.V_2024_01_08,
    headers = Headers(),
    body = body
) {
    init {
        headers = headers.setIdempotencyKey(idempotencyKey).setXKey("")
    }
}
