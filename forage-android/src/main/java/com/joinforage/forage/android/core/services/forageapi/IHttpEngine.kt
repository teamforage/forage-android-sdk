package com.joinforage.forage.android.core.services.forageapi

internal interface IHttpEngine {
    suspend fun sendRequest(request: BaseApiRequest): String
}
