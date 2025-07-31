package com.joinforage.forage.android.core.forageapi

import com.joinforage.forage.android.core.services.forageapi.requests.BaseApiRequest
import com.joinforage.forage.android.core.services.forageapi.requests.Headers

internal abstract class BaseServerPostApiRequest(
    url: Pair<String, String?>,
    authHeader: String,
    body: String
) : BaseApiRequest(url, HttpVerb.POST, authHeader, Headers(), body)
