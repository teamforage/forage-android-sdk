package com.joinforage.forage.android.pos

import com.joinforage.forage.android.core.services.forageapi.engine.IHttpEngine
import com.joinforage.forage.android.core.services.forageapi.requests.BaseApiRequest

internal class TestStringResponseHttpEngine(private val mockResponse: String) : IHttpEngine {
    override suspend fun sendRequest(request: BaseApiRequest): String {
        return mockResponse
    }
}
