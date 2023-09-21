package com.joinforage.forage.android.collect

import android.content.Context
import com.joinforage.forage.android.BuildConfig
import com.joinforage.forage.android.VaultType
import com.joinforage.forage.android.core.telemetry.Log
import com.joinforage.forage.android.core.telemetry.UserAction
import com.joinforage.forage.android.core.telemetry.VaultProxyResponseMonitor
import com.joinforage.forage.android.model.EncryptionKeys
import com.joinforage.forage.android.model.PaymentMethod
import com.joinforage.forage.android.network.ForageConstants
import com.joinforage.forage.android.network.model.ForageApiError
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ForageError
import com.joinforage.forage.android.ui.ForagePINEditText
import com.verygoodsecurity.vgscollect.VGSCollectLogger
import com.verygoodsecurity.vgscollect.core.HTTPMethod
import com.verygoodsecurity.vgscollect.core.VGSCollect
import com.verygoodsecurity.vgscollect.core.VgsCollectResponseListener
import com.verygoodsecurity.vgscollect.core.model.network.VGSRequest
import com.verygoodsecurity.vgscollect.core.model.network.VGSResponse
import org.json.JSONException
import java.util.UUID
import kotlin.coroutines.suspendCoroutine

internal class VGSPinCollector(
    private val context: Context,
    private val pinForageEditText: ForagePINEditText,
    private val merchantAccount: String
) : PinCollector {
    private val logger = Log.getInstance()
    private val vaultType = VaultType.VGS_VAULT_TYPE

    override fun getVaultType(): VaultType {
        return vaultType
    }

    override suspend fun collectPinForBalanceCheck(
        paymentMethodRef: String,
        cardToken: String,
        encryptionKey: String
    ): ForageApiResponse<String> = suspendCoroutine { continuation ->
        val vgsCollect = buildVGSCollect(context)

        val inputField = pinForageEditText.getTextInputEditText()
        vgsCollect.bindView(inputField)

        // This block is used for Metrics Tracking!
        // ------------------------------------------------------
        val path = balancePath(paymentMethodRef)
        val method = HTTPMethod.POST

        val measurement = VaultProxyResponseMonitor.newMeasurement(
            vault = vaultType,
            userAction = UserAction.BALANCE,
            logger
        )
            .setPath(path)
            .setMethod(method.toString())
        // ------------------------------------------------------

        vgsCollect.addOnResponseListeners(object : VgsCollectResponseListener {
            override fun onResponse(response: VGSResponse?) {
                measurement.end()

                vgsCollect.onDestroy()
                inputField.setText("")

                when (response) {
                    is VGSResponse.SuccessResponse -> {
                        measurement.setHttpStatusCode(response.code).logResult()
                        logger.i(
                            "[VGS] Received successful response from VGS",
                            attributes = mapOf(
                                "merchant_ref" to merchantAccount,
                                "payment_method_ref" to paymentMethodRef
                            )
                        )
                        continuation.resumeWith(
                            Result.success(
                                ForageApiResponse.Success(response.body!!)
                            )
                        )
                    }
                    is VGSResponse.ErrorResponse -> {
                        // Attempt to see if this error is a Forage error
                        try {
                            val forageApiError = ForageApiError.ForageApiErrorMapper.from(response.toString())
                            val error = forageApiError.errors[0]
                            logger.e(
                                "[VGS] Received an error while submitting balance request to VGS: ${response.body}",
                                attributes = mapOf(
                                    "merchant_ref" to merchantAccount,
                                    "payment_method_ref" to paymentMethodRef
                                )
                            )

                            val httpStatusCode = response.errorCode
                            measurement.setHttpStatusCode(httpStatusCode).setForageErrorCode(error.code).logResult()

                            continuation.resumeWith(
                                Result.success(
                                    ForageApiResponse.Failure(listOf(
                                        ForageError(
                                            httpStatusCode,
                                            error.code,
                                            error.message
                                        )
                                    ))
                                )
                            )
                            return
                        } catch (e: JSONException) { }

                        // If we have made if this far, then this isn't a Forage error
                        measurement.setHttpStatusCode(response.code).logResult()
                        logger.e(
                            "[VGS] Received an error while submitting balance request to VGS: ${response.body}",
                            attributes = mapOf(
                                "merchant_ref" to merchantAccount,
                                "payment_method_ref" to paymentMethodRef
                            )
                        )
                        continuation.resumeWith(
                            Result.success(
                                ForageApiResponse.Failure(listOf(ForageError(response.errorCode, "user_error", "Invalid Data")))
                            )
                        )
                    }
                    null -> {
                        val unknownVgsErrorCode = 500
                        measurement.setHttpStatusCode(unknownVgsErrorCode).logResult()
                        logger.e(
                            "[VGS] Received an unknown error while submitting balance request to VGS",
                            attributes = mapOf(
                                "merchant_ref" to merchantAccount,
                                "payment_method_ref" to paymentMethodRef
                            )
                        )
                        continuation.resumeWith(
                            Result.success(
                                ForageApiResponse.Failure(listOf(ForageError(unknownVgsErrorCode, "unknown_server_error", "Unknown Server Error")))
                            )
                        )
                    }
                }
            }
        })

        val request: VGSRequest = VGSRequest.VGSRequestBuilder()
            .setMethod(method)
            .setPath(path)
            .setCustomHeader(
                buildHeaders(
                    merchantAccount,
                    encryptionKey,
                    traceId = logger.getTraceIdValue()
                )
            )
            .setCustomData(buildRequestBody(cardToken))
            .build()

        logger.i("[VGS] Sending balance check to VGS")

        measurement.start()
        vgsCollect.asyncSubmit(request)
    }

    override suspend fun collectPinForCapturePayment(
        paymentRef: String,
        cardToken: String,
        encryptionKey: String
    ): ForageApiResponse<String> = suspendCoroutine { continuation ->
        val vgsCollect = buildVGSCollect(context)

        vgsCollect.bindView(pinForageEditText.getTextInputEditText())
        val inputField = pinForageEditText.getTextInputEditText()

        // This block is used for Metrics Tracking!
        // ------------------------------------------------------
        val path = capturePaymentPath(paymentRef)
        val method = HTTPMethod.POST

        val measurement = VaultProxyResponseMonitor.newMeasurement(
            vault = vaultType,
            userAction = UserAction.CAPTURE,
            logger
        )
            .setPath(path)
            .setMethod(method.toString())
        // ------------------------------------------------------

        vgsCollect.addOnResponseListeners(object : VgsCollectResponseListener {
            override fun onResponse(response: VGSResponse?) {
                measurement.end()

                vgsCollect.onDestroy()
                inputField.setText("")

                when (response) {
                    is VGSResponse.SuccessResponse -> {
                        measurement.setHttpStatusCode(response.code).logResult()

                        logger.i(
                            "[VGS] Received successful response from VGS",
                            attributes = mapOf(
                                "merchant_ref" to merchantAccount,
                                "payment_ref" to paymentRef
                            )
                        )
                        continuation.resumeWith(
                            Result.success(
                                ForageApiResponse.Success(response.body!!)
                            )
                        )
                    }
                    is VGSResponse.ErrorResponse -> {
                        // Attempt to see if this error is a Forage error
                        try {
                            val forageApiError = ForageApiError.ForageApiErrorMapper.from(response.toString())
                            val error = forageApiError.errors[0]
                            logger.e(
                                "[VGS] Received an error while submitting capture request to VGS: ${response.body}",
                                attributes = mapOf(
                                    "merchant_ref" to merchantAccount,
                                    "payment_ref" to paymentRef
                                )
                            )

                            val httpStatusCode = response.errorCode
                            measurement.setHttpStatusCode(httpStatusCode).setForageErrorCode(error.code).logResult()

                            continuation.resumeWith(
                                Result.success(
                                    ForageApiResponse.Failure(listOf(
                                        ForageError(
                                            httpStatusCode,
                                            error.code,
                                            error.message
                                        )
                                    ))
                                )
                            )
                            return
                        } catch (e: JSONException) { }

                        measurement.setHttpStatusCode(response.code).logResult()
                        logger.e(
                            "[VGS] Received an error while submitting capture request to VGS: ${response.body}",
                            attributes = mapOf(
                                "merchant_ref" to merchantAccount,
                                "payment_ref" to paymentRef
                            )
                        )
                        continuation.resumeWith(
                            Result.success(
                                ForageApiResponse.Failure(listOf(ForageError(response.errorCode, "user_error", "Invalid Data")))
                            )
                        )
                    }
                    null -> {
                        val unknownVgsErrorCode = 500
                        measurement.setHttpStatusCode(unknownVgsErrorCode).logResult()

                        logger.e(
                            "[VGS] Received an unknown error while submitting capture request to VGS",
                            attributes = mapOf(
                                "merchant_ref" to merchantAccount,
                                "payment_ref" to paymentRef
                            )
                        )
                        continuation.resumeWith(
                            Result.success(
                                ForageApiResponse.Failure(listOf(ForageError(500, "unknown_server_error", "Unknown Server Error")))
                            )
                        )
                    }
                }
            }
        })

        val request: VGSRequest = VGSRequest.VGSRequestBuilder()
            .setMethod(method)
            .setPath(path)
            .setCustomHeader(
                buildHeaders(
                    merchantAccount,
                    encryptionKey,
                    idempotencyKey = paymentRef,
                    traceId = logger.getTraceIdValue()
                )
            )
            .setCustomData(buildRequestBody(cardToken))
            .build()

        logger.i(
            "[VGS] Sending payment capture to VGS",
            attributes = mapOf(
                "merchant_ref" to merchantAccount,
                "payment_ref" to paymentRef
            )
        )

        measurement.start()
        vgsCollect.asyncSubmit(request)
    }

    override fun parseEncryptionKey(encryptionKeys: EncryptionKeys): String {
        return encryptionKeys.vgsAlias
    }

    override fun parseVaultToken(paymentMethod: PaymentMethod): String {
        val token = paymentMethod.card.token
        if (token.contains(CollectorConstants.TOKEN_DELIMITER)) {
            return token.split(CollectorConstants.TOKEN_DELIMITER)[0]
        }
        return token
    }

    companion object {
        private const val VAULT_ID = BuildConfig.VGS_VAULT_ID
        private const val VGS_ENVIRONMENT = BuildConfig.VGS_VAULT_TYPE

        private fun buildVGSCollect(context: Context): VGSCollect {
            VGSCollectLogger.isEnabled = false
            return VGSCollect.Builder(context, VAULT_ID)
                .setEnvironment(VGS_ENVIRONMENT)
                .create()
        }

        private fun buildRequestBody(cardToken: String): HashMap<String, String> {
            val body = HashMap<String, String>()
            body[ForageConstants.RequestBody.CARD_NUMBER_TOKEN] = cardToken
            return body
        }

        private fun buildHeaders(
            merchantAccount: String,
            encryptionKey: String,
            idempotencyKey: String = UUID.randomUUID().toString(),
            traceId: String = ""
        ): HashMap<String, String> {
            val headers = HashMap<String, String>()
            headers[ForageConstants.Headers.X_KEY] = encryptionKey
            headers[ForageConstants.Headers.MERCHANT_ACCOUNT] = merchantAccount
            headers[ForageConstants.Headers.IDEMPOTENCY_KEY] = idempotencyKey
            headers[ForageConstants.Headers.TRACE_ID] = traceId
            return headers
        }

        private fun balancePath(paymentMethodRef: String) =
            "/api/payment_methods/$paymentMethodRef/balance/"

        private fun capturePaymentPath(paymentRef: String) =
            "/api/payments/$paymentRef/capture/"
    }
}
