package com.joinforage.forage.android.pos.integration.forageapi

import com.joinforage.forage.android.core.services.EnvConfig
import com.joinforage.forage.android.core.services.forageapi.requests.makeApiUrl
import java.util.Base64

internal fun makeBasicAuthHeader(username: String, password: String) =
    "Basic " + Base64.getEncoder().encodeToString("$username:$password".toByteArray())

internal class AccessTokenRequest(
    username: String,
    password: String,
    env: EnvConfig
) : AuthRequest(
    url = makeApiUrl(env, "o/token/"),
    authHeader = makeBasicAuthHeader(username, password),
    body = "grant_type=client_credentials&scope=pinpad_only&expiration=60"
)

internal suspend fun getAccessToken(username: String, password: String, env: EnvConfig): String {
//    val jsonString = OkHttpEngine().sendRequest(
//        AccessTokenRequest(username, password, env)
//    )
//    return JSONObject(jsonString).getString("access_token")
    return "staging_RzrycWOLtRbod45z40srA4bFWmVPZe"
}
