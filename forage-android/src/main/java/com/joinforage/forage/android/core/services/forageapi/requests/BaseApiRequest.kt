package com.joinforage.forage.android.core.services.forageapi.requests

internal abstract class BaseApiRequest(
    val url: Pair<String, String?>,
    val verb: HttpVerb,
    authHeader: String,
    var headers: Headers,
    val body: String
) {
    init {
        headers = headers.setAuthorization(authHeader)
    }
    enum class HttpVerb { GET, POST }
}
