package com.joinforage.forage.android.pos.services.init

import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.ClientApiRequest
import com.joinforage.forage.android.core.services.forageapi.Headers
import com.joinforage.forage.android.core.services.forageapi.makeVaultUrl
import org.json.JSONObject

internal class CertificateSigningRequest(
    forageConfig: ForageConfig,
    traceId: String,
    posTerminalId: String,
    csr: String
) : ClientApiRequest.PostRequest(
    url = makeVaultUrl(forageConfig, "api/terminal/certificate/"),
    forageConfig = forageConfig,
    traceId = traceId,
    apiVersion = Headers.ApiVersion.V_DEFAULT,
    headers = Headers(posTerminalId = posTerminalId),
    body = JSONObject().apply {
        put("csr", csr)
    }
)
