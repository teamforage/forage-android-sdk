package com.joinforage.forage.android.collect

import android.content.Context
import com.basistheory.android.service.BasisTheoryElements
import com.basistheory.android.service.ProxyRequest
import com.joinforage.forage.android.BuildConfig
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
import kotlinx.coroutines.runBlocking
import java.util.UUID
import kotlin.coroutines.suspendCoroutine

internal class BTPinCollector(
    private val context: Context,
    private val pinForageEditText: ForagePINEditText
) : PinCollector {
    override suspend fun collectPinForBalanceCheck(
        paymentMethodRef: String,
        cardToken: String,
        encryptionKey: String,
        merchantAccount: String
    ): ForageApiResponse<String> = suspendCoroutine { continuation ->
        val btProxyKey = "N31FZgKpYZpo3oQ6XiM6M6"
        val btApiKey = "key_AZfcBuKUsV38PEeYu6ZV8x"
        val xKey = "8e4acfbc-943e-4e54-a3f2-e51ea7f39ca8"
        val pmRef = "f7e9641c67"
        val pmToken = "78e288ae-5cb8-4871-a348-c686cf0d29cf"
        val bt = BasisTheoryElements.builder()
            .apiKey(btApiKey)
            .build()

        val proxyRequest: ProxyRequest = ProxyRequest().apply {
            headers = mapOf(
                "X-KEY" to xKey,
                "Merchant-Account" to merchantAccount,
                "BT-PROXY-KEY" to btProxyKey,
                "BT-API-KEY" to btApiKey,
                "IDEMPOTENCY-KEY" to UUID.randomUUID().toString(),
                "Content-Type" to "application/json"
            )
            body = object {
                val pin = pinForageEditText.getTextElement()
                val card_number_token = pmToken
            }
            path = balancePath(pmRef)
        }

        runBlocking {
            val response = bt.proxy.post(proxyRequest)
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
        encryptionKey: String,
        merchantAccount: String
    ): ForageApiResponse<String> = suspendCoroutine { continuation ->
        val vgsCollect = buildVGSCollect(context)

        vgsCollect.bindView(pinForageEditText.getTextInputEditText())

        vgsCollect.addOnResponseListeners(object : VgsCollectResponseListener {
            override fun onResponse(response: VGSResponse?) {
                vgsCollect.onDestroy()

                when (response) {
                    is VGSResponse.SuccessResponse -> continuation.resumeWith(
                        Result.success(
                            ForageApiResponse.Success(response.body!!)
                        )
                    )
                    is VGSResponse.ErrorResponse -> continuation.resumeWith(
                        Result.success(
                            ForageApiResponse.Failure(listOf(ForageError(response.errorCode, "user_error", "Invalid Data")))
                        )
                    )
                    null -> continuation.resumeWith(
                        Result.success(
                            ForageApiResponse.Failure(listOf(ForageError(500, "unknown_server_error", "Unknown Server Error")))
                        )
                    )
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

        vgsCollect.asyncSubmit(request)
    }

    companion object {
        private const val VAULT_ID = BuildConfig.VAULT_ID
        private val VGS_ENVIRONMENT = BuildConfig.VGS_VAULT_TYPE

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
