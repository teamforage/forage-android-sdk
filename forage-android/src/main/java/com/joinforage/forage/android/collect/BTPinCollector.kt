package com.joinforage.forage.android.collect

import android.content.Context
import com.basistheory.android.service.BasisTheoryElements
import com.basistheory.android.service.ProxyRequest
import com.joinforage.forage.android.BuildConfig
import com.joinforage.forage.android.network.model.ForageApiError
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ForageError
import com.joinforage.forage.android.ui.ForagePINEditText
import kotlinx.coroutines.runBlocking
import org.json.JSONException
import kotlin.coroutines.suspendCoroutine

internal class BTPinCollector(
    private val pinForageEditText: ForagePINEditText,
    private val merchantAccount: String
) : PinCollector {
    override suspend fun collectPinForBalanceCheck(
        paymentMethodRef: String,
        cardToken: String,
        encryptionKey: String
    ): ForageApiResponse<String> = suspendCoroutine { continuation ->
        val bt = buildBt()

        val proxyRequest: ProxyRequest = ProxyRequest().apply {
            headers = mapOf(
                "X-KEY" to encryptionKey,
                "Merchant-Account" to merchantAccount,
                "BT-PROXY-KEY" to PROXY_ID,
                "Content-Type" to "application/json"
            )
            body = object {
                val pin = pinForageEditText.getTextElement()
                val card_number_token = cardToken
            }
            path = balancePath(paymentMethodRef)
        }

        runBlocking {
            val response = bt.proxy.post(proxyRequest)
            pinForageEditText.getTextElement().setText("")
            // Try to parse the response as an error first
            try {
                val forageApiError = ForageApiError.ForageApiErrorMapper.from(response.toString())
                val error = forageApiError.errors[0]
                continuation.resumeWith(
                    Result.success(
                        ForageApiResponse.Failure(listOf(ForageError(400, error.code, error.message)))
                    )
                )
            } catch (e: JSONException) {}
            continuation.resumeWith(
                Result.success(
                    ForageApiResponse.Success(response.toString())
                )
            )
        }
    }


    override suspend fun collectPinForCapturePayment(
        paymentRef: String,
        cardToken: String,
        encryptionKey: String
    ): ForageApiResponse<String> = suspendCoroutine { continuation ->
        val bt = buildBt()

        val proxyRequest: ProxyRequest = ProxyRequest().apply {
            headers = mapOf(
                "X-KEY" to encryptionKey,
                "Merchant-Account" to merchantAccount,
                "BT-PROXY-KEY" to PROXY_ID,
                "IDEMPOTENCY-KEY" to paymentRef,
                "Content-Type" to "application/json"
            )
            body = object {
                val pin = pinForageEditText.getTextElement()
                val card_number_token = cardToken
            }
            path = capturePaymentPath(paymentRef)
        }

        runBlocking {
            val response = bt.proxy.post(proxyRequest)
            pinForageEditText.getTextElement().setText("")
            // Try to parse the response as an error first
            try {
                val forageApiError = ForageApiError.ForageApiErrorMapper.from(response.toString())
                val error = forageApiError.errors[0]
                continuation.resumeWith(
                    Result.success(
                        ForageApiResponse.Failure(listOf(ForageError(400, error.code, error.message)))
                    )
                )
            } catch (e: JSONException) {}
            continuation.resumeWith(
                Result.success(
                    ForageApiResponse.Success(response.toString())
                )
            )
        }
    }

    companion object {
        private const val PROXY_ID = BuildConfig.BT_PROXY_ID
        private const val API_KEY = BuildConfig.BT_API_KEY

        private fun buildBt(): BasisTheoryElements {
            return BasisTheoryElements.builder()
                .apiKey(API_KEY)
                .build()
        }

        private fun balancePath(paymentMethodRef: String) =
            "/api/payment_methods/$paymentMethodRef/balance/"

        private fun capturePaymentPath(paymentRef: String) =
            "/api/payments/$paymentRef/capture/"
    }
}
