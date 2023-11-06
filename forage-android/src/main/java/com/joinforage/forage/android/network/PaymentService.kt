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

internal class PaymentService(
    private val httpUrl: String,
    okHttpClient: OkHttpClient,
    private val logger: Log
) : NetworkService(okHttpClient, logger) {
    suspend fun getPayment(paymentRef: String): ForageApiResponse<String> = try {
        logger.i(
            "[HTTP] GET request for Payment Method $paymentRef",
            attributes = mapOf(
                "payment_ref" to paymentRef
            )
        )
        getPaymentToCoroutine(paymentRef)
    } catch (ex: IOException) {
        logger.e(
            "[HTTP] Failed while trying to GET Payment $paymentRef",
            ex,
            attributes = mapOf("payment_ref" to paymentRef)
        )
        ForageApiResponse.Failure(listOf(ForageError(500, "unknown_server_error", ex.message.orEmpty())))
    }

    private suspend fun getPaymentToCoroutine(paymentRef: String): ForageApiResponse<String> {
        val url = getPaymentUrl(paymentRef)

        val request: Request = Request.Builder()
            .url(url)
            .header(ForageConstants.Headers.API_VERSION, "2023-05-15")
            .get()
            .build()

        return convertCallbackToCoroutine(request)
    }

    private fun getPaymentUrl(paymentRef: String): HttpUrl = httpUrl.toHttpUrlOrNull()!!
            .newBuilder()
            .addPathSegment(ForageConstants.PathSegment.API)
            .addPathSegment(ForageConstants.PathSegment.PAYMENTS)
            .addPathSegment(paymentRef)
            .addTrailingSlash()
            .build()
}
