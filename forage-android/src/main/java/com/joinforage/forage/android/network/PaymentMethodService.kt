package com.joinforage.forage.android.network

import com.joinforage.forage.android.core.telemetry.Log
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ForageError
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

internal class PaymentMethodService(
    private val httpUrl: HttpUrl,
    okHttpClient: OkHttpClient,
    private val logger: Log
) : NetworkService(okHttpClient, logger) {
    suspend fun getPaymentMethod(paymentMethodRef: String): ForageApiResponse<String> = try {
        logger.i(
            "[HTTP] GET request for Payment Method $paymentMethodRef",
            attributes = mapOf(
                "payment_method_ref" to paymentMethodRef
            )
        )
        getPaymentMethodToCoroutine(paymentMethodRef)
    } catch (ex: IOException) {
        logger.e(
            "[HTTP] Failed while trying to GET Payment Method $paymentMethodRef",
            ex,
            attributes = mapOf("payment_method_ref" to paymentMethodRef)
        )
        ForageApiResponse.Failure(listOf(ForageError(500, "unknown_server_error", ex.message.orEmpty())))
    }

    private suspend fun getPaymentMethodToCoroutine(paymentMethodRef: String): ForageApiResponse<String> {
        val url = getPaymentMethodUrl(paymentMethodRef)

        val request: Request = Request.Builder()
            .url(url)
            .header(ForageConstants.Headers.API_VERSION, "2023-05-15")
            .get()
            .build()

        return convertCallbackToCoroutine(request)
    }

    private fun getPaymentMethodUrl(paymentMethodRef: String): HttpUrl {
        return httpUrl
            .newBuilder()
            .addPathSegment(ForageConstants.PathSegment.API)
            .addPathSegment(ForageConstants.PathSegment.PAYMENT_METHODS)
            .addPathSegment(paymentMethodRef)
            .addPathSegment("")
            .build()
    }
}
