package com.joinforage.forage.android.pos.integration.forageapi

import com.joinforage.forage.android.core.services.forageapi.Headers

internal abstract class AuthRequest(
    url: String,
    authHeader: String,
    body: String
) : BaseServerPostApiRequest(url, authHeader, body) {
    init {
        headers = headers
            .setContentType(Headers.ContentType.APPLICATION_FORM_URLENCODED)
    }
}
