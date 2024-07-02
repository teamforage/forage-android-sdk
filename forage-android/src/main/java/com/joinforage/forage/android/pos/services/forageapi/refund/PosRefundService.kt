package com.joinforage.forage.android.pos.services.forageapi.refund

import com.joinforage.forage.android.core.services.ForageConstants
import com.joinforage.forage.android.core.services.addTrailingSlash
import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.forageapi.network.NetworkService
import com.joinforage.forage.android.core.services.forageapi.network.UnknownErrorApiResponse
import com.joinforage.forage.android.core.services.telemetry.Log
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

// For POS in-store transactions only.
internal class PosRefundService(
    private val httpUrl: String,
    private val logger: Log,
    okHttpClient: OkHttpClient
) : NetworkService(okHttpClient, logger) {
    suspend fun getRefund(paymentRef: String, refundRef: String): ForageApiResponse<String> = try {
        logger.addAttribute("refund_ref", refundRef)
        logger.i("[HTTP] GET Refund $refundRef for Payment $paymentRef")
        getRefundToCoroutine(paymentRef, refundRef)
    } catch (ex: IOException) {
        logger.e("[HTTP] Failed while trying to GET Refund $refundRef for Payment $paymentRef", ex)
        UnknownErrorApiResponse
    }

    private suspend fun getRefundToCoroutine(paymentRef: String, refundRef: String): ForageApiResponse<String> {
        val url = getRefundForPaymentUrl(paymentRef, refundRef)

        val request: Request = Request.Builder()
            .url(url)
            .header(ForageConstants.Headers.API_VERSION, "2023-05-15")
            .get()
            .build()

        return convertCallbackToCoroutine(request)
    }

    private fun getRefundForPaymentUrl(paymentRef: String, refundRef: String): HttpUrl = httpUrl.toHttpUrlOrNull()!!
        .newBuilder()
        .addPathSegment(ForageConstants.PathSegment.API)
        .addPathSegment(ForageConstants.PathSegment.PAYMENTS)
        .addPathSegment(paymentRef)
        .addPathSegment(ForageConstants.PathSegment.REFUNDS)
        .addPathSegment(refundRef)
        .addTrailingSlash()
        .build()
}
