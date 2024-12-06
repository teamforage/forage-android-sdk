package com.joinforage.forage.android.core.services.forageapi.requests

import com.joinforage.forage.android.core.services.EnvConfig
import com.joinforage.forage.android.core.services.ForageConfig
import org.json.JSONObject

internal fun getApiBase(token: String) = EnvConfig.fromSessionToken(token).apiBaseUrl
internal fun makeApiUrl(token: String, path: String) = "${getApiBase(token)}$path"
internal fun makeApiUrl(forageConfig: ForageConfig, path: String) =
    makeApiUrl(forageConfig.sessionToken, path)
internal fun makeApiUrl(env: EnvConfig, path: String) = "${env.apiBaseUrl}$path"

internal fun getVaultBase(token: String) = EnvConfig.fromSessionToken(token).vaultBaseUrl
internal fun makeVaultUrl(token: String, path: String) = "${getVaultBase(token)}$path"
internal fun makeVaultUrl(forageConfig: ForageConfig, path: String) =
    makeVaultUrl(forageConfig.sessionToken, path)

internal fun makeBearerAuthHeader(token: String) = "Bearer $token"

internal sealed class ClientApiRequest(
    url: String,
    verb: HttpVerb,
    forageConfig: ForageConfig,
    traceId: String,
    apiVersion: Headers.ApiVersion,
    headers: Headers,
    body: JSONObject
) : BaseApiRequest(
    url = url,
    verb = verb,
    authHeader = makeBearerAuthHeader(forageConfig.sessionToken),
    headers = headers
        .setMerchantAccount(forageConfig.merchantId)
        .setApiVersion(apiVersion)
        .setTraceId(traceId)
        .setContentType(Headers.ContentType.APPLICATION_JSON),
    body = body.toString()
) {

    internal abstract class PostRequest(
        url: String,
        forageConfig: ForageConfig,
        traceId: String,
        apiVersion: Headers.ApiVersion,
        headers: Headers,
        body: JSONObject
    ) : ClientApiRequest(
        url,
        HttpVerb.POST,
        forageConfig,
        traceId,
        apiVersion,
        headers,
        body
    )

    internal abstract class GetRequest(
        path: String,
        forageConfig: ForageConfig,
        traceId: String,
        apiVersion: Headers.ApiVersion
    ) : ClientApiRequest(
        url = makeApiUrl(forageConfig, path),
        HttpVerb.GET,
        forageConfig,
        traceId,
        apiVersion,
        Headers(),
        JSONObject()
    )
}
