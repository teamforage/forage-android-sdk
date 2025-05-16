package com.joinforage.forage.android.core

import com.joinforage.forage.android.core.services.forageapi.engine.HttpRequestFailedException
import com.joinforage.forage.android.core.services.forageapi.engine.IHttpEngine
import com.joinforage.forage.android.core.services.forageapi.requests.BaseApiRequest

internal class TestFailedRequestHttpEngine(private val exception: Exception) : IHttpEngine {
    override suspend fun sendRequest(request: BaseApiRequest): String {
        throw HttpRequestFailedException(exception)
    }
}
