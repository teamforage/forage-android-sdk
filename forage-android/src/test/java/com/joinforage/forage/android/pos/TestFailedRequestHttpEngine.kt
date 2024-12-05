package com.joinforage.forage.android.pos

import com.joinforage.forage.android.core.services.forageapi.BaseApiRequest
import com.joinforage.forage.android.core.services.forageapi.HttpRequestFailedException
import com.joinforage.forage.android.core.services.forageapi.IHttpEngine

internal class TestFailedRequestHttpEngine(private val exception: Exception) : IHttpEngine {
    override suspend fun sendRequest(request: BaseApiRequest): String {
        throw HttpRequestFailedException(exception)
    }
}
