package com.joinforage.forage.android.pos.integration.forageapi

import com.joinforage.forage.android.core.services.forageapi.BaseApiRequest
import com.joinforage.forage.android.core.services.forageapi.Headers

internal abstract class BaseServerPostApiRequest(
    url: String,
    authHeader: String,
    body: String
) : BaseApiRequest(url, HttpVerb.POST, authHeader, Headers(), body)
