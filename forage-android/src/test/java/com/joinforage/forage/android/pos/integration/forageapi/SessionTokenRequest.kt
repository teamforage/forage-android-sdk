package com.joinforage.forage.android.pos.integration.forageapi

import com.joinforage.forage.android.core.services.forageapi.OkHttpEngine
import com.joinforage.forage.android.core.services.forageapi.makeApiUrl
import com.joinforage.forage.android.core.services.forageapi.makeBearerAuthHeader
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
    val jsonString = OkHttpEngine().sendRequest(
        SessionTokenRequest(accessToken, merchantRef)
    )
    return JSONObject(jsonString).getString("token")
}
