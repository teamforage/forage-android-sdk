package com.joinforage.forage.android.collect

import android.content.Context
import com.joinforage.forage.android.BuildConfig
import com.joinforage.forage.android.core.Log
import com.joinforage.forage.android.model.EncryptionKeys
import com.joinforage.forage.android.model.PaymentMethod
import com.joinforage.forage.android.network.ForageConstants
import com.joinforage.forage.android.network.model.ForageApiResponse
import com.joinforage.forage.android.network.model.ForageError
import com.joinforage.forage.android.ui.ForagePINEditText
import com.verygoodsecurity.vgscollect.VGSCollectLogger
import com.verygoodsecurity.vgscollect.core.HTTPMethod
import com.verygoodsecurity.vgscollect.core.VGSCollect
import com.verygoodsecurity.vgscollect.core.VgsCollectResponseListener
import com.verygoodsecurity.vgscollect.core.model.network.VGSRequest
import com.verygoodsecurity.vgscollect.core.model.network.VGSResponse
import java.util.UUID
import kotlin.coroutines.suspendCoroutine

internal class VGSPinCollector(
    private val context: Context,
    private val pinForageEditText: ForagePINEditText,
    private val merchantAccount: String
) : PinCollector {
    private val logger = Log.getInstance()
    override suspend fun collectPinForBalanceCheck(
        paymentMethodRef: String,
        cardToken: String,
        encryptionKey: String
    ): ForageApiResponse<String> = suspendCoroutine { continuation ->
        val vgsCollect = buildVGSCollect(context)

        val inputField = pinForageEditText.getTextInputEditText()
        vgsCollect.bindView(inputField)

        vgsCollect.addOnResponseListeners(object : VgsCollectResponseListener {
            override fun onResponse(response: VGSResponse?) {
                vgsCollect.onDestroy()
                inputField.setText("")

                when (response) {
                    is VGSResponse.SuccessResponse -> {
                        logger.i("[VGS] Received successful response from VGS")
                        continuation.resumeWith(
                            Result.success(
                                ForageApiResponse.Success(response.body!!)
                            )
                        )
                    }
                    is VGSResponse.ErrorResponse -> {
                        logger.e("[VGS] Received an error while submitting balance request to VGS: ${response.body}")
                        continuation.resumeWith(
                            Result.success(
                                ForageApiResponse.Failure(listOf(ForageError(response.errorCode, "user_error", "Invalid Data")))
                            )
                        )
                    }
                    null -> {
                        logger.e("[VGS] Received an unknown error while submitting balance request to VGS")
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
            .setMethod(HTTPMethod.POST)
            .setPath(balancePath(paymentMethodRef))
            .setCustomHeader(
                buildHeaders(
                    merchantAccount,
                    encryptionKey
                )
            )
            .setCustomData(buildRequestBody(cardToken))
            .build()

        logger.i("[VGS] Sending balance check to VGS")

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

        vgsCollect.addOnResponseListeners(object : VgsCollectResponseListener {
            override fun onResponse(response: VGSResponse?) {
                vgsCollect.onDestroy()
                inputField.setText("")

                when (response) {
                    is VGSResponse.SuccessResponse -> {
                        logger.i("[VGS] Received successful response from VGS")
                        continuation.resumeWith(
                            Result.success(
                                ForageApiResponse.Success(response.body!!)
                            )
                        )
                    }
                    is VGSResponse.ErrorResponse -> {
                        logger.e("[VGS] Received an error while submitting capture request to VGS: ${response.body}")
                        continuation.resumeWith(
                            Result.success(
                                ForageApiResponse.Failure(listOf(ForageError(response.errorCode, "user_error", "Invalid Data")))
                            )
                        )
                    }
                    null -> {
                        logger.e("[VGS] Received an unknown error while submitting capture request to VGS")
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
            .setMethod(HTTPMethod.POST)
            .setPath(capturePaymentPath(paymentRef))
            .setCustomHeader(
                buildHeaders(
                    merchantAccount,
                    encryptionKey,
                    idempotencyKey = paymentRef
                )
            )
            .setCustomData(buildRequestBody(cardToken))
            .build()

        logger.i("[VGS] Sending payment capture to VGS")

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
            idempotencyKey: String = UUID.randomUUID().toString()
        ): HashMap<String, String> {
            val headers = HashMap<String, String>()
            headers[ForageConstants.Headers.X_KEY] = encryptionKey
            headers[ForageConstants.Headers.MERCHANT_ACCOUNT] = merchantAccount
            headers[ForageConstants.Headers.IDEMPOTENCY_KEY] = idempotencyKey
            return headers
        }

        private fun balancePath(paymentMethodRef: String) =
            "/api/payment_methods/$paymentMethodRef/balance/"

        private fun capturePaymentPath(paymentRef: String) =
            "/api/payments/$paymentRef/capture/"
    }
}
