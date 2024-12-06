package com.joinforage.forage.android.pos.services.init

import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.ClientApiRequest
import com.joinforage.forage.android.core.services.forageapi.Headers
import com.joinforage.forage.android.core.services.forageapi.makeVaultUrl
import org.json.JSONObject

internal class InitializePosRequest(
    forageConfig: ForageConfig,
    traceId: String,
    posTerminalId: String,
    base64PublicKeyPEM: String
) : ClientApiRequest.PostRequest(
    url = makeVaultUrl(forageConfig, "api/terminal/initialize/"),
    forageConfig = forageConfig,
    traceId = traceId,
    apiVersion = Headers.ApiVersion.V_DEFAULT,
    headers = Headers(posTerminalId = posTerminalId),
    body = JSONObject().apply {
        put("certificate", base64PublicKeyPEM)
    }
)
