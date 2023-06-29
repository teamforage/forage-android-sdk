package com.joinforage.forage.android.network

import com.joinforage.forage.android.core.DDManager
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ForageError
import com.joinforage.forage.android.network.model.PaymentMethodRequestBody
import com.joinforage.forage.android.network.model.toJSONObject
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

internal class TokenizeCardService(
    private val httpUrl: HttpUrl,
    okHttpClient: OkHttpClient
) : NetworkService(okHttpClient) {
    suspend fun tokenizeCard(cardNumber: String, customerId: String): ForageApiResponse<String> = try {
        tokenizeCardCoroutine(cardNumber, customerId)
    } catch (ex: IOException) {
        val logger = DDManager.getLogger()
        logger.e("Failed while tokenizing PaymentMethod", ex)
        ForageApiResponse.Failure(listOf(ForageError(500, "unknown_server_error", ex.message.orEmpty())))
    }

    private suspend fun tokenizeCardCoroutine(cardNumber: String, customerId: String): ForageApiResponse<String> {
        val url = getTokenizeCardUrl()

        val requestBody =
            PaymentMethodRequestBody(cardNumber = cardNumber, customerId = customerId).toJSONObject().toString()

        val body: RequestBody =
            requestBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        val request: Request = Request.Builder()
            .url(url)
            .header(ForageConstants.Headers.API_VERSION, "2023-05-15")
            .post(body)
            .build()

        return convertCallbackToCoroutine(request)
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
