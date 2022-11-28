package com.joinforage.forage.android.network.data

import com.joinforage.forage.android.collect.PinCollector
import com.joinforage.forage.android.network.EncryptionKeyService
import com.joinforage.forage.android.network.model.EncryptionKey
import com.joinforage.forage.android.network.model.ForageApiResponse

internal class CapturePaymentRepository(
    private val pinCollector: PinCollector,
    private val encryptionKeyService: EncryptionKeyService
) {
    suspend fun capturePayment(paymentRef: String, cardToken: String): ForageApiResponse<String> {
        return when (val response = encryptionKeyService.getEncryptionKey()) {
            is ForageApiResponse.Success -> collectPinToCapturePayment(
                paymentRef = paymentRef,
                cardToken = cardToken,
                EncryptionKey.ModelMapper.from(response.data).alias
            )
            else -> response
        }
    }

    private suspend fun collectPinToCapturePayment(
        paymentRef: String,
        cardToken: String,
        encryptionKey: String
    ): ForageApiResponse<String> {
        return pinCollector.collectPinForCapturePayment(
            paymentRef = paymentRef,
            cardToken = cardToken,
            encryptionKey = encryptionKey
        )
    }
}
