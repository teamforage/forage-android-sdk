package com.joinforage.forage.android.network

import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.PaymentMethodRequestBody
import com.joinforage.forage.android.network.model.toJSONObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.suspendCoroutine

internal class TokenizeCardService(
    private val okHttpClient: OkHttpClient,
    private val httpUrl: HttpUrl
) {
    suspend fun tokenizeCard(cardNumber: String): ForageApiResponse<String> = try {
        tokenizeCardCoroutine(cardNumber)
    } catch (ex: IOException) {
        ForageApiResponse.Failure(message = ex.message.orEmpty())
    }

    private suspend fun tokenizeCardCoroutine(cardNumber: String): ForageApiResponse<String> {
        val url = getTokenizeCardUrl()

        val requestBody =
            PaymentMethodRequestBody(cardNumber = cardNumber).toJSONObject().toString()

        val body: RequestBody =
            requestBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        val request: Request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        return suspendCoroutine { continuation ->
            okHttpClient.newCall(request).enqueue(
                object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        continuation.resumeWith(Result.failure(e))
                    }

                    override fun onResponse(call: Call, response: Response) {
                        response.use {
                            if (response.isSuccessful.not()) {
                                continuation.resumeWith(
                                    Result.success(
                                        ForageApiResponse.Failure(response.body?.string().orEmpty())
                                    )
                                )
                            } else {
                                continuation.resumeWith(
                                    Result.success(
                                        ForageApiResponse.Success(
                                            response.body?.string().orEmpty()
                                        )
                                    )
                                )
                            }
                        }
                    }
                }
            )
        }
    }

    private fun getTokenizeCardUrl(): HttpUrl {
        return httpUrl
            .newBuilder()
            .addPathSegment(ForageConstants.PathSegment.API)
            .addPathSegment(ForageConstants.PathSegment.PAYMENT_METHODS)
            .addPathSegment("")
            .build()
    }
}
