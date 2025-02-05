package com.joinforage.forage.android.ecom.services.vault.requests

import com.joinforage.forage.android.core.services.ForageConfig
import com.joinforage.forage.android.core.services.forageapi.requests.ClientApiRequest
import com.joinforage.forage.android.core.services.forageapi.requests.Headers
import com.joinforage.forage.android.core.services.forageapi.requests.makeApiUrl
import org.json.JSONObject

internal class PostPaymentMethodRequest(
    forageConfig: ForageConfig,
    traceId: String,
    requestBody: JSONObject,
) : ClientApiRequest.PostRequest(
    url = makeApiUrl(forageConfig, "api/payment_methods/"),
    forageConfig = forageConfig,
    traceId = traceId,
    apiVersion = Headers.ApiVersion.V_2023_05_15,
    headers = Headers(),
    body = requestBody
) 