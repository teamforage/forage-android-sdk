package com.joinforage.forage.android.network

import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ForageError
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

internal class PaymentService(
    private val httpUrl: HttpUrl,
    okHttpClient: OkHttpClient
) : NetworkService(okHttpClient) {
    suspend fun getPayment(paymentRef: String): ForageApiResponse<String> = try {
        getPaymentToCoroutine(paymentRef)
    } catch (ex: IOException) {
        ForageApiResponse.Failure(listOf(ForageError(500, "unknown_server_error", ex.message.orEmpty())))
    }

    private suspend fun getPaymentToCoroutine(paymentRef: String): ForageApiResponse<String> {
        val url = getPaymentUrl(paymentRef)

        val request: Request = Request.Builder()
            .url(url)
            .get()
            .build()

        return convertCallbackToCoroutine(request)
    }

    private fun getPaymentUrl(paymentRef: String): HttpUrl {
        return httpUrl
            .newBuilder()
            .addPathSegment(ForageConstants.PathSegment.API)
            .addPathSegment(ForageConstants.PathSegment.PAYMENTS)
            .addPathSegment(paymentRef)
            .build()
    }
}