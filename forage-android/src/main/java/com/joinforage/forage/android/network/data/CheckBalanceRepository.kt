package com.joinforage.forage.android.network.data

import com.joinforage.forage.android.collect.PinCollector
import com.joinforage.forage.android.network.EncryptionKeyService
import com.joinforage.forage.android.network.model.EncryptionKey
import com.joinforage.forage.android.network.model.ForageApiResponse

internal class CheckBalanceRepository(
    private val pinCollector: PinCollector,
    private val encryptionKeyService: EncryptionKeyService
) {
    suspend fun checkBalance(
        paymentMethodRef: String,
        cardToken: String
    ): ForageApiResponse<String> {
        return when (val response = encryptionKeyService.getEncryptionKey()) {
            is ForageApiResponse.Success -> collectPinToCheckBalance(
                paymentMethodRef = paymentMethodRef,
                cardToken = cardToken,
                EncryptionKey.ModelMapper.from(response.data).alias
            )
            else -> response
        }
    }

    private suspend fun collectPinToCheckBalance(
        paymentMethodRef: String,
        cardToken: String,
        encryptionKey: String
    ): ForageApiResponse<String> {
        return pinCollector.collectPinForBalanceCheck(
            paymentMethodRef = paymentMethodRef,
            cardToken = cardToken,
            encryptionKey = encryptionKey
        )
    }
}
