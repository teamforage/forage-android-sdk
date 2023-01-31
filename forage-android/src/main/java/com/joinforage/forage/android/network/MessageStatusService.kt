package com.joinforage.forage.android.network

import com.joinforage.forage.android.network.model.ForageApiResponse
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

internal class MessageStatusService(
    private val httpUrl: HttpUrl,
    okHttpClient: OkHttpClient
) : NetworkService(okHttpClient) {
    suspend fun getStatus(contentId: String): ForageApiResponse<String> = try {
        getStatusToCoroutine(contentId)
    } catch (ex: IOException) {
        ForageApiResponse.Failure(500, "server_error", ex.message.orEmpty())
    }

    private suspend fun getStatusToCoroutine(contentId: String): ForageApiResponse<String> {
        val url = getMessageStatusUrl(contentId)

        val request: Request = Request.Builder()
            .url(url)
            .get()
            .build()

        return convertCallbackToCoroutine(request)
    }

    private fun getMessageStatusUrl(contentId: String): HttpUrl {
        return httpUrl
            .newBuilder()
            .addPathSegment(ForageConstants.PathSegment.API)
            .addPathSegment(ForageConstants.PathSegment.MESSAGE)
            .addPathSegment(contentId)
            .build()
    }
}
