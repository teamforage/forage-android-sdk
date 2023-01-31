package com.joinforage.forage.android.network

import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ForageError
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

internal class CapturePaymentResponseService(
    private val httpUrl: HttpUrl,
    okHttpClient: OkHttpClient
) : NetworkService(okHttpClient) {
    suspend fun retrieveCapturePaymentResponse(paymentRef: String): ForageApiResponse<String> =
        try {
            getPaymentResponseToCoroutine(paymentRef)
        } catch (ex: IOException) {
            ForageApiResponse.Failure(500, listOf(ForageError(500, "server_error", ex.message.orEmpty())))
        }

    private suspend fun getPaymentResponseToCoroutine(
        paymentRef: String
    ): ForageApiResponse<String> {
        val url = getPaymentResponseUrl(paymentRef)

        val request: Request = Request.Builder()
            .url(url)
            .get()
            .build()

        return convertCallbackToCoroutine(request)
    }

    private fun getPaymentResponseUrl(paymentRef: String): HttpUrl {
        return httpUrl
            .newBuilder()
            .addPathSegment(ForageConstants.PathSegment.API)
            .addPathSegment(ForageConstants.PathSegment.PAYMENTS)
            .addPathSegment(paymentRef)
            .build()
    }
}
