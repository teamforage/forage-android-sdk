package com.joinforage.forage.android.network

import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ForageError
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

internal class CheckBalanceResponseService(
    private val httpUrl: HttpUrl,
    okHttpClient: OkHttpClient
) : NetworkService(okHttpClient) {

    suspend fun retrieveBalanceResponse(paymentMethodRef: String): ForageApiResponse<String> = try {
        getBalanceResponseToCoroutine(paymentMethodRef)
    } catch (ex: IOException) {
        ForageApiResponse.Failure(listOf(ForageError(500, "unknown_server_error", ex.message.orEmpty())))
    }

    private suspend fun getBalanceResponseToCoroutine(
        paymentMethodRef: String
    ): ForageApiResponse<String> {
        val url = getBalanceUrl(paymentMethodRef)

        val request: Request = Request.Builder()
            .url(url)
            .get()
            .build()

        return convertCallbackToCoroutine(request)
    }

    private fun getBalanceUrl(paymentMethodRef: String): HttpUrl {
        return httpUrl
            .newBuilder()
            .addPathSegment(ForageConstants.PathSegment.API)
            .addPathSegment(ForageConstants.PathSegment.PAYMENT_METHODS)
            .addPathSegment(paymentMethodRef)
            .build()
    }
}
