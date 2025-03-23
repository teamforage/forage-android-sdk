package com.joinforage.forage.android.core.forageapi

import com.joinforage.forage.android.core.services.forageapi.requests.makeApiUrl
import com.joinforage.forage.android.core.services.forageapi.requests.makeBearerAuthHeader
import com.joinforage.forage.android.pos.services.forageapi.engine.PosOkHttpEngine
import org.json.JSONObject

internal class SessionTokenRequest(
    accessToken: String,
    merchantRef: String
) : AuthRequest(
    url = makeApiUrl(accessToken, "api/session_token/"),
    authHeader = makeBearerAuthHeader(accessToken),
    body = "grant_type=client_credentials&scope=pinpad_only"
) {
    init {
        headers = headers.setMerchantAccount(merchantRef)
    }
}

internal suspend fun getSessionToken(accessToken: String, merchantRef: String): String {
    val jsonString = PosOkHttpEngine().sendRequest(
        SessionTokenRequest(accessToken, merchantRef)
    )
    return JSONObject(jsonString).getString("token")
}
