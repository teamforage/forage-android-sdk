package com.joinforage.forage.android.core.services.forageapi

import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.network.error.ForageError
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.suspendCoroutine

internal class OkHttpEngine : IHttpEngine {

    fun onFailure(e: IOException): Result<String> = Result.failure(HttpRequestFailedException(e))

    fun onResponse(response: Response): Result<String> = response.use {
        val jsonBody = it.body?.string().orEmpty()
        if (it.isSuccessful) {
            Result.success(jsonBody)
        } else {
            val error = ForageError(it.code, jsonBody)
            Result.failure(ForageErrorResponseException(error))
        }
    }

    fun buildOkHttpRequest(request: BaseApiRequest): Request {
        val builder = Request.Builder().url(request.url)
        request.headers.forEach { (key, value) ->
            builder.addHeader(key, value)
        }
        when (request) {
            is ClientApiRequest.GetRequest -> {
                builder.get()
            }
            else -> {
                val bodyString = request.body
                val mediaType = request.headers.contentType!!.mediaType.toMediaType()
                builder.post(
                    bodyString.toRequestBody(mediaType)
                )
            }
        }
        return builder.build()
    }

    override suspend fun sendRequest(request: BaseApiRequest): String {
        return suspendCoroutine { continuation ->
            val okHttpRequest = buildOkHttpRequest(request)
            val okHttpCallback = object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWith(onFailure(e))
                }

                override fun onResponse(call: Call, response: Response) {
                    continuation.resumeWith(onResponse(response))
                }
            }
            singletonClient.newCall(okHttpRequest).enqueue(okHttpCallback)
        }
    }

    suspend fun sendRequestForageApiResponse(request: BaseApiRequest): ForageApiResponse<String> =
        ForageApiResponse.Success(sendRequest(request))

    companion object {
        // use a singleton OkHttpClient to share connection
        // and thread pools across instances. However, we are
        // able to customize the interceptors and headers for
        // derived clients using the `.newBuilder()` pattern
        // (see provideOkHttpClient implementation). The
        // derived instances share the same resources but retrain
        // distinct configurations. See this SO thread
        // https://stackoverflow.com/questions/72348948/okhttp-newbuilder-per-request
        private val singletonClient: OkHttpClient = OkHttpClient()
    }
}
