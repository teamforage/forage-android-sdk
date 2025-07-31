package com.joinforage.forage.android.core.forageapi

import com.joinforage.forage.android.core.services.forageapi.requests.Headers

internal abstract class AuthRequest(
    url: Pair<String, String?>,
    authHeader: String,
    body: String
) : BaseServerPostApiRequest(url, authHeader, body) {
    init {
        headers = headers
            .setContentType(Headers.ContentType.APPLICATION_FORM_URLENCODED)
    }
}
