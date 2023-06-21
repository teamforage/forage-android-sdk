package com.joinforage.forage.android.collect

import com.basistheory.android.service.BasisTheoryElements
import com.basistheory.android.service.ProxyRequest
import com.joinforage.forage.android.BuildConfig
import com.joinforage.forage.android.network.model.ForageApiError
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ForageError
import com.joinforage.forage.android.ui.ForagePINEditText
import org.json.JSONException

internal class BTPinCollector(
    private val pinForageEditText: ForagePINEditText,
    private val merchantAccount: String
) : PinCollector {
    override suspend fun collectPinForBalanceCheck(
        paymentMethodRef: String,
        cardToken: String,
        encryptionKey: String
    ): ForageApiResponse<String> {
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

        val response = runCatching {
            bt.proxy.post(proxyRequest)
        }

        // MUST reset the PIN value after submitting
        pinForageEditText.getTextElement().setText("")

        if (response.isSuccess) {
            val forageResponse = response.getOrNull()
            try {
                val forageApiError = ForageApiError.ForageApiErrorMapper.from(forageResponse.toString())
                val error = forageApiError.errors[0]
                return ForageApiResponse.Failure(
                    listOf(
                        ForageError(
                            // Error code hardcoded as 400 because of lack of information
                            400,
                            error.code,
                            error.message
                        )
                    )
                )
            } catch (e: JSONException) { }
            return ForageApiResponse.Success(forageResponse.toString())
        }

        // This should return the BT ApiException, which is a string value of the API response
        // TODO: Log this value to Datadog.
//        val btErrorResponse = response.exceptionOrNull()
        return ForageApiResponse.Failure(
            listOf(
                ForageError(500, "unknown_server_error", "Unknown Server Error")
            )
        )
    }

    override suspend fun collectPinForCapturePayment(
        paymentRef: String,
        cardToken: String,
        encryptionKey: String
    ): ForageApiResponse<String> {
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

        val response = runCatching {
            bt.proxy.post(proxyRequest)
        }

        // MUST reset the PIN value after submitting
        pinForageEditText.getTextElement().setText("")

        if (response.isSuccess) {
            val forageResponse = response.getOrNull()
            try {
                val forageApiError = ForageApiError.ForageApiErrorMapper.from(forageResponse.toString())
                val error = forageApiError.errors[0]
                return ForageApiResponse.Failure(
                    listOf(
                        ForageError(
                            // Error code hardcoded as 400 because of lack of information
                            400,
                            error.code,
                            error.message
                        )
                    )
                )
            } catch (e: JSONException) { }
            return ForageApiResponse.Success(forageResponse.toString())
        }
        // This should return the BT ApiException, which is a string value of the API response
        // TODO: Log this value to Datadog.
//        val btErrorResponse = response.exceptionOrNull()

        return ForageApiResponse.Failure(
            listOf(
                ForageError(500, "unknown_server_error", "Unknown Server Error")
            )
        )
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
