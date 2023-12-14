package com.joinforage.forage.android.network.data

import com.joinforage.forage.android.collect.PinCollector
import com.joinforage.forage.android.core.telemetry.Log
import com.joinforage.forage.android.model.EncryptionKeys
import com.joinforage.forage.android.model.Payment
import com.joinforage.forage.android.model.PaymentMethod
import com.joinforage.forage.android.network.EncryptionKeyService
import com.joinforage.forage.android.network.MessageStatusService
import com.joinforage.forage.android.network.PaymentMethodService
import com.joinforage.forage.android.network.PaymentService
import com.joinforage.forage.android.network.model.ForageApiResponse

internal class CollectPinRepository(
    private val pinCollector: PinCollector,
    private val encryptionKeyService: EncryptionKeyService,
    private val paymentMethodService: PaymentMethodService,
    private val paymentService: PaymentService,
) {
    suspend fun collectPin(paymentRef: String): ForageApiResponse<String> {
        return when (val response = encryptionKeyService.getEncryptionKey()) {
            is ForageApiResponse.Success -> getPaymentMethodFromPayment(
                paymentRef = paymentRef,
                encryptionKey = pinCollector.parseEncryptionKey(
                    EncryptionKeys.ModelMapper.from(response.data)
                )
            )
            else -> response
        }
    }

    private suspend fun getPaymentMethodFromPayment(
        paymentRef: String,
        encryptionKey: String
    ): ForageApiResponse<String> {
        return when (val response = paymentService.getPayment(paymentRef)) {
            is ForageApiResponse.Success -> getTokenFromPaymentMethod(
                paymentRef = paymentRef,
                paymentMethodRef = Payment.ModelMapper.from(response.data).paymentMethod,
                encryptionKey = encryptionKey
            )
            else -> response
        }
    }

    private suspend fun getTokenFromPaymentMethod(
        paymentRef: String,
        paymentMethodRef: String,
        encryptionKey: String
    ): ForageApiResponse<String> {
        return when (val response = paymentMethodService.getPaymentMethod(paymentMethodRef)) {
            is ForageApiResponse.Success -> submitCollectPin(
                paymentRef = paymentRef,
                cardToken = pinCollector.parseVaultToken(PaymentMethod.ModelMapper.from(response.data)),
                encryptionKey = encryptionKey
            )
            else -> response
        }
    }

    private suspend fun submitCollectPin(
        paymentRef: String,
        cardToken: String,
        encryptionKey: String
    ): ForageApiResponse<String> {
        return pinCollector.collectPinForDeferredCapture(
            paymentRef = paymentRef,
            cardToken = cardToken,
            encryptionKey = encryptionKey
        )
    }

    companion object {
        private fun ForageApiResponse<String>.getStringResponse() = when (this) {
            is ForageApiResponse.Failure -> this.errors[0].message
            is ForageApiResponse.Success -> this.data
        }
    }
}
