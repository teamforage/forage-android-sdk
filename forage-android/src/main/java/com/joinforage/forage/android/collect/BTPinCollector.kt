package com.joinforage.forage.android.collect

import com.basistheory.android.service.BasisTheoryElements
import com.basistheory.android.service.HttpMethod
import com.basistheory.android.service.ProxyRequest
import com.joinforage.forage.android.BuildConfig
import com.joinforage.forage.android.VaultType
import com.joinforage.forage.android.core.telemetry.ActionType
import com.joinforage.forage.android.core.telemetry.Log
import com.joinforage.forage.android.core.telemetry.VaultProxyResponseMonitor
import com.joinforage.forage.android.model.EncryptionKeys
import com.joinforage.forage.android.model.PaymentMethod
import com.joinforage.forage.android.network.ForageConstants
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
    private val logger = Log.getInstance()
    private val vaultType = VaultType.BT_VAULT_TYPE

    override fun getVaultType(): VaultType {
        return vaultType
    }

    override suspend fun collectPinForBalanceCheck(
        paymentMethodRef: String,
        cardToken: String,
        encryptionKey: String
    ): ForageApiResponse<String> {
        val bt = buildBt()

        // This block is used for Metrics Tracking!
        // ------------------------------------------------------
        val requestPath = balancePath(paymentMethodRef)
        val method = HttpMethod.POST

        val measurement = VaultProxyResponseMonitor.newMeasurement(
            vault = vaultType,
            vaultAction = ActionType.BALANCE,
            logger
        )
            .setPath(requestPath)
            .setMethod(method.toString())
        // ------------------------------------------------------

        val proxyRequest: ProxyRequest = ProxyRequest().apply {
            headers = buildHeaders(encryptionKey, merchantAccount, traceId = logger.getTraceIdValue())
            body = object {
                val pin = pinForageEditText.getTextElement()
                val card_number_token = cardToken
            }
            path = requestPath
        }

        logger.i(
            "[BT] Sending balance check to BasisTheory",
            attributes = mapOf(
                "merchant_ref" to merchantAccount,
                "payment_method_ref" to paymentMethodRef
            )
        )

        measurement.start()
        val response = runCatching {
            bt.proxy.post(proxyRequest)
        }
        measurement.end()

        // MUST reset the PIN value after submitting
        pinForageEditText.getTextElement().setText("")

        if (response.isSuccess) {
            val forageResponse = response.getOrNull()
            try {
                val forageApiError = ForageApiError.ForageApiErrorMapper.from(forageResponse.toString())

                // Error code hardcoded as 400 because of lack of information
                val httpStatusCode = 400
                measurement.setHttpStatusCode(httpStatusCode).logResult()

                val error = forageApiError.errors[0]
                logger.e(
                    "[BT] Received an error while submitting balance request to BasisTheory: $error.message",
                    attributes = mapOf(
                        "merchant_ref" to merchantAccount,
                        "payment_method_ref" to paymentMethodRef
                    )
                )
                return ForageApiResponse.Failure(
                    listOf(
                        ForageError(
                            httpStatusCode,
                            error.code,
                            error.message
                        )
                    )
                )
            } catch (e: JSONException) { }
            logger.i(
                "[BT] Received successful response from BasisTheory",
                attributes = mapOf(
                    "merchant_ref" to merchantAccount,
                    "payment_method_ref" to paymentMethodRef
                )
            )

            // Status Code hardcoded because of lack of knowledge
            val httpStatusCode = 200
            measurement.setHttpStatusCode(httpStatusCode).logResult()

            return ForageApiResponse.Success(forageResponse.toString())
        }

        val btErrorResponse = response.exceptionOrNull()
        logger.e(
            "[BT] Received BasisTheory API exception on balance check: $btErrorResponse",
            attributes = mapOf(
                "merchant_ref" to merchantAccount,
                "payment_method_ref" to paymentMethodRef
            )
        )

        val unknownBtStatusCode = 500
        measurement.setHttpStatusCode(unknownBtStatusCode).logResult()

        return ForageApiResponse.Failure(
            listOf(
                ForageError(unknownBtStatusCode, "unknown_server_error", "Unknown Server Error")
            )
        )
    }

    override suspend fun collectPinForCapturePayment(
        paymentRef: String,
        cardToken: String,
        encryptionKey: String
    ): ForageApiResponse<String> {
        val bt = buildBt()

        // This block is used for Metrics Tracking!
        // ------------------------------------------------------
        val requestPath = capturePaymentPath(paymentRef)
        val method = HttpMethod.POST

        val measurement = VaultProxyResponseMonitor.newMeasurement(
            vault = vaultType,
            vaultAction = ActionType.CAPTURE,
            logger
        )
            .setPath(requestPath)
            .setMethod(method.toString())
        // ------------------------------------------------------

        val proxyRequest: ProxyRequest = ProxyRequest().apply {
            headers = buildHeaders(encryptionKey, merchantAccount, paymentRef, traceId = logger.getTraceIdValue())
            body = object {
                val pin = pinForageEditText.getTextElement()
                val card_number_token = cardToken
            }
            path = capturePaymentPath(paymentRef)
        }

        logger.i(
            "[BT] Sending payment capture to BasisTheory",
            attributes = mapOf(
                "merchant_ref" to merchantAccount,
                "payment_ref" to paymentRef
            )
        )

        measurement.start()
        val response = runCatching {
            bt.proxy.post(proxyRequest)
        }
        measurement.end()

        // MUST reset the PIN value after submitting
        pinForageEditText.getTextElement().setText("")

        if (response.isSuccess) {
            val forageResponse = response.getOrNull()
            try {
                val forageApiError = ForageApiError.ForageApiErrorMapper.from(forageResponse.toString())
                val error = forageApiError.errors[0]
                logger.e(
                    "[BT] Received an error while submitting capture request to BasisTheory: $error.message",
                    attributes = mapOf(
                        "merchant_ref" to merchantAccount,
                        "payment_ref" to paymentRef
                    )
                )

                // Error code hardcoded as 400 because of lack of information
                val httpStatusCode = 400
                measurement.setHttpStatusCode(httpStatusCode).logResult()

                return ForageApiResponse.Failure(
                    listOf(
                        ForageError(
                            httpStatusCode,
                            error.code,
                            error.message
                        )
                    )
                )
            } catch (e: JSONException) { }
            logger.i(
                "[BT] Received successful response from BasisTheory",
                attributes = mapOf(
                    "merchant_ref" to merchantAccount,
                    "payment_ref" to paymentRef
                )
            )

            // Status Code hardcoded because of lack of knowledge
            val httpStatusCode = 200
            measurement.setHttpStatusCode(httpStatusCode).logResult()

            return ForageApiResponse.Success(forageResponse.toString())
        }
        val btErrorResponse = response.exceptionOrNull()
        logger.e(
            "[BT] Received BasisTheory API exception on payment capture: $btErrorResponse",
            attributes = mapOf(
                "merchant_ref" to merchantAccount,
                "payment_ref" to paymentRef
            )
        )

        val unknownBtStatusCode = 500
        measurement.setHttpStatusCode(unknownBtStatusCode).logResult()

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
        logger.e(
            "[BT] BT Token wasn't found on card",
            attributes = mapOf(
                "merchant_ref" to merchantAccount,
                "payment_method_ref" to paymentMethod.ref
            )
        )
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
            idempotencyKey: String = UUID.randomUUID().toString(),
            traceId: String = ""
        ): Map<String, String> {
            val headers = HashMap<String, String>()
            headers[ForageConstants.Headers.X_KEY] = encryptionKey
            headers[ForageConstants.Headers.MERCHANT_ACCOUNT] = merchantAccount
            headers[ForageConstants.Headers.IDEMPOTENCY_KEY] = idempotencyKey
            headers[ForageConstants.Headers.BT_PROXY_KEY] = PROXY_ID
            headers[ForageConstants.Headers.CONTENT_TYPE] = "application/json"
            headers[ForageConstants.Headers.TRACE_ID] = traceId
            return headers
        }

        private fun balancePath(paymentMethodRef: String) =
            "/api/payment_methods/$paymentMethodRef/balance/"

        private fun capturePaymentPath(paymentRef: String) =
            "/api/payments/$paymentRef/capture/"
    }
}
