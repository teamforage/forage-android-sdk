package com.joinforage.forage.android.network

import com.joinforage.forage.android.BuildConfig
import com.joinforage.forage.android.core.Logger
import com.joinforage.forage.android.network.ForageAPI.ENCRYPTION_KEY_URL
import com.joinforage.forage.android.network.ForageAPI.TOKENIZE_URL
import com.joinforage.forage.android.network.core.get
import com.joinforage.forage.android.network.core.post
import com.joinforage.forage.android.network.model.PaymentMethodRequestBody
import com.joinforage.forage.android.network.model.Response
import com.joinforage.forage.android.network.model.ResponseListener
import com.joinforage.forage.android.network.model.toJSONObject
import okhttp3.Call
import okhttp3.Callback
import java.io.IOException
import okhttp3.Response as OkHttpResponse

private val logger = Logger.getInstance(BuildConfig.DEBUG)

fun tokenizeCard(
    merchantAccount: String,
    bearer: String,
    inputField: String,
    responseCallback: ResponseListener
) {
    val requestBody = PaymentMethodRequestBody(cardNumber = inputField).toJSONObject()

    post(
        url = TOKENIZE_URL,
        json = requestBody.toString(),
        merchantAccount = merchantAccount,
        bearer = bearer,
        responseCallback = object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                responseCallback.onResponse(Response.ErrorResponse(rawResponse = e.message))
            }

            override fun onResponse(call: Call, response: OkHttpResponse) {
                when {
                    response.isSuccessful -> {
                        val jsonData = response.body?.string()

                        logger.info("Create Payment Method Response:")
                        logger.info("$jsonData")

                        responseCallback.onResponse(
                            Response.SuccessResponse(
                                successCode = 200,
                                rawResponse = jsonData
                            )
                        )
                    }
                    else -> {
                        val jsonData = response.body?.string()

                        logger.info("Create Payment Method Response:")
                        logger.info("$jsonData")

                        responseCallback.onResponse(
                            Response.ErrorResponse(
                                errorCode = response.code,
                                rawResponse = jsonData.orEmpty()
                            )
                        )
                    }
                }
            }
        }
    )
}

fun getEncryptionKey(
    bearer: String,
    responseCallback: Callback
) {
    get(
        url = ENCRYPTION_KEY_URL,
        bearer = bearer,
        responseCallback = responseCallback
    )
}
