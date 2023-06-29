package com.joinforage.forage.android.collect

import com.basistheory.android.service.BasisTheoryElements
import com.basistheory.android.service.ProxyRequest
import com.joinforage.forage.android.BuildConfig
import com.joinforage.forage.android.core.DDManager
import com.joinforage.forage.android.model.EncryptionKeys
import com.joinforage.forage.android.model.PaymentMethod
import com.joinforage.forage.android.network.model.ForageApiError
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ForageError
import com.joinforage.forage.android.ui.ForagePINEditText
import org.json.JSONException
import java.util.*

internal class BTPinCollector(
    private val pinForageEditText: ForagePINEditText,
    private val merchantAccount: String
) : PinCollector {
    private val logger = DDManager.getLogger()
    override suspend fun collectPinForBalanceCheck(
        paymentMethodRef: String,
        cardToken: String,
        encryptionKey: String
    ): ForageApiResponse<String> {
        val bt = buildBt()

        val proxyRequest: ProxyRequest = ProxyRequest().apply {
            headers = buildHeaders(encryptionKey, merchantAccount)
            body = object {
                val pin = pinForageEditText.getTextElement()
                val card_number_token = cardToken
            }
            path = balancePath(paymentMethodRef)
        }

        logger.i("Sending balance check to BasisTheory")
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
                logger.e("Received an error while submitting balance request to BasisTheory: $error.message")
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
            logger.i("Received successful response from BasisTheory")
            return ForageApiResponse.Success(forageResponse.toString())
        }

        val btErrorResponse = response.exceptionOrNull()
        logger.e("Received BasisTheory API exception on balance check: $btErrorResponse")
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
            headers = buildHeaders(encryptionKey, merchantAccount, paymentRef)
            body = object {
                val pin = pinForageEditText.getTextElement()
                val card_number_token = cardToken
            }
            path = capturePaymentPath(paymentRef)
        }

        logger.i("Sending payment capture to BasisTheory")
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
                logger.e("Received an error while submitting capture request to BasisTheory: $error.message")
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
            logger.i("Received successful response from BasisTheory")
            return ForageApiResponse.Success(forageResponse.toString())
        }
        val btErrorResponse = response.exceptionOrNull()
        logger.e("Received BasisTheory API exception on payment capture: $btErrorResponse")

        return ForageApiResponse.Failure(
            listOf(
                ForageError(500, "unknown_server_error", "Unknown Server Error")
            )
        )
    }

    override fun parseEncryptionKey(encryptionKeys: EncryptionKeys): String {
        return encryptionKeys.btAlias
    }

    override fun parseVaultToken(paymentMethod: PaymentMethod): String {
        val token = paymentMethod.card.token
        if (token.contains(CollectorConstants.TOKEN_DELIMITER)) {
            return token.split(CollectorConstants.TOKEN_DELIMITER)[1]
        }
        logger.e("BT Token wasn't found on card")
        throw RuntimeException("BT token not found on card!")
    }

    companion object {
        private const val PROXY_ID = BuildConfig.BT_PROXY_ID
        private const val API_KEY = BuildConfig.BT_API_KEY

        private fun buildBt(): BasisTheoryElements {
            return BasisTheoryElements.builder()
                .apiKey(API_KEY)
                .build()
        }

        private fun buildHeaders(
            encryptionKey: String,
            merchantAccount: String,
            idempotencyKey: String = UUID.randomUUID().toString()
        ): Map<String, String> {
            return mapOf(
                "X-KEY" to encryptionKey,
                "Merchant-Account" to merchantAccount,
                "BT-PROXY-KEY" to PROXY_ID,
                "IDEMPOTENCY-KEY" to idempotencyKey,
                "Content-Type" to "application/json"
            )
        }

        private fun balancePath(paymentMethodRef: String) =
            "/api/payment_methods/$paymentMethodRef/balance/"

        private fun capturePaymentPath(paymentRef: String) =
            "/api/payments/$paymentRef/capture/"
    }
}
