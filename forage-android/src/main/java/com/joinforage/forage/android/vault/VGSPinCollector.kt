package com.joinforage.forage.android.vault

import android.content.Context
import com.joinforage.forage.android.VaultType
import com.joinforage.forage.android.core.StopgapGlobalState
import com.joinforage.forage.android.core.telemetry.Log
import com.joinforage.forage.android.core.telemetry.NetworkMonitor
import com.joinforage.forage.android.core.telemetry.UserAction
import com.joinforage.forage.android.core.telemetry.VaultProxyResponseMonitor
import com.joinforage.forage.android.model.EncryptionKeys
import com.joinforage.forage.android.model.PaymentMethod
import com.joinforage.forage.android.network.ForageConstants
import com.joinforage.forage.android.network.data.BaseVaultRequestParams
import com.joinforage.forage.android.network.model.ForageApiError
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ForageError
import com.joinforage.forage.android.pos.PosVaultRequestParams
import com.joinforage.forage.android.ui.ForagePINEditText
import com.verygoodsecurity.vgscollect.VGSCollectLogger
import com.verygoodsecurity.vgscollect.core.HTTPMethod
import com.verygoodsecurity.vgscollect.core.VGSCollect
import com.verygoodsecurity.vgscollect.core.VgsCollectResponseListener
import com.verygoodsecurity.vgscollect.core.model.network.VGSRequest
import com.verygoodsecurity.vgscollect.core.model.network.VGSResponse
import org.json.JSONException
import java.util.UUID
import kotlin.coroutines.Continuation
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

    override suspend fun submitBalanceCheck(
        paymentMethodRef: String,
        vaultRequestParams: BaseVaultRequestParams
    ): ForageApiResponse<String> = suspendCoroutine { continuation ->
        val logAttributes = mapOf(
            "merchant_ref" to merchantAccount,
            "payment_method_ref" to paymentMethodRef
        )

        // If the PIN isn't valid (less than 4 numbers) then return a response here.
        if (!pinForageEditText.getElementState().isComplete) {
            returnIncompletePinError(logAttributes, continuation, logger)
            return@suspendCoroutine
        }

        val vgsCollect = buildVGSCollect(context)
        val inputField = pinForageEditText.getTextInputEditText()
        vgsCollect.bindView(inputField)
        val measurement = setupMeasurement(balancePath(paymentMethodRef), UserAction.BALANCE)

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
                            attributes = logAttributes
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
                            logger.e(
                                "[VGS] Received an error while submitting balance request to VGS: ${response.body}",
                                attributes = logAttributes
                            )
                            returnForageError(response, measurement, continuation)
                            return
                        } catch (_: JSONException) { }
                        logger.e(
                            "[VGS] Received an error while submitting balance request to VGS: ${response.body}",
                            attributes = logAttributes
                        )
                        returnVgsError(response, measurement, continuation)
                    }
                    null -> {
                        logger.e(
                            "[VGS] Received an unknown error while submitting balance request to VGS",
                            attributes = logAttributes
                        )
                        returnUnknownError(measurement, continuation)
                    }
                }
            }
        })

        val request: VGSRequest = VGSRequest.VGSRequestBuilder()
            .setMethod(HTTPMethod.POST)
            .setPath(balancePath(paymentMethodRef))
            .setCustomHeader(
                buildHeaders(
                    merchantAccount,
                    vaultRequestParams.encryptionKey,
                    traceId = logger.getTraceIdValue()
                )
            )
            .setCustomData(buildRequestBody(vaultRequestParams))
            .build()

        logger.i(
            "[VGS] Sending balance check to VGS",
            attributes = logAttributes
        )

        measurement.start()
        vgsCollect.asyncSubmit(request)
    }

    override suspend fun submitPaymentCapture(
        paymentRef: String,
        vaultRequestParams: BaseVaultRequestParams
    ): ForageApiResponse<String> = suspendCoroutine { continuation ->
        val logAttributes = mapOf(
            "merchant_ref" to merchantAccount,
            "payment_ref" to paymentRef
        )

        // If the PIN isn't valid (less than 4 numbers) then return a response here.
        if (!pinForageEditText.getElementState().isComplete) {
            returnIncompletePinError(logAttributes, continuation, logger)
            return@suspendCoroutine
        }

        val vgsCollect = buildVGSCollect(context)
        vgsCollect.bindView(pinForageEditText.getTextInputEditText())
        val inputField = pinForageEditText.getTextInputEditText()
        val measurement = setupMeasurement(capturePaymentPath(paymentRef), UserAction.CAPTURE)

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
                            attributes = logAttributes
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
                            logger.e(
                                "[VGS] Received an error while submitting capture request to VGS: ${response.body}",
                                attributes = logAttributes
                            )
                            returnForageError(response, measurement, continuation)
                            return
                        } catch (_: JSONException) { }
                        logger.e(
                            "[VGS] Received an unknown error while submitting capture request to VGS: ${response.body}",
                            attributes = logAttributes
                        )
                        returnVgsError(response, measurement, continuation)
                    }
                    null -> {
                        logger.e(
                            "[VGS] Received an unknown error while submitting capture request to VGS",
                            attributes = logAttributes
                        )
                        returnUnknownError(measurement, continuation)
                    }
                }
            }
        })

        val request: VGSRequest = VGSRequest.VGSRequestBuilder()
            .setMethod(HTTPMethod.POST)
            .setPath(capturePaymentPath(paymentRef))
            .setCustomHeader(
                buildHeaders(
                    merchantAccount,
                    vaultRequestParams.encryptionKey,
                    idempotencyKey = paymentRef,
                    traceId = logger.getTraceIdValue()
                )
            )
            .setCustomData(buildRequestBody(vaultRequestParams))
            .build()

        logger.i(
            "[VGS] Sending payment capture to VGS",
            attributes = logAttributes
        )

        measurement.start()
        vgsCollect.asyncSubmit(request)
    }

    override suspend fun submitDeferPaymentCapture(
        paymentRef: String,
        vaultRequestParams: BaseVaultRequestParams
    ): ForageApiResponse<String> = suspendCoroutine { continuation ->
        val logAttributes = mapOf(
            "merchant_ref" to merchantAccount,
            "payment_ref" to paymentRef
        )

        // If the PIN isn't valid (less than 4 numbers) then return a response here.
        if (!pinForageEditText.getElementState().isComplete) {
            returnIncompletePinError(logAttributes, continuation, logger)
            return@suspendCoroutine
        }

        val vgsCollect = buildVGSCollect(context)

        vgsCollect.bindView(pinForageEditText.getTextInputEditText())
        val inputField = pinForageEditText.getTextInputEditText()

        val measurement = setupMeasurement(deferPaymentCapturePath(paymentRef), UserAction.DEFER_CAPTURE)

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
                            attributes = logAttributes
                        )
                        continuation.resumeWith(
                            Result.success(
                                ForageApiResponse.Success("")
                            )
                        )
                    }
                    is VGSResponse.ErrorResponse -> {
                        // Attempt to see if this error is a Forage error
                        try {
                            logger.e(
                                "[VGS] Received an error while submitting defer payment capture request to VGS: ${response.body}",
                                attributes = logAttributes
                            )
                            returnForageError(response, measurement, continuation)
                            return
                        } catch (_: JSONException) { }
                        logger.e(
                            "[VGS] Received an unknown error while submitting defer payment capture request to VGS: ${response.body}",
                            attributes = logAttributes
                        )
                        returnVgsError(response, measurement, continuation)
                    }
                    null -> {
                        logger.e(
                            "[VGS] Received an unknown error while submitting defer payment capture request to VGS",
                            attributes = logAttributes
                        )
                        returnUnknownError(measurement, continuation)
                    }
                }
            }
        })

        val request: VGSRequest = VGSRequest.VGSRequestBuilder()
            .setMethod(HTTPMethod.POST)
            .setPath(deferPaymentCapturePath(paymentRef))
            .setCustomHeader(
                buildHeaders(
                    merchantAccount,
                    vaultRequestParams.encryptionKey,
                    idempotencyKey = paymentRef,
                    traceId = logger.getTraceIdValue()
                )
            )
            .setCustomData(buildRequestBody(vaultRequestParams))
            .build()

        logger.i(
            "[VGS] Sending defer payment capture to VGS",
            attributes = logAttributes
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

    private fun setupMeasurement(path: String, action: UserAction): NetworkMonitor {
        return VaultProxyResponseMonitor.newMeasurement(
            vault = vaultType,
            userAction = action,
            logger
        )
            .setPath(path)
            .setMethod(HTTPMethod.POST.toString())
    }

    companion object {
        // this code assumes that .setForageConfig() has been called
        // on a Forage***EditText before PROXY_ID or API_KEY get
        // referenced
        private val VAULT_ID = StopgapGlobalState.envConfig.vgsVaultId
        private val VGS_ENVIRONMENT = StopgapGlobalState.envConfig.vgsVaultType

        private fun buildVGSCollect(context: Context): VGSCollect {
            VGSCollectLogger.isEnabled = false
            return VGSCollect.Builder(context, VAULT_ID)
                .setEnvironment(VGS_ENVIRONMENT)
                .create()
        }

        internal fun buildRequestBody(requestBodyParams: BaseVaultRequestParams): HashMap<String, Any> {
            return when (requestBodyParams) {
                is PosVaultRequestParams -> buildPosRequestBody(requestBodyParams)
                else -> buildBaseRequestBody(requestBodyParams)
            }
        }

        private fun buildPosRequestBody(posRequestBodyParams: PosVaultRequestParams): HashMap<String, Any> {
            val body = buildBaseRequestBody(posRequestBodyParams)
            body[ForageConstants.RequestBody.POS_TERMINAL] = hashMapOf(
                ForageConstants.RequestBody.PROVIDER_TERMINAL_ID to posRequestBodyParams.posTerminalId
            )
            return body
        }

        private fun buildBaseRequestBody(params: BaseVaultRequestParams): HashMap<String, Any> {
            return hashMapOf(
                ForageConstants.RequestBody.CARD_NUMBER_TOKEN to params.cardNumberToken
            )
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

        internal fun returnForageError(response: VGSResponse.ErrorResponse, measurement: NetworkMonitor, continuation: Continuation<ForageApiResponse<String>>) {
            val forageApiError = ForageApiError.ForageApiErrorMapper.from(response.toString())
            val error = forageApiError.errors[0]

            val httpStatusCode = response.errorCode
            measurement.setHttpStatusCode(httpStatusCode).setForageErrorCode(error.code).logResult()

            continuation.resumeWith(
                Result.success(
                    ForageApiResponse.Failure(
                        listOf(ForageError(httpStatusCode, error.code, error.message))
                    )
                )
            )
        }

        internal fun returnVgsError(response: VGSResponse.ErrorResponse, measurement: NetworkMonitor, continuation: Continuation<ForageApiResponse<String>>) {
            measurement.setHttpStatusCode(response.code).logResult()
            continuation.resumeWith(
                Result.success(
                    ForageApiResponse.Failure(listOf(ForageError(response.errorCode, "unknown_server_error", "Unknown Server Error")))
                )
            )
        }

        internal fun returnUnknownError(measurement: NetworkMonitor, continuation: Continuation<ForageApiResponse<String>>) {
            val unknownVgsErrorCode = 500
            measurement.setHttpStatusCode(unknownVgsErrorCode).logResult()
            continuation.resumeWith(
                Result.success(
                    ForageApiResponse.Failure(listOf(ForageError(500, "unknown_server_error", "Unknown Server Error")))
                )
            )
        }

        internal fun returnIncompletePinError(logAttributes: Map<String, String>, continuation: Continuation<ForageApiResponse<String>>, logger: Log) {
            logger.w(
                "[VGS] User attempted to submit an invalid PIN",
                attributes = logAttributes
            )
            continuation.resumeWith(
                Result.success(
                    ForageApiResponse.Failure(
                        ForageConstants.ErrorResponseObjects.INCOMPLETE_PIN_ERROR
                    )
                )
            )
        }

        private fun balancePath(paymentMethodRef: String) =
            "/api/payment_methods/$paymentMethodRef/balance/"

        private fun capturePaymentPath(paymentRef: String) =
            "/api/payments/$paymentRef/capture/"

        private fun deferPaymentCapturePath(paymentRef: String) =
            "/api/payments/$paymentRef/collect_pin/"
    }
}