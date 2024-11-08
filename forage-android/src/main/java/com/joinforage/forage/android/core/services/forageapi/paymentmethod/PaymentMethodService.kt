package com.joinforage.forage.android.core.services.forageapi.paymentmethod

import com.joinforage.forage.android.core.services.ForageConstants
import com.joinforage.forage.android.core.services.addTrailingSlash
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.network.NetworkService
import com.joinforage.forage.android.core.services.telemetry.Log
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

internal class PaymentMethodService(
    private val httpUrl: String,
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
        ForageApiResponse.Failure(
            500,
            "unknown_server_error",
            ex.message.orEmpty()
        )
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

    private fun getPaymentMethodUrl(paymentMethodRef: String): HttpUrl = httpUrl.toHttpUrlOrNull()!!
        .newBuilder()
        .addPathSegment(ForageConstants.PathSegment.API)
        .addPathSegment(ForageConstants.PathSegment.PAYMENT_METHODS)
        .addPathSegment(paymentMethodRef)
        .addTrailingSlash()
        .build()
}
