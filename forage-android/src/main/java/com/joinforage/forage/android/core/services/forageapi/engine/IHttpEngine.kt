package com.joinforage.forage.android.core.services.forageapi.engine

import com.joinforage.forage.android.core.services.forageapi.requests.BaseApiRequest

internal interface IHttpEngine {
    suspend fun sendRequest(request: BaseApiRequest): String
}
