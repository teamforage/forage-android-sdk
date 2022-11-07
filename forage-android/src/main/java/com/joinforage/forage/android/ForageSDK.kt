package com.joinforage.forage.android

import android.content.Context
import android.util.Log
import com.joinforage.forage.android.core.Logger
import com.joinforage.forage.android.model.PanEntry
import com.joinforage.forage.android.model.getPanNumber
import com.joinforage.forage.android.network.getEncryptionKey
import com.joinforage.forage.android.network.mapper.toResponse
import com.joinforage.forage.android.network.model.EncryptionKey
import com.joinforage.forage.android.network.model.Response
import com.joinforage.forage.android.network.model.ResponseListener
import com.joinforage.forage.android.network.tokenizeCard
import com.joinforage.forage.android.ui.ForagePINEditText
import com.verygoodsecurity.vgscollect.core.Environment
import com.verygoodsecurity.vgscollect.core.HTTPMethod
import com.verygoodsecurity.vgscollect.core.VGSCollect
import com.verygoodsecurity.vgscollect.core.VgsCollectResponseListener
import com.verygoodsecurity.vgscollect.core.model.network.VGSRequest
import com.verygoodsecurity.vgscollect.core.model.network.VGSResponse
import okhttp3.Call
import okhttp3.Callback
import java.io.IOException
import okhttp3.Response as OkHttpResponse

object ForageSDK : ForageSDKApi {
    private const val VAULT_ID = BuildConfig.VAULT_ID
    private val ENVIRONMENT = Environment.SANDBOX
    private val TAG = ForageSDK::class.simpleName

    private var panEntry: PanEntry = PanEntry.Invalid("")
    private val logger = Logger.getInstance(BuildConfig.DEBUG)

    override fun tokenizeEBTCard(
        merchantAccount: String,
        bearerToken: String,
        responseCallback: ResponseListener
    ) {
        val currentEntry = panEntry
        logger.info("Tokenize $currentEntry")

        when {
            shouldTokenize(currentEntry) -> tokenizeCard(
                merchantAccount,
                bearerToken,
                currentEntry.getPanNumber(),
                responseCallback
            )
            else -> responseCallback.onResponse(Response.ErrorResponse(localizeMessage = "Invalid PAN entry"))
        }
    }

    private fun shouldTokenize(panEntry: PanEntry): Boolean {
        return panEntry is PanEntry.Valid || BuildConfig.DEBUG
    }

    override fun checkBalance(
        context: Context,
        pinForageEditText: ForagePINEditText,
        merchantAccount: String,
        bearerToken: String,
        paymentMethodRef: String,
        cardToken: String,
        onResponseListener: ResponseListener
    ) {
        val vgsForm = VGSCollect.Builder(context, VAULT_ID)
            .setEnvironment(ENVIRONMENT)
            .create()

        vgsForm.bindView(pinForageEditText.getTextInputEditText())

        vgsForm.addOnResponseListeners(object : VgsCollectResponseListener {
            override fun onResponse(response: VGSResponse?) {
                onResponseListener.onResponse(response?.toResponse())
                vgsForm.onDestroy()
            }
        })

        getEncryptionKey(
            bearer = bearerToken,
            responseCallback = object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    TODO("Not yet implemented")
                }

                override fun onResponse(call: Call, response: OkHttpResponse) {
                    when {
                        response.isSuccessful -> {
                            val jsonData = response.body?.string()
                            Log.d(TAG, "Get Encryption Key Response:")
                            Log.d(TAG, "$jsonData")

                            val encryptionKey = EncryptionKey.ModelMapper.from(jsonData!!)

                            val request: VGSRequest = VGSRequest.VGSRequestBuilder()
                                .setMethod(HTTPMethod.POST)
                                .setPath(balancePath(paymentMethodRef))
                                .setCustomHeader(
                                    checkBalanceHeaders(
                                        merchantAccount,
                                        encryptionKey,
                                        paymentMethodRef
                                    )
                                )
                                .setCustomData(checkBalanceBody(cardToken))
                                .build()

                            vgsForm.asyncSubmit(request)
                        }
                        else -> {}
                    }
                }
            }
        )
    }

    override fun capturePayment(
        context: Context,
        pinForageEditText: ForagePINEditText,
        merchantAccount: String,
        bearerToken: String,
        paymentRef: String,
        cardToken: String,
        onResponseListener: ResponseListener
    ) {
        val vgsForm = VGSCollect.Builder(context, VAULT_ID)
            .setEnvironment(ENVIRONMENT)
            .create()

        vgsForm.bindView(pinForageEditText.getTextInputEditText())

        vgsForm.addOnResponseListeners(object : VgsCollectResponseListener {
            override fun onResponse(response: VGSResponse?) {
                onResponseListener.onResponse(response?.toResponse())
                vgsForm.onDestroy()
            }
        })

        getEncryptionKey(
            bearer = bearerToken,
            responseCallback = object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    TODO("Not yet implemented")
                }

                override fun onResponse(call: Call, response: OkHttpResponse) {
                    when {
                        response.isSuccessful -> {
                            val jsonData = response.body?.string()
                            Log.d(TAG, "Get Encryption Key Response:")
                            Log.d(TAG, "$jsonData")

                            val encryptionKey = EncryptionKey.ModelMapper.from(jsonData!!)

                            val request: VGSRequest = VGSRequest.VGSRequestBuilder()
                                .setMethod(HTTPMethod.POST)
                                .setPath(capturePath(paymentRef))
                                .setCustomHeader(
                                    checkBalanceHeaders(
                                        merchantAccount,
                                        encryptionKey,
                                        paymentRef
                                    )
                                )
                                .setCustomData(checkBalanceBody(cardToken))
                                .build()

                            vgsForm.asyncSubmit(request)
                        }
                        else -> {}
                    }
                }
            }
        )
    }

    private fun checkBalanceBody(cardToken: String): HashMap<String, String> {
        val body = HashMap<String, String>()
        body["card_number_token"] = cardToken
        return body
    }

    private fun checkBalanceHeaders(
        merchantAccount: String,
        encryptionKey: EncryptionKey,
        paymentMethodRef: String
    ): HashMap<String, String> {
        val headers = HashMap<String, String>()
        headers["X-KEY"] = encryptionKey.alias
        headers["Merchant-Account"] = merchantAccount
        headers["IDEMPOTENCY-KEY"] = paymentMethodRef
        return headers
    }

    private fun balancePath(paymentMethodRef: String) =
        "/api/payment_methods/$paymentMethodRef/balance/"

    private fun capturePath(paymentRef: String) =
        "/api/payments/$paymentRef/capture/"

    internal fun storeEntry(entry: PanEntry) {
        panEntry = entry
    }
}
