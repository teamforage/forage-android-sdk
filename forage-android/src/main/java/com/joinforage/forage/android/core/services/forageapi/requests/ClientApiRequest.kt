package com.joinforage.forage.android.core.services.forageapi.requests

import com.joinforage.forage.android.core.services.EnvConfig
import com.joinforage.forage.android.core.services.ForageConfig
import org.json.JSONObject

internal fun getApiBase(token: String): Pair<String, String?> =
    EnvConfig.fromSessionToken(token).let { Pair(it.apiBaseUrl, it.apiHost) }
internal fun makeApiUrl(token: String, path: String): Pair<String, String?> =
    getApiBase(token).let { Pair("${it.first}$path", it.second) }
internal fun makeApiUrl(forageConfig: ForageConfig, path: String) =
    makeApiUrl(forageConfig.sessionToken, path)
internal fun makeApiUrl(env: EnvConfig, path: String) =
    Pair("${env.apiBaseUrl}$path", env.apiHost)

internal fun getVaultBase(token: String): Pair<String, String?> =
    EnvConfig.fromSessionToken(token).let { Pair(it.vaultBaseUrl, it.vaultHost) }
internal fun makeVaultUrl(token: String, path: String): Pair<String, String?> =
    getVaultBase(token).let { Pair("${it.first}$path", it.second) }
internal fun makeVaultUrl(forageConfig: ForageConfig, path: String) =
    makeVaultUrl(forageConfig.sessionToken, path)

internal fun makeBearerAuthHeader(token: String) = "Bearer $token"

internal sealed class ClientApiRequest(
    url: Pair<String, String?>,
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
        url: Pair<String, String?>,
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
