package com.joinforage.forage.android.core.services.vault

import com.joinforage.forage.android.core.services.ForageConstants
import com.joinforage.forage.android.core.services.addTrailingSlash
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.network.NetworkService
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.PaymentMethodRequestBody
import com.joinforage.forage.android.core.services.forageapi.paymentmethod.RequestBody
import com.joinforage.forage.android.core.services.telemetry.Log
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

internal class TokenizeCardService(
    private val httpUrl: String,
    okHttpClient: OkHttpClient,
    private val logger: Log
) : NetworkService(okHttpClient, logger) {
    suspend fun tokenizeCard(cardNumber: String, customerId: String? = null, reusable: Boolean = true): ForageApiResponse<String> = try {
        logger.i(
            "[HTTP] POST request for Payment Method",
            attributes = mapOf(
                "customer_id" to customerId
            )
        )
        tokenizeCardCoroutine(
            PaymentMethodRequestBody(
                cardNumber = cardNumber,
                customerId = customerId,
                reusable = reusable
            )
        )
    } catch (ex: IOException) {
        logger.e("[HTTP] Failed while tokenizing PaymentMethod", ex, attributes = mapOf("customer_id" to customerId))
        ForageApiResponse.Failure(500, "unknown_server_error", ex.message.orEmpty())
    }

    private suspend fun tokenizeCardCoroutine(requestBody: RequestBody): ForageApiResponse<String> {
        val url = getTokenizeCardUrl()
        val okHttpRequestBody = requestBody
            .toJSONObject()
            .toString()
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        val request: Request = Request.Builder()
            .url(url)
            .header(ForageConstants.Headers.API_VERSION, "2023-05-15")
            .post(okHttpRequestBody)
            .build()

        return convertCallbackToCoroutine(request)
    }

    private fun getTokenizeCardUrl(): HttpUrl = httpUrl.toHttpUrlOrNull()!!
        .newBuilder()
        .addPathSegment(ForageConstants.PathSegment.API)
        .addPathSegment(ForageConstants.PathSegment.PAYMENT_METHODS)
        .addTrailingSlash()
        .build()
}
