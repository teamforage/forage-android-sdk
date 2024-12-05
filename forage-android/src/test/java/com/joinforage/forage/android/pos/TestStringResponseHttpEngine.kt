package com.joinforage.forage.android.pos

import com.joinforage.forage.android.core.services.forageapi.BaseApiRequest
import com.joinforage.forage.android.core.services.forageapi.IHttpEngine

internal class TestStringResponseHttpEngine(private val mockResponse: String) : IHttpEngine {
    override suspend fun sendRequest(request: BaseApiRequest): String {
        return mockResponse
    }
}
