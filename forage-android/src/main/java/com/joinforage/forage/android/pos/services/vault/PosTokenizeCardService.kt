package com.joinforage.forage.android.pos.services.vault

import com.joinforage.forage.android.core.services.forageapi.network.ForageApiResponse
import com.joinforage.forage.android.core.services.telemetry.Log
import com.joinforage.forage.android.core.services.vault.TokenizeCardService
import com.joinforage.forage.android.pos.services.forageapi.paymentmethod.PosPaymentMethodRequestBody
import okhttp3.OkHttpClient
import java.io.IOException

internal class PosTokenizeCardService(
    private val httpUrl: String,
    okHttpClient: OkHttpClient,
    private val logger: Log
) : TokenizeCardService(httpUrl, okHttpClient, logger) {

    suspend fun tokenizeMagSwipeCard(track2Data: String, reusable: Boolean = true): ForageApiResponse<String> = try {
        logger.i("[POS] POST request for Payment Method with Track 2 data")
        tokenizeCardCoroutine(
            PosPaymentMethodRequestBody(
                track2Data = track2Data,
                reusable = reusable
            )
        )
    } catch (ex: IOException) {
        logger.e("[POS] Failed while tokenizing PaymentMethod", ex)
        ForageApiResponse.Failure(500, "unknown_server_error", ex.message.orEmpty())
    }
}
