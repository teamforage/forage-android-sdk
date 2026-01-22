package com.joinforage.android.example.network

import com.squareup.moshi.Moshi
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Simple HTTP client wrapping OkHttp with Moshi JSON serialization.
 * Provides suspend functions for async HTTP calls.
 */
internal class HttpClient(
    val baseUrl: String,
    val okHttpClient: OkHttpClient,
    val moshi: Moshi
) {
    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    /**
     * Performs a GET request and deserializes the response.
     */
    suspend inline fun <reified T> get(
        path: String,
        headers: Map<String, String> = emptyMap()
    ): T {
        val request = Request.Builder()
            .url("$baseUrl$path")
            .get()
            .apply {
                headers.forEach { (key, value) -> addHeader(key, value) }
            }
            .build()

        return executeRequest(request)
    }

    /**
     * Performs a POST request with a JSON body and deserializes the response.
     */
    suspend inline fun <reified T, reified R> post(
        path: String,
        body: T,
        headers: Map<String, String> = emptyMap()
    ): R {
        val adapter = moshi.adapter(T::class.java)
        val jsonBody = adapter.toJson(body)

        val request = Request.Builder()
            .url("$baseUrl$path")
            .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
            .apply {
                headers.forEach { (key, value) -> addHeader(key, value) }
            }
            .build()

        return executeRequest(request)
    }

    /**
     * Performs a POST request without a body and deserializes the response.
     */
    suspend inline fun <reified R> postEmpty(
        path: String,
        headers: Map<String, String> = emptyMap()
    ): R {
        val request = Request.Builder()
            .url("$baseUrl$path")
            .post("".toRequestBody(JSON_MEDIA_TYPE))
            .apply {
                headers.forEach { (key, value) -> addHeader(key, value) }
            }
            .build()

        return executeRequest(request)
    }

    /**
     * Executes the OkHttp request asynchronously and returns the deserialized response.
     */
    suspend inline fun <reified T> executeRequest(request: Request): T {
        return suspendCancellableCoroutine { continuation ->
            val call = okHttpClient.newCall(request)

            continuation.invokeOnCancellation {
                call.cancel()
            }

            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        val responseBody = it.body?.string().orEmpty()

                        if (!it.isSuccessful) {
                            continuation.resumeWithException(
                                HttpException(
                                    statusCode = it.code,
                                    responseBody = responseBody,
                                    message = "HTTP ${it.code}: $responseBody"
                                )
                            )
                            return
                        }

                        try {
                            val adapter = moshi.adapter(T::class.java)
                            val result = adapter.fromJson(responseBody)
                            if (result != null) {
                                continuation.resume(result)
                            } else {
                                continuation.resumeWithException(
                                    IOException("Failed to parse response body")
                                )
                            }
                        } catch (e: Exception) {
                            continuation.resumeWithException(e)
                        }
                    }
                }
            })
        }
    }
}