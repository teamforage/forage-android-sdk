package com.joinforage.forage.android.network

import com.joinforage.forage.android.addTrailingSlash
import com.joinforage.forage.android.core.telemetry.Log
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ForageError
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

internal class MessageStatusService(
    private val httpUrl: String,
    okHttpClient: OkHttpClient,
    private val logger: Log
) : NetworkService(okHttpClient, logger) {
    suspend fun getStatus(contentId: String): ForageApiResponse<String> = try {
        logger.i("[HTTP] GET request for SQS Message", attributes = mapOf("content_id" to contentId))
        getStatusToCoroutine(contentId)
    } catch (ex: IOException) {
        logger.e(
            "[HTTP] Failed while trying to GET SQS Message",
            ex,
            attributes = mapOf("content_id" to contentId)
        )
        ForageApiResponse.Failure(listOf(ForageError(500, "unknown_server_error", ex.message.orEmpty())))
    }

    private suspend fun getStatusToCoroutine(contentId: String): ForageApiResponse<String> {
        val url = getMessageStatusUrl(contentId)

        val request: Request = Request.Builder()
            .url(url)
            .header(ForageConstants.Headers.API_VERSION, "2023-02-01")
            .get()
            .build()

        return convertCallbackToCoroutine(request)
    }

    private fun getMessageStatusUrl(contentId: String): HttpUrl = httpUrl.toHttpUrlOrNull()!!
            .newBuilder()
            .addPathSegment(ForageConstants.PathSegment.API)
            .addPathSegment(ForageConstants.PathSegment.MESSAGE)
            .addPathSegment(contentId)
            .addTrailingSlash()
            .build()
}
